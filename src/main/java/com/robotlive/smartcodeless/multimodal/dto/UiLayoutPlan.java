package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class UiLayoutPlan implements Serializable {

    private String pageType = "unknown";

    private String language = "zh-CN";

    private String imageDescription;

    private UiThemePlan theme = new UiThemePlan();

    private Map<String, Object> layout = new LinkedHashMap<>();

    private List<UiComponentNode> components = new ArrayList<>();

    private List<Map<String, Object>> assets = new ArrayList<>();

    private Double confidence = 0.0;

    private List<String> warnings = new ArrayList<>();
}
