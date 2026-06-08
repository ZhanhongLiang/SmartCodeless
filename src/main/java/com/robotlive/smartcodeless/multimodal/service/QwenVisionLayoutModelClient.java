package com.robotlive.smartcodeless.multimodal.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.robotlive.smartcodeless.model.entity.ReferenceImage;
import com.robotlive.smartcodeless.multimodal.config.ModelRouterProperties;
import com.robotlive.smartcodeless.multimodal.dto.UiLayoutPlan;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;
import com.robotlive.smartcodeless.multimodal.parser.UiLayoutPlanJsonParser;
import com.robotlive.smartcodeless.multimodal.prompt.VisionLayoutPromptBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class QwenVisionLayoutModelClient implements VisionLayoutModelClient {

    @Resource
    private ModelRouterProperties modelRouterProperties;

    @Resource
    private ReferenceImageService referenceImageService;

    @Resource
    private VisionLayoutPromptBuilder promptBuilder;

    @Resource
    private UiLayoutPlanJsonParser parser;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public VisionAnalysisResult analyze(ReferenceImage image, String userPrompt) {
        if (StrUtil.isBlank(modelRouterProperties.getApiKey())) {
            return fallback(image, "Qwen-VL API Key 未配置，已使用安全兜底布局计划");
        }
        try {
            byte[] imageBytes = Files.readAllBytes(referenceImageService.getImagePath(image));
            String base64 = Base64.encode(imageBytes);
            String dataUrl = "data:" + image.getMimeType() + ";base64," + base64;
            log.info("Calling Qwen-VL model={}, baseUrl={}, imageId={}, mimeType={}, bytes={}",
                    modelRouterProperties.getModelName(),
                    modelRouterProperties.getBaseUrl(),
                    image.getId(),
                    image.getMimeType(),
                    imageBytes.length);
            String raw = callDashScopeVisionModel(promptBuilder.build(userPrompt), dataUrl);
            UiLayoutPlan plan = parser.parse(raw);
            return VisionAnalysisResult.builder()
                    .imageId(image.getId())
                    .layoutPlan(plan)
                    .rawJson(JSONUtil.toJsonStr(plan))
                    .imageDescription(imageDescription(plan))
                    .fallback(false)
                    .summary(summary(plan))
                    .build();
        } catch (Exception e) {
            log.warn("Qwen-VL vision layout analysis failed, imageId={}", image.getId(), e);
            return fallback(image, "Qwen-VL 直连 DashScope 调用失败，已使用安全兜底布局计划："
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private String callDashScopeVisionModel(String prompt, String imageDataUrl) throws Exception {
        String endpoint = normalizeBaseUrl(modelRouterProperties.getBaseUrl()) + "/chat/completions";
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelRouterProperties.getModelName());
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", modelRouterProperties.getMaxTokens());

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是前端页面视觉分析器。只输出合法 JSON，不要解释。");

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode userContent = userMessage.putArray("content");
        ObjectNode textPart = userContent.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        ObjectNode imagePart = userContent.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = imagePart.putObject("image_url");
        imageUrl.put("url", imageDataUrl);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.info("DashScope Qwen-VL request prepared, endpoint={}, bodyChars={}, imageDataChars={}",
                endpoint, jsonBody.length(), imageDataUrl.length());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(modelRouterProperties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + modelRouterProperties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(modelRouterProperties.getTimeoutSeconds()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DashScope HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode body = objectMapper.readTree(response.body());
        JsonNode choices = body.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("DashScope response has no choices: " + response.body());
        }
        JsonNode message = choices.get(0).path("message");
        if (message.isMissingNode() || message.isNull()) {
            throw new IllegalStateException("DashScope response has no message: " + response.body());
        }
        JsonNode content = message.path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("DashScope response has empty content: " + response.body());
        }
        if (content.isTextual()) {
            return content.asText();
        }
        return content.toString();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StrUtil.blankToDefault(baseUrl, "https://dashscope.aliyuncs.com/compatible-mode/v1");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private VisionAnalysisResult fallback(ReferenceImage image, String warning) {
        UiLayoutPlan plan = new UiLayoutPlan();
        plan.setPageType("unknown");
        plan.getTheme().setPrimaryColor("#1677ff");
        plan.getTheme().setBackgroundColor("#ffffff");
        plan.getTheme().setTextColor("#1f2937");
        plan.getTheme().getStyleKeywords().add("clean");
        plan.getTheme().getStyleKeywords().add("reference-image-inspired");
        plan.getLayout().put("structure", "navbar + hero + content sections + footer");
        plan.getLayout().put("grid", "responsive");
        plan.getLayout().put("responsiveHints", List.of("移动端优先", "卡片在小屏幕堆叠"));
        plan.setConfidence(0.35);
        plan.getWarnings().add(warning);
        return VisionAnalysisResult.builder()
                .imageId(image.getId())
                .layoutPlan(plan)
                .rawJson(JSONUtil.toJsonStr(plan))
                .imageDescription(imageDescription(plan))
                .fallback(true)
                .summary(summary(plan))
                .build();
    }

    private String imageDescription(UiLayoutPlan plan) {
        if (StrUtil.isNotBlank(plan.getImageDescription())) {
            return plan.getImageDescription();
        }
        String keywords = plan.getTheme() != null && plan.getTheme().getStyleKeywords() != null
                ? String.join("、", plan.getTheme().getStyleKeywords())
                : "";
        String structure = plan.getLayout() != null && plan.getLayout().get("structure") != null
                ? String.valueOf(plan.getLayout().get("structure"))
                : "";
        return "图片视觉参考：" + StrUtil.blankToDefault(keywords, "未识别到明确关键词")
                + "；生成建议：" + StrUtil.blankToDefault(structure, "根据图片内容生成相关页面");
    }

    private String summary(UiLayoutPlan plan) {
        if (StrUtil.isNotBlank(plan.getImageDescription())) {
            return plan.getImageDescription();
        }
        return "页面类型：" + plan.getPageType()
                + "，组件数：" + plan.getComponents().size()
                + "，置信度：" + plan.getConfidence();
    }
}
