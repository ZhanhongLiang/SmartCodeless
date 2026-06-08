package com.robotlive.smartcodeless.multimodal.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UiComponentNode implements Serializable {

    private String type;

    private String name;

    private List<String> text = new ArrayList<>();

    private String position;

    private List<String> styleHints = new ArrayList<>();
}
