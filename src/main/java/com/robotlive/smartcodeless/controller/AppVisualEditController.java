package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.service.UserService;
import com.robotlive.smartcodeless.visual.VisualEditService;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyResponse;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/visual/edit")
public class AppVisualEditController {

    @Resource
    private VisualEditService visualEditService;

    @Resource
    private UserService userService;

    @PostMapping("/preview")
    public BaseResponse<VisualEditPreviewResponse> preview(@RequestBody VisualEditPreviewRequest previewRequest,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(visualEditService.preview(previewRequest, loginUser));
    }

    @PostMapping("/apply")
    public BaseResponse<VisualEditApplyResponse> apply(@RequestBody VisualEditApplyRequest applyRequest,
                                                       HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(visualEditService.apply(applyRequest, loginUser));
    }
}
