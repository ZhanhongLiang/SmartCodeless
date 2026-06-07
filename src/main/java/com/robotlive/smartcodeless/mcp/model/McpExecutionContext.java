package com.robotlive.smartcodeless.mcp.model;

import com.robotlive.smartcodeless.model.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpExecutionContext {

    private String requestId;

    private User loginUser;
}

