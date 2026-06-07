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
import java.util.Locale;
import java.util.Map;

@Component
public class NpmScriptPolicyChecker {

    @Resource
    private QualityPolicyProperties policyProperties;

    @Resource
    private DependencyPolicyViolationMapper violationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QualityCheckResult check(Long taskId, Long appId, Path appRoot) {
        List<DependencyPolicyViolation> violations = new ArrayList<>();
        Path packageJson = appRoot.resolve("package.json").normalize();
        if (!Files.isRegularFile(packageJson)) {
            return QualityCheckResult.builder().status("SKIP").message("缺少 package.json，跳过脚本策略检查").violations(List.of()).build();
        }
        try {
            Map<String, Object> json = objectMapper.readValue(Files.readString(packageJson, StandardCharsets.UTF_8), new TypeReference<>() {
            });
            Object scriptsObject = json.get("scripts");
            if (scriptsObject instanceof Map<?, ?> scripts) {
                scripts.forEach((name, command) -> checkScript(taskId, appId, String.valueOf(name), String.valueOf(command), violations));
            }
        } catch (Exception e) {
            violations.add(violation(taskId, appId, "package.json", "", "SCRIPT_PARSE_ERROR", "ERROR", "脚本策略解析失败"));
        }
        violations.forEach(violationMapper::insert);
        return QualityCheckResult.builder()
                .status(violations.isEmpty() ? "PASS" : "FAIL")
                .failureCategory(violations.isEmpty() ? null : QualityFailureCategoryEnum.NPM_SCRIPT_POLICY_ERROR.name())
                .message(violations.isEmpty() ? "npm script 策略检查通过" : "npm script 策略发现 " + violations.size() + " 个问题")
                .violations(violations)
                .build();
    }

    private void checkScript(Long taskId, Long appId, String scriptName, String command, List<DependencyPolicyViolation> violations) {
        String normalizedName = scriptName.toLowerCase(Locale.ROOT);
        String normalizedCommand = command.toLowerCase(Locale.ROOT);
        for (String blocked : policyProperties.getBlockedScripts()) {
            String token = blocked.toLowerCase(Locale.ROOT);
            if (normalizedName.equals(token) || normalizedCommand.contains(token)) {
                violations.add(violation(taskId, appId, scriptName, command, "DANGEROUS_NPM_SCRIPT", "ERROR", "npm script 包含危险片段：" + blocked));
            }
        }
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
