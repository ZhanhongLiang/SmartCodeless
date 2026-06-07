package com.robotlive.smartcodeless.visual.dto;

import lombok.Data;

@Data
public class VisualEditApplyRequest {

    private Long appId;

    private VisualPatchResult patch;

    private String summary;
}
