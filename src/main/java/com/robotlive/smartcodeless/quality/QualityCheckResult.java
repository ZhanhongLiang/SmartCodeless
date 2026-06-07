package com.robotlive.smartcodeless.quality;

import com.robotlive.smartcodeless.model.entity.DependencyPolicyViolation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QualityCheckResult {

    private String status;

    private String failureCategory;

    private String message;

    private List<DependencyPolicyViolation> violations;
}
