package com.robotlive.smartcodeless.model.vo;

import com.robotlive.smartcodeless.model.entity.BuildTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BuildTaskVO implements Serializable {

    private Long id;

    private Long appId;

    private Long userId;

    private String triggerType;

    private String status;

    private String sourceDir;

    private String distDir;

    private String deployKey;

    private String deployUrl;

    private String errorMessage;

    private LocalDateTime queuedTime;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private static final long serialVersionUID = 1L;

    public static BuildTaskVO objToVo(BuildTask buildTask, String deployUrl) {
        if (buildTask == null) {
            return null;
        }
        BuildTaskVO vo = new BuildTaskVO();
        BeanUtils.copyProperties(buildTask, vo);
        vo.setDeployUrl(deployUrl);
        return vo;
    }
}
