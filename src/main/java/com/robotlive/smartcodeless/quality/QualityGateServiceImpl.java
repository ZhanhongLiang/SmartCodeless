package com.robotlive.smartcodeless.quality;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.mapper.DependencyPolicyViolationMapper;
import com.robotlive.smartcodeless.mapper.QualityCheckReportMapper;
import com.robotlive.smartcodeless.mapper.QualityCheckTaskMapper;
import com.robotlive.smartcodeless.model.dto.quality.QualityCheckRequest;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.BuildLog;
import com.robotlive.smartcodeless.model.entity.BuildTask;
import com.robotlive.smartcodeless.model.entity.DependencyPolicyViolation;
import com.robotlive.smartcodeless.model.entity.QualityCheckReport;
import com.robotlive.smartcodeless.model.entity.QualityCheckTask;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.BuildStatusEnum;
import com.robotlive.smartcodeless.model.enums.QualityCheckStatusEnum;
import com.robotlive.smartcodeless.model.enums.QualityFailureCategoryEnum;
import com.robotlive.smartcodeless.model.enums.QualityStageEnum;
import com.robotlive.smartcodeless.model.enums.QualityTriggerTypeEnum;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.model.vo.quality.DependencyPolicyViolationVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckReportVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckTaskVO;
import com.robotlive.smartcodeless.service.BuildLogService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QualityGateServiceImpl implements QualityGateService {

    @Resource
    private QualityCheckTaskMapper taskMapper;

    @Resource
    private QualityCheckReportMapper reportMapper;

    @Resource
    private DependencyPolicyViolationMapper violationMapper;

    @Resource
    private QualityAppResolver appResolver;

    @Resource
    private DependencyPolicyChecker dependencyPolicyChecker;

    @Resource
    private NpmScriptPolicyChecker npmScriptPolicyChecker;

    @Resource
    private BuildTaskService buildTaskService;

    @Resource
    private BuildLogService buildLogService;

    @Resource
    private BuildLogErrorClassifier buildLogErrorClassifier;

    @Resource
    private PreviewSmokeTestService previewSmokeTestService;

    @Resource
    private QualityScoreCalculator qualityScoreCalculator;

    @Resource
    private SelfHealingRepairService selfHealingRepairService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    @Qualifier("agentExecutor")
    private ExecutorService agentExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QualityCheckTaskVO submitQualityCheck(QualityCheckRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        App app = appResolver.getAuthorizedApp(request.getAppId(), loginUser);
        appResolver.resolveAppRoot(app);
        QualityCheckTask task = QualityCheckTask.builder()
                .appId(app.getId())
                .userId(loginUser.getId())
                .triggerType(StrUtil.blankToDefault(request.getTriggerType(), QualityTriggerTypeEnum.MANUAL.name()))
                .status(QualityCheckStatusEnum.QUEUED.getValue())
                .currentStage(QualityStageEnum.POLICY_CHECKING.getValue())
                .createTime(LocalDateTime.now())
                .build();
        taskMapper.insert(task);
        agentExecutor.submit(() -> runQualityTask(task.getId(), loginUser, Boolean.TRUE.equals(request.getAutoRepair())));
        return QualityCheckTaskVO.objToVo(task);
    }

    @Override
    public QualityCheckTaskVO getTask(Long taskId, User loginUser) {
        QualityCheckTask task = getAuthorizedTask(taskId, loginUser);
        return QualityCheckTaskVO.objToVo(task);
    }

    @Override
    public List<QualityCheckReportVO> listReports(Long appId, User loginUser) {
        appResolver.getAuthorizedApp(appId, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false);
        return reportMapper.selectListByQuery(queryWrapper).stream().map(QualityCheckReportVO::objToVo).toList();
    }

    @Override
    public QualityCheckReportVO latestReport(Long appId, User loginUser) {
        appResolver.getAuthorizedApp(appId, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false)
                .limit(1);
        return QualityCheckReportVO.objToVo(reportMapper.selectOneByQuery(queryWrapper));
    }

    @Override
    public List<DependencyPolicyViolationVO> listViolations(Long taskId, User loginUser) {
        QualityCheckTask task = getAuthorizedTask(taskId, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create().eq("taskId", task.getId()).orderBy("createTime", true);
        return violationMapper.selectListByQuery(queryWrapper).stream().map(DependencyPolicyViolationVO::objToVo).toList();
    }

    private void runQualityTask(Long taskId, User loginUser, boolean autoRepair) {
        QualityCheckTask task = taskMapper.selectOneById(taskId);
        if (task == null) {
            return;
        }
        RLock lock = redissonClient.getLock("lock:quality:app:" + task.getAppId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                markFailed(task, QualityFailureCategoryEnum.UNKNOWN_ERROR.name(), "该应用已有质量检查正在运行");
                return;
            }
            App app = appResolver.getAuthorizedApp(task.getAppId(), loginUser);
            Path appRoot = appResolver.resolveAppRoot(app);
            markStage(task, QualityCheckStatusEnum.RUNNING.getValue(), QualityStageEnum.POLICY_CHECKING.getValue(), null, null, null);
            QualityCheckResult dependencyResult = dependencyPolicyChecker.check(taskId, app.getId(), appRoot);
            QualityCheckResult scriptResult = npmScriptPolicyChecker.check(taskId, app.getId(), appRoot);
            boolean dependencyPass = "PASS".equals(dependencyResult.getStatus());
            boolean scriptPass = "PASS".equals(scriptResult.getStatus());
            if (!dependencyPass || !scriptPass) {
                String category = !dependencyPass ? dependencyResult.getFailureCategory() : scriptResult.getFailureCategory();
                finishWithReport(task, app, dependencyResult, scriptResult, null, null, false, false, category, autoRepair, loginUser);
                return;
            }
            markStage(task, QualityCheckStatusEnum.RUNNING.getValue(), QualityStageEnum.BUILD_CHECKING.getValue(), null, null, null);
            BuildTaskSubmitVO buildTask = buildTaskService.submitBuildTask(app.getId(), loginUser, "QUALITY_GATE");
            markStage(task, QualityCheckStatusEnum.RUNNING.getValue(), QualityStageEnum.BUILD_CHECKING.getValue(), buildTask.getTaskId(), null, null);
            BuildTask finishedBuild = waitForBuild(buildTask.getTaskId());
            boolean buildPass = finishedBuild != null && BuildStatusEnum.SUCCESS.getValue().equals(finishedBuild.getStatus());
            String logs = collectBuildLogs(buildTask.getTaskId());
            String failureCategory = buildPass ? null : buildLogErrorClassifier.classify(logs);
            QualityCheckResult previewResult = QualityCheckResult.builder().status("SKIP").message("构建未通过，跳过预览冒烟检查").build();
            boolean previewPass = false;
            if (buildPass) {
                markStage(task, QualityCheckStatusEnum.RUNNING.getValue(), QualityStageEnum.PREVIEW_CHECKING.getValue(), buildTask.getTaskId(), null, null);
                App refreshedApp = appResolver.getAuthorizedApp(app.getId(), loginUser);
                previewResult = previewSmokeTestService.check(refreshedApp, loginUser);
                previewPass = "PASS".equals(previewResult.getStatus());
                if (!previewPass) {
                    failureCategory = previewResult.getFailureCategory();
                }
            }
            finishWithReport(task, app, dependencyResult, scriptResult, finishedBuild, previewResult, buildPass, previewPass, failureCategory, autoRepair, loginUser);
        } catch (Exception e) {
            log.error("Run quality task failed, taskId={}", taskId, e);
            markFailed(task, QualityFailureCategoryEnum.UNKNOWN_ERROR.name(), e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void finishWithReport(QualityCheckTask task,
                                  App app,
                                  QualityCheckResult dependencyResult,
                                  QualityCheckResult scriptResult,
                                  BuildTask buildTask,
                                  QualityCheckResult previewResult,
                                  boolean buildPass,
                                  boolean previewPass,
                                  String failureCategory,
                                  boolean autoRepair,
                                  User loginUser) throws Exception {
        markStage(task, QualityCheckStatusEnum.RUNNING.getValue(), QualityStageEnum.REPORTING.getValue(), buildTask == null ? task.getBuildTaskId() : buildTask.getId(), failureCategory, null);
        boolean dependencyPass = "PASS".equals(dependencyResult.getStatus());
        boolean scriptPass = "PASS".equals(scriptResult.getStatus());
        boolean repairPass = failureCategory == null;
        int score = qualityScoreCalculator.calculate(dependencyPass, scriptPass, buildPass, previewPass, repairPass);
        String sanitizedLogs = buildTask == null ? "" : buildLogErrorClassifier.sanitize(collectBuildLogs(buildTask.getId()));
        Map<String, Object> summary = new HashMap<>();
        summary.put("dependencyPolicy", dependencyResult.getMessage());
        summary.put("scriptPolicy", scriptResult.getMessage());
        summary.put("buildStatus", buildTask == null ? "未执行" : buildTask.getStatus());
        summary.put("previewSmoke", previewResult == null ? "未执行" : previewResult.getMessage());
        summary.put("failureCategory", failureCategory);
        QualityCheckReport report = QualityCheckReport.builder()
                .taskId(task.getId())
                .appId(app.getId())
                .userId(loginUser.getId())
                .dependencyPolicyStatus(dependencyResult.getStatus())
                .scriptPolicyStatus(scriptResult.getStatus())
                .buildStatus(buildTask == null ? "SKIP" : buildTask.getStatus())
                .previewSmokeStatus(previewResult == null ? "SKIP" : previewResult.getStatus())
                .repairStatus(autoRepair ? "PENDING" : "SKIP")
                .failureCategory(failureCategory)
                .score(score)
                .summaryJson(objectMapper.writeValueAsString(summary))
                .sanitizedLogSummary(sanitizedLogs)
                .createTime(LocalDateTime.now())
                .build();
        reportMapper.insert(report);
        String finalStatus = failureCategory == null ? QualityCheckStatusEnum.SUCCESS.getValue() : QualityCheckStatusEnum.REPAIRABLE.getValue();
        markStage(task, finalStatus, QualityStageEnum.REPORTING.getValue(), buildTask == null ? task.getBuildTaskId() : buildTask.getId(), failureCategory, score);
        if (failureCategory != null && autoRepair) {
            selfHealingRepairService.createRepairAttempt(task.getId(), loginUser);
        }
    }

    private BuildTask waitForBuild(Long buildTaskId) throws InterruptedException {
        if (buildTaskId == null) {
            return null;
        }
        for (int i = 0; i < 60; i++) {
            BuildTask buildTask = buildTaskService.getById(buildTaskId);
            if (buildTask != null && BuildStatusEnum.isTerminal(buildTask.getStatus())) {
                return buildTask;
            }
            Thread.sleep(2000);
        }
        return buildTaskService.getById(buildTaskId);
    }

    private String collectBuildLogs(Long buildTaskId) {
        if (buildTaskId == null) {
            return "";
        }
        List<BuildLog> logs = buildLogService.listLogs(buildTaskId, null, 300);
        StringBuilder builder = new StringBuilder();
        logs.forEach(log -> builder.append(log.getLogType()).append(": ").append(log.getContent()).append('\n'));
        return builder.toString();
    }

    private void markStage(QualityCheckTask task, String status, String stage, Long buildTaskId, String failureCategory, Integer score) {
        QualityCheckTask update = new QualityCheckTask();
        update.setId(task.getId());
        update.setStatus(status);
        update.setCurrentStage(stage);
        update.setBuildTaskId(buildTaskId);
        update.setFailureCategory(failureCategory);
        update.setScore(score);
        update.setUpdateTime(LocalDateTime.now());
        if (QualityCheckStatusEnum.isTerminal(status)) {
            update.setFinishTime(LocalDateTime.now());
        }
        taskMapper.update(update);
        task.setStatus(status);
        task.setCurrentStage(stage);
        task.setBuildTaskId(buildTaskId);
        task.setFailureCategory(failureCategory);
        task.setScore(score);
    }

    private void markFailed(QualityCheckTask task, String category, String message) {
        QualityCheckTask update = new QualityCheckTask();
        update.setId(task.getId());
        update.setStatus(QualityCheckStatusEnum.FAILED.getValue());
        update.setFailureCategory(category);
        update.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(message, "质量检查失败"), 1024));
        update.setFinishTime(LocalDateTime.now());
        taskMapper.update(update);
    }

    private QualityCheckTask getAuthorizedTask(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        QualityCheckTask task = taskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "质量检查任务不存在");
        appResolver.getAuthorizedApp(task.getAppId(), loginUser);
        return task;
    }
}
