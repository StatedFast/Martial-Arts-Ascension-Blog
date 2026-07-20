package spring.ai.example.spring_ai_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import spring.ai.example.spring_ai_demo.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
