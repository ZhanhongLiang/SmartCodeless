package com.robotlive.smartcodeless.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Set;

@Getter
public enum BuildStatusEnum {

    NONE("none", "NONE"),
    QUEUED("queued", "QUEUED"),
    RUNNING("running", "RUNNING"),
    SUCCESS("success", "SUCCESS"),
    FAILED("failed", "FAILED"),
    CANCELED("canceled", "CANCELED");

    private static final Set<String> TERMINAL_VALUES = Set.of(SUCCESS.value, FAILED.value, CANCELED.value);

    private final String text;

    private final String value;

    BuildStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static BuildStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (BuildStatusEnum item : BuildStatusEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public static boolean isTerminal(String value) {
        return TERMINAL_VALUES.contains(value);
    }
}
