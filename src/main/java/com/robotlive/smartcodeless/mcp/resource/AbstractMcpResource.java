package com.robotlive.smartcodeless.mcp.resource;

import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpResourceReadResult;

public abstract class AbstractMcpResource implements McpResource {

    @Override
    public String mimeType() {
        return "text/plain";
    }

    @Override
    public McpResourceReadResult read(McpExecutionContext context) {
        return McpResourceReadResult.builder()
                .uri(uri())
                .mimeType(mimeType())
                .text(text())
                .build();
    }

    protected abstract String text();
}

