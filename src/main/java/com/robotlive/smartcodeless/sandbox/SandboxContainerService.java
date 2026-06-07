package com.robotlive.smartcodeless.sandbox;

import com.robotlive.smartcodeless.build.VueProjectBuildExecutor;
import com.robotlive.smartcodeless.model.dto.SandboxStatusResponse;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;

public interface SandboxContainerService {

    SandboxRoute startForBuild(App app, VueProjectBuildExecutor.BuildExecutionResult buildResult, Long taskId);

    SandboxStatusResponse startAppPreview(Long appId, User loginUser);

    SandboxStatusResponse stopAppPreview(Long appId, User loginUser);

    SandboxStatusResponse getStatus(Long appId, User loginUser);
}
