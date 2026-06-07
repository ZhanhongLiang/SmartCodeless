package com.robotlive.smartcodeless.quality;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.mapper.DependencyPolicyViolationMapper;
import com.robotlive.smartcodeless.mapper.QualityCheckTaskMapper;
import com.robotlive.smartcodeless.mapper.SelfHealingAttemptMapper;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.DependencyPolicyViolation;
import com.robotlive.smartcodeless.model.entity.QualityCheckTask;
import com.robotlive.smartcodeless.model.entity.SelfHealingAttempt;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.RepairAttemptStatusEnum;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.model.vo.quality.SelfHealingAttemptVO;
import com.robotlive.smartcodeless.service.AppVersionService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SelfHealingRepairServiceImpl implements SelfHealingRepairService {

    private static final int MAX_ATTEMPTS = 2;

    @Resource
    private SelfHealingAttemptMapper attemptMapper;

    @Resource
    private QualityCheckTaskMapper taskMapper;

    @Resource
    private DependencyPolicyViolationMapper violationMapper;

    @Resource
    private QualityAppResolver appResolver;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private BuildTaskService buildTaskService;

    @Resource
    private RedissonClient redissonClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SelfHealingAttemptVO createRepairAttempt(Long taskId, User loginUser) {
        QualityCheckTask task = getAuthorizedTask(taskId, loginUser);
        long existingAttempts = attemptMapper.selectCountByQuery(QueryWrapper.create().eq("taskId", taskId));
        ThrowUtils.throwIf(existingAttempts >= MAX_ATTEMPTS, ErrorCode.OPERATION_ERROR, "自愈修复次数已达上限");
        App app = appResolver.getAuthorizedApp(task.getAppId(), loginUser);
        Path appRoot = appResolver.resolveAppRoot(app);
        QueryWrapper violationQuery = QueryWrapper.create().eq("taskId", taskId).orderBy("createTime", true);
        List<DependencyPolicyViolation> violations = violationMapper.selectListByQuery(violationQuery);
        ThrowUtils.throwIf(violations.isEmpty(), ErrorCode.OPERATION_ERROR, "当前任务没有可自动修复的策略问题");
        SelfHealingAttempt attempt = buildPackageJsonRepairAttempt(task, appRoot, violations, (int) existingAttempts + 1);
        attemptMapper.insert(attempt);
        return SelfHealingAttemptVO.objToVo(attempt);
    }

    @Override
    public SelfHealingAttemptVO applyRepairAttempt(Long attemptId, User loginUser) {
        ThrowUtils.throwIf(attemptId == null || attemptId <= 0, ErrorCode.PARAMS_ERROR, "修复尝试 ID 不能为空");
        SelfHealingAttempt attempt = attemptMapper.selectOneById(attemptId);
        ThrowUtils.throwIf(attempt == null, ErrorCode.NOT_FOUND_ERROR, "修复尝试不存在");
        QualityCheckTask task = getAuthorizedTask(attempt.getTaskId(), loginUser);
        App app = appResolver.getAuthorizedApp(task.getAppId(), loginUser);
        RLock lock = redissonClient.getLock("lock:repair:app:" + app.getId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "该应用已有自愈修复正在执行");
            Path appRoot = appResolver.resolveAppRoot(app);
            Path target = appRoot.resolve(attempt.getPatchTargetFile()).normalize();
            if (!target.startsWith(appRoot) || !Files.isRegularFile(target)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "修复目标文件非法");
            }
            String content = Files.readString(target, StandardCharsets.UTF_8);
            if (!content.contains(attempt.getBeforeSnippet())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "修复补丁和当前文件不匹配，请重新生成修复建议");
            }
            Files.writeString(target, content.replace(attempt.getBeforeSnippet(), attempt.getAfterSnippet()), StandardCharsets.UTF_8);
            appVersionService.createAiGenerationVersion(app.getId(), loginUser,
                    "SELF_HEAL_FIX_" + attempt.getAttemptNo() + ": " + attempt.getFailureCategory() + " for quality task " + task.getId(),
                    null);
            BuildTaskSubmitVO buildTask = buildTaskService.submitBuildTask(app.getId(), loginUser, "SELF_HEAL");
            SelfHealingAttempt update = new SelfHealingAttempt();
            update.setId(attempt.getId());
            update.setStatus(RepairAttemptStatusEnum.APPLIED.name());
            update.setDiagnosis("补丁已应用，已提交自愈构建任务 #" + buildTask.getTaskId());
            update.setFinishTime(LocalDateTime.now());
            attemptMapper.update(update);
            attempt.setStatus(update.getStatus());
            attempt.setDiagnosis(update.getDiagnosis());
            attempt.setFinishTime(update.getFinishTime());
            return SelfHealingAttemptVO.objToVo(attempt);
        } catch (BusinessException e) {
            markFailed(attempt, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(attempt, e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用自愈补丁失败：" + e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<SelfHealingAttemptVO> listAttempts(Long taskId, User loginUser) {
        getAuthorizedTask(taskId, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create().eq("taskId", taskId).orderBy("attemptNo", true);
        return attemptMapper.selectListByQuery(queryWrapper).stream().map(SelfHealingAttemptVO::objToVo).toList();
    }

    private SelfHealingAttempt buildPackageJsonRepairAttempt(QualityCheckTask task,
                                                             Path appRoot,
                                                             List<DependencyPolicyViolation> violations,
                                                             int attemptNo) {
        try {
            Path packageJson = appRoot.resolve("package.json").normalize();
            String before = Files.readString(packageJson, StandardCharsets.UTF_8);
            Map<String, Object> json = objectMapper.readValue(before, new TypeReference<>() {
            });
            removeRiskyDependencies(json, violations);
            removeRiskyScripts(json, violations);
            String after = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json) + System.lineSeparator();
            if (before.equals(after)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有可生成的确定性修复补丁");
            }
            return SelfHealingAttempt.builder()
                    .taskId(task.getId())
                    .appId(task.getAppId())
                    .userId(task.getUserId())
                    .attemptNo(attemptNo)
                    .status(RepairAttemptStatusEnum.GENERATED.name())
                    .failureCategory(task.getFailureCategory())
                    .patchTargetFile("package.json")
                    .beforeSnippet(before)
                    .afterSnippet(after)
                    .unifiedDiff("--- a/package.json\n+++ b/package.json\n@@ self-healing policy repair @@\n- " + compact(before) + "\n+ " + compact(after))
                    .diagnosis("根据依赖和 npm script 策略违规生成 package.json 修复补丁")
                    .createTime(LocalDateTime.now())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成自愈修复补丁失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void removeRiskyDependencies(Map<String, Object> json, List<DependencyPolicyViolation> violations) {
        for (String section : List.of("dependencies", "devDependencies")) {
            Object depsObject = json.get(section);
            if (!(depsObject instanceof Map<?, ?> deps)) {
                continue;
            }
            Map<String, Object> mutableDeps = (Map<String, Object>) deps;
            violations.stream()
                    .filter(v -> "UNKNOWN_DEPENDENCY".equals(v.getViolationType()) || "SUSPICIOUS_VERSION".equals(v.getViolationType()))
                    .map(DependencyPolicyViolation::getPackageName)
                    .forEach(mutableDeps::remove);
        }
    }

    @SuppressWarnings("unchecked")
    private void removeRiskyScripts(Map<String, Object> json, List<DependencyPolicyViolation> violations) {
        Object scriptsObject = json.get("scripts");
        if (!(scriptsObject instanceof Map<?, ?> scripts)) {
            return;
        }
        Map<String, Object> mutableScripts = (Map<String, Object>) scripts;
        violations.stream()
                .filter(v -> "DANGEROUS_NPM_SCRIPT".equals(v.getViolationType()))
                .map(DependencyPolicyViolation::getPackageName)
                .forEach(mutableScripts::remove);
    }

    private String compact(String value) {
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() > 1200 ? compacted.substring(0, 1200) : compacted;
    }

    private QualityCheckTask getAuthorizedTask(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        QualityCheckTask task = taskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "质量检查任务不存在");
        appResolver.getAuthorizedApp(task.getAppId(), loginUser);
        return task;
    }

    private void markFailed(SelfHealingAttempt attempt, String message) {
        SelfHealingAttempt update = new SelfHealingAttempt();
        update.setId(attempt.getId());
        update.setStatus(RepairAttemptStatusEnum.FAILED.name());
        update.setErrorMessage(message);
        update.setFinishTime(LocalDateTime.now());
        attemptMapper.update(update);
    }
}
