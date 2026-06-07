package com.robotlive.smartcodeless.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class VueProjectBuildExecutor {

    private static final long NPM_INSTALL_TIMEOUT_SECONDS = Duration.ofMinutes(5).toSeconds();
    private static final long NPM_BUILD_TIMEOUT_SECONDS = Duration.ofMinutes(3).toSeconds();

    @Resource
    private ProcessCommandRunner processCommandRunner;

    @Resource
    private BuildLogAppender buildLogAppender;

    public BuildExecutionResult execute(Long taskId, App app) {
        Long appId = app.getId();
        String deployKey = StrUtil.blankToDefault(app.getDeployKey(), RandomUtil.randomString(6));
        String sourceDirName = app.getCodeGenType() + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Source directory not found: " + sourceDirPath);
        }

        File deploySourceDir = sourceDir;
        String distDirPath = null;
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            buildVueProject(taskId, appId, sourceDir);
            File distDir = new File(sourceDir, "dist");
            if (!distDir.exists() || !distDir.isDirectory()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue build finished but dist directory was not generated");
            }
            deploySourceDir = distDir;
            distDirPath = distDir.getAbsolutePath();
        }

        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        buildLogAppender.system(taskId, appId, "Copying build output to " + deployDirPath);
        FileUtil.copyContent(deploySourceDir, new File(deployDirPath), true);
        String deployUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
        return new BuildExecutionResult(sourceDirPath, distDirPath, deployKey, deployDirPath, deployUrl);
    }

    private void buildVueProject(Long taskId, Long appId, File projectDir) {
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "package.json not found: " + projectDir.getAbsolutePath());
        }
        buildLogAppender.system(taskId, appId, "Building Vue project: " + projectDir.getAbsolutePath());
        runCommand(taskId, appId, projectDir, List.of(npmCommand(), "install"), NPM_INSTALL_TIMEOUT_SECONDS, "npm install");
        runCommand(taskId, appId, projectDir, List.of(npmCommand(), "run", "build"), NPM_BUILD_TIMEOUT_SECONDS, "npm run build");
    }

    private void runCommand(Long taskId, Long appId, File projectDir, List<String> command, long timeoutSeconds, String label) {
        buildLogAppender.system(taskId, appId, "Running " + label);
        ProcessCommandRunner.CommandResult result = processCommandRunner.run(taskId, appId, projectDir, command, timeoutSeconds);
        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, label + " failed: " + result.getMessage());
        }
        buildLogAppender.system(taskId, appId, label + " completed");
    }

    private String npmCommand() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
    }

    @Data
    @AllArgsConstructor
    public static class BuildExecutionResult {
        private String sourceDir;
        private String distDir;
        private String deployKey;
        private String deployDir;
        private String deployUrl;
    }
}
