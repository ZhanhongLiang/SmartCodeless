package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.build.BuildTaskSubmitRequest;
import com.robotlive.smartcodeless.model.entity.BuildLog;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.BuildLogVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskVO;
import com.robotlive.smartcodeless.service.BuildLogService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/build")
public class AppBuildController {

    @Resource
    private BuildTaskService buildTaskService;

    @Resource
    private BuildLogService buildLogService;

    @Resource
    private UserService userService;

    @PostMapping("/submit")
    public BaseResponse<BuildTaskSubmitVO> submitBuildTask(@RequestBody BuildTaskSubmitRequest buildTaskSubmitRequest,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(buildTaskSubmitRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        BuildTaskSubmitVO result = buildTaskService.submitBuildTask(
                buildTaskSubmitRequest.getAppId(),
                loginUser,
                buildTaskSubmitRequest.getTriggerType()
        );
        return ResultUtils.success(result);
    }

    @GetMapping("/task/{taskId}")
    public BaseResponse<BuildTaskVO> getBuildTask(@PathVariable Long taskId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(buildTaskService.getBuildTaskVO(taskId, loginUser));
    }

    @GetMapping("/logs")
    public BaseResponse<List<BuildLogVO>> listBuildLogs(@RequestParam Long taskId,
                                                        @RequestParam(required = false) Long lastId,
                                                        @RequestParam(defaultValue = "100") int pageSize) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "taskId is required");
        List<BuildLog> logs = buildLogService.listLogs(taskId, lastId, pageSize);
        List<BuildLogVO> logVOList = logs.stream().map(BuildLogVO::objToVo).toList();
        return ResultUtils.success(logVOList);
    }
}
