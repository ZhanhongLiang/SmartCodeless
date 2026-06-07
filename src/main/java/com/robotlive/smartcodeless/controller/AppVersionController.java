package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.version.AppVersionRollbackRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.AppRollbackVO;
import com.robotlive.smartcodeless.model.vo.AppVersionVO;
import com.robotlive.smartcodeless.service.AppVersionService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app/version")
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    public BaseResponse<List<AppVersionVO>> listAppVersions(Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appVersionService.listAppVersions(appId, loginUser));
    }

    @GetMapping("/detail/{versionId}")
    public BaseResponse<AppVersionVO> getAppVersion(@PathVariable Long versionId, HttpServletRequest request) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "versionId is required");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appVersionService.getAppVersionVO(versionId, loginUser));
    }

    @PostMapping("/rollback")
    public BaseResponse<AppRollbackVO> rollback(@RequestBody AppVersionRollbackRequest rollbackRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(rollbackRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        AppRollbackVO result = appVersionService.rollback(
                rollbackRequest.getAppId(),
                rollbackRequest.getCommitId(),
                rollbackRequest.getReason(),
                loginUser);
        return ResultUtils.success(result);
    }
}

