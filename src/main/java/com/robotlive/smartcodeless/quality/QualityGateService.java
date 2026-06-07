package com.robotlive.smartcodeless.quality;

import com.robotlive.smartcodeless.model.dto.quality.QualityCheckRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.quality.DependencyPolicyViolationVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckReportVO;
import com.robotlive.smartcodeless.model.vo.quality.QualityCheckTaskVO;

import java.util.List;

public interface QualityGateService {

    QualityCheckTaskVO submitQualityCheck(QualityCheckRequest request, User loginUser);

    QualityCheckTaskVO getTask(Long taskId, User loginUser);

    List<QualityCheckReportVO> listReports(Long appId, User loginUser);

    QualityCheckReportVO latestReport(Long appId, User loginUser);

    List<DependencyPolicyViolationVO> listViolations(Long taskId, User loginUser);
}
