package com.robotlive.smartcodeless.ai.stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStreamEvent {

    private String requestId;

    private Long appId;

    private AgentStreamEventType type;

    private Long timestamp;

    private Map<String, Object> payload;
}
