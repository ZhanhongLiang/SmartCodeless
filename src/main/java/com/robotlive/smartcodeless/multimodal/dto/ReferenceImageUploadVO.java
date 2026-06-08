package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ReferenceImageUploadVO implements Serializable {

    private String imageId;

    private String originalName;

    private String mimeType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private String previewUrl;
}
