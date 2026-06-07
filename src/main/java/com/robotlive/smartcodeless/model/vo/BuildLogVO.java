package com.robotlive.smartcodeless.model.vo;

import com.robotlive.smartcodeless.model.entity.BuildLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BuildLogVO implements Serializable {

    private Long id;

    private Long taskId;

    private Long appId;

    private String logType;

    private String content;

    private Integer lineNo;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;

    public static BuildLogVO objToVo(BuildLog buildLog) {
        if (buildLog == null) {
            return null;
        }
        BuildLogVO vo = new BuildLogVO();
        BeanUtils.copyProperties(buildLog, vo);
        return vo;
    }
}
