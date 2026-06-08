package com.robotlive.smartcodeless.multimodal.service;

import com.robotlive.smartcodeless.model.entity.ReferenceImage;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.ReferenceImageUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ReferenceImageService {

    ReferenceImageUploadVO upload(MultipartFile file, Long appId, User loginUser);

    ReferenceImage getAuthorizedImage(String imageId, User loginUser);

    Path getImagePath(ReferenceImage image);

    ResponseEntity<Resource> readImage(String imageId, User loginUser);

    void bindToApp(String imageId, Long appId, User loginUser);
}
