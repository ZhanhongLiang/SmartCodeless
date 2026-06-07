package com.robotlive.smartcodeless.mcp;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class McpPermissionGuard {

    @Resource
    private AppService appService;

    public App requireAppAccess(Long appId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission for this app");
        }
        return app;
    }

    public Long appId(Map<String, Object> arguments) {
        Object appId = arguments == null ? null : arguments.get("appId");
        if (appId instanceof Number number) {
            return number.longValue();
        }
        if (appId instanceof String text && StrUtil.isNotBlank(text)) {
            return Long.parseLong(text);
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId is required");
    }

    public String stringArg(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        if (!(value instanceof String text) || StrUtil.isBlank(text)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, name + " is required");
        }
        return text;
    }

    public Path resolveAppFile(App app, String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "path is required");
        }
        Path input = Path.of(relativePath);
        if (input.isAbsolute() || relativePath.contains("..") || relativePath.matches("^[a-zA-Z]:.*")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "unsafe path");
        }
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path appRoot = root.resolve(app.getCodeGenType() + "_" + app.getId()).normalize();
        Path resolved = appRoot.resolve(relativePath).normalize();
        if (!appRoot.startsWith(root) || !resolved.startsWith(appRoot)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "path escapes app sandbox");
        }
        try {
            Path current = appRoot;
            for (Path part : appRoot.relativize(resolved)) {
                current = current.resolve(part);
                if (Files.exists(current) && Files.isSymbolicLink(current)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "symlink is not allowed");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "path validation failed");
        }
        return resolved;
    }

    public Path resolveAppRoot(App app) {
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path appRoot = root.resolve(app.getCodeGenType() + "_" + app.getId()).normalize();
        if (!appRoot.startsWith(root)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "path escapes code output root");
        }
        return appRoot;
    }
}
