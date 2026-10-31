package spring.ai.example.spring_ai_demo.service;

import spring.ai.example.spring_ai_demo.dto.AiWritingDTO;

/**
 * AI 写作助手：发文页里的润色、续写、大纲、标题、摘要、标签。
 *
 * 只产出文本，不落库、不改文章：采纳与否由用户在编辑器中决定。
 */
public interface AiWritingService {

    /**
     * 执行一次写作动作，返回可直接展示/插入的纯文本。
     *
     * @param userId 当前登录用户（仅用于日志与配额追踪）
     * @param dto    动作与编辑器上下文
     */
    String assist(Long userId, AiWritingDTO dto);
}
