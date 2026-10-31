package spring.ai.example.spring_ai_demo.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.AiWritingDTO;
import spring.ai.example.spring_ai_demo.service.AiWritingService;

import java.util.Map;

/**
 * AI 写作助手接口（发文页内嵌工具栏）。
 *
 * 与 /api/ai/chat 的区别：
 * - 无记忆、无工具，单次请求单次产出
 * - 走写作专属每日配额（默认 30 次），不占用问答的 10 次额度
 * - 只返回文本，落不落笔由用户在编辑器里决定
 */
@RestController
@RequestMapping("/api/ai/writing")
public class AiWritingController {

    private final AiWritingService aiWritingService;

    public AiWritingController(AiWritingService aiWritingService) {
        this.aiWritingService = aiWritingService;
    }

    /**
     * 写作动作：polish（润色选区）/ continue（续写）/ outline（大纲）/
     * title（标题候选）/ summary（摘要）/ tags（标签）
     */
    @RateLimit(limit = 12, window = 60, message = "AI 写作请求过于频繁，请稍后再试")
    @PostMapping
    public Result<?> assist(@RequestAttribute("userId") Long userId,
                            @Valid @RequestBody AiWritingDTO dto) {
        String result = aiWritingService.assist(userId, dto);
        return Result.success(Map.of(
                "action", dto.getAction().trim().toLowerCase(),
                "result", result));
    }
}
