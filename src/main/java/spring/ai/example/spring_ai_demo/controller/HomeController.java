package spring.ai.example.spring_ai_demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Spring Blog - 首页");
        return "index";
    }

    @GetMapping("/article/{id}")
    public String article(@PathVariable Long id, Model model) {
        model.addAttribute("articleId", id);
        model.addAttribute("title", "文章详情");
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

    @GetMapping("/write")
    public String write() {
        return "write";
    }
}
