package com.robotlive.smartcodeless.ai.stream;

import com.robotlive.smartcodeless.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
@Component
public class AgentStreamPublisher {

    private final ExecutorService agentExecutor;

    public AgentStreamPublisher(@Qualifier("agentExecutor") ExecutorService agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    public Flux<ServerSentEvent<String>> publish(Long appId, String clientRequestId, Consumer<AgentStreamEmitter> task) {
        String requestId = clientRequestId == null || clientRequestId.isBlank()
                ? UUID.randomUUID().toString()
                : clientRequestId;
        return Flux.create(sink -> {
            AgentStreamEmitter emitter = new AgentStreamEmitter(requestId, appId, sink);
            sink.onCancel(() -> {
                emitter.close();
                AgentStreamContextHolder.clear(appId);
            });
            sink.onDispose(() -> {
                emitter.close();
                AgentStreamContextHolder.clear(appId);
            });
            agentExecutor.execute(() -> {
                AgentStreamContextHolder.set(emitter);
                try {
                    task.accept(emitter);
                    emitter.complete();
                } catch (BusinessException e) {
                    emitter.publish(AgentStreamEventType.BUSINESS_ERROR,
                            AgentStreamPayloads.error(String.valueOf(e.getCode()), e.getMessage()));
                    sink.complete();
                } catch (Throwable e) {
                    log.error("Agent stream failed, requestId={}, appId={}", requestId, appId, e);
                    emitter.publish(AgentStreamEventType.ERROR,
                            AgentStreamPayloads.error("SYSTEM_ERROR",
                                    "生成失败：" + AgentStreamPayloads.errorMessage(e)));
                    sink.complete();
                } finally {
                    emitter.close();
                    AgentStreamContextHolder.clear(appId);
                }
            });
        });
    }
}
