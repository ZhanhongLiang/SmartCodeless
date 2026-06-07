package com.robotlive.smartcodeless.model.dto.build;

import lombok.Data;

import java.io.Serializable;

@Data
public class BuildTaskSubmitRequest implements Serializable {

    private Long appId;

    private String triggerType;

    private static final long serialVersionUID = 1L;
}
