package com.robotlive.smartcodeless.ai.stream;

import cn.hutool.json.JSONUtil;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentStreamEmitter {

    private final String requestId;
    private final Long appId;
    private final FluxSink<ServerSentEvent<String>> sink;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public AgentStreamEmitter(String requestId, Long appId, FluxSink<ServerSentEvent<String>> sink) {
        this.requestId = requestId;
        this.appId = appId;
        this.sink = sink;
    }

    public String getRequestId() {
        return requestId;
    }

    public Long getAppId() {
        return appId;
    }

    public boolean isClosed() {
        return closed.get() || sink.isCancelled();
    }

    public void publish(AgentStreamEventType type, Map<String, Object> payload) {
        if (isClosed()) {
            return;
        }
        AgentStreamEvent event = AgentStreamEvent.builder()
                .requestId(requestId)
                .appId(appId)
                .type(type)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();
        sink.next(ServerSentEvent.<String>builder()
                .event(type.getEventName())
                .data(JSONUtil.toJsonStr(event))
                .build());
    }

    public void status(String stage, String message) {
        publish(AgentStreamEventType.STATUS, AgentStreamPayloads.status(stage, message));
    }

    public void complete() {
        if (closed.compareAndSet(false, true)) {
            AgentStreamEvent event = AgentStreamEvent.builder()
                    .requestId(requestId)
                    .appId(appId)
                    .type(AgentStreamEventType.DONE)
                    .timestamp(System.currentTimeMillis())
                    .payload(AgentStreamPayloads.done("completed"))
                    .build();
            sink.next(ServerSentEvent.<String>builder()
                    .event(AgentStreamEventType.DONE.getEventName())
                    .data(JSONUtil.toJsonStr(event))
                    .build());
            sink.complete();
        }
    }

    public void close() {
        closed.set(true);
    }
}
