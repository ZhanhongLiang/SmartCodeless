package com.robotlive.smartcodeless.model.vo.quality;

import com.robotlive.smartcodeless.model.entity.DependencyPolicyViolation;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DependencyPolicyViolationVO implements Serializable {

    private Long id;
    private Long taskId;
    private Long appId;
    private String packageName;
    private String versionSpec;
    private String violationType;
    private String severity;
    private String message;
    private LocalDateTime createTime;

    public static DependencyPolicyViolationVO objToVo(DependencyPolicyViolation violation) {
        if (violation == null) {
            return null;
        }
        DependencyPolicyViolationVO vo = new DependencyPolicyViolationVO();
        BeanUtils.copyProperties(violation, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
