package com.robotlive.smartcodeless.visual.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VisualPatchResult {

    private String targetFile;

    private String baseHash;

    private String beforeSnippet;

    private String afterSnippet;

    private String unifiedDiff;

    private Boolean applicable;

    private Boolean requiresConfirmation;

    private String message;

    private List<VisualEditTarget> candidates;
}
