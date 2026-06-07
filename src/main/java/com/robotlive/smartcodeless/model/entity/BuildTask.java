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
@Table("build_task")
public class BuildTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    @Column("triggerType")
    private String triggerType;

    private String status;

    @Column("sourceDir")
    private String sourceDir;

    @Column("distDir")
    private String distDir;

    @Column("deployKey")
    private String deployKey;

    @Column("errorMessage")
    private String errorMessage;

    @Column("queuedTime")
    private LocalDateTime queuedTime;

    @Column("startTime")
    private LocalDateTime startTime;

    @Column("finishTime")
    private LocalDateTime finishTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
