package spring.ai.example.spring_ai_demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 对话请求
 */
public class AiChatDTO {

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /** 会话ID（可选：不传则每个用户使用同一默认会话，传入可隔离多会话） */
    private String conversationId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
