package com.robotlive.smartcodeless.visual.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisualEditPreviewResponse {

    private Long appId;

    private VisualPatchResult patch;
}
