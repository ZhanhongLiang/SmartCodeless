package com.robotlive.smartcodeless.model.enums;

import java.util.Set;

public enum SandboxStatusEnum {

    STARTING("starting", "STARTING"),
    RUNNING("running", "RUNNING"),
    FAILED("failed", "FAILED"),
    STOPPED("stopped", "STOPPED"),
    UNAVAILABLE("unavailable", "UNAVAILABLE");

    private static final Set<String> ACTIVE_VALUES = Set.of(STARTING.value, RUNNING.value);

    private final String value;

    private final String text;

    SandboxStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static boolean isActive(String value) {
        return ACTIVE_VALUES.contains(value);
    }
}
