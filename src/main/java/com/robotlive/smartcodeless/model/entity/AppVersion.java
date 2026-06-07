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
@Table("app_version")
public class AppVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    @Column("roundNo")
    private Integer roundNo;

    @Column("commitId")
    private String commitId;

    @Column("commitMessage")
    private String commitMessage;

    @Column("promptSummary")
    private String promptSummary;

    @Column("codeGenType")
    private String codeGenType;

    @Column("versionType")
    private String versionType;

    @Column("rollbackFromCommitId")
    private String rollbackFromCommitId;

    @Column("buildTaskId")
    private Long buildTaskId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

