package com.robotlive.smartcodeless.mcp.tool;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.mcp.McpPermissionGuard;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.model.entity.App;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class CodeFileReadMcpTool extends AbstractMcpTool {

    private static final int MAX_CONTENT_LENGTH = 12000;

    @Resource
    private McpPermissionGuard permissionGuard;

    @Override
    public String name() {
        return "code.file.read";
    }

    @Override
    public String description() {
        return "Read a file from an app code output directory with sandbox checks.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId", "path"), Map.of(
                "appId", prop("number", "Application id"),
                "path", stringProp("Relative file path inside the app code directory", 512)
        ));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        App app = permissionGuard.requireAppAccess(permissionGuard.appId(arguments), context.getLoginUser());
        String relativePath = permissionGuard.stringArg(arguments, "path");
        Path path = permissionGuard.resolveAppFile(app, relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "file not found");
        }
        try {
            String content = Files.readString(path);
            boolean truncated = content.length() > MAX_CONTENT_LENGTH;
            return McpToolCallResult.builder()
                    .success(true)
                    .data(Map.of(
                            "path", relativePath,
                            "content", StrUtil.subPre(content, MAX_CONTENT_LENGTH),
                            "truncated", truncated))
                    .message("file read")
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "read file failed");
        }
    }
}

