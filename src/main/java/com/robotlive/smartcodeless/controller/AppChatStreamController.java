package com.robotlive.smartcodeless.controller;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.ai.stream.AgentStreamPublisher;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.service.MultimodalCodeGenFacade;
import com.robotlive.smartcodeless.ratelimiter.annotation.RateLimit;
import com.robotlive.smartcodeless.ratelimiter.enums.RateLimitType;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/app")
public class AppChatStreamController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private AgentStreamPublisher agentStreamPublisher;

    @Resource
    private MultimodalCodeGenFacade multimodalCodeGenFacade;

    @GetMapping(value = "/chat/gen/code/v2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI chat requests are too frequent, please try again later")
    public Flux<ServerSentEvent<String>> chatToGenCodeV2(@RequestParam Long appId,
                                                         @RequestParam String message,
                                                         @RequestParam(required = false) String clientRequestId,
                                                         @RequestParam(required = false) String imageId,
                                                         @RequestParam(defaultValue = "true") boolean enableDiff,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is invalid");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message cannot be blank");
        User loginUser = userService.getLoginUser(request);
        return agentStreamPublisher.publish(appId, clientRequestId,
                emitter -> {
                    String generationPrompt = multimodalCodeGenFacade.enrichPromptWithLayout(message, imageId, loginUser, emitter);
                    appService.chatToGenCodeV2(appId, message, generationPrompt, loginUser, emitter, enableDiff);
                });
    }
}
