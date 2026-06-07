package com.robotlive.smartcodeless.mcp.resource;

import org.springframework.stereotype.Component;

@Component
public class Vue3AntdTemplateResource extends AbstractMcpResource {

    @Override
    public String uri() {
        return "code://templates/vue3-antd";
    }

    @Override
    public String name() {
        return "Vue 3 + Ant Design Vue Template";
    }

    @Override
    public String description() {
        return "Compact guidance for generated Vue 3 + Ant Design Vue apps.";
    }

    @Override
    protected String text() {
        return """
                Use Vue 3 Composition API, Vite, vue-router, and Ant Design Vue.
                Keep generated apps self-contained under src/.
                Prefer pages/, components/, router/, and styles.css.
                Avoid remote secrets, hard-coded API keys, and heavyweight state stores.
                For deployable Vue projects, package.json must include build script.
                """;
    }
}

