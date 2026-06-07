package com.robotlive.smartcodeless.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.build.BuildLogAppender;
import com.robotlive.smartcodeless.build.VueProjectBuildExecutor;
import com.robotlive.smartcodeless.build.dto.BuildTaskMessage;
import com.robotlive.smartcodeless.config.BuildRabbitConfig;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.mapper.BuildTaskMapper;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.BuildTask;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.BuildStatusEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskVO;
import com.robotlive.smartcodeless.sandbox.SandboxContainerService;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BuildTaskServiceImpl extends ServiceImpl<BuildTaskMapper, BuildTask> implements BuildTaskService {

    @Resource
    private AppService appService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private BuildLogAppender buildLogAppender;

    @Resource
    private VueProjectBuildExecutor vueProjectBuildExecutor;

    @Resource
    private SandboxContainerService sandboxContainerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildTaskSubmitVO submitBuildTask(Long appId, User loginUser, String triggerType) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        checkAppAuth(app, loginUser);

        String normalizedTriggerType = StrUtil.blankToDefault(triggerType, "DEPLOY");
        BuildTask buildTask = BuildTask.builder()
                .appId(appId)
                .userId(loginUser.getId())
                .triggerType(normalizedTriggerType)
                .status(BuildStatusEnum.QUEUED.getValue())
                .deployKey(app.getDeployKey())
                .queuedTime(LocalDateTime.now())
                .build();
        boolean saved = this.save(buildTask);
        ThrowUtils.throwIf(!saved || buildTask.getId() == null, ErrorCode.OPERATION_ERROR, "create build task failed");

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployStatus(BuildStatusEnum.QUEUED.getValue());
        updateApp.setBuildTaskId(buildTask.getId());
        updateApp.setBuildErrorMessage("");
        boolean updated = appService.updateById(updateApp);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "update app build status failed");

        BuildTaskMessage message = BuildTaskMessage.builder()
                .taskId(buildTask.getId())
                .appId(appId)
                .userId(loginUser.getId())
                .triggerType(normalizedTriggerType)
                .build();
        publishAfterCommit(message);
        return BuildTaskSubmitVO.builder()
                .taskId(buildTask.getId())
                .appId(appId)
                .status(BuildStatusEnum.QUEUED.getValue())
                .message("Build task queued")
                .build();
    }

    @Override
    public BuildTaskVO getBuildTaskVO(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "taskId is required");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        BuildTask buildTask = this.getById(taskId);
        ThrowUtils.throwIf(buildTask == null, ErrorCode.NOT_FOUND_ERROR, "build task not found");
        App app = appService.getById(buildTask.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        checkAppAuth(app, loginUser);
        String deployUrl = StrUtil.isBlank(buildTask.getDeployKey()) ? null : String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, buildTask.getDeployKey());
        return BuildTaskVO.objToVo(buildTask, deployUrl);
    }

    @Override
    public boolean executeBuildTask(Long taskId) {
        BuildTask task = this.getById(taskId);
        if (task == null) {
            log.warn("Build task not found: {}", taskId);
            return true;
        }
        RLock taskLock = redissonClient.getLock("lock:build:task:" + taskId);
        RLock appLock = redissonClient.getLock("lock:build:app:" + task.getAppId());
        boolean taskLocked = false;
        boolean appLocked = false;
        try {
            taskLocked = taskLock.tryLock(5, TimeUnit.SECONDS);
            if (!taskLocked) {
                log.warn("Skip build task because task lock is busy: {}", taskId);
                return true;
            }
            appLocked = appLock.tryLock(5, TimeUnit.SECONDS);
            if (!appLocked) {
                log.warn("Skip build task because app lock is busy: {}", task.getAppId());
                return true;
            }
            BuildTask latestTask = this.getById(taskId);
            if (latestTask == null || BuildStatusEnum.isTerminal(latestTask.getStatus())) {
                return true;
            }
            if (!BuildStatusEnum.QUEUED.getValue().equals(latestTask.getStatus())) {
                return true;
            }
            return doExecute(latestTask);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(task, "Build interrupted");
            return false;
        } catch (Exception e) {
            log.error("Execute build task failed: {}", taskId, e);
            markFailed(task, e.getMessage());
            return false;
        } finally {
            if (appLocked && appLock.isHeldByCurrentThread()) {
                appLock.unlock();
            }
            if (taskLocked && taskLock.isHeldByCurrentThread()) {
                taskLock.unlock();
            }
        }
    }

    private boolean doExecute(BuildTask task) {
        App app = appService.getById(task.getAppId());
        if (app == null) {
            markFailed(task, "App not found");
            return false;
        }
        markRunning(task);
        try {
            buildLogAppender.system(task.getId(), task.getAppId(), "Build task started");
            VueProjectBuildExecutor.BuildExecutionResult result = vueProjectBuildExecutor.execute(task.getId(), app);
            markSuccess(task, result);
            buildLogAppender.system(task.getId(), task.getAppId(), "Build task succeeded");
            startSandboxPreview(app, result, task.getId());
            appService.generateAppScreenshotAsync(app.getId(), result.getDeployUrl());
            return true;
        } catch (Exception e) {
            String message = StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
            buildLogAppender.error(task.getId(), task.getAppId(), message);
            markFailed(task, message);
            return false;
        }
    }

    private void markRunning(BuildTask task) {
        BuildTask updateTask = new BuildTask();
        updateTask.setId(task.getId());
        updateTask.setStatus(BuildStatusEnum.RUNNING.getValue());
        updateTask.setStartTime(LocalDateTime.now());
        this.updateById(updateTask);

        App updateApp = new App();
        updateApp.setId(task.getAppId());
        updateApp.setDeployStatus(BuildStatusEnum.RUNNING.getValue());
        appService.updateById(updateApp);
    }

    private void markSuccess(BuildTask task, VueProjectBuildExecutor.BuildExecutionResult result) {
        BuildTask updateTask = new BuildTask();
        updateTask.setId(task.getId());
        updateTask.setStatus(BuildStatusEnum.SUCCESS.getValue());
        updateTask.setSourceDir(result.getSourceDir());
        updateTask.setDistDir(result.getDistDir());
        updateTask.setDeployKey(result.getDeployKey());
        updateTask.setFinishTime(LocalDateTime.now());
        this.updateById(updateTask);

        App updateApp = new App();
        updateApp.setId(task.getAppId());
        updateApp.setDeployStatus(BuildStatusEnum.SUCCESS.getValue());
        updateApp.setDeployKey(result.getDeployKey());
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setBuildErrorMessage("");
        appService.updateById(updateApp);
    }

    private void markFailed(BuildTask task, String errorMessage) {
        String safeMessage = StrUtil.subPre(StrUtil.blankToDefault(errorMessage, "Build failed"), 1024);
        BuildTask updateTask = new BuildTask();
        updateTask.setId(task.getId());
        updateTask.setStatus(BuildStatusEnum.FAILED.getValue());
        updateTask.setErrorMessage(safeMessage);
        updateTask.setFinishTime(LocalDateTime.now());
        this.updateById(updateTask);

        App updateApp = new App();
        updateApp.setId(task.getAppId());
        updateApp.setDeployStatus(BuildStatusEnum.FAILED.getValue());
        updateApp.setBuildErrorMessage(safeMessage);
        appService.updateById(updateApp);
    }

    private void startSandboxPreview(App app, VueProjectBuildExecutor.BuildExecutionResult result, Long taskId) {
        try {
            sandboxContainerService.startForBuild(app, result, taskId);
        } catch (Exception e) {
            log.warn("Sandbox preview startup failed for app {}", app.getId(), e);
            buildLogAppender.error(taskId, app.getId(), "Sandbox preview unavailable, static preview fallback remains available");
        }
    }

    private void publishAfterCommit(BuildTaskMessage message) {
        Runnable publisher = () -> rabbitTemplate.convertAndSend(BuildRabbitConfig.BUILD_EXCHANGE, BuildRabbitConfig.BUILD_ROUTING_KEY, message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
        } else {
            publisher.run();
        }
    }

    private void checkAppAuth(App app, User loginUser) {
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission for this app");
        }
    }
}
