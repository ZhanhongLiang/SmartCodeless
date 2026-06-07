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
@Table("build_log")
public class BuildLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("taskId")
    private Long taskId;

    @Column("appId")
    private Long appId;

    @Column("logType")
    private String logType;

    private String content;

    @Column("lineNo")
    private Integer lineNo;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
