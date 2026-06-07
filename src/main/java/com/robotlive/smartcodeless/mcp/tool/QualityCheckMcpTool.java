package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.model.dto.quality.QualityCheckRequest;
import com.robotlive.smartcodeless.quality.QualityGateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QualityCheckMcpTool extends AbstractMcpTool {

    @Resource
    private QualityGateService qualityGateService;

    @Override
    public String name() {
        return "quality.check";
    }

    @Override
    public String description() {
        return "Submit a quality gate check for a generated app.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId"), Map.of(
                "appId", prop("integer", "App id"),
                "triggerType", stringProp("Quality trigger type", 64),
                "autoRepair", prop("boolean", "Whether to create repair attempt automatically")
        ));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        QualityCheckRequest request = new QualityCheckRequest();
        request.setAppId(Long.valueOf(String.valueOf(arguments.get("appId"))));
        request.setTriggerType(String.valueOf(arguments.getOrDefault("triggerType", "MCP")));
        Object autoRepair = arguments.get("autoRepair");
        request.setAutoRepair(autoRepair instanceof Boolean b && b);
        return McpToolCallResult.builder()
                .success(true)
                .data(qualityGateService.submitQualityCheck(request, context.getLoginUser()))
                .message("质量检查任务已提交")
                .build();
    }
}
