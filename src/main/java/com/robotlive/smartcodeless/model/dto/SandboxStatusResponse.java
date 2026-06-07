package com.robotlive.smartcodeless.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SandboxStatusResponse implements Serializable {

    private Long appId;

    private String deployKey;

    private String status;

    private String previewUrl;

    private String staticUrl;

    private String errorMessage;

    private static final long serialVersionUID = 1L;
}
