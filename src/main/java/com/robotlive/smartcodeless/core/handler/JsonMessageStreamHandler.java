package com.robotlive.smartcodeless.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.robotlive.smartcodeless.ai.model.message.*;
import com.robotlive.smartcodeless.ai.tools.BaseTool;
import com.robotlive.smartcodeless.ai.tools.ToolManager;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.core.builder.VueProjectBuilder;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.ChatHistoryMessageTypeEnum;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {


    @Resource
    private VueProjectBuilder vueProjectBuilder;


    @Resource
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        // 这个是保存后面AI返回的信息, 就是需要保存AI返回的信息到chatHistoryService历史对话中
        return originFlux
                .map(chunk ->{
                    // 实时收集 AI 响应的内容
//                    aiResponseBuilder.append(chunk);
//                    return chunk;
                    return handleJsonMessageChunk(chunk,chatHistoryStringBuilder,seenToolIds);
                }).doOnComplete(() ->{
                    // 流式返回完成后，保存 AI 消息到对话历史中
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId,aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                    // 异步构造 Vue 项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                }).doOnError(error->{
                    // 如果 AI 回复失败，也需要保存记录到数据库中
                    String errorMessage = "AI 回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    /**
     *
     * @param chunk
     * @param chatHistoryStringBuilder
     * @param seenToolIds 跟踪见过的工具ID
     * @return
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        // 消息类型，并且转为枚举类
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        // 判断枚举
        switch (typeEnum) {
            case AI_RESPONSE -> {
                // 把chunk转为AiResponseMessage
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    // 根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 返回格式化的工具调用信息
                    return tool.generateToolRequestResponse();
//                    return "\n\n[选择工具] 写入文件\n\n";
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
//                String relativeFilePath = jsonObject.getStr("relativeFilePath");
//                String suffix = FileUtil.getSuffix(relativeFilePath);
//                String content = jsonObject.getStr("content");
//                String result = String.format("""
//                        [工具调用] 写入文件 %s
//                        ```%s
//                        %s
//                        ```
//                        """, relativeFilePath, suffix, content);
                // 根据工具名称获取工具实例
                String toolName = toolExecutedMessage.getName();
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }

    }
}