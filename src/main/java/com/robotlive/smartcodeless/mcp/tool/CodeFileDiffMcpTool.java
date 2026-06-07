package com.robotlive.smartcodeless.mcp.tool;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.git.GitCommandRunner;
import com.robotlive.smartcodeless.mcp.McpPermissionGuard;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpToolCallResult;
import com.robotlive.smartcodeless.model.entity.App;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CodeFileDiffMcpTool extends AbstractMcpTool {

    private static final int MAX_DIFF_LENGTH = 12000;

    @Resource
    private McpPermissionGuard permissionGuard;

    @Resource
    private GitCommandRunner gitCommandRunner;

    @Override
    public String name() {
        return "code.file.diff";
    }

    @Override
    public String description() {
        return "Read current git diff for an app or a sandboxed relative file path.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return objectSchema(List.of("appId"), Map.of(
                "appId", prop("number", "Application id"),
                "path", stringProp("Optional relative file path", 512)
        ));
    }

    @Override
    public McpToolCallResult execute(McpExecutionContext context, Map<String, Object> arguments) {
        App app = permissionGuard.requireAppAccess(permissionGuard.appId(arguments), context.getLoginUser());
        Path appRoot = permissionGuard.resolveAppRoot(app);
        if (!Files.exists(appRoot.resolve(".git"))) {
            return McpToolCallResult.builder()
                    .success(true)
                    .data(Map.of("diff", "", "truncated", false, "message", "git repository not initialized"))
                    .message("no git repository")
                    .build();
        }
        Object pathArg = arguments.get("path");
        GitCommandRunner.GitCommandResult result;
        if (pathArg instanceof String relativePath && StrUtil.isNotBlank(relativePath)) {
            permissionGuard.resolveAppFile(app, relativePath);
            result = gitCommandRunner.run(appRoot, Duration.ofSeconds(15), "diff", "--", relativePath);
        } else {
            result = gitCommandRunner.run(appRoot, Duration.ofSeconds(15), "diff");
        }
        if (!result.success()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git diff failed");
        }
        String diff = result.getStdout();
        return McpToolCallResult.builder()
                .success(true)
                .data(Map.of("diff", StrUtil.subPre(diff, MAX_DIFF_LENGTH), "truncated", diff.length() > MAX_DIFF_LENGTH))
                .message("diff read")
                .build();
    }
}

