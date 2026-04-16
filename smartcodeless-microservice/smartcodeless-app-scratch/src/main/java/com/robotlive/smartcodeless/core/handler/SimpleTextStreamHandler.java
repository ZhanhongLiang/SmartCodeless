package com.robotlive.smartcodeless.core.handler;

import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.ChatHistoryMessageTypeEnum;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {


    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService, long appId, User loginUser) {
        // 7. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 这个是保存后面AI返回的信息, 就是需要保存AI返回的信息到chatHistoryService历史对话中
        return originFlux
                .map(chunk ->{
                    // 实时收集 AI 响应的内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                }).doOnComplete(() ->{
                    // 流式返回完成后，保存 AI 消息到对话历史中
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId,aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                }).doOnError(error->{
                    // 如果 AI 回复失败，也需要保存记录到数据库中
                    String errorMessage = "AI 回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}