package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.McpPermissionGuard;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.service.BuildTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class VueBuildSubmitMcpTool extends AbstractMcpTool {

    @Resource
    private McpPermissionGuard permissionGuard;

    @Resource
    private BuildTaskService buildTaskService;

    @Override
    public String name() {
        return "code.build.vue.submit";
    }

    @Override
    public String description() {
        return "Submit an asynchronous Stage 2 build task for an app.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId"), Map.of(
                "appId", prop("number", "Application id"),
                "triggerType", stringProp("Optional trigger type, default MCP", 32)
        ));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        App app = permissionGuard.requireAppAccess(permissionGuard.appId(arguments), context.getLoginUser());
        String triggerType = arguments.get("triggerType") instanceof String text ? text : "MCP";
        BuildTaskSubmitVO result = buildTaskService.submitBuildTask(app.getId(), context.getLoginUser(), triggerType);
        return McpToolCallResult.builder()
                .success(true)
                .data(result)
                .message("build task queued")
                .build();
    }
}

