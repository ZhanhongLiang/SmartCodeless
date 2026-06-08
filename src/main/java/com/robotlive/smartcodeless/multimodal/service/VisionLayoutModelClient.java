package com.robotlive.smartcodeless.multimodal.service;

import com.robotlive.smartcodeless.model.entity.ReferenceImage;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;

public interface VisionLayoutModelClient {

    VisionAnalysisResult analyze(ReferenceImage image, String userPrompt);
}
