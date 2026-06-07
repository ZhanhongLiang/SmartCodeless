package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.model.dto.quality.QualityCheckRequest;
import com.robotlive.smartcodeless.model.dto.quality.QualityRepairApplyRequest;
import com.robotlive.smartcodeless.model.dto.quality.QualityRepairRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.quality.DependencyPolicyViolationVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckReportVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckTaskVO;
import com.robotlive.smartcodeless.model.vo.quality.SelfHealingAttemptVO;
import com.robotlive.smartcodeless.quality.QualityGateService;
import com.robotlive.smartcodeless.quality.SelfHealingRepairService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app/quality")
public class AppQualityController {

    @Resource
    private QualityGateService qualityGateService;

    @Resource
    private SelfHealingRepairService selfHealingRepairService;

    @Resource
    private UserService userService;

    @PostMapping("/check")
    public BaseResponse<QualityCheckTaskVO> check(@RequestBody QualityCheckRequest checkRequest,
                                                  HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(qualityGateService.submitQualityCheck(checkRequest, loginUser));
    }

    @GetMapping("/task/{taskId}")
    public BaseResponse<QualityCheckTaskVO> getTask(@PathVariable Long taskId,
                                                    HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(qualityGateService.getTask(taskId, loginUser));
    }

    @GetMapping("/report/{appId}")
    public BaseResponse<List<QualityCheckReportVO>> listReports(@PathVariable Long appId,
                                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(qualityGateService.listReports(appId, loginUser));
    }

    @GetMapping("/report/{appId}/latest")
    public BaseResponse<QualityCheckReportVO> latestReport(@PathVariable Long appId,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(qualityGateService.latestReport(appId, loginUser));
    }

    @GetMapping("/violations")
    public BaseResponse<List<DependencyPolicyViolationVO>> listViolations(@RequestParam Long taskId,
                                                                          HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(qualityGateService.listViolations(taskId, loginUser));
    }

    @PostMapping("/repair")
    public BaseResponse<SelfHealingAttemptVO> repair(@RequestBody QualityRepairRequest repairRequest,
                                                     HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(selfHealingRepairService.createRepairAttempt(repairRequest.getTaskId(), loginUser));
    }

    @PostMapping("/repair/apply")
    public BaseResponse<SelfHealingAttemptVO> applyRepair(@RequestBody QualityRepairApplyRequest applyRequest,
                                                          HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(selfHealingRepairService.applyRepairAttempt(applyRequest.getAttemptId(), loginUser));
    }

    @GetMapping("/repair/attempts/{taskId}")
    public BaseResponse<List<SelfHealingAttemptVO>> listAttempts(@PathVariable Long taskId,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(selfHealingRepairService.listAttempts(taskId, loginUser));
    }
}
