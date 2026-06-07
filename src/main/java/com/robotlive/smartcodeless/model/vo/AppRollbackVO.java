package com.robotlive.smartcodeless.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AppRollbackVO implements Serializable {

    private Long appId;

    private String targetCommitId;

    private Long rollbackVersionId;

    private Long buildTaskId;

    private String status;

    private String message;

    private static final long serialVersionUID = 1L;
}

