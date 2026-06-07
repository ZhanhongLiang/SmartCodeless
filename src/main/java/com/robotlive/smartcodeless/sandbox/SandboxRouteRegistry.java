package com.robotlive.smartcodeless.sandbox;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SandboxRouteRegistry {

    private final Map<Long, SandboxRoute> routesByAppId = new ConcurrentHashMap<>();

    private final Map<String, SandboxRoute> routesByDeployKey = new ConcurrentHashMap<>();

    public void register(SandboxRoute route) {
        if (route == null || route.getAppId() == null) {
            return;
        }
        routesByAppId.put(route.getAppId(), route);
        if (route.getDeployKey() != null) {
            routesByDeployKey.put(route.getDeployKey(), route);
        }
    }

    public Optional<SandboxRoute> getByAppId(Long appId) {
        return Optional.ofNullable(routesByAppId.get(appId));
    }

    public Optional<SandboxRoute> getByDeployKey(String deployKey) {
        return Optional.ofNullable(routesByDeployKey.get(deployKey));
    }

    public void remove(Long appId) {
        SandboxRoute removed = routesByAppId.remove(appId);
        if (removed != null && removed.getDeployKey() != null) {
            routesByDeployKey.remove(removed.getDeployKey());
        }
    }
}
