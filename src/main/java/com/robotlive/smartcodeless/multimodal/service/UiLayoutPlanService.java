package com.robotlive.smartcodeless.multimodal.service;

import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;

public interface UiLayoutPlanService {

    VisionAnalysisResult analyze(String imageId, String userPrompt, User loginUser);
}
