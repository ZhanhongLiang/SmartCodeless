package com.robotlive.smartcodeless.model.enums;

import lombok.Getter;

@Getter
public enum QualityStageEnum {

    POLICY_CHECKING("POLICY_CHECKING", "依赖和脚本策略检查"),
    BUILD_CHECKING("BUILD_CHECKING", "构建和类型检查"),
    PREVIEW_CHECKING("PREVIEW_CHECKING", "预览冒烟检查"),
    REPORTING("REPORTING", "生成质量报告"),
    SELF_HEALING("SELF_HEALING", "自愈修复");

    private final String value;

    private final String text;

    QualityStageEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
