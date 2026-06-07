package com.robotlive.smartcodeless.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SandboxStartRequest implements Serializable {

    private Long appId;

    private static final long serialVersionUID = 1L;
}
