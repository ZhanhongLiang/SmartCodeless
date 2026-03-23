/*
 * @Author: jean cw252128385@gmail.com
 * @Date: 2026-03-21 15:45:42
 * @LastEditors: jean cw252128385@gmail.com
 * @LastEditTime: 2026-03-21 16:56:56
 * @FilePath: \ai-code-mother-scratch\src\main\java\com\yupi\aicodemotherscratch\ai\AiCodeGeneratorServiceFactory.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.robotlive.smartcodeless.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务创建工厂
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 创建 AI 代码生成器服务
     *
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
//        return AiServices.create(AiCodeGeneratorService.class, chatModel);
    }
}