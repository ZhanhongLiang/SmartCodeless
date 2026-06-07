package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.model.McpToolDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMcpTool implements McpTool {

    public McpToolDefinition definition() {
        return McpToolDefinition.builder()
                .name(name())
                .description(description())
                .inputSchema(inputSchema())
                .build();
    }

    protected Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", required);
        schema.put("properties", properties);
        return schema;
    }

    protected Map<String, Object> prop(String type, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", type);
        property.put("description", description);
        return property;
    }

    protected Map<String, Object> stringProp(String description, int maxLength) {
        Map<String, Object> property = prop("string", description);
        property.put("maxLength", maxLength);
        return property;
    }

    protected Map<String, Object> ok(Object data) {
        return Map.of("data", data);
    }
}

