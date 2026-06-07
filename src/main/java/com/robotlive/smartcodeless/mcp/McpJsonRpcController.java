package com.robotlive.smartcodeless.mcp;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.mcp.model.McpErrorCodes;
import com.robotlive.smartcodeless.mcp.model.McpExecutionContext;
import com.robotlive.smartcodeless.mcp.model.McpJsonRpcRequest;
import com.robotlive.smartcodeless.mcp.model.McpJsonRpcResponse;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpJsonRpcController {

    @Resource
    private McpMethodRouter methodRouter;

    @Resource
    private UserService userService;

    @PostMapping
    public McpJsonRpcResponse handle(@RequestBody McpJsonRpcRequest rpcRequest, HttpServletRequest request) {
        Object id = rpcRequest == null ? null : rpcRequest.getId();
        try {
            validateRequest(rpcRequest);
            User loginUser = userService.getLoginUser(request);
            McpExecutionContext context = McpExecutionContext.builder()
                    .requestId(String.valueOf(rpcRequest.getId()))
                    .loginUser(loginUser)
                    .build();
            Object result = methodRouter.route(rpcRequest.getMethod(), rpcRequest.getParams(), context);
            return McpJsonRpcResponse.success(id, result);
        } catch (BusinessException e) {
            return McpJsonRpcResponse.error(id, toRpcCode(e), e.getMessage(), null);
        } catch (Exception e) {
            log.error("MCP JSON-RPC failed", e);
            return McpJsonRpcResponse.error(id, McpErrorCodes.INTERNAL_ERROR, "Internal error", null);
        }
    }

    private void validateRequest(McpJsonRpcRequest request) {
        if (request == null || !"2.0".equals(request.getJsonrpc()) || StrUtil.isBlank(request.getMethod())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid JSON-RPC request");
        }
    }

    private int toRpcCode(BusinessException e) {
        if (e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            return McpErrorCodes.UNAUTHORIZED;
        }
        if (e.getCode() == ErrorCode.NO_AUTH_ERROR.getCode() || e.getCode() == ErrorCode.FORBIDDEN_ERROR.getCode()) {
            return McpErrorCodes.FORBIDDEN;
        }
        if (e.getCode() == ErrorCode.NOT_FOUND_ERROR.getCode()) {
            return McpErrorCodes.METHOD_NOT_FOUND;
        }
        if (e.getCode() == ErrorCode.PARAMS_ERROR.getCode()) {
            return McpErrorCodes.INVALID_PARAMS;
        }
        return McpErrorCodes.INTERNAL_ERROR;
    }
}

