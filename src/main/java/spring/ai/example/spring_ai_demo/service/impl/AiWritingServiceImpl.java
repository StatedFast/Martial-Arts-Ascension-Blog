package spring.ai.example.spring_ai_demo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.dto.AiWritingDTO;
import spring.ai.example.spring_ai_demo.exception.BusinessException;
import spring.ai.example.spring_ai_demo.service.AiWritingService;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * AI 写作助手实现。
 *
 * 与 AiController 的对话链路刻意分开：
 * - 不挂 ChatMemory：写作是单次任务，历史对话只会污染输出、白烧 token
 * - 不挂工具：模型在写作时去查文章会拖长耗时，结果也不可控
 * - 输入先截断再拼提示词：正文可能上万字，原样送模型会超上下文并推高费用
 */
@Service
@Slf4j
public class AiWritingServiceImpl implements AiWritingService {

    /** 送入模型的正文字数上限 */
    private static final int MAX_CONTENT = 6000;
    /** 润色选区的字数上限（再长用户应该分段润色） */
    private static final int MAX_SELECTION = 3000;
    /** 续写只看正文末尾，避免整篇长文进上下文 */
    private static final int MAX_TAIL = 1500;
    /** 起标题/标签/大纲/摘要所需的正文长度 */
    private static final int MIN_CONTENT = 20;

    private static final String SYSTEM_PROMPT = """
            你是中文技术博客「修仙心得」的资深编辑，擅长把技术文章改得清晰、有节奏。
            输出规则：
            - 只输出成品内容，不要寒暄、不要解释、不要复述要求。
            - 保留 Markdown 语法；代码块内部原样保留，不要改写代码逻辑。
            - 使用简体中文，专有名词与代码保持原样。
            """;

    private final ChatClient chatClient;

