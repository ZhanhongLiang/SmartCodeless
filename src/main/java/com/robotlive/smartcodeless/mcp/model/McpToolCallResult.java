package com.robotlive.smartcodeless.mcp.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class McpToolCallResult implements Serializable {

    private boolean success;

    private Object data;

    private String message;

    private static final long serialVersionUID = 1L;
}

