package com.robotlive.smartcodeless.multimodal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.open-ai.qwen-vl-chat-model")
public class ModelRouterProperties {

    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String apiKey = "";

    private String modelName = "qwen3-vl-flash";

    private Integer timeoutSeconds = 90;

    private Integer maxTokens = 1600;
}
