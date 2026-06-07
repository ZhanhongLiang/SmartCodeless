package com.robotlive.smartcodeless.mcp.resource;

import org.springframework.stereotype.Component;

@Component
public class NpmAllowlistPolicyResource extends AbstractMcpResource {

    @Override
    public String uri() {
        return "code://policy/npm-allowlist";
    }

    @Override
    public String name() {
        return "NPM Build Policy";
    }

    @Override
    public String description() {
        return "Allowed build commands and dependency policy for generated apps.";
    }

    @Override
    protected String text() {
        return """
                Stage 2 build tasks may run only fixed commands controlled by backend code:
                npm install
                npm run build
                MCP tools must never execute arbitrary package scripts from user input.
                Build execution must go through BuildTaskService and RabbitMQ state machine.
                """;
    }
}

