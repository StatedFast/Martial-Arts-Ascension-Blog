package spring.ai.example.spring_ai_demo.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.service.UserService;

import java.time.Duration;

/**
 * 文字头像生成：以用户名/昵称的第一个字（英文取首字母）作为圆形头像。
 *
 * <p>新注册用户默认把 avatar 指向本接口（{@code /avatar/text/{id}}），
 * 这样文章、评论、导航栏、个人中心等所有 {@code <img src>} 展示位都能直接显示，
 * 无需改动各处前端逻辑；用户后续自行上传图片时 avatar 会被替换为
 * {@code /uploads/avatars/...}，本接口不再被引用，自定义头像自然保持不变。
 *
 * <p>接口对外公开（路径不在 {@code /api/**} 下，不经过登录拦截器），
 * 且每次实时读取昵称，改昵称后头像随之更新。
 */
@RestController
public class AvatarController {

    /** 文字头像访问前缀，注册时拼装 avatar URL 用 */
    public static final String TEXT_AVATAR_PREFIX = "/avatar/text/";

    private static final MediaType SVG_TYPE = MediaType.valueOf("image/svg+xml;charset=UTF-8");

    /** 圆形头像配色（起止渐变色），按字符稳定取色，让不同用户颜色不同 */
    private static final String[][] PALETTE = {
            {"#3b82f6", "#1e40af"}, {"#8b5cf6", "#5b21b6"}, {"#ec4899", "#9d174d"},
            {"#f97316", "#c2410c"}, {"#10b981", "#047857"}, {"#06b6d4", "#0e7490"},
            {"#6366f1", "#3730a3"}, {"#ef4444", "#b91c1c"}, {"#14b8a6", "#0f766e"},
            {"#a855f7", "#6b21a8"}, {"#eab308", "#a16207"}, {"#64748b", "#334155"},
    };

    private final UserService userService;

    public AvatarController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = TEXT_AVATAR_PREFIX + "{id:\\d+}", produces = "image/svg+xml")
    public ResponseEntity<String> textAvatar(@PathVariable Long id) {
        User user = userService.getUserById(id);
        String name = user == null ? "?" : displayName(user);
        String ch = name.isEmpty() ? "?" : name.substring(0, 1);
        return ResponseEntity.ok()
                .contentType(SVG_TYPE)
                // 允许浏览器/CDN 缓存，但昵称可能变动，给一个较短的 TTL 兜底刷新
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(buildSvg(ch, id == null ? 0L : id));
    }

    private String displayName(User user) {
        String nickname = user.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        String username = user.getUsername();
        return username == null ? "" : username.trim();
    }

    /**
     * 生成 100x100 圆形渐变底 + 大号首字。字号取 64：在半径 50 的圆内尽量放大，
     * 单字半高 32、对角半径 32*√2≈45 仍小于 50，通过 central 基线 + 中锚居中，不超出圆框。
     */
    private String buildSvg(String ch, long id) {
        String[] colors = PALETTE[Math.abs((int) (id * 31 + ch.charAt(0))) % PALETTE.length];
        String gid = "g" + id;
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"100\" height=\"100\">"
                + "<defs><linearGradient id=\"" + gid + "\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
                + "<stop offset=\"0\" stop-color=\"" + colors[0] + "\"/>"
                + "<stop offset=\"1\" stop-color=\"" + colors[1] + "\"/>"
                + "</linearGradient></defs>"
                + "<circle cx=\"50\" cy=\"50\" r=\"50\" fill=\"url(#" + gid + ")\"/>"
                + "<text x=\"50\" y=\"50\" dy=\"0.05em\" text-anchor=\"middle\" dominant-baseline=\"central\""
                + " font-family=\"'PingFang SC','Microsoft YaHei','Hiragino Sans GB',sans-serif\""
                + " font-size=\"64\" font-weight=\"700\" fill=\"#ffffff\">" + escapeXml(ch) + "</text>"
                + "</svg>";
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
