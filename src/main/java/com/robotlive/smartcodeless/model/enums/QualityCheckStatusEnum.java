package com.robotlive.smartcodeless.model.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum QualityCheckStatusEnum {

    QUEUED("QUEUED", "排队中"),
    RUNNING("RUNNING", "运行中"),
    SUCCESS("SUCCESS", "检查通过"),
    FAILED("FAILED", "检查失败"),
    REPAIRABLE("REPAIRABLE", "可自愈修复");

    private static final Set<String> TERMINAL_VALUES = Set.of(SUCCESS.value, FAILED.value, REPAIRABLE.value);

    private final String value;

    private final String text;

    QualityCheckStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static boolean isTerminal(String value) {
        return TERMINAL_VALUES.contains(value);
    }
}
