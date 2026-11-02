package spring.ai.example.spring_ai_demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Cultivator;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Realm;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Style;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Trait;
import spring.ai.example.spring_ai_demo.cultivation.CultivationCatalog;
import spring.ai.example.spring_ai_demo.cultivation.DuelEngine;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.service.UserService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 修仙排行榜（人界篇）接口：与「渡劫看板」「访问看板」同口径 —— 全站公开、只读、没有写。
 *
 * <p>公开的前提是这份数据本来就是小说设定，不含任何真实用户信息；
 * 人物昵称与头像取自 blog_user 里的角色账号，取不到时退回设定里的本名，榜单不会因此空一格。
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final CultivationCatalog catalog;
    private final DuelEngine duelEngine;
    private final UserService userService;

    /** 榜单一次性快照：境界塔 + 30 个人物 + 路数说明，前端只需要这一个请求就能铺满全页 */
    @NoAuth
    @RateLimit(limit = 30, window = 1)
    @GetMapping("/roster")
    public Result<Map<String, Object>> roster() {
        Map<Long, User> accounts = loadAccounts();
        List<Cultivator> ranked = catalog.ranked();
        double minPower = ranked.get(ranked.size() - 1).power();
        double maxPower = ranked.get(0).power();

        List<Map<String, Object>> people = new ArrayList<>();
        int rank = 1;
        for (Cultivator c : ranked) {
            people.add(personView(c, accounts, rank++, minPower, maxPower));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scope", "凡人修仙传 · 人界篇");
        payload.put("scopeNote", "上古古魔入侵后灵气稀薄，人界修士最高只能到化神初期；"
                + "本榜取的是「人界篇终局」的境界，不是他们中途最风光的那一刻。");
        payload.put("total", people.size());
        payload.put("realms", realmShapes(accounts, minPower, maxPower));
        payload.put("people", people);
        payload.put("styles", styleGuide());
        return Result.success(payload);
    }

    /** 两人对打：结果由服务端算，同种子必同结果，前端只负责播 */
    @NoAuth
    @RateLimit(limit = 10, window = 1, message = "斗法过于频繁，先运一轮气")
    @GetMapping("/duel")
    public Result<DuelEngine.Duel> duel(@RequestParam Long a, @RequestParam Long b) {
        if (a == null || b == null || a.equals(b)) {
            return Result.error(400, "请选择两位不同的人物");
        }
        Cultivator ca = catalog.find(a).orElse(null);
        Cultivator cb = catalog.find(b).orElse(null);
        if (ca == null || cb == null) {
            return Result.error(404, "此人不在人界篇榜单内（榜单只收 101-130 这三十位）");
        }
        return duelEngine.duel(ca, cb, avatars())
                .map(Result::success)
                .orElseGet(() -> Result.error(500, "推演失败，稍后再试"));
    }

    // ==================== 装配 ====================

    private Map<String, Object> personView(Cultivator c, Map<Long, User> accounts, int rank,
                                           double minPower, double maxPower) {
        User account = accounts.get(c.userId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", c.userId());
        m.put("rank", rank);
        // 昵称以库里的为准：博主哪天给角色改了名，榜单会跟着改，不会出现两处对不上
        m.put("name", account != null && hasText(account.getNickname()) ? account.getNickname() : c.name());
        m.put("avatar", account != null && hasText(account.getAvatar())
                ? account.getAvatar() : "/images/default-avatar.svg");
        m.put("homepage", "/user/" + c.userId());
        m.put("realm", c.realm().name());
        m.put("realmLabel", c.realm().getLabel());
        m.put("realmColor", c.realm().getColor());
        m.put("subText", c.subText());
        m.put("title", c.title());
        m.put("origin", c.origin());
        m.put("technique", c.technique());
        m.put("style", c.style().name());
        m.put("styleLabel", c.style().getLabel());
        m.put("styleEssence", c.style().getEssence());
        m.put("traits", c.traits().stream().map(Trait::getLabel).toList());
        m.put("power", Math.round(c.power()));
        m.put("powerBar", powerBar(c.power(), minPower, maxPower));
        m.put("basis", c.basis().name());
        m.put("basisLabel", c.basis().getLabel());
        m.put("basisNote", c.basisNote());
        m.put("quote", c.quote());
        m.put("skills", c.skills());
        return m;
    }

    /** 境界塔的每一层：人数、代表人物、寿元 —— 图形化直接吃这个数组 */
    private List<Map<String, Object>> realmShapes(Map<Long, User> accounts, double minPower, double maxPower) {
        Map<Realm, List<Cultivator>> grouped = catalog.groupByRealm();
        List<Map<String, Object>> realms = new ArrayList<>();
        for (Realm realm : Realm.values()) {
            List<Cultivator> people = grouped.get(realm);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("realm", realm.name());
            m.put("label", realm.getLabel());
            m.put("rule", realm.getRule());
            m.put("color", realm.getColor());
            m.put("lifespan", realm.getLifespan());
            m.put("note", realm.getNote());
            m.put("count", people.size());
            m.put("base", Math.round(realm.getBase()));
            m.put("names", people.stream().map(Cultivator::name).toList());
            m.put("top", people.isEmpty() ? null : personView(people.get(0), accounts, 0, minPower, maxPower));
            realms.add(m);
        }
        return realms;
    }

    private Map<String, Object> styleGuide() {
        Map<String, Object> guide = new LinkedHashMap<>();
        for (Style s : Style.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", s.getLabel());
            m.put("essence", s.getEssence());
            m.put("beats", readableTokens(s.getBeatsRaw()));
            m.put("weak", readableTokens(s.getWeakRaw()));
            guide.put(s.name(), m);
        }
        return guide;
    }

    /** 把 "SWORD,MAGNET" 翻成 "剑修、元磁"，前端直接显示，不用再拿枚举去查 */
    private List<String> readableTokens(String csv) {
        List<String> labels = new ArrayList<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            if ("ALL".equals(t)) {
                labels.add("一切");
                continue;
            }
            try {
                labels.add(Style.valueOf(t).getLabel());
            } catch (IllegalArgumentException ignored) {
                // 表里写了不存在的路数名：宁可少显示一项，也不要把整个榜单接口打挂
            }
        }
        return labels;
    }

    /** 战力跨了四个数量级，线性条会把凡人压成 0，这里取对数再铺满 */
    private double powerBar(double power, double min, double max) {
        double lo = Math.log(Math.max(1, min));
        double hi = Math.log(Math.max(2, max));
        if (hi <= lo) {
            return 100;
        }
        double ratio = (Math.log(Math.max(1, power)) - lo) / (hi - lo);
        return Math.round(Math.max(0, Math.min(1, ratio)) * 1000) / 10.0;
    }

    private Map<Long, User> loadAccounts() {
        List<Long> ids = catalog.roster().stream().map(Cultivator::userId).toList();
        Map<Long, User> accounts = new LinkedHashMap<>();
        try {
            for (User u : userService.lambdaQuery().in(User::getId, ids).list()) {
                accounts.put(u.getId(), u);
            }
        } catch (Exception e) {
            // 榜单是设定 + 昵称头像，查不到用户也要能出图，不能因为一次查询失败就整页 500
        }
        return accounts;
    }

    private Map<Long, String> avatars() {
        Map<Long, String> map = new LinkedHashMap<>();
        loadAccounts().forEach((id, u) -> map.put(id, hasText(u.getAvatar()) ? u.getAvatar() : null));
        return map;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
