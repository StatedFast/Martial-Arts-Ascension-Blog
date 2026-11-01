package spring.ai.example.spring_ai_demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.service.UserService;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    /** 博主名片兜底文案（管理员资料缺失时使用） */
    private static final String DEFAULT_AUTHOR_NAME = "博主";
    private static final String DEFAULT_AUTHOR_AVATAR = "/images/default-avatar.svg";
    private static final String DEFAULT_AUTHOR_BIO = "记录技术成长 · 分享编程心得";

    private final UserService userService;

    /** 首页顶部封面背景图（/images/xxx.jpg）；留空则用默认渐变占位 */
    @Value("${home.banner-image:}")
    private String bannerImage;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "修仙心得 - 首页");
        model.addAttribute("author", loadAuthorCard());
        // 未配置背景图时不输出 style 属性，页面直接走渐变占位，避免一次 404 请求
        String bannerUrl = hasText(bannerImage) ? bannerImage.trim() : null;
        if (bannerUrl != null) {
            // bannerUrl 用于 head 里的 preload，与 CSS 变量里的地址保持一致才能命中同一份缓存
            model.addAttribute("bannerUrl", bannerUrl);
            model.addAttribute("bannerStyle", "--home-banner: url('" + bannerUrl + "');");
        }
        return "index";
    }

    @GetMapping("/article/{id}")
    public String article(@PathVariable Long id, Model model) {
        model.addAttribute("articleId", id);
        model.addAttribute("title", "文章详情 - 修仙心得");
        return "article";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    /** 他人主页：/user/{id} */
    @GetMapping("/user/{id}")
    public String userHome(@PathVariable Long id, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("title", "用户主页 - 修仙心得");
        return "user";
    }

    /** 通知中心 */
    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("title", "通知中心 - 修仙心得");
        return "notifications";
    }

    @GetMapping("/write")
    public String write() {
        return "write";
    }

    /** 配置中心（仅管理员，页面内会二次校验角色；非管理员接口会返回 403） */
    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("title", "配置中心 - 修仙心得");
        return "config";
    }

    /** 渡劫看板：全站公开入口（数据走 @NoAuth 的 /api/monitor/**），游客也能看内存曲线 */
    @GetMapping("/monitor")
    public String monitor(Model model) {
        model.addAttribute("title", "JVM 渡劫看板 - 修仙心得");
        return "monitor";
    }

    /** 配置项（k-v 全表，仅管理员）：从看板里拆出来，避免监控台和改配置混在同一屏 */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("title", "配置项 - 修仙心得");
        return "settings";
    }

    /**
     * 首页博主名片数据：取站点管理员账号，缺失字段回退到默认文案。
     * 只读单行查询，走主键/角色索引，避免前端再发一次请求。
     */
    private Map<String, String> loadAuthorCard() {
        Map<String, String> card = new HashMap<>();
        card.put("name", DEFAULT_AUTHOR_NAME);
        card.put("avatar", DEFAULT_AUTHOR_AVATAR);
        card.put("bio", DEFAULT_AUTHOR_BIO);

        User admin = userService.lambdaQuery()
                .eq(User::getRole, "ADMIN")
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId)
                .last("LIMIT 1")
                .one();
        if (admin == null) {
            return card;
        }
        if (hasText(admin.getNickname())) {
            card.put("name", admin.getNickname());
        } else if (hasText(admin.getUsername())) {
            card.put("name", admin.getUsername());
        }
        if (hasText(admin.getAvatar())) {
            card.put("avatar", admin.getAvatar());
        }
        if (hasText(admin.getBio())) {
            card.put("bio", admin.getBio());
        }
        return card;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
