package spring.ai.example.spring_ai_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import spring.ai.example.spring_ai_demo.entity.Comment;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 原子更新评论点赞数，理由同 ArticleMapper.updateLikeCount */
    @Update("UPDATE blog_comment SET like_count = GREATEST(like_count + #{delta}, 0) "
            + "WHERE id = #{id} AND deleted = 0")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
