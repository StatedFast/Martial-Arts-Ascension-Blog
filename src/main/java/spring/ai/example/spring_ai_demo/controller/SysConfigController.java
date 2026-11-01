package spring.ai.example.spring_ai_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.config.LeakSimulator;
import spring.ai.example.spring_ai_demo.entity.SysConfig;
import spring.ai.example.spring_ai_demo.service.MonitorMetricsService;
import spring.ai.example.spring_ai_demo.service.SysConfigService;

import java.util.List;
import java.util.Map;

/**
 * 配置中心接口（仅管理员）。
 *
 * 认证：/api/** 已由 JwtInterceptor 统一校验登录并把 userId/username/role 写入 request attribute，
 * 这里只做「角色 = ADMIN」的二次校验，非管理员直接 403。
 *
 * 指标采集本身放在 {@link MonitorMetricsService}：对所有人开放的「渡劫看板」
 * （{@link MonitorController}）与这里的配置中心必须共用同一份口径。
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class SysConfigController {

    private static final String ADMIN_ROLE = "ADMIN";

    private final SysConfigService sysConfigService;
    private final LeakSimulator leakSimulator;
    private final MonitorMetricsService monitorMetricsService;

    /** 配置项列表（含说明），供「配置项」页面渲染 */
    @GetMapping
    public Result<?> list(@RequestAttribute("role") String role) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可访问配置中心");
        }
        List<SysConfig> configs = sysConfigService.listAll();
        return Result.success(configs);
    }

    /** 更新单个配置项 */
    @PutMapping
    public Result<?> update(@RequestAttribute("role") String role,
                            @RequestBody Map<String, String> body) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可修改配置");
        }
        String key = body.get("configKey");
        String value = body.get("configValue");
        if (key == null || key.isBlank()) {
            return Result.error(400, "configKey 不能为空");
        }
        boolean ok = sysConfigService.updateValue(key, value == null ? "" : value.trim());
        return ok ? Result.success("已保存") : Result.error("保存失败");
    }

    /**
     * 批量更新配置（「一键剧本」用）。
     * 一个演练剧本往往要同时改总开关 + 泄漏类型 + 注入速率 + 修复模式，
     * 逐条 PUT 会留下「半套状态」（比如开关已开、类型还没改），所以一次请求内按顺序落库。
     */
    @PutMapping("/batch")
    public Result<?> updateBatch(@RequestAttribute("role") String role,
                                 @RequestBody Map<String, String> body) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可修改配置");
        }
        if (body == null || body.isEmpty()) {
            return Result.error(400, "配置项不能为空");
        }
        body.forEach((key, value) -> sysConfigService.updateValue(key, value == null ? "" : value.trim()));
        return Result.success("已应用 " + body.size() + " 项配置", null);
    }

    /** 实时内存快照（MB）：管理员视角，含全量演练状态 */
    @GetMapping("/memory")
    public Result<?> memory(@RequestAttribute("role") String role) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可访问配置中心");
        }
        return Result.success(monitorMetricsService.memorySnapshot());
    }

    /**
     * JVM 核心参数（只读，仅管理员）。
     * 管理员版给全量，含 JDK 版本与操作系统 —— 排查环境差异时才用得上。
     */
    @GetMapping("/jvm")
    public Result<?> jvmInfo(@RequestAttribute("role") String role) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可访问配置中心");
        }
        return Result.success(monitorMetricsService.jvmInfo(true));
    }

    /**
     * 回收已泄漏内存（仅管理员、仅演练用）：堆内 + 堆外一起。
     * 堆内：摘除泄漏缓存条目的强引用；堆外：清 DirectByteBuffer 引用池。
     * 最后由 purgeHeapLeak 触发一次 GC，同时让堆内垃圾回收、堆外 Cleaner 工作，曲线当场回落。
     */
    @PostMapping("/recover")
    public Result<?> recover(@RequestAttribute("role") String role) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可操作");
        }
        int directReleased = leakSimulator.releaseDirectBuffers();
        int heapRemoved = leakSimulator.purgeHeapLeak(); // 内部已含 System.gc()
        return Result.success("已回收：堆内泄漏条目 " + heapRemoved + " 个，堆外 Buffer " + directReleased + " 个", null);
    }

    private boolean isAdmin(String role) {
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role.trim());
    }
}
