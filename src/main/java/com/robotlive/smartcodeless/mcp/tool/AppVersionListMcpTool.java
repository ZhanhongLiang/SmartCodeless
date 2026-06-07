package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.McpPermissionGuard;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AppVersionListMcpTool extends AbstractMcpTool {

    @Resource
    private McpPermissionGuard permissionGuard;

    @Resource
    private AppVersionService appVersionService;

    @Override
    public String name() {
        return "code.version.list";
    }

    @Override
    public String description() {
        return "List Git-backed versions for an app.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId"), Map.of("appId", prop("number", "Application id")));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        App app = permissionGuard.requireAppAccess(permissionGuard.appId(arguments), context.getLoginUser());
        return McpToolCallResult.builder()
                .success(true)
                .data(appVersionService.listAppVersions(app.getId(), context.getLoginUser()))
                .message("versions listed")
                .build();
    }
}

