package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private Long authorId;
    private String category;
    private String tags;
    private Long viewCount;
    private Long commentCount;
    private Long likeCount;
    private Long favoriteCount;
    private Integer status;
    private Integer isTop;

    // ======== 当前登录用户的互动状态（非数据库列） ========
    @TableField(exist = false)
    private Boolean liked;
    @TableField(exist = false)
    private Boolean favorited;
    // ======== 作者展示字段（列表/收藏页避免额外查询） ========
    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
