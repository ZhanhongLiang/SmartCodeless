package com.robotlive.smartcodeless.quality;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class QualityAppResolver {

    @Resource
    private AppService appService;

    public App getAuthorizedApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该应用");
        }
        return app;
    }

    public Path resolveAppRoot(App app) {
        String codeGenType = StrUtil.blankToDefault(app.getCodeGenType(), CodeGenTypeEnum.VUE_PROJECT.getValue());
        Path primary = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType + "_" + app.getId()).toAbsolutePath().normalize();
        if (Files.isDirectory(primary)) {
            return primary;
        }
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            Path fallback = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, type.getValue() + "_" + app.getId()).toAbsolutePath().normalize();
            if (Files.isDirectory(fallback)) {
                return fallback;
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成应用源码目录不存在");
    }
}
