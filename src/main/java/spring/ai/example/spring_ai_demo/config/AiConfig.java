package spring.ai.example.spring_ai_demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 *
 * - ChatClient：业务侧使用的高层 API（自动编排工具调用循环、拦截器链）
 * - ChatMemory：多轮对话记忆（滑动窗口，控制 token 成本）
 */
@Configuration
public class AiConfig {

    /** 记忆窗口：只保留最近 20 条消息（控制上下文长度与成本） */
    private static final int MAX_MESSAGES = 20;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        你是博客系统的 AI 助手"小博"。
                        你可以：1) 搜索和推荐文章；2) 获取热门文章；3) 总结文章内容；4) 回答技术问题。
                        规则：
                        - 涉及站内文章数据（搜索、排行、详情、统计）时，必须调用工具获取真实数据，禁止编造。
                        - 回答使用简体中文，风格简洁友好。
                        - 如果用户要求执行删除、修改数据等操作，礼貌拒绝，说明当前只支持查询。
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
