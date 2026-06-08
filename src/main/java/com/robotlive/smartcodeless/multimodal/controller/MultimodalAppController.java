package com.robotlive.smartcodeless.multimodal.controller;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.app.AppAddRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.CreateAppWithImageRequest;
import com.robotlive.smartcodeless.multimodal.dto.ReferenceImageUploadVO;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;
import com.robotlive.smartcodeless.multimodal.service.ReferenceImageService;
import com.robotlive.smartcodeless.multimodal.service.UiLayoutPlanService;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/app/multimodal")
public class MultimodalAppController {

    @Resource
    private ReferenceImageService referenceImageService;

    @Resource
    private UiLayoutPlanService uiLayoutPlanService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @PostMapping("/upload-image")
    public BaseResponse<ReferenceImageUploadVO> uploadImage(@RequestParam("file") MultipartFile file,
                                                            @RequestParam(required = false) Long appId,
                                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(referenceImageService.upload(file, appId, loginUser));
    }

    @GetMapping("/image/{imageId}")
    public ResponseEntity<org.springframework.core.io.Resource> readImage(@PathVariable String imageId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return referenceImageService.readImage(imageId, loginUser);
    }

    @PostMapping("/analyze")
    public BaseResponse<VisionAnalysisResult> analyze(@RequestParam String imageId,
                                                      @RequestParam(required = false) String prompt,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(uiLayoutPlanService.analyze(imageId, prompt, loginUser));
    }

    @PostMapping("/create")
    public BaseResponse<Long> create(@RequestBody CreateAppWithImageRequest createRequest,
                                     HttpServletRequest request) {
        ThrowUtils.throwIf(createRequest == null || StrUtil.isBlank(createRequest.getInitPrompt()),
                ErrorCode.PARAMS_ERROR, "初始提示词不能为空");
        User loginUser = userService.getLoginUser(request);
        AppAddRequest appAddRequest = new AppAddRequest();
        appAddRequest.setInitPrompt(createRequest.getInitPrompt());
        Long appId = appService.createApp(appAddRequest, loginUser);
        referenceImageService.bindToApp(createRequest.getImageId(), appId, loginUser);
        return ResultUtils.success(appId);
    }
}
