package com.robotlive.smartcodeless.mcp;

import com.robotlive.smartcodeless.mcp.model.McpResourceDefinition;
import com.robotlive.smartcodeless.mcp.resource.McpResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class McpResourceRegistry {

    private final Map<String, McpResource> resources = new LinkedHashMap<>();

    public McpResourceRegistry(List<McpResource> resourceList) {
        for (McpResource resource : resourceList) {
            resources.put(resource.uri(), resource);
        }
    }

    public McpResource get(String uri) {
        return resources.get(uri);
    }

    public List<McpResourceDefinition> definitions() {
        return resources.values().stream().map(McpResource::definition).toList();
    }
}

