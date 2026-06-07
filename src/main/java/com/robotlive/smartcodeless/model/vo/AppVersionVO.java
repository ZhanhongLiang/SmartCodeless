package com.robotlive.smartcodeless.model.vo;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.model.entity.AppVersion;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppVersionVO implements Serializable {

    private Long id;

    private Long appId;

    private Long userId;

    private Integer roundNo;

    private String commitId;

    private String shortCommitId;

    private String commitMessage;

    private String promptSummary;

    private String codeGenType;

    private String versionType;

    private String rollbackFromCommitId;

    private Long buildTaskId;

    private LocalDateTime createTime;

    public static AppVersionVO objToVo(AppVersion appVersion) {
        if (appVersion == null) {
            return null;
        }
        AppVersionVO vo = new AppVersionVO();
        vo.setId(appVersion.getId());
        vo.setAppId(appVersion.getAppId());
        vo.setUserId(appVersion.getUserId());
        vo.setRoundNo(appVersion.getRoundNo());
        vo.setCommitId(appVersion.getCommitId());
        vo.setShortCommitId(StrUtil.subPre(appVersion.getCommitId(), 8));
        vo.setCommitMessage(appVersion.getCommitMessage());
        vo.setPromptSummary(appVersion.getPromptSummary());
        vo.setCodeGenType(appVersion.getCodeGenType());
        vo.setVersionType(appVersion.getVersionType());
        vo.setRollbackFromCommitId(appVersion.getRollbackFromCommitId());
        vo.setBuildTaskId(appVersion.getBuildTaskId());
        vo.setCreateTime(appVersion.getCreateTime());
        return vo;
    }

    private static final long serialVersionUID = 1L;
}

