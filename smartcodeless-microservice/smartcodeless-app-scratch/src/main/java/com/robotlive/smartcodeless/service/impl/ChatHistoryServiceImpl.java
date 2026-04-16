package com.robotlive.smartcodeless.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.mapper.ChatHistoryMapper;
import com.robotlive.smartcodeless.model.dto.chathistory.ChatHistoryQueryRequest;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.ChatHistory;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.ChatHistoryMessageTypeEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.ChatHistoryService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{


    @Resource
    @Lazy
    private AppService appService;

//    @Resource
//    private UserService userService;

    /**
     * 添加对话历史
     *
     * @param appId       应用 id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户 id
     * @return 是否成功
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 添加对话历史
        // 先需要参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "应用ID不为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message),ErrorCode.PARAMS_ERROR,"消息不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR,"消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId < 0,ErrorCode.PARAMS_ERROR,"用户ID不为空");
        // 获得消息类型
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR);
        // 直接用新特征，用builder来构建对象
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }


    /**
     * 删除应用
     * @param appId
     * @return
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 根据游标查询，不是普通的分页查询
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 获得所有请求参数
        // 构建查询，利用QueryWrapper
        QueryWrapper queryWrapper = new QueryWrapper();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        int pageNum = chatHistoryQueryRequest.getPageNum();
        int pageSize = chatHistoryQueryRequest.getPageSize();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列, 因为这个最早时间排在前面，所以需要降序排序
            // 越大的时间放在后面
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 核心的游标查询算法， 为什么这里不接受Request请求体，因为是后面GetMapping调用
     *  这个是用户调用的, 用户只能根据appId和lastCreateTime查询
     *  不能通过message、MessageType参数查询
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {

        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        App app = appService.getById(appId); // 获得app，根据appId查询到旧的App
        ThrowUtils.throwIf(app==null, ErrorCode.NOT_FOUND_ERROR,"应用不存在");
        boolean admin = loginUser != null && UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
//        boolean admin = userService.isAdmin(loginUser);
        ThrowUtils.throwIf(!admin, ErrorCode.NOT_LOGIN_ERROR,"无权访问该应用");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 根据查询条件拼接
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 根据分页查询返回结果
        // 查询分页数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 加载数据库历史记录到记忆中
     * @param appId
     * @param chatMemory
     * @param maxCount 最多加载多少条
     * @return
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            /**
             * 1.查询起始点设置为1而不是0,这是为了排除最新的用户消息。因为在对话流程中,用户消息被添加到数据
             * 库后,AI服务也会自动将用户消息添加到记忆中,如果不排除会导致消息重复。
             */
//            QueryWrapper queryWrapper = new QueryWrapper();
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount); // 从第一个开始查询, 因为加载AiService的时候
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按照时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按照时间顺序将消息添加到记忆中
            int loadedCount = 0;
            // 把historyList里面的历史对话取出来
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                }
                loadedCount++;
            }
            log.info("成功为 appId: {} 加载 {} 条历史消息", appId, loadedCount);
            return loadedCount;
        }catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


}
