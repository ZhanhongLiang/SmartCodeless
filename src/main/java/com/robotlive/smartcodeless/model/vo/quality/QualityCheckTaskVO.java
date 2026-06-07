package com.robotlive.smartcodeless.model.vo.quality;

import com.robotlive.smartcodeless.model.entity.QualityCheckTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QualityCheckTaskVO implements Serializable {

    private Long id;
    private Long appId;
    private Long userId;
    private Long versionId;
    private String commitId;
    private Long buildTaskId;
    private String triggerType;
    private String status;
    private String currentStage;
    private String failureCategory;
    private Integer score;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishTime;

    public static QualityCheckTaskVO objToVo(QualityCheckTask task) {
        if (task == null) {
            return null;
        }
        QualityCheckTaskVO vo = new QualityCheckTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
