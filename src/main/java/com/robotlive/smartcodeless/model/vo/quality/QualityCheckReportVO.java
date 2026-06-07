package com.robotlive.smartcodeless.model.vo.quality;

import com.robotlive.smartcodeless.model.entity.QualityCheckReport;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QualityCheckReportVO implements Serializable {

    private Long id;
    private Long taskId;
    private Long appId;
    private Long userId;
    private String dependencyPolicyStatus;
    private String scriptPolicyStatus;
    private String buildStatus;
    private String previewSmokeStatus;
    private String repairStatus;
    private String failureCategory;
    private Integer score;
    private String summaryJson;
    private String sanitizedLogSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static QualityCheckReportVO objToVo(QualityCheckReport report) {
        if (report == null) {
            return null;
        }
        QualityCheckReportVO vo = new QualityCheckReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
