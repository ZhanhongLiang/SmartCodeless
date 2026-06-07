package com.robotlive.smartcodeless.visual.dto;

import com.robotlive.smartcodeless.model.vo.AppVersionVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisualEditApplyResponse {

    private Long appId;

    private AppVersionVO version;

    private BuildTaskSubmitVO buildTask;

    private String message;
}
