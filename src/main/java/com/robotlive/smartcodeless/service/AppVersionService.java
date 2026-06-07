package com.robotlive.smartcodeless.service;

import com.mybatisflex.core.service.IService;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import com.robotlive.smartcodeless.model.entity.AppVersion;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.AppRollbackVO;
import com.robotlive.smartcodeless.model.vo.AppVersionVO;

import java.util.List;
import java.util.Optional;

public interface AppVersionService extends IService<AppVersion> {

    Optional<AppVersionVO> createAiGenerationVersion(Long appId, User loginUser, String prompt, AgentStreamEmitter emitter);

    List<AppVersionVO> listAppVersions(Long appId, User loginUser);

    AppVersionVO getAppVersionVO(Long versionId, User loginUser);

    AppRollbackVO rollback(Long appId, String commitId, String reason, User loginUser);
}

