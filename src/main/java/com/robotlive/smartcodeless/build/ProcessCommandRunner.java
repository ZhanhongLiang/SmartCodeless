package com.robotlive.smartcodeless.build;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ProcessCommandRunner {

    public CommandResult run(Long taskId, Long appId, File workingDir, List<String> command, long timeoutSeconds) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(false);
            Process process = processBuilder.start();
            AtomicInteger lineNo = new AtomicInteger(1);
            Thread stdoutReader = Thread.startVirtualThread(() -> readStream(process.getInputStream(), taskId, appId, "STDOUT", lineNo));
            Thread stderrReader = Thread.startVirtualThread(() -> readStream(process.getErrorStream(), taskId, appId, "STDERR", lineNo));
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(false, -1, "Command timed out after " + timeoutSeconds + " seconds");
            }
            stdoutReader.join(TimeUnit.SECONDS.toMillis(5));
            stderrReader.join(TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            return new CommandResult(exitCode == 0, exitCode, exitCode == 0 ? "ok" : "Command exited with code " + exitCode);
        } catch (Exception e) {
            log.error("Run command failed: {}", command, e);
            return new CommandResult(false, -1, e.getMessage());
        }
    }

    private void readStream(InputStream inputStream, Long taskId, Long appId, String logType, AtomicInteger lineNo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendLog(taskId, appId, logType, line, lineNo.getAndIncrement());
            }
        } catch (Exception e) {
            log.warn("Read process stream failed", e);
        }
    }

    @Resource
    private BuildLogAppender buildLogAppender;

    private void appendLog(Long taskId, Long appId, String logType, String line, int lineNo) {
        buildLogAppender.append(taskId, appId, logType, line, lineNo);
    }

    @Data
    @AllArgsConstructor
    public static class CommandResult {
        private boolean success;
        private int exitCode;
        private String message;
    }
}
