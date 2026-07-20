package spring.ai.example.spring_ai_demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.LoginDTO;
import spring.ai.example.spring_ai_demo.dto.RegisterDTO;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.service.UserService;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    @GetMapping("/profile")
    public Result<?> profile(@RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestAttribute("userId") Long userId,
                                   @RequestParam(required = false) String nickname,
                                   @RequestParam(required = false) String bio,
                                   @RequestParam(required = false) String email) {
        boolean updated = userService.updateProfile(userId, nickname, bio, email);
        return updated ? Result.success() : Result.error("更新失败");
    }

    @PutMapping("/avatar")
    public Result<?> updateAvatar(@RequestAttribute("userId") Long userId,
                                  @RequestParam String avatarUrl) {
        boolean updated = userService.updateAvatar(userId, avatarUrl);
        return updated ? Result.success() : Result.error("头像更新失败");
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

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
