package com.robotlive.smartcodeless.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class BuildTaskSubmitVO implements Serializable {

    private Long taskId;

    private Long appId;

    private String status;

    private String deployUrl;

    private String message;

    private static final long serialVersionUID = 1L;
}
