package spring.ai.example.spring_ai_demo.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long replyToUserId) { this.replyToUserId = replyToUserId; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getReplyToUsername() { return replyToUsername; }
    public void setReplyToUsername(String replyToUsername) { this.replyToUsername = replyToUsername; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public List<CommentDTO> getChildren() { return children; }
    public void setChildren(List<CommentDTO> children) { this.children = children; }
    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }
}
