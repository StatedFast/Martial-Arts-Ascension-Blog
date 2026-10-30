package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.dto.LoginDTO;
import spring.ai.example.spring_ai_demo.dto.RegisterDTO;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.exception.BusinessException;
import spring.ai.example.spring_ai_demo.exception.TokenInvalidException;
import spring.ai.example.spring_ai_demo.mapper.ArticleMapper;
import spring.ai.example.spring_ai_demo.mapper.CommentMapper;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;
import spring.ai.example.spring_ai_demo.service.FileStorageService;
import spring.ai.example.spring_ai_demo.service.LoginAttemptService;
import spring.ai.example.spring_ai_demo.service.TokenBlacklistService;
import spring.ai.example.spring_ai_demo.service.UserService;
import spring.ai.example.spring_ai_demo.utils.JwtUtils;
import spring.ai.example.spring_ai_demo.utils.PasswordValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final JwtUtils jwtUtils;
    private final LocalCacheService cacheService;
    private final TokenBlacklistService blacklistService;
    private final LoginAttemptService loginAttemptService;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;

    public UserServiceImpl(JwtUtils jwtUtils, LocalCacheService cacheService,
                           TokenBlacklistService blacklistService,
                           LoginAttemptService loginAttemptService,
                           FileStorageService fileStorageService,
                           ArticleMapper articleMapper, CommentMapper commentMapper) {
        this.jwtUtils = jwtUtils;
        this.cacheService = cacheService;
        this.blacklistService = blacklistService;
        this.loginAttemptService = loginAttemptService;
        this.fileStorageService = fileStorageService;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.passwordEncoder = new BCryptPasswordEncoder(12); // cost=12，企业级强度
    }

    private static final String USER_CACHE_PREFIX = "blog:user:";

    @Override
    @Transactional
    public Map<String, Object> register(RegisterDTO dto) {
        // 密码强度校验
        String passwordError = PasswordValidator.validate(dto.getPassword());
        if (passwordError != null) {
            throw new BusinessException(400, passwordError);
        }

        long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(409, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // BCrypt加密
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        user.setAvatar("/images/default-avatar.svg");
        save(user);

        // 生成双令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", 1800); // 30分钟（秒）
        result.put("user", buildUserVO(user));
        return result;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        return login(dto, null);
    }

    @Override
    public Map<String, Object> login(LoginDTO dto, String ip) {
        // 检查IP是否被限制
        if (ip != null && loginAttemptService.isIpBlocked(ip)) {
            log.warn("IP被限制登录: ip={}", ip);
            throw new BusinessException(429, "操作过于频繁，请稍后再试");
        }

        // 检查账号是否被锁定
        if (loginAttemptService.isAccountLocked(dto.getUsername())) {
            log.warn("账号被锁定: username={}", dto.getUsername());
            throw new BusinessException(423, "账号已被锁定，请30分钟后再试");
        }

        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            loginAttemptService.loginFailed(dto.getUsername(), ip != null ? ip : "unknown");
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(dto.getUsername(), ip != null ? ip : "unknown");
            int remaining = loginAttemptService.getRemainingAttempts(dto.getUsername());
            if (remaining <= 2) {
                throw new BusinessException(401, "用户名或密码错误，剩余尝试次数: " + remaining);
            }
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 登录成功，清除失败记录
        loginAttemptService.loginSucceeded(dto.getUsername(), ip != null ? ip : "unknown");

        // 生成双令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        cacheUser(user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", 1800); // 30分钟（秒）
        result.put("user", buildUserVO(user));
        return result;
    }

    @Override
    public void logout(String token) {
        try {
            Claims claims = jwtUtils.parseToken(token);
            String jti = jwtUtils.getJtiFromClaims(claims);
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();

            // 将当前Token加入黑名单
            blacklistService.addToBlacklist(jti, remainingMillis);

            log.info("用户登出成功: userId={}, jti={}", jwtUtils.getUserIdFromClaims(claims), jti);
        } catch (Exception e) {
            // Token已无效，无需处理（可能已过期）
            log.debug("登出时Token已无效: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        // 验证Refresh Token
        Claims claims;
        try {
            claims = jwtUtils.parseToken(refreshToken);
        } catch (Exception e) {
            throw new TokenInvalidException("Refresh Token无效或已过期，请重新登录");
        }

        // 检查Token类型必须是refresh
        String tokenType = jwtUtils.getTokenTypeFromClaims(claims);
        if (!"refresh".equals(tokenType)) {
            throw new TokenInvalidException("提供的不是Refresh Token");
        }

        // 检查黑名单
        String jti = jwtUtils.getJtiFromClaims(claims);
        if (blacklistService.isBlacklisted(jti)) {
            throw new TokenInvalidException("Refresh Token已被吊销，请重新登录");
        }

        // 获取用户信息
        Long userId = jwtUtils.getUserIdFromClaims(claims);
        User user = getUserById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(403, "用户不存在或已被禁用");
        }

        // 将旧的Refresh Token加入黑名单（一次性使用，防止重放）
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        blacklistService.addToBlacklist(jti, remainingMillis);

        // 签发新的双令牌
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        log.info("Token刷新成功: userId={}", userId);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", 1800);
        return result;
    }

    @Override
    public User getUserById(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        User cached = (User) cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        User user = getById(id);
        if (user != null) {
            cacheUser(user);
        }
        return user;
    }

    @Override
    @Transactional
    public boolean updateProfile(Long userId, String nickname, String bio, String email) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setBio(bio);
        user.setEmail(email);
        boolean updated = updateById(user);
        if (updated) {
            cacheService.delete(USER_CACHE_PREFIX + userId);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean updateAvatar(Long userId, String avatarUrl) {
        User existing = getById(userId);
        if (existing == null) {
            return false;
        }
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        boolean updated = updateById(user);
        if (updated) {
            cacheService.delete(USER_CACHE_PREFIX + userId);
            // 清理旧头像文件（仅删除上传目录下的文件，默认头像/外链自动跳过）
            if (existing.getAvatar() != null && !existing.getAvatar().equals(avatarUrl)) {
                fileStorageService.deleteAvatar(existing.getAvatar());
            }
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword, String currentToken) {
        // 新密码强度校验
        String passwordError = PasswordValidator.validate(newPassword);
        if (passwordError != null) {
            throw new BusinessException(400, passwordError);
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        boolean updated = updateById(user);

        // 修改密码后吊销当前Token，强制重新登录
        if (updated && currentToken != null) {
            try {
                Claims claims = jwtUtils.parseToken(currentToken);
                String jti = jwtUtils.getJtiFromClaims(claims);
                long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
                blacklistService.addToBlacklist(jti, remainingMillis);
                log.info("修改密码后吊销Token: userId={}, jti={}", userId, jti);
            } catch (Exception e) {
                log.debug("修改密码时Token已无效: {}", e.getMessage());
            }
        }

        return updated;
    }

    private void cacheUser(User user) {
        cacheService.set(USER_CACHE_PREFIX + user.getId(), user, 1, TimeUnit.HOURS);
    }

    @Override
    public Map<String, Object> getPublicProfile(Long userId) {
        User user = getUserById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(404, "用户不存在");
        }

        // 白名单字段：禁止把 password / email / deleted 等内部字段带给前端
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername());
        vo.put("avatar", user.getAvatar());
        vo.put("bio", user.getBio());
        vo.put("role", user.getRole());
        vo.put("createTime", user.getCreateTime());

        // 统计：文章数/评论数走 count，获赞与阅读量走聚合，均命中对应索引
        vo.put("articleCount", articleMapper.selectCount(
                new LambdaQueryWrapper<spring.ai.example.spring_ai_demo.entity.Article>()
                        .eq(spring.ai.example.spring_ai_demo.entity.Article::getAuthorId, userId)
                        .eq(spring.ai.example.spring_ai_demo.entity.Article::getStatus, 1)));
        vo.put("commentCount", commentMapper.selectCount(
                new LambdaQueryWrapper<spring.ai.example.spring_ai_demo.entity.Comment>()
                        .eq(spring.ai.example.spring_ai_demo.entity.Comment::getUserId, userId)
                        .eq(spring.ai.example.spring_ai_demo.entity.Comment::getStatus, 1)));
        vo.put("likeCount", articleMapper.sumLikeCountByAuthor(userId));
        vo.put("viewCount", articleMapper.sumViewCountByAuthor(userId));
        return vo;
    }

    @Override
    public List<User> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim();
        return list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .and(w -> w.likeRight(User::getNickname, kw).or().likeRight(User::getUsername, kw))
                .last("LIMIT " + Math.max(1, Math.min(limit, 10))));
    }

    private Map<String, Object> buildUserVO(User user) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname());
        vo.put("email", user.getEmail());
        vo.put("avatar", user.getAvatar());
        vo.put("bio", user.getBio());
        vo.put("role", user.getRole());
        return vo;
    }
}
