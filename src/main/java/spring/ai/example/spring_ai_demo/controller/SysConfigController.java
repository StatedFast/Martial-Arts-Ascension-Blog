package spring.ai.example.spring_ai_demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.config.BoundedCacheService;
import spring.ai.example.spring_ai_demo.entity.SysConfig;
import spring.ai.example.spring_ai_demo.service.SysConfigService;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置中心接口（仅管理员）。
 *
 * 认证：/api/** 已由 JwtInterceptor 统一校验登录并把 userId/username/role 写入 request attribute，
 * 这里只做「角色 = ADMIN」的二次校验，非管理员直接 403。
 */
@RestController
@RequestMapping("/api/admin/config")
public class SysConfigController {

    private static final String ADMIN_ROLE = "ADMIN";

    private final SysConfigService sysConfigService;
    private final BoundedCacheService boundedCacheService;

    public SysConfigController(SysConfigService sysConfigService, BoundedCacheService boundedCacheService) {
        this.sysConfigService = sysConfigService;
        this.boundedCacheService = boundedCacheService;
    }

    /** 配置项列表（含说明），供配置中心页面渲染 */
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
     * 实时内存快照（MB）。配置中心页面据此画出内存曲线，
     * 演练开启后可直观看到堆 / 直接内存阶梯上涨。
     */
    @GetMapping("/memory")
    public Result<?> memory(@RequestAttribute("role") String role) {
        if (!isAdmin(role)) {
            return Result.forbidden("仅管理员可访问配置中心");
        }
        Runtime runtime = Runtime.getRuntime();
        long heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long heapMaxMb = runtime.maxMemory() / (1024 * 1024);

        long nonHeapUsedMb = ManagementFactory.getMemoryMXBean()
                .getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("heapUsedMb", heapUsedMb);
        data.put("heapMaxMb", heapMaxMb);
        data.put("nonHeapUsedMb", nonHeapUsedMb);
        data.put("faultEnabled", sysConfigService.getBoolean(SysConfigService.FAULT_ENABLED));
        data.put("faultType", sysConfigService.getFaultType());
        data.put("faultFixed", sysConfigService.getBoolean(SysConfigService.FAULT_FIXED));
        // 修复模式生效时，展示有界 LRU 当前条目数与累计淘汰次数，直观证明「淘汰在工作、内存封顶」
        data.put("boundedCacheSize", boundedCacheService.size());
        data.put("boundedCacheEvicted", boundedCacheService.evictedCount());
        return Result.success(data);
    }

    private boolean isAdmin(String role) {
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role.trim());
    }
}
