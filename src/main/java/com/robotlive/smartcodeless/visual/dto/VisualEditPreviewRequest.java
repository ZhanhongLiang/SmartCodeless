package com.robotlive.smartcodeless.visual.dto;

import lombok.Data;

@Data
public class VisualEditPreviewRequest {

    private Long appId;

    private VisualElementSelection element;

    private VisualEditChangeSet changes;

    private String instruction;
}
