package com.robotlive.smartcodeless.mcp.resource;

import org.springframework.stereotype.Component;

@Component
public class AgentStagePolicyResource extends AbstractMcpResource {

    @Override
    public String uri() {
        return "code://policy/agent-stage-boundaries";
    }

    @Override
    public String name() {
        return "Agent Stage Boundaries";
    }

    @Override
    public String description() {
        return "Current stage responsibilities and boundaries.";
    }

    @Override
    protected String text() {
        return """
                Stage 1: Agent SSE observability remains unchanged.
                Stage 2: Builds go through RabbitMQ build task state machine.
                Stage 3: Git version chain and rollback remain the source of version truth.
                Stage 4: MCP adapter exposes tools/resources through JSON-RPC without replacing LangChain4j tools.
                Do not implement Docker sandbox, dynamic gateway, stdio MCP server, or remote marketplace in this stage.
                """;
    }
}

