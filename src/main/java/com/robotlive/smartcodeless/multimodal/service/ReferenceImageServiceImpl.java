package com.robotlive.smartcodeless.multimodal.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.mapper.ReferenceImageMapper;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.ReferenceImage;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.ReferenceImageUploadVO;
import com.robotlive.smartcodeless.multimodal.security.ImageUploadGuard;
import com.robotlive.smartcodeless.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class ReferenceImageServiceImpl extends ServiceImpl<ReferenceImageMapper, ReferenceImage>
        implements ReferenceImageService {

    private static final String IMAGE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/reference_images";

    @Resource
    private ImageUploadGuard imageUploadGuard;

    @Resource
    private AppService appService;

    @Override
    public ReferenceImageUploadVO upload(MultipartFile file, Long appId, User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        imageUploadGuard.validate(file);
        if (appId != null) {
            assertCanUseApp(appId, loginUser);
        }
        String mimeType = file.getContentType().toLowerCase();
        String imageId = IdUtil.fastSimpleUUID();
        String extension = imageUploadGuard.extensionOf(mimeType);
        File dir = new File(IMAGE_ROOT_DIR);
        FileUtil.mkdir(dir);
        File target = new File(dir, imageId + extension);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "参考图片保存失败");
        }
        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(target);
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (IOException ignored) {
        }
        ReferenceImage referenceImage = ReferenceImage.builder()
                .id(imageId)
                .appId(appId)
                .userId(loginUser.getId())
                .originalName(StrUtil.sub(file.getOriginalFilename(), 0, 255))
                .storagePath(target.getAbsolutePath())
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .width(width)
                .height(height)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        boolean saved = this.save(referenceImage);
        if (!saved) {
            FileUtil.del(target);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "参考图片记录保存失败");
        }
        return ReferenceImageUploadVO.builder()
                .imageId(imageId)
                .originalName(referenceImage.getOriginalName())
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .width(width)
                .height(height)
                .previewUrl("/api/app/multimodal/image/" + imageId)
                .build();
    }

    @Override
    public ReferenceImage getAuthorizedImage(String imageId, User loginUser) {
        if (StrUtil.isBlank(imageId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "imageId 不能为空");
        }
        ReferenceImage image = this.getById(imageId);
        if (image == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参考图片不存在");
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!image.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该参考图片");
        }
        return image;
    }

    @Override
    public Path getImagePath(ReferenceImage image) {
        File root = new File(IMAGE_ROOT_DIR);
        File file = new File(image.getStoragePath());
        try {
            Path rootPath = root.getCanonicalFile().toPath();
            Path filePath = file.getCanonicalFile().toPath();
            if (!filePath.startsWith(rootPath)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "非法图片路径");
            }
            return filePath;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "参考图片路径解析失败");
        }
    }

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> readImage(String imageId, User loginUser) {
        ReferenceImage image = getAuthorizedImage(imageId, loginUser);
        Path path = getImagePath(image);
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参考图片文件不存在");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .body(resource);
    }

    @Override
    public void bindToApp(String imageId, Long appId, User loginUser) {
        if (StrUtil.isBlank(imageId) || appId == null) {
            return;
        }
        ReferenceImage image = getAuthorizedImage(imageId, loginUser);
        assertCanUseApp(appId, loginUser);
        ReferenceImage update = new ReferenceImage();
        update.setId(image.getId());
        update.setAppId(appId);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);
    }

    private void assertCanUseApp(Long appId, User loginUser) {
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权使用该应用");
        }
    }
}
