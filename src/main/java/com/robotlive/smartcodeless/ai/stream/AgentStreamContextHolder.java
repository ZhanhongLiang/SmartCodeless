package com.robotlive.smartcodeless.ai.stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentStreamContextHolder {

    private static final ThreadLocal<AgentStreamEmitter> CURRENT = new ThreadLocal<>();
    private static final Map<Long, AgentStreamEmitter> BY_APP_ID = new ConcurrentHashMap<>();

    private AgentStreamContextHolder() {
    }

    public static void set(AgentStreamEmitter emitter) {
        CURRENT.set(emitter);
        if (emitter != null && emitter.getAppId() != null) {
            BY_APP_ID.put(emitter.getAppId(), emitter);
        }
    }

    public static AgentStreamEmitter get() {
        AgentStreamEmitter emitter = CURRENT.get();
        if (emitter != null) {
            return emitter;
        }
        return null;
    }

    public static AgentStreamEmitter get(Long appId) {
        AgentStreamEmitter emitter = CURRENT.get();
        if (emitter != null) {
            return emitter;
        }
        return appId == null ? null : BY_APP_ID.get(appId);
    }

    public static void clear() {
        AgentStreamEmitter emitter = CURRENT.get();
        CURRENT.remove();
        if (emitter != null && emitter.getAppId() != null) {
            BY_APP_ID.remove(emitter.getAppId(), emitter);
        }
    }

    public static void clear(Long appId) {
        CURRENT.remove();
        if (appId != null) {
            BY_APP_ID.remove(appId);
        }
    }
}
