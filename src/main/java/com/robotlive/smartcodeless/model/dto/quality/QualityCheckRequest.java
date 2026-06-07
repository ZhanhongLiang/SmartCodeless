package com.robotlive.smartcodeless.model.dto.quality;

import lombok.Data;

import java.io.Serializable;

@Data
public class QualityCheckRequest implements Serializable {

    private Long appId;

    private String triggerType;

    private Boolean autoRepair;

    private static final long serialVersionUID = 1L;
}
