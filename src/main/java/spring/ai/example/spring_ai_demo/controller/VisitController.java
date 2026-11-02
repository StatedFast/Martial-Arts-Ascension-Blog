package spring.ai.example.spring_ai_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.service.VisitAnalyticsService;

/**
 * 访问看板接口：对所有人开放（含未登录游客），与「渡劫看板」同一口径 —— 只有读、没有写。
 *
 * <p>公开边界的安全前提：返回的全是聚合数字（PV/UV/趋势/分布），
 * 不含任何原始 IP、UA 明细，所以暴露的只是「本站热度」，不构成隐私泄露面。
 * 真正能读原始日志明细的能力（若有）应另设 ADMIN 专属接口，这里不提供。
 */
@RestController
@RequestMapping("/api/visit")
@RequiredArgsConstructor
public class VisitController {

    private final VisitAnalyticsService visitAnalyticsService;

    /** 看板一次性快照：核心指标 + 14 日趋势 + 今日时段分布 + 热门页 + 终端/来源分布 */
    @NoAuth
    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.success(visitAnalyticsService.summary());
    }
}
