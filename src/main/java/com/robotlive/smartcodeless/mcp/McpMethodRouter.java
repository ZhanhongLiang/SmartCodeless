package com.robotlive.smartcodeless.mcp;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.mcp.resource.McpResource;
import com.robotlive.smartcodeless.mcp.tool.McpTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class McpMethodRouter {

    @Resource
    private McpToolRegistry toolRegistry;

    @Resource
    private McpResourceRegistry resourceRegistry;

    @Resource
    private McpSchemaValidator schemaValidator;

    @Resource
    private McpAuditLogger auditLogger;

    @SuppressWarnings("unchecked")
    public Object route(String method, Map<String, Object> params, McpExecutionContext context) {
        return switch (method) {
            case "ping" -> Map.of("pong", true);
            case "server/info" -> Map.of(
                    "name", "SmartCodeless MCP Adapter",
                    "version", "stage-4",
                    "methods", new String[]{"tools/list", "tools/call", "resources/list", "resources/read"});
            case "tools/list" -> Map.of("tools", toolRegistry.definitions());
            case "resources/list" -> Map.of("resources", resourceRegistry.definitions());
            case "resources/read" -> readResource(params, context);
            case "tools/call" -> callTool(params, context);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "MCP method not found: " + method);
        };
    }

    @SuppressWarnings("unchecked")
    private Object callTool(Map<String, Object> params, McpExecutionContext context) {
        String name = stringParam(params, "name");
        Object argsObj = params == null ? null : params.get("arguments");
        Map<String, Object> arguments = argsObj instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        McpTool tool = toolRegistry.get(name);
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "MCP tool not found: " + name);
        }
        schemaValidator.validate(tool.inputSchema(), arguments);
        long startedAt = auditLogger.started(context.getRequestId(), context.getLoginUser(), name, arguments);
        try {
            McpToolCallResult result = tool.execute(context, arguments);
            auditLogger.success(context.getRequestId(), context.getLoginUser(), name, startedAt, result.getMessage());
            return result;
        } catch (BusinessException e) {
            auditLogger.failed(context.getRequestId(), context.getLoginUser(), name, startedAt, e);
            throw e;
        } catch (Exception e) {
            auditLogger.failed(context.getRequestId(), context.getLoginUser(), name, startedAt, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "tool execution failed");
        }
    }

    private Object readResource(Map<String, Object> params, McpExecutionContext context) {
        String uri = stringParam(params, "uri");
        McpResource resource = resourceRegistry.get(uri);
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "MCP resource not found: " + uri);
        }
        return resource.read(context);
    }

    private String stringParam(Map<String, Object> params, String name) {
        Object value = params == null ? null : params.get(name);
        if (!(value instanceof String text) || StrUtil.isBlank(text)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, name + " is required");
        }
        return text;
    }
}

