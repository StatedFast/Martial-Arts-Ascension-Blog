package spring.ai.example.spring_ai_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import spring.ai.example.spring_ai_demo.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 原子更新点赞数。
     *
     * 为什么不用「先查再写」：并发下会丢失更新。
     * 为什么用 GREATEST(...,0)：取消点赞与事务重试可能重复扣减，兜住负数。
     * 自定义 SQL 不走 MyBatis-Plus 的逻辑删除拦截，所以条件里显式带上 deleted = 0。
     */
    @Update("UPDATE blog_article SET like_count = GREATEST(like_count + #{delta}, 0), update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE blog_article SET favorite_count = GREATEST(favorite_count + #{delta}, 0), update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int updateFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE blog_article SET comment_count = GREATEST(comment_count + #{delta}, 0), update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int updateCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /** 作者获赞总数（个人主页展示用，走 idx_article_author 索引） */
    @Select("SELECT COALESCE(SUM(like_count), 0) FROM blog_article "
            + "WHERE author_id = #{authorId} AND status = 1 AND deleted = 0")
    long sumLikeCountByAuthor(@Param("authorId") Long authorId);

    /** 作者总浏览量 */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM blog_article "
            + "WHERE author_id = #{authorId} AND status = 1 AND deleted = 0")
    long sumViewCountByAuthor(@Param("authorId") Long authorId);
}
