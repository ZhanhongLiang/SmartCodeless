package com.robotlive.smartcodeless.mcp;

import com.robotlive.smartcodeless.mcp.model.McpToolDefinition;
import com.robotlive.smartcodeless.mcp.tool.AbstractMcpTool;
import com.robotlive.smartcodeless.mcp.tool.McpTool;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class McpToolRegistry {

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public McpToolRegistry(List<McpTool> toolList) {
        for (McpTool tool : toolList) {
            tools.put(tool.name(), tool);
        }
    }

    public Collection<McpTool> all() {
        return tools.values();
    }

    public McpTool get(String name) {
        return tools.get(name);
    }

    public List<McpToolDefinition> definitions() {
        return tools.values().stream()
                .map(tool -> tool instanceof AbstractMcpTool abstractTool
                        ? abstractTool.definition()
                        : McpToolDefinition.builder()
                        .name(tool.name())
                        .description(tool.description())
                        .inputSchema(tool.inputSchema())
                        .build())
                .toList();
    }
}

