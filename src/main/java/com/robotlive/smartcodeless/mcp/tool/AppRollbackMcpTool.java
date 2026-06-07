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
public class AppRollbackMcpTool extends AbstractMcpTool {

    @Resource
    private McpPermissionGuard permissionGuard;

    @Resource
    private AppVersionService appVersionService;

    @Override
    public String name() {
        return "code.rollback.toCommit";
    }

    @Override
    public String description() {
        return "Rollback an app to a commit recorded in app_version and queue Stage 2 build.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId", "commitId"), Map.of(
                "appId", prop("number", "Application id"),
                "commitId", stringProp("Commit id from app_version", 64),
                "reason", stringProp("Optional rollback reason", 512)
        ));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        App app = permissionGuard.requireAppAccess(permissionGuard.appId(arguments), context.getLoginUser());
        String commitId = permissionGuard.stringArg(arguments, "commitId");
        String reason = arguments.get("reason") instanceof String text ? text : "MCP rollback";
        return McpToolCallResult.builder()
                .success(true)
                .data(appVersionService.rollback(app.getId(), commitId, reason, context.getLoginUser()))
                .message("rollback queued")
                .build();
    }
}

