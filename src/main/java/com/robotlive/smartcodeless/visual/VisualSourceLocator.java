package com.robotlive.smartcodeless.visual;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.visual.dto.VisualEditTarget;
import com.robotlive.smartcodeless.visual.dto.VisualElementSelection;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class VisualSourceLocator {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".vue", ".html", ".css", ".scss", ".js", ".ts");

    public List<VisualEditTarget> locate(Path appRoot, VisualElementSelection element) {
        ThrowUtils.throwIf(appRoot == null || !Files.isDirectory(appRoot), ErrorCode.NOT_FOUND_ERROR, "应用源码目录不存在");
        List<VisualEditTarget> targets = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(appRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSupported)
                    .filter(this::isSafeSourceFile)
                    .forEach(path -> collectTargets(appRoot, path, element, targets));
        } catch (IOException e) {
            throw new IllegalStateException("扫描源码文件失败", e);
        }
        targets.sort(Comparator.comparing(VisualEditTarget::getScore).reversed());
        return targets.size() > 8 ? targets.subList(0, 8) : targets;
    }

    private void collectTargets(Path appRoot, Path path, VisualElementSelection element, List<VisualEditTarget> targets) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                int score = score(lines[i], element);
                if (score <= 0) {
                    continue;
                }
                int start = Math.max(0, i - 2);
                int end = Math.min(lines.length - 1, i + 2);
                targets.add(VisualEditTarget.builder()
                        .filePath(appRoot.relativize(path).toString().replace('\\', '/'))
                        .startLine(start + 1)
                        .endLine(end + 1)
                        .score(score)
                        .reason(reason(score, element))
                        .snippet(String.join("\n", List.of(lines).subList(start, end + 1)))
                        .build());
            }
        } catch (IOException ignored) {
            // Ignore unreadable generated files. The preview step will report no candidates if nothing matches.
        }
    }

    private int score(String line, VisualElementSelection element) {
        String normalizedLine = StrUtil.blankToDefault(line, "");
        int score = 0;
        if (StrUtil.isNotBlank(element.getId()) && normalizedLine.contains(element.getId())) {
            score += 80;
        }
        if (StrUtil.isNotBlank(element.getClassName())) {
            for (String className : element.getClassName().split("\\s+")) {
                if (StrUtil.isNotBlank(className) && normalizedLine.contains(className)) {
                    score += 24;
                }
            }
        }
        if (StrUtil.isNotBlank(element.getTextContent()) && normalizedLine.contains(element.getTextContent())) {
            score += 70;
        }
        if (StrUtil.isNotBlank(element.getHref()) && normalizedLine.contains(element.getHref())) {
            score += 40;
        }
        if (StrUtil.isNotBlank(element.getSrc()) && normalizedLine.contains(element.getSrc())) {
            score += 40;
        }
        if (StrUtil.isNotBlank(element.getTagName()) && normalizedLine.toLowerCase(Locale.ROOT).contains("<" + element.getTagName().toLowerCase(Locale.ROOT))) {
            score += 16;
        }
        if (StrUtil.isNotBlank(element.getNearbyText()) && element.getNearbyText().length() > 16
                && normalizedLine.contains(StrUtil.subPre(element.getNearbyText(), Math.min(60, element.getNearbyText().length())))) {
            score += 20;
        }
        return score;
    }

    private String reason(int score, VisualElementSelection element) {
        if (StrUtil.isNotBlank(element.getId())) {
            return "命中 id/class/文本特征，得分=" + score;
        }
        return "命中可视化选择特征，得分=" + score;
    }

    private boolean isSupported(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private boolean isSafeSourceFile(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return !normalized.contains("/node_modules/")
                && !normalized.contains("/dist/")
                && !normalized.contains("/target/")
                && !normalized.contains("/.git/");
    }
}
