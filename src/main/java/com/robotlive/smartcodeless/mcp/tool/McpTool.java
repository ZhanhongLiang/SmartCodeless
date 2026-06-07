package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;

import java.util.Map;

public interface McpTool {

    String name();

    String description();

    Map<String, Object> inputSchema();

    McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments);
}

