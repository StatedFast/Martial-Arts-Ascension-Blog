package spring.ai.example.spring_ai_demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 写作助手请求。
 *
 * 所有字段都由前端按当前编辑器状态回传，后端不再回查数据库：
 * 写作对象是「还没发布的草稿」，库里本来就没有。
 */
@Data
public class AiWritingDTO {

    /** 动作：polish / continue / outline / title / summary / tags */
    @NotBlank(message = "写作类型不能为空")
    private String action;

    /** 文章标题（大纲、续写时作为上下文） */
    private String title;

    /** 文章正文 */
    private String content;

    /** 正文中被选中的文字（润色专用） */
    private String selection;

    /** 文章分类（影响语气与术语） */
    private String category;
}
