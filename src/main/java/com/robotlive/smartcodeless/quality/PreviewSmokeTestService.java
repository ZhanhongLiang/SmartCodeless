package com.robotlive.smartcodeless.quality;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.model.dto.SandboxStatusResponse;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.sandbox.SandboxContainerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class PreviewSmokeTestService {

    @Resource
    private SandboxContainerService sandboxContainerService;

    public QualityCheckResult check(App app, User loginUser) {
        try {
            SandboxStatusResponse status = sandboxContainerService.getStatus(app.getId(), loginUser);
            if (status != null && "running".equals(status.getStatus())) {
                return QualityCheckResult.builder().status("PASS").message("沙盒预览运行中").build();
            }
        } catch (Exception ignored) {
            // Fall back to static deployment signal below.
        }
        if (StrUtil.isNotBlank(app.getDeployKey())) {
            return QualityCheckResult.builder().status("PASS").message("静态预览地址存在").build();
        }
        return QualityCheckResult.builder()
                .status("FAIL")
                .failureCategory("PREVIEW_HTTP_ERROR")
                .message("未发现可用预览地址")
                .build();
    }
}
