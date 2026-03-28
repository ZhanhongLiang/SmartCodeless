package com.robotlive.smartcodeless.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 * 用户初始填入initPrompt, 会跟模型进行对话
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
} 