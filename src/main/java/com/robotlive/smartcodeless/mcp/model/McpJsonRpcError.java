package com.robotlive.smartcodeless.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcError implements Serializable {

    private int code;

    private String message;

    private Object data;

    private static final long serialVersionUID = 1L;
}

