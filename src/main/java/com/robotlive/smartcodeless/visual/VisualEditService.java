package com.robotlive.smartcodeless.visual;

import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyResponse;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewResponse;

public interface VisualEditService {

    VisualEditPreviewResponse preview(VisualEditPreviewRequest request, User loginUser);

    VisualEditApplyResponse apply(VisualEditApplyRequest request, User loginUser);
}
