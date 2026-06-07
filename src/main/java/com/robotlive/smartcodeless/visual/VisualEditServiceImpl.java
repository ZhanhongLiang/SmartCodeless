package com.robotlive.smartcodeless.visual;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.model.vo.AppVersionVO;
import com.robotlive.smartcodeless.model.vo.BuildTaskSubmitVO;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.AppVersionService;
import com.robotlive.smartcodeless.service.BuildTaskService;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditApplyResponse;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewRequest;
import com.robotlive.smartcodeless.visual.dto.VisualEditPreviewResponse;
import com.robotlive.smartcodeless.visual.dto.VisualEditTarget;
import com.robotlive.smartcodeless.visual.dto.VisualPatchResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class VisualEditServiceImpl implements VisualEditService {

    @Resource
    private AppService appService;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private BuildTaskService buildTaskService;

    @Resource
    private VisualSourceLocator visualSourceLocator;

    @Resource
    private VisualPatchPlanner visualPatchPlanner;

    @Resource
    private VisualPatchApplier visualPatchApplier;

    @Override
    public VisualEditPreviewResponse preview(VisualEditPreviewRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        App app = getAuthorizedApp(request.getAppId(), loginUser);
        ThrowUtils.throwIf(request.getElement() == null, ErrorCode.PARAMS_ERROR, "请选择要编辑的页面元素");
        Path appRoot = getAppRoot(app);
        List<VisualEditTarget> candidates = visualSourceLocator.locate(appRoot, request.getElement());
        VisualPatchResult patch = visualPatchPlanner.preview(appRoot, request.getElement(), request.getChanges(), candidates);
        return VisualEditPreviewResponse.builder()
                .appId(app.getId())
                .patch(patch)
                .build();
    }

    @Override
    public VisualEditApplyResponse apply(VisualEditApplyRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        App app = getAuthorizedApp(request.getAppId(), loginUser);
        Path appRoot = getAppRoot(app);
        visualPatchApplier.apply(appRoot, request.getPatch());
        String summary = StrUtil.blankToDefault(request.getSummary(), "可视化编辑补丁已应用");
        Optional<AppVersionVO> version = appVersionService.createAiGenerationVersion(app.getId(), loginUser, summary, null);
        BuildTaskSubmitVO buildTask = buildTaskService.submitBuildTask(app.getId(), loginUser, "VISUAL_EDIT");
        return VisualEditApplyResponse.builder()
                .appId(app.getId())
                .version(version.orElse(null))
                .buildTask(buildTask)
                .message("可视化编辑已应用，版本快照已创建，构建任务已提交")
                .build();
    }

    private App getAuthorizedApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑该应用");
        }
        return app;
    }

    private Path getAppRoot(App app) {
        String codeGenType = StrUtil.blankToDefault(app.getCodeGenType(), CodeGenTypeEnum.VUE_PROJECT.getValue());
        Path primary = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType + "_" + app.getId()).normalize();
        if (Files.isDirectory(primary)) {
            return primary;
        }
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            Path fallback = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, type.getValue() + "_" + app.getId()).normalize();
            if (Files.isDirectory(fallback)) {
                return fallback;
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成应用源码目录不存在");
    }
}
