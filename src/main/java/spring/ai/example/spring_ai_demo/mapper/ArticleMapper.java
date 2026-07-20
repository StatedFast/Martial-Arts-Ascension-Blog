package spring.ai.example.spring_ai_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import spring.ai.example.spring_ai_demo.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
