package com.robotlive.smartcodeless.visual;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.visual.dto.VisualEditChangeSet;
import com.robotlive.smartcodeless.visual.dto.VisualEditTarget;
import com.robotlive.smartcodeless.visual.dto.VisualElementSelection;
import com.robotlive.smartcodeless.visual.dto.VisualPatchResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Component
public class VisualPatchPlanner {

    public VisualPatchResult preview(Path appRoot,
                                     VisualElementSelection element,
                                     VisualEditChangeSet changes,
                                     List<VisualEditTarget> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return VisualPatchResult.builder()
                    .applicable(false)
                    .requiresConfirmation(false)
                    .message("没有找到该元素对应的源码位置")
                    .candidates(List.of())
                    .build();
        }
        VisualEditTarget target = candidates.get(0);
        Path targetPath = appRoot.resolve(target.getFilePath()).normalize();
        try {
            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            String nextContent = applyChanges(content, element, changes);
            if (content.equals(nextContent)) {
                return VisualPatchResult.builder()
                        .targetFile(target.getFilePath())
                        .baseHash(sha256(content))
                        .beforeSnippet(target.getSnippet())
                        .afterSnippet(target.getSnippet())
                        .unifiedDiff("")
                        .applicable(false)
                        .requiresConfirmation(false)
                        .message("未能生成安全的确定性补丁。建议先修改文本、类名、href/src、alt 或行内样式。")
                        .candidates(candidates)
                        .build();
            }
            String afterSnippet = snippetAround(nextContent, target.getStartLine(), target.getEndLine());
            return VisualPatchResult.builder()
                    .targetFile(target.getFilePath())
                    .baseHash(sha256(content))
                    .beforeSnippet(target.getSnippet())
                    .afterSnippet(afterSnippet)
                    .unifiedDiff(buildUnifiedDiff(target.getFilePath(), content, nextContent))
                    .applicable(true)
                    .requiresConfirmation(candidates.size() > 1 && candidates.get(1).getScore() + 20 >= target.getScore())
                    .message("差异预览已生成，请确认后再应用。")
                    .candidates(candidates)
                    .build();
        } catch (Exception e) {
            return VisualPatchResult.builder()
                    .targetFile(target.getFilePath())
                    .applicable(false)
                    .requiresConfirmation(false)
                    .message("生成差异预览失败：" + e.getMessage())
                    .candidates(candidates)
                    .build();
        }
    }

    String applyChanges(String content, VisualElementSelection element, VisualEditChangeSet changes) {
        String next = content;
        if (changes == null) {
            return next;
        }
        if (StrUtil.isNotBlank(changes.getTextContent()) && StrUtil.isNotBlank(element.getTextContent())) {
            next = next.replaceFirst(java.util.regex.Pattern.quote(element.getTextContent()),
                    java.util.regex.Matcher.quoteReplacement(changes.getTextContent()));
        }
        if (StrUtil.isNotBlank(changes.getClassName())) {
            next = replaceAttribute(next, element, "class", element.getClassName(), changes.getClassName());
        }
        if (changes.getAttributes() != null) {
            for (Map.Entry<String, String> entry : changes.getAttributes().entrySet()) {
                next = replaceAttribute(next, element, entry.getKey(), findCurrentAttributeValue(element, entry.getKey()), entry.getValue());
            }
        }
        if (changes.getInlineStyle() != null && !changes.getInlineStyle().isEmpty()) {
            String mergedStyle = mergeInlineStyle(findCurrentAttributeValue(element, "style"), changes.getInlineStyle());
            next = replaceAttribute(next, element, "style", findCurrentAttributeValue(element, "style"), mergedStyle);
        }
        return next;
    }

    private String replaceAttribute(String content, VisualElementSelection element, String attrName, String oldValue, String newValue) {
        if (StrUtil.isBlank(attrName) || newValue == null) {
            return content;
        }
        if (StrUtil.isNotBlank(oldValue)) {
            String[] patterns = {
                    attrName + "=\"" + oldValue + "\"",
                    attrName + "='" + oldValue + "'",
                    ":" + attrName + "=\"" + oldValue + "\"",
                    ":" + attrName + "='" + oldValue + "'"
            };
            for (String pattern : patterns) {
                if (content.contains(pattern)) {
                    return content.replaceFirst(java.util.regex.Pattern.quote(pattern),
                            java.util.regex.Matcher.quoteReplacement(attrName + "=\"" + newValue + "\""));
                }
            }
        }
        String tagName = StrUtil.blankToDefault(element.getTagName(), "").toLowerCase();
        if (StrUtil.isBlank(tagName)) {
            return content;
        }
        String marker = "<" + tagName;
        int index = content.toLowerCase().indexOf(marker);
        if (index < 0) {
            return content;
        }
        int insertAt = content.indexOf('>', index);
        if (insertAt < 0) {
            return content;
        }
        return content.substring(0, insertAt) + " " + attrName + "=\"" + newValue + "\"" + content.substring(insertAt);
    }

    private String findCurrentAttributeValue(VisualElementSelection element, String attrName) {
        if ("class".equals(attrName)) {
            return element.getClassName();
        }
        if ("href".equals(attrName)) {
            return element.getHref();
        }
        if ("src".equals(attrName)) {
            return element.getSrc();
        }
        if (element.getAttributes() == null) {
            return "";
        }
        return element.getAttributes().stream()
                .filter(attr -> attrName.equals(attr.getName()))
                .map(attr -> StrUtil.blankToDefault(attr.getValue(), ""))
                .findFirst()
                .orElse("");
    }

    private String mergeInlineStyle(String currentStyle, Map<String, String> styleChanges) {
        StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(currentStyle, "").trim());
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ';') {
            builder.append(';');
        }
        styleChanges.forEach((name, value) -> {
            if (StrUtil.isAllNotBlank(name, value) && isSafeStyleName(name)) {
                builder.append(' ').append(name).append(": ").append(value).append(';');
            }
        });
        return builder.toString().trim();
    }

    private boolean isSafeStyleName(String name) {
        return name.matches("[a-zA-Z-]{2,40}");
    }

    private String snippetAround(String content, Integer startLine, Integer endLine) {
        String[] lines = content.split("\\R", -1);
        int start = Math.max(0, (startLine == null ? 1 : startLine) - 1);
        int end = Math.min(lines.length - 1, (endLine == null ? start + 4 : endLine) - 1);
        return String.join("\n", List.of(lines).subList(start, end + 1));
    }

    private String buildUnifiedDiff(String file, String before, String after) {
        String beforeSnippet = compact(before);
        String afterSnippet = compact(after);
        return "--- a/" + file + System.lineSeparator()
                + "+++ b/" + file + System.lineSeparator()
                + "@@ visual edit preview @@" + System.lineSeparator()
                + "- " + beforeSnippet + System.lineSeparator()
                + "+ " + afterSnippet;
    }

    private String compact(String text) {
        String compacted = StrUtil.subPre(text.replaceAll("\\s+", " ").trim(), 1000);
        return compacted.isEmpty() ? "(empty)" : compacted;
    }

    String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
