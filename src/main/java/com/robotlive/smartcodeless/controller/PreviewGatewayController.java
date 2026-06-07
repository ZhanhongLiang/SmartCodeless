package com.robotlive.smartcodeless.controller;

import com.robotlive.smartcodeless.sandbox.PreviewProxyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@RestController
@RequestMapping("/preview")
public class PreviewGatewayController {

    @Resource
    private PreviewProxyService previewProxyService;

    @GetMapping("/{appId}/**")
    public ResponseEntity<byte[]> proxy(@PathVariable Long appId, HttpServletRequest request) {
        String requestPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix = "/preview/" + appId;
        String remainingPath = requestPath == null || requestPath.length() <= prefix.length()
                ? "/"
                : requestPath.substring(prefix.length());
        return previewProxyService.proxy(appId, remainingPath);
    }
}
