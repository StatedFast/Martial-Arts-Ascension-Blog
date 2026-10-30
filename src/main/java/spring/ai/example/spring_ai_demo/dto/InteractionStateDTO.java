package spring.ai.example.spring_ai_demo.dto;

/**
 * 点赞/收藏操作后的最新状态，供前端直接刷新按钮与计数，避免再发一次详情请求。
 */
public class InteractionStateDTO {

    private Boolean liked;
    private Boolean favorited;
    private Long likeCount;
    private Long favoriteCount;

    public InteractionStateDTO() {
    }

    public InteractionStateDTO(Boolean liked, Boolean favorited, Long likeCount, Long favoriteCount) {
        this.liked = liked;
        this.favorited = favorited;
        this.likeCount = likeCount;
        this.favoriteCount = favoriteCount;
    }

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
}
