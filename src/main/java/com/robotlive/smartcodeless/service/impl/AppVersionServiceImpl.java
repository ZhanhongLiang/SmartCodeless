package com.robotlive.smartcodeless.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.git.LocalGitVersionManager;
import com.robotlive.smartcodeless.mapper.AppVersionMapper;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.AppVersion;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.AppVersionTypeEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.model.vo.AppRollbackVO;
import com.robotlive.smartcodeless.model.vo.AppVersionVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.AppVersionService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Resource
    private AppService appService;

    @Resource
    private BuildTaskService buildTaskService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private LocalGitVersionManager localGitVersionManager;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Optional<AppVersionVO> createAiGenerationVersion(Long appId, User loginUser, String prompt, AgentStreamEmitter emitter) {
        try {
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
            ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
            checkAppAuth(app, loginUser);
            return withGitLock(appId, () -> {
                if (emitter != null) {
                    emitter.status("versioning", "Creating Git version snapshot");
                }
                int roundNo = nextRoundNo(appId);
                String promptSummary = summarize(prompt, 1024);
                String commitMessage = summarize("AI_Round_" + roundNo + ": " + promptSummary, 512);
                Optional<String> commitIdOpt = localGitVersionManager.commitVersion(app, roundNo, commitMessage);
                if (commitIdOpt.isEmpty()) {
                    return Optional.empty();
                }
                String commitId = commitIdOpt.get();
                AppVersion appVersion = AppVersion.builder()
                        .appId(appId)
                        .userId(loginUser.getId())
                        .roundNo(roundNo)
                        .commitId(commitId)
                        .commitMessage(commitMessage)
                        .promptSummary(promptSummary)
                        .codeGenType(app.getCodeGenType())
                        .versionType(AppVersionTypeEnum.AI_GENERATION.getValue())
                        .createTime(LocalDateTime.now())
                        .build();
                boolean saved = this.save(appVersion);
                ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "save app version failed");
                chatHistoryService.attachVersionToRecentMessages(appId, loginUser.getId(), roundNo, commitId);
                if (emitter != null) {
                    emitter.status("versioned", "Git commit completed: " + StrUtil.subPre(commitId, 8));
                }
                return Optional.of(AppVersionVO.objToVo(appVersion));
            });
        } catch (Exception e) {
            log.error("Create app version failed, appId={}", appId, e);
            if (emitter != null) {
                emitter.status("version-failed", "Git version snapshot failed");
            }
            return Optional.empty();
        }
    }

    @Override
    public List<AppVersionVO> listAppVersions(Long appId, User loginUser) {
        App app = getAuthorizedApp(appId, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", app.getId())
                .orderBy("roundNo", false)
                .orderBy("createTime", false);
        return this.list(queryWrapper).stream()
                .map(AppVersionVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public AppVersionVO getAppVersionVO(Long versionId, User loginUser) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "versionId is required");
        AppVersion appVersion = this.getById(versionId);
        ThrowUtils.throwIf(appVersion == null, ErrorCode.NOT_FOUND_ERROR, "version not found");
        getAuthorizedApp(appVersion.getAppId(), loginUser);
        return AppVersionVO.objToVo(appVersion);
    }

    @Override
    public AppRollbackVO rollback(Long appId, String commitId, String reason, User loginUser) {
        App app = getAuthorizedApp(appId, loginUser);
        localGitVersionManager.validateCommitId(commitId);
        AppVersion targetVersion = getTargetVersion(appId, commitId);
        return withGitLock(appId, () -> {
            String currentCommitId = null;
            try {
                currentCommitId = localGitVersionManager.currentCommitId(app);
            } catch (Exception e) {
                log.warn("Get current commit before rollback failed, appId={}: {}", appId, e.getMessage());
            }
            localGitVersionManager.rollback(app, targetVersion.getCommitId());
            int roundNo = nextRoundNo(appId);
            String promptSummary = summarize(StrUtil.blankToDefault(reason, "Rollback to " + StrUtil.subPre(commitId, 8)), 1024);
            AppVersion rollbackVersion = AppVersion.builder()
                    .appId(appId)
                    .userId(loginUser.getId())
                    .roundNo(roundNo)
                    .commitId(targetVersion.getCommitId())
                    .commitMessage(summarize("ROLLBACK_Round_" + roundNo + ": " + promptSummary, 512))
                    .promptSummary(promptSummary)
                    .codeGenType(app.getCodeGenType())
                    .versionType(AppVersionTypeEnum.ROLLBACK.getValue())
                    .rollbackFromCommitId(currentCommitId)
                    .createTime(LocalDateTime.now())
                    .build();
            boolean saved = this.save(rollbackVersion);
            ThrowUtils.throwIf(!saved || rollbackVersion.getId() == null, ErrorCode.OPERATION_ERROR, "save rollback version failed");
            BuildTaskSubmitVO buildTask = buildTaskService.submitBuildTask(appId, loginUser, "ROLLBACK");
            AppVersion update = new AppVersion();
            update.setId(rollbackVersion.getId());
            update.setBuildTaskId(buildTask.getTaskId());
            this.updateById(update);
            return AppRollbackVO.builder()
                    .appId(appId)
                    .targetCommitId(targetVersion.getCommitId())
                    .rollbackVersionId(rollbackVersion.getId())
                    .buildTaskId(buildTask.getTaskId())
                    .status(buildTask.getStatus())
                    .message("Rollback completed and build task queued")
                    .build();
        });
    }

    private AppVersion getTargetVersion(Long appId, String commitId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("commitId", commitId)
                .limit(1);
        AppVersion targetVersion = this.getOne(queryWrapper);
        ThrowUtils.throwIf(targetVersion == null, ErrorCode.NOT_FOUND_ERROR, "commit does not belong to this app");
        return targetVersion;
    }

    private int nextRoundNo(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("roundNo", false)
                .limit(1);
        AppVersion latest = this.getOne(queryWrapper);
        return latest == null || latest.getRoundNo() == null ? 1 : latest.getRoundNo() + 1;
    }

    private App getAuthorizedApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        checkAppAuth(app, loginUser);
        return app;
    }

    private void checkAppAuth(App app, User loginUser) {
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission for this app");
        }
    }

    private String summarize(String text, int maxLength) {
        return StrUtil.subPre(StrUtil.blankToDefault(text, "AI generation"), maxLength);
    }

    private <T> T withGitLock(Long appId, GitOperation<T> operation) {
        RLock lock = redissonClient.getLock("lock:git:app:" + appId);
        boolean locked = false;
        try {
            locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "git operation is busy");
            }
            return operation.execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git operation interrupted");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @FunctionalInterface
    private interface GitOperation<T> {
        T execute();
    }
}

