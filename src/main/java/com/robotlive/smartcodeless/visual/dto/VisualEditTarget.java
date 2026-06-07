package com.robotlive.smartcodeless.visual.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisualEditTarget {

    private String filePath;

    private Integer startLine;

    private Integer endLine;

    private Integer score;

    private String reason;

    private String snippet;
}
