/*
 * @Author: jean cw252128385@gmail.com
 * @Date: 2026-03-21 15:45:42
 * @LastEditors: jean cw252128385@gmail.com
 * @LastEditTime: 2026-03-21 16:56:56
 * @FilePath: \ai-code-mother-scratch\src\main\java\com\yupi\aicodemotherscratch\ai\AiCodeGeneratorServiceFactory.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.robotlive.smartcodeless.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 创建 AI 代码生成器服务
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();


    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建新的 AI 服务实例
     *
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        /**
         * 然后就可以在初始化AI Service的对话记忆时调用了,这相当于是懒加载,对话时才会加载记忆,节约内存。
         */
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 一个AiService，但是可以多个appId, 也就是利用这个实现了隔离
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory) // 加载记忆
                .build();
    }

//    /**
//     * 创建 AI 代码生成器服务
//     *  需要实现对话历史隔离, 就是只利用一个AiServices实例，就能实现对话历史隔离
//     *    这个如何实现，就是通过传入AppId实现
//     * @return
//     */
//    @Bean
//    public AiCodeGeneratorService aiCodeGeneratorService(long appId) {
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(streamingChatModel)
//                .build();
////        return AiServices.create(AiCodeGeneratorService.class, chatModel);
//    }

    /**
     * 创建 AI 代码生成器服务
     *  这个办法只需要创建一个AiServices实例, 内部实现隔离
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }
}