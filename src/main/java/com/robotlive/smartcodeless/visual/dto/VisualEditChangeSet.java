package com.robotlive.smartcodeless.visual.dto;

import lombok.Data;

import java.util.Map;

@Data
public class VisualEditChangeSet {

    private String textContent;

    private String className;

    private Map<String, String> inlineStyle;

    private Map<String, String> attributes;
}
