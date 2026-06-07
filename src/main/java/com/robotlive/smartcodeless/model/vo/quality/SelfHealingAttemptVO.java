package com.robotlive.smartcodeless.model.vo.quality;

import com.robotlive.smartcodeless.model.entity.SelfHealingAttempt;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SelfHealingAttemptVO implements Serializable {

    private Long id;
    private Long taskId;
    private Long appId;
    private Long userId;
    private Integer attemptNo;
    private String status;
    private String failureCategory;
    private String patchTargetFile;
    private String beforeSnippet;
    private String afterSnippet;
    private String unifiedDiff;
    private String diagnosis;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishTime;

    public static SelfHealingAttemptVO objToVo(SelfHealingAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        SelfHealingAttemptVO vo = new SelfHealingAttemptVO();
        BeanUtils.copyProperties(attempt, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
