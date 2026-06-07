package com.robotlive.smartcodeless.sandbox;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.build.BuildLogAppender;
import com.robotlive.smartcodeless.build.VueProjectBuildExecutor;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.SandboxStatusResponse;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.SandboxStatusEnum;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DockerCliSandboxContainerService implements SandboxContainerService {

    private static final Duration DOCKER_TIMEOUT = Duration.ofSeconds(30);

    private static final Pattern PORT_PATTERN = Pattern.compile("(?:(?:127\\.0\\.0\\.1|0\\.0\\.0\\.0):)?(\\d+)$");

    @Resource
    private AppService appService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private BuildLogAppender buildLogAppender;

    @Resource
    private SandboxCommandRunner commandRunner;

    @Resource
    private SandboxRouteRegistry routeRegistry;

    @Resource
    private SandboxNamingPolicy namingPolicy;

    @Resource
    private SandboxSecurityPolicy securityPolicy;

    @Override
    public SandboxRoute startForBuild(App app, VueProjectBuildExecutor.BuildExecutionResult buildResult, Long taskId) {
        try {
            String mountDir = StrUtil.blankToDefault(buildResult.getDeployDir(), buildResult.getDistDir());
            SandboxRoute route = startInternal(app, buildResult.getDeployKey(), mountDir, taskId);
            buildLogAppender.system(taskId, app.getId(), "Sandbox preview status: " + route.getStatus());
            return route;
        } catch (Exception e) {
            String message = StrUtil.subPre(StrUtil.blankToDefault(e.getMessage(), "Sandbox unavailable"), 512);
            buildLogAppender.error(taskId, app.getId(), "Sandbox preview unavailable: " + message);
            SandboxRoute failed = failedRoute(app, buildResult.getDeployKey(), message);
            routeRegistry.register(failed);
            return failed;
        }
    }

    @Override
    public SandboxStatusResponse startAppPreview(Long appId, User loginUser) {
        App app = requireAppAccess(appId, loginUser);
        String deployKey = app.getDeployKey();
        ThrowUtils.throwIf(StrUtil.isBlank(deployKey), ErrorCode.OPERATION_ERROR, "deploy first before starting sandbox");
        String deployDir = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        SandboxRoute route = startInternal(app, deployKey, deployDir, null);
        return toResponse(app, route);
    }

    @Override
    public SandboxStatusResponse stopAppPreview(Long appId, User loginUser) {
        App app = requireAppAccess(appId, loginUser);
        RLock lock = redissonClient.getLock("lock:sandbox:app:" + appId);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "sandbox lock is busy");
            SandboxRoute route = routeRegistry.getByAppId(appId).orElse(null);
            if (route != null) {
                removeContainer(route.getContainerName());
                route.setStatus(SandboxStatusEnum.STOPPED.getValue());
                route.setStopTime(LocalDateTime.now());
                routeRegistry.register(route);
                return toResponse(app, route);
            }
            return unavailableResponse(app, "Sandbox route not found");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "stop sandbox interrupted");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SandboxStatusResponse getStatus(Long appId, User loginUser) {
        App app = requireAppAccess(appId, loginUser);
        SandboxRoute route = routeRegistry.getByAppId(appId).orElse(null);
        if (route == null) {
            return unavailableResponse(app, "Sandbox route not found");
        }
        return toResponse(app, route);
    }

    private SandboxRoute startInternal(App app, String deployKey, String distDir, Long taskId) {
        Path mountDir = securityPolicy.validateDistDir(distDir);
        RLock lock = redissonClient.getLock("lock:sandbox:app:" + app.getId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 60, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "sandbox lock is busy");
            ensureDockerAvailable();
            String containerName = namingPolicy.containerName(app.getId(), deployKey);
            routeRegistry.getByAppId(app.getId()).ifPresent(route -> removeContainer(route.getContainerName()));
            removeContainer(containerName);
            SandboxRoute starting = SandboxRoute.builder()
                    .appId(app.getId())
                    .userId(app.getUserId())
                    .deployKey(deployKey)
                    .containerName(containerName)
                    .host("127.0.0.1")
                    .status(SandboxStatusEnum.STARTING.getValue())
                    .startTime(LocalDateTime.now())
                    .build();
            routeRegistry.register(starting);
            List<String> command = List.of(
                    "docker", "run", "-d",
                    "--name", containerName,
                    "--read-only",
                    "--tmpfs", "/var/cache/nginx:rw,noexec,nosuid,size=64m",
                    "--tmpfs", "/var/run:rw,noexec,nosuid,size=8m",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=16m",
                    "--cpus=0.5",
                    "--memory=256m",
                    "--network", "bridge",
                    "-p", "127.0.0.1::80",
                    "-v", mountDir + ":/usr/share/nginx/html:ro",
                    "nginx:alpine"
            );
            log.info("Starting sandbox container for app {} with {}", app.getId(), containerName);
            buildLogAppender.system(taskId, app.getId(), "Starting sandbox nginx container: " + containerName);
            SandboxCommandRunner.CommandResult runResult = commandRunner.run(command, DOCKER_TIMEOUT);
            if (!runResult.isSuccess()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "docker run failed: " + safeMessage(runResult.message()));
            }
            String containerId = runResult.getStdout();
            Integer hostPort = inspectPort(containerName);
            SandboxRoute running = SandboxRoute.builder()
                    .appId(app.getId())
                    .userId(app.getUserId())
                    .deployKey(deployKey)
                    .containerName(containerName)
                    .containerId(containerId)
                    .host("127.0.0.1")
                    .hostPort(hostPort)
                    .status(SandboxStatusEnum.RUNNING.getValue())
                    .startTime(LocalDateTime.now())
                    .build();
            routeRegistry.register(running);
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "start sandbox interrupted");
        } catch (BusinessException e) {
            SandboxRoute failed = failedRoute(app, deployKey, e.getMessage());
            routeRegistry.register(failed);
            throw e;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void ensureDockerAvailable() {
        SandboxCommandRunner.CommandResult result = commandRunner.run(List.of("docker", "version", "--format", "{{.Server.Version}}"), Duration.ofSeconds(10));
        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Docker unavailable: " + safeMessage(result.message()));
        }
    }

    private Integer inspectPort(String containerName) {
        SandboxCommandRunner.CommandResult result = commandRunner.run(List.of("docker", "port", containerName, "80/tcp"), DOCKER_TIMEOUT);
        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "docker port failed: " + safeMessage(result.message()));
        }
        Matcher matcher = PORT_PATTERN.matcher(result.getStdout().trim());
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "cannot parse sandbox host port");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private void removeContainer(String containerName) {
        if (StrUtil.isBlank(containerName)) {
            return;
        }
        commandRunner.run(List.of("docker", "rm", "-f", containerName), Duration.ofSeconds(15));
    }

    private App requireAppAccess(Long appId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId is required");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission for this app");
        }
        return app;
    }

    private SandboxStatusResponse toResponse(App app, SandboxRoute route) {
        String deployKey = StrUtil.blankToDefault(route.getDeployKey(), app.getDeployKey());
        return SandboxStatusResponse.builder()
                .appId(app.getId())
                .deployKey(deployKey)
                .status(route.getStatus())
                .previewUrl(route.isRunning() ? "/api/preview/" + app.getId() + "/" : null)
                .staticUrl(StrUtil.isBlank(deployKey) ? null : AppConstant.CODE_DEPLOY_HOST + "/" + deployKey)
                .errorMessage(route.getErrorMessage())
                .build();
    }

    private SandboxStatusResponse unavailableResponse(App app, String message) {
        String deployKey = app.getDeployKey();
        return SandboxStatusResponse.builder()
                .appId(app.getId())
                .deployKey(deployKey)
                .status(SandboxStatusEnum.UNAVAILABLE.getValue())
                .previewUrl(null)
                .staticUrl(StrUtil.isBlank(deployKey) ? null : AppConstant.CODE_DEPLOY_HOST + "/" + deployKey)
                .errorMessage(message)
                .build();
    }

    private SandboxRoute failedRoute(App app, String deployKey, String message) {
        return SandboxRoute.builder()
                .appId(app.getId())
                .userId(app.getUserId())
                .deployKey(deployKey)
                .containerName(namingPolicy.containerName(app.getId(), deployKey))
                .host("127.0.0.1")
                .status(SandboxStatusEnum.FAILED.getValue())
                .errorMessage(safeMessage(message))
                .stopTime(LocalDateTime.now())
                .build();
    }

    private String safeMessage(String message) {
        return StrUtil.subPre(StrUtil.blankToDefault(message, "Sandbox unavailable"), 512);
    }
}
