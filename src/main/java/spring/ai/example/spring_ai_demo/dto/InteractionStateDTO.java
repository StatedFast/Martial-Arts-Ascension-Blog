package spring.ai.example.spring_ai_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞/收藏操作后的最新状态，供前端直接刷新按钮与计数，避免再发一次详情请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionStateDTO {

    private Boolean liked;
    private Boolean favorited;
    private Long likeCount;
    private Long favoriteCount;
}
