package com.robotlive.smartcodeless.quality;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QualityPolicyProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Set<String> dependencies = Set.of();

    private Set<String> devDependencies = Set.of();

    private Set<String> blockedScripts = Set.of();

    @PostConstruct
    public void load() throws Exception {
        ClassPathResource resource = new ClassPathResource("policy/npm-allowlist.json");
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<String>> data = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            dependencies = new HashSet<>(data.getOrDefault("dependencies", List.of()));
            devDependencies = new HashSet<>(data.getOrDefault("devDependencies", List.of()));
            blockedScripts = new HashSet<>(data.getOrDefault("blockedScripts", List.of()));
        }
    }

    public boolean isDependencyAllowed(String name) {
        return dependencies.contains(name) || devDependencies.contains(name);
    }

    public Set<String> getBlockedScripts() {
        return blockedScripts;
    }
}
