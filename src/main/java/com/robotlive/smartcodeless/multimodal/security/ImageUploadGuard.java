package com.robotlive.smartcodeless.multimodal.security;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class ImageUploadGuard {

    private static final long MAX_SIZE = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传参考图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参考图片不能超过 8MB");
        }
        String contentType = file.getContentType();
        if (StrUtil.isBlank(contentType) || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 PNG、JPEG、WebP 图片，不支持 SVG");
        }
    }

    public String extensionOf(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的图片类型");
        };
    }
}
