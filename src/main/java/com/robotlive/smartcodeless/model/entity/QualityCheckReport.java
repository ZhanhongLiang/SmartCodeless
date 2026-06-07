package com.robotlive.smartcodeless.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("quality_check_report")
public class QualityCheckReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("taskId")
    private Long taskId;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    @Column("dependencyPolicyStatus")
    private String dependencyPolicyStatus;

    @Column("scriptPolicyStatus")
    private String scriptPolicyStatus;

    @Column("buildStatus")
    private String buildStatus;

    @Column("previewSmokeStatus")
    private String previewSmokeStatus;

    @Column("repairStatus")
    private String repairStatus;

    @Column("failureCategory")
    private String failureCategory;

    private Integer score;

    @Column("summaryJson")
    private String summaryJson;

    @Column("sanitizedLogSummary")
    private String sanitizedLogSummary;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
