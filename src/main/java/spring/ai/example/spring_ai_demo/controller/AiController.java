package spring.ai.example.spring_ai_demo.controller;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.AiChatDTO;
import spring.ai.example.spring_ai_demo.tool.BlogTools;

import java.util.Map;

/**
 * AI 助手接口（Agent = LLM + 工具 + 记忆）
 *
 * 所有接口需要登录（JWT）：
 * - 登录用户 ID 通过 ToolContext 传给工具，用于个人数据隔离
 * - 会话 ID 按用户隔离，保证多轮对话互不串扰
 * - 接口均有限流，防止 AI 调用被刷导致费用失控
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final BlogTools blogTools;

    public AiController(ChatClient chatClient, BlogTools blogTools) {
        this.chatClient = chatClient;
        this.blogTools = blogTools;
    }

    /**
     * 同步对话：模型自动决定是否调用博客工具，多轮对话带记忆
     */
    @RateLimit(limit = 10, window = 60, message = "AI 请求过于频繁，请稍后再试")
    @PostMapping("/chat")
    public Result<?> chat(@RequestAttribute("userId") Long userId,
                          @Valid @RequestBody AiChatDTO dto) {
        String conversationId = resolveConversationId(userId, dto.getConversationId());
        String reply = chatClient.prompt()
                .user(dto.getMessage())
                .tools(blogTools)
                .toolContext(Map.of("userId", userId))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return Result.success(Map.of("reply", reply, "conversationId", conversationId));
    }

    /**
     * 流式对话（SSE）：逐字返回，适合前端打字机效果
     */
    @RateLimit(limit = 10, window = 60, message = "AI 请求过于频繁，请稍后再试")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestAttribute("userId") Long userId,
                                   @Valid @RequestBody AiChatDTO dto) {
        String conversationId = resolveConversationId(userId, dto.getConversationId());
        return chatClient.prompt()
                .user(dto.getMessage())
                .tools(blogTools)
                .toolContext(Map.of("userId", userId))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * 会话 ID 规则：不传 conversationId 时每个用户一个默认会话；传了则按用户+会话隔离
     */
    private String resolveConversationId(Long userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "user-" + userId;
        }
        return "user-" + userId + "-" + conversationId;
    }
}
