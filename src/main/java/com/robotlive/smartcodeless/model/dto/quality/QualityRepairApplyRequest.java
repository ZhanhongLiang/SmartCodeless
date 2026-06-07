package com.robotlive.smartcodeless.model.dto.quality;

import lombok.Data;

import java.io.Serializable;

@Data
public class QualityRepairApplyRequest implements Serializable {

    private Long attemptId;

    private static final long serialVersionUID = 1L;
}
