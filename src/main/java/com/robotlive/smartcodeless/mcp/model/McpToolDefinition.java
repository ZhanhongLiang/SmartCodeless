package com.robotlive.smartcodeless.mcp.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
public class McpToolDefinition implements Serializable {

    private String name;

    private String description;

    private Map<String, Object> inputSchema;

    private static final long serialVersionUID = 1L;
}

