package com.robotlive.smartcodeless.sandbox;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SandboxSecurityPolicy {

    public Path validateDistDir(String distDir) {
        if (StrUtil.isBlank(distDir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "dist directory is required");
        }
        try {
            Path deployRoot = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR).toAbsolutePath().normalize();
            Path outputRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
            Path resolved = Path.of(distDir).toAbsolutePath().normalize();
            boolean inAllowedRoot = resolved.startsWith(deployRoot) || resolved.startsWith(outputRoot);
            if (!inAllowedRoot || !Files.isDirectory(resolved)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "invalid sandbox dist directory");
            }
            if (Files.isSymbolicLink(resolved)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "sandbox dist directory cannot be symlink");
            }
            Path indexFile = resolved.resolve("index.html").normalize();
            if (!indexFile.startsWith(resolved) || !Files.isRegularFile(indexFile)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "index.html not found in sandbox dist directory");
            }
            return resolved;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "validate sandbox dist directory failed");
        }
    }

    public String normalizeProxyPath(String path) {
        String requestPath = StrUtil.blankToDefault(path, "/");
        if (requestPath.contains("..")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "unsafe preview path");
        }
        if (!requestPath.startsWith("/")) {
            requestPath = "/" + requestPath;
        }
        Path normalized = Path.of(requestPath).normalize();
        String safePath = normalized.toString().replace("\\", "/");
        if (safePath.contains("..")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "unsafe preview path");
        }
        if (!safePath.startsWith("/")) {
            safePath = "/" + safePath;
        }
        return "/".equals(safePath) ? "/index.html" : safePath;
    }
}
