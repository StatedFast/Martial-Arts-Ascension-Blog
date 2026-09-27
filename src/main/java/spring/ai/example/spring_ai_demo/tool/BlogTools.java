package spring.ai.example.spring_ai_demo.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.service.ArticleService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 博客系统 AI 工具集（Tool Calling 核心）
 *
 * 原理：模型只负责"决定调用哪个工具、传什么参数"（返回 JSON 指令），
 * 真正的 Java 方法执行永远发生在本进程，执行结果回灌给模型后继续推理，
 * 直到模型不再请求工具，产出最终回答（tool loop）。
 *
 * 设计原则：
 * - 只读优先：目前只暴露查询类工具，避免 Agent 误操作业务数据
 * - 返回精简：只回传模型需要的关键字段，控制 token 消耗
 * - 权限隔离：用户身份通过 ToolContext 注入，不作为工具参数暴露给模型
 */
@Component
public class BlogTools {

    /** 文章正文返回给模型的最大长度，超出截断，控制 token 成本 */
    private static final int MAX_CONTENT_LENGTH = 1500;

    private final ArticleService articleService;

    public BlogTools(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 按关键词/分类搜索文章（用户想找文章时调用） */
    @Tool(description = "根据关键词和分类搜索博客文章，返回匹配文章的ID、标题、分类、浏览量和摘要。当用户想查找、搜索、推荐文章时使用。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词，如 JWT、Spring、缓存") String keyword,
            @ToolParam(description = "文章分类，用户没有指定分类时传空字符串", required = false) String category) {
        Page<Article> page = articleService.getArticlePage(1, 5, category, keyword);
        if (page.getRecords().isEmpty()) {
            return "没有找到匹配的文章（关键词：" + keyword + "）";
        }
        return page.getRecords().stream()
                .map(this::formatBrief)
                .collect(Collectors.joining("\n"));
    }

    /** 热门文章排行（用户问什么文章最受欢迎时调用） */
    @Tool(description = "获取全站最热门的文章排行榜（按热度排序）。当用户问什么文章最受欢迎、最热、最火时使用。")
    public String getHotArticles(
            @ToolParam(description = "返回文章数量，默认5，最大10") int limit) {
        int size = Math.max(1, Math.min(limit, 10));
        List<Article> hot = articleService.getHotArticles(size);
        if (hot == null || hot.isEmpty()) {
            return "暂时没有热门文章数据";
        }
        return hot.stream()
                .map(this::formatBrief)
                .collect(Collectors.joining("\n"));
    }

    /** 文章详情（需要总结或分析文章内容时调用） */
    @Tool(description = "根据文章ID获取完整标题和正文内容，用于总结、分析或评价文章。用户要求总结某篇文章时使用。")
    public String getArticleDetail(
            @ToolParam(description = "文章ID，例如 1") Long articleId) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            return "文章不存在，ID：" + articleId;
        }
        String content = article.getContent() == null ? "" : article.getContent();
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH) + "……（内容过长已截断）";
        }
        return "标题：" + article.getTitle()
                + "\n分类：" + article.getCategory()
                + "\n标签：" + article.getTags()
                + "\n浏览量：" + article.getViewCount()
                + "\n正文：\n" + content;
    }

    /** 当前登录用户的文章统计（通过 ToolContext 获取身份，安全隔离） */
    @Tool(description = "获取当前登录用户自己的文章发布统计（按月份分组）。当用户问自己发布了多少文章时使用。")
    public String getMyArticleStats(ToolContext toolContext) {
        Object userId = toolContext.getContext().get("userId");
        if (userId == null) {
            return "当前用户未登录，无法获取个人文章统计";
        }
        List<Map<String, Object>> stats = articleService.getArchiveStats(Long.valueOf(userId.toString()));
        if (stats == null || stats.isEmpty()) {
            return "当前用户还没有发布过文章";
        }
        return stats.stream()
                .map(m -> m.get("month") + "：" + m.get("count") + " 篇")
                .collect(Collectors.joining("\n", "文章归档统计（月份：数量）\n", ""));
    }

    /** 统一格式化：控制返回给模型的字段数量，节省 token */
    private String formatBrief(Article a) {
        return "ID:" + a.getId() + " | " + a.getTitle()
                + " | 分类:" + a.getCategory()
                + " | 浏览:" + a.getViewCount() + " | 点赞:" + a.getLikeCount()
                + " | 摘要:" + a.getSummary();
    }
}
