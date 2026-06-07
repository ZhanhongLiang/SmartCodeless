package com.robotlive.smartcodeless.service;

import com.mybatisflex.core.service.IService;
import com.robotlive.smartcodeless.model.entity.BuildTask;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskVO;

public interface BuildTaskService extends IService<BuildTask> {

    BuildTaskSubmitVO submitBuildTask(Long appId, User loginUser, String triggerType);

    BuildTaskVO getBuildTaskVO(Long taskId, User loginUser);

    boolean executeBuildTask(Long taskId);
}
