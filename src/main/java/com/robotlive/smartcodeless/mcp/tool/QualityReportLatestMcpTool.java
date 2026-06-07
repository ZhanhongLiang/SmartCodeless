package com.robotlive.smartcodeless.mcp.tool;

import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.quality.QualityGateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QualityReportLatestMcpTool extends AbstractMcpTool {

    @Resource
    private QualityGateService qualityGateService;

    @Override
    public String name() {
        return "quality.report.latest";
    }

    @Override
    public String description() {
        return "Read latest quality gate report for a generated app.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId"), Map.of("appId", prop("integer", "App id")));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        Long appId = Long.valueOf(String.valueOf(arguments.get("appId")));
        return McpToolCallResult.builder()
                .success(true)
                .data(qualityGateService.latestReport(appId, context.getLoginUser()))
                .message("已读取最新质量报告")
                .build();
    }
}
