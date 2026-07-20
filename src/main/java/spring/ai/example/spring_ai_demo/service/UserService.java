package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import spring.ai.example.spring_ai_demo.dto.LoginDTO;
import spring.ai.example.spring_ai_demo.dto.RegisterDTO;
import spring.ai.example.spring_ai_demo.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /** 注册 */
    Map<String, Object> register(RegisterDTO dto);

    /** 登录，返回AccessToken + RefreshToken */
    Map<String, Object> login(LoginDTO dto);

    /** 登录（带IP记录，用于登录限流） */
    Map<String, Object> login(LoginDTO dto, String ip);

    /** 登出（将Token加入黑名单） */
    void logout(String token);

    /** 使用RefreshToken刷新AccessToken */
    Map<String, Object> refreshToken(String refreshToken);

    /** 根据ID获取用户 */
    User getUserById(Long id);

    /** 更新用户信息 */
    boolean updateProfile(Long userId, String nickname, String bio, String email);

    /** 更新头像 */
    boolean updateAvatar(Long userId, String avatarUrl);

    /** 修改密码（同时吊销当前Token） */
    boolean changePassword(Long userId, String oldPassword, String newPassword, String currentToken);
}
