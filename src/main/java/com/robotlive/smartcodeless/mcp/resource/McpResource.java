package com.robotlive.smartcodeless.mcp.resource;

import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpResourceDefinition;
import com.robotlive.smartcodeless.mcp.model.McpResourceReadResult;

public interface McpResource {

    String uri();

    String name();

    String description();

    String mimeType();

    McpResourceReadResult read(McpExecutionContext context);

    default McpResourceDefinition definition() {
        return McpResourceDefinition.builder()
                .uri(uri())
                .name(name())
                .description(description())
                .mimeType(mimeType())
                .build();
    }
}

