package com.robotlive.smartcodeless.git;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.model.entity.App;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LocalGitVersionManager {

    private static final Duration GIT_TIMEOUT = Duration.ofSeconds(30);

    private static final Pattern COMMIT_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{7,64}$");

    @Resource
    private GitCommandRunner gitCommandRunner;

    @Resource
    private RedissonClient redissonClient;

    public Optional<String> commitVersion(App app, Integer roundNo, String commitMessage) {
        return withGitLock(app.getId(), () -> {
            Path appDir = resolveAppDir(app);
            ensureGitRepository(appDir);
            GitCommandRunner.GitCommandResult status = gitCommandRunner.run(appDir, GIT_TIMEOUT, "status", "--porcelain");
            if (StrUtil.isBlank(status.getStdout())) {
                log.info("Skip git commit because working tree has no changes, appId={}", app.getId());
                return Optional.empty();
            }
            assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "add", "."), "git add failed");
            GitCommandRunner.GitCommandResult commit = gitCommandRunner.run(appDir, GIT_TIMEOUT, "commit", "-m", commitMessage);
            if (!commit.success()) {
                if (commit.getStderr().contains("nothing to commit")) {
                    return Optional.empty();
                }
                assertSuccess(commit, "git commit failed");
            }
            GitCommandRunner.GitCommandResult revParse = gitCommandRunner.run(appDir, GIT_TIMEOUT, "rev-parse", "HEAD");
            assertSuccess(revParse, "git rev-parse failed");
            return Optional.of(revParse.getStdout().trim());
        });
    }

    public void rollback(App app, String commitId) {
        validateCommitId(commitId);
        withGitLock(app.getId(), () -> {
            Path appDir = resolveAppDir(app);
            ensureGitRepository(appDir);
            assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "cat-file", "-e", commitId + "^{commit}"),
                    "commit does not exist");
            assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "reset", "--hard", commitId),
                    "git reset failed");
            assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "clean", "-fd"),
                    "git clean failed");
            return null;
        });
    }

    public String currentCommitId(App app) {
        Path appDir = resolveAppDir(app);
        ensureGitRepository(appDir);
        GitCommandRunner.GitCommandResult revParse = gitCommandRunner.run(appDir, GIT_TIMEOUT, "rev-parse", "HEAD");
        assertSuccess(revParse, "git rev-parse failed");
        return revParse.getStdout().trim();
    }

    public void validateCommitId(String commitId) {
        if (StrUtil.isBlank(commitId) || !COMMIT_ID_PATTERN.matcher(commitId).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid commitId");
        }
    }

    private Path resolveAppDir(App app) {
        if (app == null || app.getId() == null || StrUtil.isBlank(app.getCodeGenType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "app code directory params are required");
        }
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path appDir = root.resolve(app.getCodeGenType() + "_" + app.getId()).normalize();
        if (!appDir.startsWith(root)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "invalid app code path");
        }
        if (!appDir.toFile().exists() || !appDir.toFile().isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "app code directory not found");
        }
        return appDir;
    }

    private void ensureGitRepository(Path appDir) {
        if (!appDir.resolve(".git").toFile().exists()) {
            assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "init"), "git init failed");
        }
        assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "config", "user.name", "SmartCodeless Bot"),
                "git config user.name failed");
        assertSuccess(gitCommandRunner.run(appDir, GIT_TIMEOUT, "config", "user.email", "smartcodeless-bot@example.local"),
                "git config user.email failed");
        ensureGitIgnore(appDir);
    }

    private void ensureGitIgnore(Path appDir) {
        Path gitIgnore = appDir.resolve(".gitignore");
        String required = """
                node_modules/
                dist/
                .cache/
                .env
                .env.*
                *.log
                """;
        if (!gitIgnore.toFile().exists()) {
            FileUtil.writeString(required, gitIgnore.toFile(), StandardCharsets.UTF_8);
            return;
        }
        String current = FileUtil.readString(gitIgnore.toFile(), StandardCharsets.UTF_8);
        StringBuilder next = new StringBuilder(current);
        for (String line : required.split("\\R")) {
            if (StrUtil.isNotBlank(line) && !current.contains(line)) {
                next.append(System.lineSeparator()).append(line);
            }
        }
        if (!next.toString().equals(current)) {
            FileUtil.writeString(next.toString(), gitIgnore.toFile(), StandardCharsets.UTF_8);
        }
    }

    private void assertSuccess(GitCommandRunner.GitCommandResult result, String message) {
        if (!result.success()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, message + ": " + StrUtil.blankToDefault(result.getStderr(), result.getStdout()));
        }
    }

    private <T> T withGitLock(Long appId, GitOperation<T> operation) {
        RLock lock = redissonClient.getLock("lock:git:app:" + appId);
        boolean locked = false;
        try {
            locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "git operation is busy");
            }
            return operation.execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git operation interrupted");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @FunctionalInterface
    private interface GitOperation<T> {
        T execute();
    }

    @Data
    @AllArgsConstructor
    public static class GitVersionSnapshot {

        private Integer roundNo;

        private String commitId;
    }
}

