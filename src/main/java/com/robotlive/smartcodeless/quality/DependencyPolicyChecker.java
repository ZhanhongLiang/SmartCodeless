package com.robotlive.smartcodeless.quality;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robotlive.smartcodeless.mapper.DependencyPolicyViolationMapper;
import com.robotlive.smartcodeless.model.entity.DependencyPolicyViolation;
import com.robotlive.smartcodeless.model.enums.QualityFailureCategoryEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DependencyPolicyChecker {

    @Resource
    private QualityPolicyProperties policyProperties;

    @Resource
    private DependencyPolicyViolationMapper violationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QualityCheckResult check(Long taskId, Long appId, Path appRoot) {
        List<DependencyPolicyViolation> violations = new ArrayList<>();
        Path packageJson = appRoot.resolve("package.json").normalize();
        if (!Files.isRegularFile(packageJson)) {
            violations.add(violation(taskId, appId, "package.json", "", "PACKAGE_JSON_MISSING", "ERROR", "缺少 package.json"));
            return persistAndResult(violations);
        }
        try {
            Map<String, Object> json = objectMapper.readValue(Files.readString(packageJson, StandardCharsets.UTF_8), new TypeReference<>() {
            });
            collectDependencyViolations(taskId, appId, json, "dependencies", violations);
            collectDependencyViolations(taskId, appId, json, "devDependencies", violations);
        } catch (Exception e) {
            violations.add(violation(taskId, appId, "package.json", "", "PACKAGE_JSON_PARSE_ERROR", "ERROR", "package.json 解析失败"));
        }
        return persistAndResult(violations);
    }

    @SuppressWarnings("unchecked")
    private void collectDependencyViolations(Long taskId, Long appId, Map<String, Object> json, String section, List<DependencyPolicyViolation> violations) {
        Object value = json.get(section);
        if (!(value instanceof Map<?, ?> rawDeps)) {
            return;
        }
        Map<String, Object> deps = (Map<String, Object>) rawDeps;
        deps.forEach((name, version) -> {
            String versionSpec = String.valueOf(version);
            if (!policyProperties.isDependencyAllowed(name)) {
                violations.add(violation(taskId, appId, name, versionSpec, "UNKNOWN_DEPENDENCY", "ERROR", "依赖不在 npm allowlist 中"));
            }
            if (versionSpec.contains("http://") || versionSpec.contains("https://") || versionSpec.contains("git+") || versionSpec.contains("file:")) {
                violations.add(violation(taskId, appId, name, versionSpec, "SUSPICIOUS_VERSION", "ERROR", "依赖版本来源不安全"));
            }
        });
    }

    private QualityCheckResult persistAndResult(List<DependencyPolicyViolation> violations) {
        violations.forEach(violationMapper::insert);
        return QualityCheckResult.builder()
                .status(violations.isEmpty() ? "PASS" : "FAIL")
                .failureCategory(violations.isEmpty() ? null : QualityFailureCategoryEnum.DEPENDENCY_POLICY_ERROR.name())
                .message(violations.isEmpty() ? "依赖策略检查通过" : "依赖策略检查发现 " + violations.size() + " 个问题")
                .violations(violations)
                .build();
    }

    private DependencyPolicyViolation violation(Long taskId, Long appId, String packageName, String versionSpec, String type, String severity, String message) {
        return DependencyPolicyViolation.builder()
                .taskId(taskId)
                .appId(appId)
                .packageName(packageName)
                .versionSpec(versionSpec)
                .violationType(type)
                .severity(severity)
                .message(message)
                .createTime(LocalDateTime.now())
                .build();
    }
}
