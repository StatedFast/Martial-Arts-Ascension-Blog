package spring.ai.example.spring_ai_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.cultivation.TreasureCatalog;
import spring.ai.example.spring_ai_demo.cultivation.TreasureCatalog.Treasure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 混沌万灵榜接口：法宝分页 + 品阶筛选 + 关键词搜索。
 *
 * <p>与榜单、看板同口径：全站公开只读，数据是小说设定的静态摘录。
 */
@RestController
@RequestMapping("/api/treasures")
@RequiredArgsConstructor
public class TreasureController {

    private final TreasureCatalog catalog;

    /** 分页 + 筛选。tier 传「全部」或空表示不筛；keyword 同时匹配名称/持有者/类型/描述 */
    @NoAuth
    @RateLimit(limit = 40, window = 1)
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String keyword) {

        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(24, size));
        String tierKey = tier == null ? "" : tier.trim();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<Treasure> filtered = new ArrayList<>();
        for (Treasure t : catalog.all()) {
            if (!tierKey.isEmpty() && !"全部".equals(tierKey) && !tierKey.equals(t.tier())) {
                continue;
            }
            if (!kw.isEmpty()) {
                String hay = (t.name() + t.owner() + t.type() + t.effect() + t.origin())
                        .toLowerCase(Locale.ROOT);
                if (!hay.contains(kw)) {
                    continue;
                }
            }
            filtered.add(t);
        }

        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        pageNo = Math.min(pageNo, pages);
        int from = (pageNo - 1) * pageSize;
        int to = Math.min(total, from + pageSize);
        List<Treasure> slice = from >= total ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Treasure t : slice) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", t.rank());
            m.put("name", t.name());
            m.put("tier", t.tier());
            m.put("type", t.type());
            m.put("owner", t.owner());
            m.put("origin", t.origin());
            m.put("effect", t.effect());
            records.add(m);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("records", records);
        payload.put("total", total);
        payload.put("page", pageNo);
        payload.put("size", pageSize);
        payload.put("pages", pages);
        payload.put("tiers", catalog.tiers());
        return Result.success(payload);
    }
}
