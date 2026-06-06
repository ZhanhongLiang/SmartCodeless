package com.robotlive.smartcodeless.ai.stream;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentStreamPayloads {

    private AgentStreamPayloads() {
    }

    public static Map<String, Object> status(String stage, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stage", stage);
        payload.put("message", safeText(message));
        return payload;
    }

    public static Map<String, Object> message(String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", safeText(content));
        return payload;
    }

    public static Map<String, Object> toolCall(String toolName, String status, String summary, Long durationMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolName", safeText(toolName));
        payload.put("status", status);
        payload.put("summary", safeText(summary));
        if (durationMs != null) {
            payload.put("durationMs", durationMs);
        }
        return payload;
    }

    public static Map<String, Object> fileDiff(String path, String changeType, String diff, boolean truncated) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("path", safePath(path));
        payload.put("changeType", changeType);
        payload.put("diff", safeText(diff));
        payload.put("truncated", truncated);
        return payload;
    }

    public static Map<String, Object> error(String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", safeText(code));
        payload.put("message", safeText(message));
        return payload;
    }

    public static Map<String, Object> done(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", safeText(message));
        return payload;
    }

    public static String safePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        int marker = normalized.lastIndexOf("vue_project_");
        if (marker >= 0) {
            int nextSlash = normalized.indexOf('/', marker);
            if (nextSlash >= 0 && nextSlash + 1 < normalized.length()) {
                normalized = normalized.substring(nextSlash + 1);
            }
        }
        while (normalized.startsWith("/") || normalized.startsWith("../")) {
            normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized.substring(3);
        }
        return normalized;
    }

    public static String safeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("(?i)(api[_-]?key|password|secret|token)\\s*[:=]\\s*[^\\s,;]+", "$1=***")
                .replaceAll("[A-Za-z]:[/\\\\][^\\s\"']+", "[server-path]")
                .replaceAll("(?<!:)//[^\\s\"']+", "//[server-path]");
    }
}
