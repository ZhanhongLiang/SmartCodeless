package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MultimodalChatRequest implements Serializable {

    private Long appId;

    private String message;

    private String imageId;
}
