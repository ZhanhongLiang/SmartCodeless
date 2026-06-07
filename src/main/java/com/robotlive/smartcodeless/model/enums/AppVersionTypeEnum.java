package com.robotlive.smartcodeless.model.enums;

import lombok.Getter;

@Getter
public enum AppVersionTypeEnum {

    AI_GENERATION("AI generation", "AI_GENERATION"),
    ROLLBACK("Rollback", "ROLLBACK"),
    MANUAL("Manual", "MANUAL");

    private final String text;

    private final String value;

    AppVersionTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}

