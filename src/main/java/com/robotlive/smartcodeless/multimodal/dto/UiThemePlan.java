package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UiThemePlan implements Serializable {

    private String primaryColor;

    private String backgroundColor;

    private String textColor;

    private List<String> styleKeywords = new ArrayList<>();
}
