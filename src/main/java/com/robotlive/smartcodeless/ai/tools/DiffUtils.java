package com.robotlive.smartcodeless.ai.tools;

import com.robotlive.smartcodeless.ai.stream.AgentStreamPayloads;

public final class DiffUtils {

    private static final int MAX_DIFF_LENGTH = 4000;

    private DiffUtils() {
    }

    public static DiffResult createDiff(String path, String before, String after, String changeType) {
        String safePath = AgentStreamPayloads.safePath(path);
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(safePath).append('\n');
        diff.append("+++ ").append(safePath).append('\n');
        if ("delete".equals(changeType)) {
            appendBlock(diff, "-", before);
        } else if ("create".equals(changeType)) {
            appendBlock(diff, "+", after);
        } else {
            appendBlock(diff, "-", before);
            appendBlock(diff, "+", after);
        }
        boolean truncated = diff.length() > MAX_DIFF_LENGTH;
        String text = truncated ? diff.substring(0, MAX_DIFF_LENGTH) + "\n...diff truncated..." : diff.toString();
        return new DiffResult(safePath, changeType, text, truncated);
    }

    public static String summarize(String action, String relativePath) {
        return action + " " + AgentStreamPayloads.safePath(relativePath);
    }

    private static void appendBlock(StringBuilder diff, String prefix, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        String[] lines = content.split("\\R", -1);
        int maxLines = Math.min(lines.length, 80);
        for (int i = 0; i < maxLines; i++) {
            diff.append(prefix).append(lines[i]).append('\n');
        }
        if (lines.length > maxLines) {
            diff.append(prefix).append("...content truncated...").append('\n');
        }
    }

    public record DiffResult(String path, String changeType, String diff, boolean truncated) {
    }
}
