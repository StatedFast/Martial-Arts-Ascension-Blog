package spring.ai.example.spring_ai_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.service.MonitorMetricsService;

/**
 * 渡劫看板接口：对所有人开放（含未登录游客）。
 *
 * <p>与配置中心的分工：这里只有「读」，没有任何写入口 ——
 * 开关、注入速率、回收这些能改变 JVM 状态的操作全部留在 SysConfigController（ADMIN 专属）。
 * 所以 @NoAuth 暴露的只是一组运行时数字，不构成越权面。
 *
 * <p>公开范围的取舍：JDK 版本、操作系统这类环境指纹对游客隐去（见 jvmInfo(false)），
 * 因为游客拿它没有任何产品价值，攻击者却能拿来比对 CVE；内存百分比和 -Xmx 才是看板要展示的内容。
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorMetricsService monitorMetricsService;

    /** 实时内存快照（MB）+ 演练状态，看板曲线的数据源 */
    @NoAuth
    @GetMapping("/memory")
    public Result<?> memory() {
        return Result.success(monitorMetricsService.memorySnapshot());
    }

    /** JVM 核心参数（只读，游客版不含环境指纹） */
    @NoAuth
    @GetMapping("/jvm")
    public Result<?> jvm() {
        return Result.success(monitorMetricsService.jvmInfo(false));
    }
}
