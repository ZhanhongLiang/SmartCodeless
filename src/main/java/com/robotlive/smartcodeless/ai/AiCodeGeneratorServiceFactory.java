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
import com.robotlive.smartcodeless.ai.tools.FileWriteTool;
import com.robotlive.smartcodeless.ai.tools.ToolManager;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.guardrail.PromptSafetyInputGuardrail;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import com.robotlive.smartcodeless.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

//    @Resource
//    private StreamingChatModel streamingChatModel;
// 这里不直接按类型注入 StreamingChatModel，避免与自动配置产生多个同类型 Bean 冲突
// 实际使用时通过 SpringContextUtil 按名称获取 prototype Bean

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * 创建 AI 代码生成器服务
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 这个是为了兼顾老逻辑, 用了方法重载, 就是直接可以调用下面不同参数的方法
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
//        return serviceCache.get(appId, this::createAiCodeGeneratorService);
        return getAiCodeGeneratorService(appId,CodeGenTypeEnum.HTML);

    }

    /**
     * 根据 appId 获取服务
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     *  创建新的 AI 服务实例, 因为需要兼容Vue工程代码生成逻辑
     *     需要额外增添Vue逻辑
     * @param appId
     * @param codeGenType 根据codeGenType来决定生成代码的逻辑
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库中加载对话历史到记忆中
        /**
         * 然后就可以在初始化AI Service的对话记忆时调用了,这相当于是懒加载,对话时才会加载记忆,节约内存。
         */
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 一个AiService，但是可以多个appId, 也就是利用这个实现了隔离
        // 根据不同类型选择不同的生成代码逻辑
        return switch (codeGenType){
//            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
//                    // 调用模型
//                    .streamingChatModel(reasoningStreamingChatModel)
//                    // 来处理appId记忆隔离问题
//                    .chatMemoryProvider(memoryId -> chatMemory)
//                    // 调用tools, 来指定agent调用写入工具类
////                    .tools(new FileWriteTool())
//                    .tools(toolManager.getAllTools())
//                    // 处理工具调用幻觉问题
//                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
//                            ToolExecutionResultMessage.from(toolExecutionRequest,
//                                    "Error: there is no tool called " + toolExecutionRequest.name())
//                    )
//                    .build();
            case VUE_PROJECT -> {
                // 使用spring的多例模式进行bean注入
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                    // 调用模型
                    .streamingChatModel(reasoningStreamingChatModel)
                    // 来处理appId记忆隔离问题
                    .chatMemoryProvider(memoryId -> chatMemory)
                    // 调用tools, 来指定agent调用写入工具类
//                    .tools(new FileWriteTool())
                    .tools(toolManager.getAllTools())
                    // 处理工具调用幻觉问题
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: there is no tool called " + toolExecutionRequest.name())
                    ).inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        .build();

            }
            case HTML,MULTI_FILE -> {
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory) // 加载记忆
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        .build();
            }
//            case HTML,MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
//                    .chatModel(chatModel)
//                    .streamingChatModel(openAiStreamingChatModel)
//                    .chatMemory(chatMemory) // 加载记忆
//                    .build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(openAiStreamingChatModel)
//                .chatMemory(chatMemory) // 加载记忆
//                .build();
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


    /**
     * 构造缓存键
     *
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}