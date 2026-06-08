package com.robotlive.smartcodeless.multimodal.parser;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.robotlive.smartcodeless.multimodal.dto.UiLayoutPlan;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class UiLayoutPlanJsonParser {

    @Resource
    private ObjectMapper objectMapper;

    public UiLayoutPlan parse(String raw) throws Exception {
        String json = extractJson(raw);
        JsonNode root = objectMapper.readTree(json);
        ObjectNode normalized = normalize(root);
        UiLayoutPlan plan = objectMapper.treeToValue(normalized, UiLayoutPlan.class);
        if (plan.getWarnings() == null) {
            plan.setWarnings(new java.util.ArrayList<>());
        }
        if (plan.getComponents() == null) {
            plan.setComponents(new java.util.ArrayList<>());
        }
        if (plan.getAssets() == null) {
            plan.setAssets(new java.util.ArrayList<>());
        }
        if (plan.getConfidence() == null) {
            plan.setConfidence(0.0);
        }
        return plan;
    }

    private ObjectNode normalize(JsonNode root) {
        ObjectNode node = root != null && root.isObject()
                ? ((ObjectNode) root).deepCopy()
                : objectMapper.createObjectNode();
        normalizeTextField(node, "pageType", "unknown");
        normalizeTextField(node, "language", "zh-CN");
        normalizeTextField(node, "imageDescription", "");
        normalizeTheme(node);
        normalizeLayout(node);
        normalizeArrayField(node, "components");
        normalizeArrayField(node, "assets");
        normalizeArrayField(node, "warnings");
        JsonNode confidence = node.get("confidence");
        if (confidence == null || confidence.isNull() || !confidence.isNumber()) {
            node.put("confidence", 0.6);
        }
        return node;
    }

    private void normalizeTextField(ObjectNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            node.put(field, defaultValue);
        } else if (!value.isTextual()) {
            node.put(field, value.asText(defaultValue));
        }
    }

    private void normalizeTheme(ObjectNode node) {
        JsonNode theme = node.get("theme");
        ObjectNode themeNode = objectMapper.createObjectNode();
        if (theme != null && theme.isObject()) {
            themeNode.setAll((ObjectNode) theme);
        } else if (theme != null && !theme.isNull()) {
            ArrayNode keywords = objectMapper.createArrayNode();
            keywords.add(theme.asText());
            themeNode.set("styleKeywords", keywords);
        }
        putIfMissing(themeNode, "primaryColor", "#1677ff");
        putIfMissing(themeNode, "backgroundColor", "#ffffff");
        putIfMissing(themeNode, "textColor", "#1f2937");
        JsonNode keywords = themeNode.get("styleKeywords");
        if (keywords == null || keywords.isNull()) {
            themeNode.set("styleKeywords", objectMapper.createArrayNode());
        } else if (!keywords.isArray()) {
            ArrayNode normalizedKeywords = objectMapper.createArrayNode();
            normalizedKeywords.add(keywords.asText());
            themeNode.set("styleKeywords", normalizedKeywords);
        }
        node.set("theme", themeNode);
    }

    private void normalizeLayout(ObjectNode node) {
        JsonNode layout = node.get("layout");
        if (layout == null || layout.isNull()) {
            ObjectNode layoutNode = objectMapper.createObjectNode();
            layoutNode.put("structure", "navbar + hero + content sections + footer");
            layoutNode.put("grid", "responsive");
            ArrayNode hints = objectMapper.createArrayNode();
            hints.add("移动端优先");
            hints.add("卡片在小屏幕堆叠");
            layoutNode.set("responsiveHints", hints);
            node.set("layout", layoutNode);
        } else if (layout.isTextual()) {
            ObjectNode layoutNode = objectMapper.createObjectNode();
            layoutNode.put("structure", layout.asText());
            layoutNode.put("grid", "responsive");
            layoutNode.set("responsiveHints", objectMapper.createArrayNode());
            node.set("layout", layoutNode);
        }
    }

    private void normalizeArrayField(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            node.set(field, objectMapper.createArrayNode());
        } else if (!value.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            array.add(value.asText());
            node.set(field, array);
        }
    }

    private void putIfMissing(ObjectNode node, String field, String value) {
        JsonNode current = node.get(field);
        if (current == null || current.isNull() || current.asText().isBlank()) {
            node.put(field, value);
        }
    }

    private String extractJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new IllegalArgumentException("Qwen-VL response is empty");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("(?s)^```(?:json)?\\s*", "");
            text = text.replaceFirst("(?s)\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
