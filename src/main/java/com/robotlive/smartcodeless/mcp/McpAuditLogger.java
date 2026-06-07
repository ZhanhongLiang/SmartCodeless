package com.robotlive.smartcodeless.mcp;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class McpAuditLogger {

    public long started(String requestId, User user, String toolName, Map<String, Object> args) {
        log.info("MCP tool started requestId={}, userId={}, tool={}, args={}",
                requestId, user == null ? null : user.getId(), toolName, summarize(args));
        return System.currentTimeMillis();
    }

    public void success(String requestId, User user, String toolName, long startedAt, Object result) {
        log.info("MCP tool success requestId={}, userId={}, tool={}, durationMs={}, result={}",
                requestId, user == null ? null : user.getId(), toolName, elapsed(startedAt), StrUtil.subPre(String.valueOf(result), 300));
    }

    public void failed(String requestId, User user, String toolName, long startedAt, Exception e) {
        log.warn("MCP tool failed requestId={}, userId={}, tool={}, durationMs={}, error={}",
                requestId, user == null ? null : user.getId(), toolName, elapsed(startedAt), e.getMessage());
    }

    private long elapsed(long startedAt) {
        return Math.max(0, System.currentTimeMillis() - startedAt);
    }

    private String summarize(Map<String, Object> args) {
        if (args == null) {
            return "{}";
        }
        return StrUtil.subPre(args.toString()
                .replaceAll("(?i)(password|secret|token|key)=([^,}]+)", "$1=***"), 500);
    }
}

