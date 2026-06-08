package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class VisionAnalysisResult implements Serializable {

    private String imageId;

    private UiLayoutPlan layoutPlan;

    private String rawJson;

    private String imageDescription;

    private boolean fallback;

    private String summary;
}
