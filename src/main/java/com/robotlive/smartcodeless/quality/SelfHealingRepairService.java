package com.robotlive.smartcodeless.quality;

import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.quality.SelfHealingAttemptVO;

import java.util.List;

public interface SelfHealingRepairService {

    SelfHealingAttemptVO createRepairAttempt(Long taskId, User loginUser);

    SelfHealingAttemptVO applyRepairAttempt(Long attemptId, User loginUser);

    List<SelfHealingAttemptVO> listAttempts(Long taskId, User loginUser);
}
