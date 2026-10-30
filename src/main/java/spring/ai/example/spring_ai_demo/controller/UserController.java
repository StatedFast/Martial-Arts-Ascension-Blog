package spring.ai.example.spring_ai_demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.dto.LoginDTO;
import spring.ai.example.spring_ai_demo.dto.RegisterDTO;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.service.ArticleService;
import spring.ai.example.spring_ai_demo.service.CommentService;
import spring.ai.example.spring_ai_demo.service.FileStorageService;
import spring.ai.example.spring_ai_demo.service.InteractionService;
import spring.ai.example.spring_ai_demo.service.UserService;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ArticleService articleService;
    private final InteractionService interactionService;
    private final CommentService commentService;

    public UserController(UserService userService, FileStorageService fileStorageService,
                          ArticleService articleService, InteractionService interactionService,
                          CommentService commentService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.articleService = articleService;
        this.interactionService = interactionService;
        this.commentService = commentService;
    }

    // ==================== 登录注册 ====================

    @NoAuth
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @NoAuth
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = IpUtils.getIpAddress(request);
        return Result.success(userService.login(dto, ip));
    }

    /**
     * 登出：将当前Token加入黑名单
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return Result.success("登出成功");
    }

    /**
     * 刷新Token：使用RefreshToken换取新的AccessToken
     */
    @NoAuth
    @PostMapping("/refresh-token")
    public Result<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error(400, "refreshToken不能为空");
        }
        return Result.success(userService.refreshToken(refreshToken));
    }

    // ==================== 自己的资料 ====================

    @GetMapping("/profile")
    public Result<?> profile(@RequestAttribute("userId") Long userId) {
        Map<String, Object> profile = userService.getPublicProfile(userId);
        // 本人可见邮箱，公开主页不返回
        User self = userService.getUserById(userId);
        profile.put("email", self == null ? null : self.getEmail());
        profile.put("username", self == null ? null : self.getUsername());
        return Result.success(profile);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestAttribute("userId") Long userId,
                                   @RequestParam(required = false) String nickname,
                                   @RequestParam(required = false) String bio,
                                   @RequestParam(required = false) String email) {
        boolean updated = userService.updateProfile(userId, nickname, bio, email);
        return updated ? Result.success() : Result.error("更新失败");
    }

    /**
     * 修改头像：上传本地图片文件，返回头像访问URL
     */
    @PostMapping("/avatar")
    public Result<?> updateAvatar(@RequestAttribute("userId") Long userId,
                                  @RequestParam("file") MultipartFile file) {
        String avatarUrl = fileStorageService.storeAvatar(file, userId);
        boolean updated = userService.updateAvatar(userId, avatarUrl);
        return updated ? Result.success(avatarUrl) : Result.error("头像更新失败");
    }

    /**
     * 修改密码（修改后当前Token失效，需重新登录）
     */
    @PutMapping("/password")
    public Result<?> changePassword(@RequestAttribute("userId") Long userId,
                                    @RequestParam String oldPassword,
                                    @RequestParam String newPassword,
                                    HttpServletRequest request) {
        String token = extractToken(request);
        userService.changePassword(userId, oldPassword, newPassword, token);
        return Result.success("密码修改成功，请重新登录");
    }

    // ==================== 他人主页（公开） ====================

    @NoAuth
    @GetMapping("/{id:\\d+}/profile")
    public Result<?> publicProfile(@PathVariable Long id) {
        return Result.success(userService.getPublicProfile(id));
    }

    @NoAuth
    @GetMapping("/{id:\\d+}/articles")
    public Result<?> authorArticles(@PathVariable Long id,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(articleService.getAuthorArticles(id, page, size, userId));
    }

    // ==================== 个人内容中心 ====================

    @GetMapping("/articles")
    public Result<?> myArticles(@RequestAttribute("userId") Long userId,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) Integer status) {
        return Result.success(articleService.getMyArticles(userId, page, size, status));
    }

    @GetMapping("/favorites")
    public Result<?> myFavorites(@RequestAttribute("userId") Long userId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(interactionService.myFavorites(userId, page, size));
    }

    @GetMapping("/likes")
    public Result<?> myLikes(@RequestAttribute("userId") Long userId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(interactionService.myLikes(userId, page, size));
    }

    @GetMapping("/comments")
    public Result<?> myComments(@RequestAttribute("userId") Long userId,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Page<CommentDTO> comments = commentService.getUserComments(userId, page, size);
        return Result.success(comments);
    }

    // ==================== @提及自动补全 ====================

    /**
     * 按昵称/用户名前缀搜索用户，只返回 id / 昵称 / 头像，供评论框 @ 下拉使用。
     */
    @GetMapping("/suggest")
    public Result<?> suggest(@RequestParam(required = false) String keyword) {
        List<Map<String, Object>> users = userService.searchUsers(keyword, 8).stream()
                .map(u -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", u.getId());
                    item.put("nickname", u.getNickname() != null && !u.getNickname().isBlank()
                            ? u.getNickname() : u.getUsername());
                    item.put("avatar", u.getAvatar());
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(users);
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
