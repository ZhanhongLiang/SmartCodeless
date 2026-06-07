package com.robotlive.smartcodeless.mcp.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class McpResourceDefinition implements Serializable {

    private String uri;

    private String name;

    private String description;

    private String mimeType;

    private static final long serialVersionUID = 1L;
}

