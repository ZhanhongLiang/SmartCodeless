package com.robotlive.smartcodeless.mcp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class McpJsonRpcRequest implements Serializable {

    private String jsonrpc;

    private Object id;

    private String method;

    private Map<String, Object> params;

    private static final long serialVersionUID = 1L;
}

