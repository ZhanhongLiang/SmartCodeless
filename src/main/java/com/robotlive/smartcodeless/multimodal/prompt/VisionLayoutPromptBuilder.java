package com.robotlive.smartcodeless.multimodal.prompt;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

@Component
public class VisionLayoutPromptBuilder {

    public String build(String userPrompt) {
        return """
                你是 Qwen-VL 通用图片理解与网站生成参考分析器。
                你的任务是先理解任意合规图片的内容，再把它转换成可供 DeepSeek 生成网站/页面使用的结构化上下文。
                严格要求：
                1. 只输出 JSON，不要 Markdown 代码块，不要解释文字。
                2. 不要生成 Vue、HTML、CSS 或任何实现代码。
                3. 图片可以是风景、人物、商品、建筑、海报、截图、草图等任意不违规图片。
                4. 先客观描述图片主体、场景、颜色、风格、可见文字和可用于网站生成的灵感。
                5. 如果用户问“这是什么 / 哪里 / 内容是什么”，要在 imageDescription 中回答；不确定就写“可能是”并加入 warnings。
                6. 如果图片不是 UI 截图，components 可以为空，但 layout 要给出适合生成网站的建议结构。
                7. 不确定的内容写入 warnings，不要编造。
                8. JSON 字段必须包含 imageDescription、pageType、language、theme、layout、components、assets、confidence、warnings。
                9. theme 必须是对象，不允许是字符串；components、assets、warnings 必须是数组。

                必须严格按照下面结构输出：
                {
                  "imageDescription": "这张图片展示了富士山、雪顶、蓝天、湖面或游客等可见内容，可作为自然旅行主题网站参考。",
                  "pageType": "landing_page | admin_dashboard | profile_page | ecommerce_page | unknown",
                  "language": "zh-CN",
                  "theme": {
                    "primaryColor": "#1677ff",
                    "backgroundColor": "#ffffff",
                    "textColor": "#1f2937",
                    "styleKeywords": ["自然风光", "清爽", "卡片式"]
                  },
                  "layout": {
                    "structure": "导航栏 + 首屏大图 + 图片内容介绍 + 亮点卡片 + 页脚",
                    "grid": "responsive",
                    "responsiveHints": ["移动端优先", "卡片在小屏幕堆叠"]
                  },
                  "components": [
                    {
                      "type": "hero",
                      "name": "首屏区域",
                      "text": ["图片中可见文案"],
                      "position": "top",
                      "styleHints": ["大标题", "背景图"]
                    }
                  ],
                  "assets": [],
                  "confidence": 0.8,
                  "warnings": []
                }

                用户需求：
                %s
                """.formatted(StrUtil.blankToDefault(userPrompt, "根据图片生成 Vue 网站"));
    }
}
