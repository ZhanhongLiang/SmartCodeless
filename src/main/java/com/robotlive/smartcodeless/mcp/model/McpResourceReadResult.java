package com.robotlive.smartcodeless.mcp.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class McpResourceReadResult implements Serializable {

    private String uri;

    private String mimeType;

    private String text;

    private static final long serialVersionUID = 1L;
}

