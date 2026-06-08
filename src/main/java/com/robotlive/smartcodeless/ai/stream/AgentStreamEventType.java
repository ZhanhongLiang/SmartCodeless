package com.robotlive.smartcodeless.ai.stream;

public enum AgentStreamEventType {
    THOUGHT("thought"),
    STATUS("status"),
    TOOL_CALL("tool_call"),
    VISION_ANALYSIS("vision_analysis"),
    LAYOUT_PLAN("layout_plan"),
    FILE_DIFF("file_diff"),
    CODE_BLOCK("code_block"),
    MESSAGE("message"),
    DONE("done"),
    ERROR("error"),
    BUSINESS_ERROR("business-error");

    private final String eventName;

    AgentStreamEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
