package com.robotlive.smartcodeless.model.dto.version;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppVersionRollbackRequest implements Serializable {

    private Long appId;

    private String commitId;

    private String reason;

    private static final long serialVersionUID = 1L;
}

