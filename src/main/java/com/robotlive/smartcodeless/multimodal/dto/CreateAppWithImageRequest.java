package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateAppWithImageRequest implements Serializable {

    private String initPrompt;

    private String imageId;
}
