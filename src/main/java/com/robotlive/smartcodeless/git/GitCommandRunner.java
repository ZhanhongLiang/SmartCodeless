package com.robotlive.smartcodeless.git;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GitCommandRunner {

    private static final int MAX_OUTPUT_LENGTH = 4000;

    public GitCommandResult run(Path workDir, Duration timeout, String... args) {
        if (workDir == null || args == null || args.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "git command params are required");
        }
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(false);
        try {
            Process process = processBuilder.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread outReader = Thread.startVirtualThread(() -> readStream(process.inputReader(StandardCharsets.UTF_8), stdout));
            Thread errReader = Thread.startVirtualThread(() -> readStream(process.errorReader(StandardCharsets.UTF_8), stderr));
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git command timeout");
            }
            outReader.join(1000);
            errReader.join(1000);
            GitCommandResult result = GitCommandResult.builder()
                    .exitCode(process.exitValue())
                    .stdout(sanitize(stdout.toString()))
                    .stderr(sanitize(stderr.toString()))
                    .build();
            if (!result.success()) {
                log.warn("Git command failed in {}: git {}, exit={}, stderr={}",
                        workDir, String.join(" ", List.of(args)), result.getExitCode(), result.getStderr());
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git command failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "git command interrupted");
        }
    }

    private void readStream(BufferedReader reader, StringBuilder output) {
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < MAX_OUTPUT_LENGTH) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            log.warn("Read git output failed: {}", e.getMessage());
        }
    }

    private String sanitize(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return StrUtil.subPre(text.replace('\u0000', ' ').trim(), MAX_OUTPUT_LENGTH);
    }

    @Data
    @Builder
    public static class GitCommandResult {

        private int exitCode;

        private String stdout;

        private String stderr;

        public boolean success() {
            return exitCode == 0;
        }

        public String output() {
            return StrUtil.blankToDefault(stdout, stderr);
        }
    }
}

