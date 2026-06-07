package com.robotlive.smartcodeless.sandbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SandboxCommandRunner {

    public CommandResult run(List<String> command, Duration timeout) {
        return run(command, null, timeout);
    }

    public CommandResult run(List<String> command, File workingDir, Duration timeout) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (workingDir != null) {
                processBuilder.directory(workingDir);
            }
            Process process = processBuilder.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread stdoutReader = Thread.startVirtualThread(() -> read(process.getInputStream(), stdout));
            Thread stderrReader = Thread.startVirtualThread(() -> read(process.getErrorStream(), stderr));
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(false, -1, "", "Command timed out after " + timeout.toSeconds() + " seconds");
            }
            stdoutReader.join(TimeUnit.SECONDS.toMillis(3));
            stderrReader.join(TimeUnit.SECONDS.toMillis(3));
            int exitCode = process.exitValue();
            return new CommandResult(exitCode == 0, exitCode, stdout.toString().trim(), stderr.toString().trim());
        } catch (Exception e) {
            log.warn("Run sandbox command failed: {}", sanitize(command), e);
            return new CommandResult(false, -1, "", e.getMessage());
        }
    }

    private void read(java.io.InputStream inputStream, StringBuilder target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (target.length() < 8000) {
                    target.append(line).append(System.lineSeparator());
                }
            }
        } catch (Exception e) {
            log.debug("Read sandbox command stream failed", e);
        }
    }

    private String sanitize(List<String> command) {
        return command == null ? "" : String.join(" ", command);
    }

    @Data
    @AllArgsConstructor
    public static class CommandResult {
        private boolean success;
        private int exitCode;
        private String stdout;
        private String stderr;

        public String message() {
            return success ? stdout : (stderr == null || stderr.isBlank() ? stdout : stderr);
        }
    }
}
