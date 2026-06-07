package com.robotlive.smartcodeless.sandbox;

import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class PreviewProxyService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Resource
    private SandboxRouteRegistry routeRegistry;

    @Resource
    private SandboxSecurityPolicy securityPolicy;

    public ResponseEntity<byte[]> proxy(Long appId, String rawPath) {
        SandboxRoute route = routeRegistry.getByAppId(appId)
                .filter(SandboxRoute::isRunning)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "sandbox preview not running"));
        String safePath = securityPolicy.normalizeProxyPath(rawPath);
        ResponseEntity<byte[]> response = request(route, safePath);
        if (response.getStatusCode() == HttpStatus.NOT_FOUND && !"/index.html".equals(safePath)) {
            return request(route, "/index.html");
        }
        return response;
    }

    private ResponseEntity<byte[]> request(SandboxRoute route, String safePath) {
        try {
            URI uri = URI.create("http://" + route.getHost() + ":" + route.getHostPort() + safePath);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            HttpHeaders headers = new HttpHeaders();
            List<String> contentTypes = response.headers().allValues("content-type");
            if (!contentTypes.isEmpty()) {
                headers.add(HttpHeaders.CONTENT_TYPE, contentTypes.get(0));
            }
            List<String> cacheControls = response.headers().allValues("cache-control");
            if (!cacheControls.isEmpty()) {
                headers.add(HttpHeaders.CACHE_CONTROL, cacheControls.get(0));
            }
            return new ResponseEntity<>(response.body(), headers, HttpStatus.valueOf(response.statusCode()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "proxy sandbox preview failed");
        }
    }
}
