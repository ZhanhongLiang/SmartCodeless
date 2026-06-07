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
@Table("self_healing_attempt")
public class SelfHealingAttempt implements Serializable {

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

    @Column("attemptNo")
    private Integer attemptNo;

    private String status;

    @Column("failureCategory")
    private String failureCategory;

    @Column("patchTargetFile")
    private String patchTargetFile;

    @Column("beforeSnippet")
    private String beforeSnippet;

    @Column("afterSnippet")
    private String afterSnippet;

    @Column("unifiedDiff")
    private String unifiedDiff;

    private String diagnosis;

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
