package com.robotlive.smartcodeless.multimodal.service;

import com.robotlive.smartcodeless.model.entity.ReferenceImage;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UiLayoutPlanServiceImpl implements UiLayoutPlanService {

    @Resource
    private ReferenceImageService referenceImageService;

    @Resource
    private VisionLayoutModelClient visionLayoutModelClient;

    @Override
    public VisionAnalysisResult analyze(String imageId, String userPrompt, User loginUser) {
        ReferenceImage image = referenceImageService.getAuthorizedImage(imageId, loginUser);
        return visionLayoutModelClient.analyze(image, userPrompt);
    }
}
