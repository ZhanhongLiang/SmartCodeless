package com.robotlive.smartcodeless.sandbox;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

@Component
public class SandboxNamingPolicy {

    public String containerName(Long appId, String deployKey) {
        String safeKey = StrUtil.blankToDefault(deployKey, "preview")
                .replaceAll("[^a-zA-Z0-9_-]", "")
                .toLowerCase();
        if (safeKey.length() > 16) {
            safeKey = safeKey.substring(0, 16);
        }
        return "omnicodegen_app_" + appId + "_" + safeKey;
    }
}
