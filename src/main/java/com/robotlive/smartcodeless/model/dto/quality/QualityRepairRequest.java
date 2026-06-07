package com.robotlive.smartcodeless.model.dto.quality;

import lombok.Data;

import java.io.Serializable;

@Data
public class QualityRepairRequest implements Serializable {

    private Long taskId;

    private Long appId;

    private static final long serialVersionUID = 1L;
}
