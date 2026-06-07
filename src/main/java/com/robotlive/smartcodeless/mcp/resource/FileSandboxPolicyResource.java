package com.robotlive.smartcodeless.mcp.resource;

import org.springframework.stereotype.Component;

@Component
public class FileSandboxPolicyResource extends AbstractMcpResource {

    @Override
    public String uri() {
        return "code://policy/file-sandbox";
    }

    @Override
    public String name() {
        return "File Sandbox Policy";
    }

    @Override
    public String description() {
        return "Path sandbox rules for MCP file tools.";
    }

    @Override
    protected String text() {
        return """
                File tools are app-scoped.
                Paths must be relative to the app code output directory.
                Reject absolute paths, '..', Windows drive prefixes, and symlink escapes.
                Do not return server absolute paths, environment variables, secrets, or long stderr traces.
                Content and diff output are length-limited.
                """;
    }
}

