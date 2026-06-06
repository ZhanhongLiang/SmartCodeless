package com.robotlive.smartcodeless.ai.tools;

import com.robotlive.smartcodeless.ai.stream.AgentStreamContextHolder;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEventType;
import com.robotlive.smartcodeless.ai.stream.AgentStreamPayloads;
import org.springframework.stereotype.Component;

@Component
public class ToolEventPublisher {

    public long started(Long appId, String toolName, String summary) {
        AgentStreamEmitter emitter = AgentStreamContextHolder.get(appId);
        if (emitter != null) {
            emitter.publish(AgentStreamEventType.TOOL_CALL,
                    AgentStreamPayloads.toolCall(toolName, "started", summary, null));
        }
        return System.currentTimeMillis();
    }

    public void success(Long appId, String toolName, String summary, long startedAt) {
        AgentStreamEmitter emitter = AgentStreamContextHolder.get(appId);
        if (emitter != null) {
            emitter.publish(AgentStreamEventType.TOOL_CALL,
                    AgentStreamPayloads.toolCall(toolName, "success", summary, elapsed(startedAt)));
        }
    }

    public void failed(Long appId, String toolName, String summary, long startedAt) {
        AgentStreamEmitter emitter = AgentStreamContextHolder.get(appId);
        if (emitter != null) {
            emitter.publish(AgentStreamEventType.TOOL_CALL,
                    AgentStreamPayloads.toolCall(toolName, "failed", summary, elapsed(startedAt)));
        }
    }

    public void fileDiff(Long appId, DiffUtils.DiffResult diffResult) {
        AgentStreamEmitter emitter = AgentStreamContextHolder.get(appId);
        if (emitter != null && diffResult != null) {
            emitter.publish(AgentStreamEventType.FILE_DIFF,
                    AgentStreamPayloads.fileDiff(
                            diffResult.path(),
                            diffResult.changeType(),
                            diffResult.diff(),
                            diffResult.truncated()));
        }
    }

    private long elapsed(long startedAt) {
        return Math.max(0, System.currentTimeMillis() - startedAt);
    }
}
