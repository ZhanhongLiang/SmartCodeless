package com.robotlive.smartcodeless.sandbox;

import com.robotlive.smartcodeless.model.enums.SandboxStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SandboxRoute {

    private Long appId;

    private Long userId;

    private String deployKey;

    private String containerName;

    private String containerId;

    private String host;

    private Integer hostPort;

    private String status;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    public boolean isRunning() {
        return SandboxStatusEnum.RUNNING.getValue().equals(status) && hostPort != null && hostPort > 0;
    }
}
