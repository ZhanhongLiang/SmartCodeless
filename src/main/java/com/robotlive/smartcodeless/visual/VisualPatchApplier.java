package com.robotlive.smartcodeless.visual;

import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.visual.dto.VisualPatchResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class VisualPatchApplier {

    @Resource
    private VisualPatchPlanner visualPatchPlanner;

    public void apply(Path appRoot, VisualPatchResult patch) {
        if (patch == null || !Boolean.TRUE.equals(patch.getApplicable())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "补丁不可应用");
        }
        try {
            Path target = appRoot.resolve(patch.getTargetFile()).normalize();
            if (!target.startsWith(appRoot) || !Files.isRegularFile(target)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "目标文件非法");
            }
            String content = Files.readString(target, StandardCharsets.UTF_8);
            String currentHash = visualPatchPlanner.sha256(content);
            if (!currentHash.equals(patch.getBaseHash())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "源码文件已变化，请重新生成差异预览");
            }
            String beforeSnippet = patch.getBeforeSnippet();
            String afterSnippet = patch.getAfterSnippet();
            if (beforeSnippet == null || afterSnippet == null || !content.contains(beforeSnippet)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "补丁片段和当前文件不匹配");
            }
            String next = content.replace(beforeSnippet, afterSnippet);
            Path tmp = Files.createTempFile(target.getParent(), ".visual-edit-", ".tmp");
            Files.writeString(tmp, next, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用可视化补丁失败：" + e.getMessage());
        }
    }
}
