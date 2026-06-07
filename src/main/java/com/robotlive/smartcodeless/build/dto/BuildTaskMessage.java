package com.robotlive.smartcodeless.build.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildTaskMessage implements Serializable {

    private Long taskId;

    private Long appId;

    private Long userId;

    private String triggerType;
}
