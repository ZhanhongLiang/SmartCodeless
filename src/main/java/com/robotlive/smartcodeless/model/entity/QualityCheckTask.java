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
@Table("quality_check_task")
public class QualityCheckTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    @Column("versionId")
    private Long versionId;

    @Column("commitId")
    private String commitId;

    @Column("buildTaskId")
    private Long buildTaskId;

    @Column("triggerType")
    private String triggerType;

    private String status;

    @Column("currentStage")
    private String currentStage;

    @Column("failureCategory")
    private String failureCategory;

    private Integer score;

    @Column("errorMessage")
    private String errorMessage;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column("finishTime")
    private LocalDateTime finishTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
