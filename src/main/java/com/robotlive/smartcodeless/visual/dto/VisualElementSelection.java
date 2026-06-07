package com.robotlive.smartcodeless.visual.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VisualElementSelection {

    private String tagName;

    private String id;

    private String className;

    private String textContent;

    private String selector;

    private String pagePath;

    private List<VisualEditAttribute> attributes;

    private Map<String, String> computedStyle;

    private Map<String, String> inlineStyle;

    private String parentSelector;

    private Integer childIndex;

    private String nearbyText;

    private String componentHint;

    private String href;

    private String src;

    private String ariaLabel;
}
