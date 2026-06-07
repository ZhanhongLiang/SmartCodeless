package com.robotlive.smartcodeless.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcResponse implements Serializable {

    @Builder.Default
    private String jsonrpc = "2.0";

    private Object id;

    private Object result;

    private McpJsonRpcError error;

    public static McpJsonRpcResponse success(Object id, Object result) {
        return McpJsonRpcResponse.builder().id(id).result(result).build();
    }

    public static McpJsonRpcResponse error(Object id, int code, String message, Object data) {
        return McpJsonRpcResponse.builder()
                .id(id)
                .error(McpJsonRpcError.builder().code(code).message(message).data(data).build())
                .build();
    }

    private static final long serialVersionUID = 1L;
}

