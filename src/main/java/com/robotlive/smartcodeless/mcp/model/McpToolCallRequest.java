package com.robotlive.smartcodeless.mcp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class McpToolCallRequest implements Serializable {

    private String name;

    private Map<String, Object> arguments;

    private static final long serialVersionUID = 1L;
}

