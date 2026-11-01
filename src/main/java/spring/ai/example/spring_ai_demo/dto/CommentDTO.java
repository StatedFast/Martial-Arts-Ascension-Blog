package spring.ai.example.spring_ai_demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private Long id;
    private Long articleId;
    private Long userId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;
    private Long replyToUserId;
    private Long likeCount;

    private String username;
    private String userAvatar;
    private String replyToUsername;
    private LocalDateTime createTime;

    /** 当前登录用户是否已点赞该评论 */
    private Boolean liked;
    /** 子回复（楼中楼）：一级评论分页后批量装配 */
    private List<CommentDTO> children = new ArrayList<>();
    /** 我的评论列表展示所属文章标题 */
    private String articleTitle;
}