    /**
     * 直接基于 ChatModel 构建：不继承全局 ChatClient 的记忆/工具配置，
     * 保证写作请求完全无状态。
     */
    public AiWritingServiceImpl(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public String assist(Long userId, AiWritingDTO dto) {
        String action = dto.getAction().trim().toLowerCase();
        String title = safe(dto.getTitle()).trim();
        String content = truncate(safe(dto.getContent()), MAX_CONTENT);
        String selection = truncate(safe(dto.getSelection()).trim(), MAX_SELECTION);

        String reply = switch (action) {
            case "draft" -> draft(title, safe(dto.getCategory()).trim(), content);
            case "polish" -> polish(selection);
            case "continue" -> continueWriting(title, safe(dto.getCategory()).trim(), content);
            case "outline" -> outline(title, safe(dto.getCategory()).trim(), content);
            case "title" -> titleCandidates(content);
            case "summary" -> summary(content);
            case "tags" -> tags(content);
            default -> throw new BusinessException(400, "不支持的 AI 写作类型：" + action);
        };

        if (reply.isBlank()) {
            throw new BusinessException(500, "AI 没有生成内容，请稍后重试");
        }
        log.info("AI 写作完成: userId={}, action={}, contentLength={}, replyLength={}",
                userId, action, content.length(), reply.length());
        return reply;
    }

    /** 初稿：只给标题也能写出一篇完整正文，这是「从零开始」的最短路径 */
    private String draft(String title, String category, String content) {
        if (title.isBlank()) {
            throw new BusinessException(400, "请先填写文章标题，AI 才能围绕标题写正文");
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("文章标题：").append(title).append('\n');
        if (!category.isBlank()) prompt.append("文章分类：").append(category).append('\n');
        if (!content.isBlank()) {
            prompt.append("已有的内容片段（可参考其风格，但不要重复）：\n")
                    .append(head(content, MAX_TAIL)).append('\n');
        }
        prompt.append("""

                请围绕标题写一篇完整的技术博客初稿：
                - 800-1200 字，用 Markdown 二级/三级小标题分节，开头一段直接进入主题
                - 结合分类给出具体做法、关键代码或配置示例（代码块标注语言）
                - 结尾给简短的实践建议，不要写「总结」套话
                - 只输出文章正文，不要输出标题，不要解释写作过程
                """);
        return chat(prompt.toString());
    }

    /** 润色：只改选中段落，保持原意与 Markdown 结构 */
    private String polish(String selection) {
        if (selection.isBlank()) {
            throw new BusinessException(400, "请先在正文中选中要润色的段落");
        }
        return chat("""
                请润色下面这段文字：保持原意、Markdown 结构与代码块不变，让表达更通顺、更有层次。
                只输出润色后的文字，不要输出解释。

                """ + selection);
    }

    /** 续写：取正文末尾作为上下文，避免整篇塞进模型 */
    private String continueWriting(String title, String category, String content) {
        if (content.isBlank()) {
            throw new BusinessException(400, "请先写几行内容，AI 才能接着往下写");
        }
        StringBuilder prompt = new StringBuilder();
        if (!title.isBlank()) prompt.append("文章标题：").append(title).append('\n');
        if (!category.isBlank()) prompt.append("文章分类：").append(category).append('\n');
        prompt.append("下面是我已经写好的内容（末尾节选）：\n")
                .append(tail(content, MAX_TAIL))
                .append("""

                        请顺着最后一句话自然续写 2-3 段，风格与上文一致；
                        不要重复已有内容，不要写总结或结束语，只输出续写部分。
                        """);
        return chat(prompt.toString());
    }

    /** 大纲：标题或正文至少有一个即可生成 */
    private String outline(String title, String category, String content) {
        if (title.isBlank() && content.isBlank()) {
            throw new BusinessException(400, "请先填写标题或写一点内容，AI 才能生成大纲");
        }
        StringBuilder prompt = new StringBuilder();
        if (!title.isBlank()) prompt.append("文章标题：").append(title).append('\n');
        if (!category.isBlank()) prompt.append("文章分类：").append(category).append('\n');
        if (!content.isBlank()) prompt.append("已有内容：\n").append(head(content, 2000)).append('\n');
        prompt.append("""

                请为这篇文章设计写作大纲：用 Markdown 列表输出，最多三级，每个要点一句话。
                只输出大纲，不要写正文。
                """);
        return chat(prompt.toString());
    }

    /** 标题：每行一个，后端再做一次清洗，前端可直接点选 */
    private String titleCandidates(String content) {
        requireContent(content, "标题");
        String text = chat("""
                请为下面的文章生成 5 个标题候选，每个不超过 24 个字，风格务实、不夸张、不堆砌关键词。
                格式要求：每行一个标题，不要序号、不要引号、不要解释。

                文章正文：
                """ + head(content, 3000));
        return normalizeList(text, 5);
    }

    /** 摘要：一段话，100-160 字 */
    private String summary(String content) {
        requireContent(content, "摘要");
        return chat("""
                请为下面文章写一段 100-160 字的中文摘要：概括主题、关键做法与结论。
                要求：一段话、不分点、不写套话，只输出摘要正文。

                文章正文：
                """ + head(content, 4000));
    }

    /** 标签：后端统一清洗成「逗号分隔、无 # 号」的形式，直接填进标签输入框 */
    private String tags(String content) {
        requireContent(content, "标签");
        String text = chat("""
                请为下面的文章推荐 5 个标签：优先技术栈、框架、方法论等可检索的词。
                格式要求：只输出一行，标签之间用英文逗号分隔，不带 # 号、不带引号、不要解释。

                文章正文：
                """ + head(content, 3000));
        return normalizeTags(text);
    }

    private void requireContent(String content, String actionName) {
        if (content.length() < MIN_CONTENT) {
            throw new BusinessException(400, "正文太短了，多写几行再让 AI 生成" + actionName);
        }
    }

    private String chat(String userPrompt) {
        String reply = chatClient.prompt().user(userPrompt).call().content();
        return reply == null ? "" : reply.trim();
    }

    /** 标题候选清洗：去掉序号、项目符号、成对引号与书名号 */
    private String normalizeList(String text, int limit) {
        return Arrays.stream(text.split("\\R"))
                .map(line -> line.replaceFirst("^\\s*(?:[-*•]|\\d+[.、)])\\s*", ""))
                .map(line -> line.replaceAll("^[\"'“”‘’《》]+|[\"'“”‘’《》]+$", "").trim())
                .filter(line -> !line.isEmpty())
                .limit(limit)
                .collect(Collectors.joining("\n"));
    }

    /** 标签清洗：兼容中英文逗号/顿号，去掉 # 与空白 */
    private String normalizeTags(String text) {
        return Arrays.stream(text.split("[,，、\\n]"))
                .map(tag -> tag.replace("#", "").replaceAll("[\\s\"'“”‘’《》]", ""))
                .filter(tag -> !tag.isEmpty())
                .limit(5)
                .collect(Collectors.joining(","));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String head(String text, int max) {
        return truncate(text, max);
    }

    private String tail(String text, int max) {
        return text.length() <= max ? text : text.substring(text.length() - max);
    }
}
