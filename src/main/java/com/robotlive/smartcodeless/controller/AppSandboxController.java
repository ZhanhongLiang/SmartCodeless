package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.SandboxStartRequest;
import com.robotlive.smartcodeless.model.dto.SandboxStatusResponse;
import com.robotlive.smartcodeless.model.dto.SandboxStopRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.sandbox.SandboxContainerService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/sandbox")
public class AppSandboxController {

    @Resource
    private SandboxContainerService sandboxContainerService;

    @Resource
    private UserService userService;

    @PostMapping("/start")
    public BaseResponse<SandboxStatusResponse> start(@RequestBody SandboxStartRequest startRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(startRequest == null || startRequest.getAppId() == null, ErrorCode.PARAMS_ERROR, "appId is required");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(sandboxContainerService.startAppPreview(startRequest.getAppId(), loginUser));
    }

    @GetMapping("/status")
    public BaseResponse<SandboxStatusResponse> status(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(sandboxContainerService.getStatus(appId, loginUser));
    }

    @PostMapping("/stop")
    public BaseResponse<SandboxStatusResponse> stop(@RequestBody SandboxStopRequest stopRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(stopRequest == null || stopRequest.getAppId() == null, ErrorCode.PARAMS_ERROR, "appId is required");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(sandboxContainerService.stopAppPreview(stopRequest.getAppId(), loginUser));
    }
}
