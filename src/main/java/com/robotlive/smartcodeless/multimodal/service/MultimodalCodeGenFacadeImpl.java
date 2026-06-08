package com.robotlive.smartcodeless.multimodal.service;

import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEventType;
import com.robotlive.smartcodeless.ai.stream.AgentStreamPayloads;
import com.robotlive.smartcodeless.multimodal.dto.UiLayoutPlan;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.multimodal.dto.VisionAnalysisResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MultimodalCodeGenFacadeImpl implements MultimodalCodeGenFacade {

    @Resource
    private UiLayoutPlanService uiLayoutPlanService;

    @Override
    public String enrichPromptWithLayout(String message, String imageId, User loginUser, AgentStreamEmitter emitter) {
        if (StrUtil.isBlank(imageId)) {
            return message;
        }
        if (emitter != null) {
            emitter.status("analyzing_reference_image", "正在分析参考图");
        }
        VisionAnalysisResult result = uiLayoutPlanService.analyze(imageId, message, loginUser);
        if (emitter != null) {
            emitter.publish(AgentStreamEventType.VISION_ANALYSIS,
                    AgentStreamPayloads.status("vision_analysis", result.getSummary()));
            emitter.publish(AgentStreamEventType.LAYOUT_PLAN,
                    java.util.Map.of("layoutPlan", result.getLayoutPlan(), "fallback", result.isFallback()));
            emitter.status("generating_vue_from_layout", "正在根据布局计划生成 Vue 项目");
        }
        return message + """

                ---
                多模态图片理解结果（来自 Qwen-VL，只作为生成上下文，不作为代码）：
                图片内容理解：
                %s

                结构化视觉上下文：
                %s

                请继续使用 DeepSeek 代码生成能力，根据用户原始需求和以上图片理解生成或修改 Vue 项目。
                要求：
                1. 保留用户原始需求。
                2. 图片可以作为主题、内容、风格、颜色、文案、版式和素材灵感参考。
                3. 如果用户只是让你围绕图片生成网站，就根据图片主体和场景设计页面内容。
                4. 不要在最终页面中提及“Qwen-VL 分析过程”。
                """.formatted(result.getImageDescription(), compactVisionContext(result.getLayoutPlan()));
    }

    private String compactVisionContext(UiLayoutPlan plan) {
        if (plan == null) {
            return "无结构化视觉上下文。";
        }
        String keywords = plan.getTheme() != null && plan.getTheme().getStyleKeywords() != null
                ? String.join("、", plan.getTheme().getStyleKeywords())
                : "";
        Object structure = plan.getLayout() == null ? "" : plan.getLayout().get("structure");
        Object grid = plan.getLayout() == null ? "" : plan.getLayout().get("grid");
        Object hints = plan.getLayout() == null ? "" : plan.getLayout().get("responsiveHints");
        List<String> componentNames = plan.getComponents() == null
                ? List.of()
                : plan.getComponents().stream()
                .limit(8)
                .map(component -> StrUtil.blankToDefault(component.getName(), component.getType()))
                .filter(StrUtil::isNotBlank)
                .toList();
        return """
                - 页面类型：%s
                - 视觉关键词：%s
                - 推荐结构：%s
                - 栅格/布局：%s
                - 响应式建议：%s
                - 识别组件：%s
                - 置信度：%s
                - 注意事项：%s
                """.formatted(
                StrUtil.blankToDefault(plan.getPageType(), "unknown"),
                StrUtil.blankToDefault(keywords, "无"),
                String.valueOf(structure),
                String.valueOf(grid),
                String.valueOf(hints),
                componentNames.isEmpty() ? "无" : String.join("、", componentNames),
                plan.getConfidence(),
                plan.getWarnings() == null || plan.getWarnings().isEmpty() ? "无" : String.join("；", plan.getWarnings()));
    }
}
