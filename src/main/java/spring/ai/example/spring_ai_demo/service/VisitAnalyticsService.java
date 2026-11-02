package spring.ai.example.spring_ai_demo.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.entity.VisitLog;
import spring.ai.example.spring_ai_demo.mapper.VisitLogMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问看板服务：负责两件事 —— 记录访问（写）与聚合看板数据（读）。
 *
 * <p>写入走 {@code @Async}，绝不能让一次埋点拖慢页面渲染；且请求在异步线程里已被回收，
 * 所以这里在调用线程内（拦截器）就把 path/ip/ua/referrer 取成字符串，异步方法只吃字符串参数。
 *
 * <p>读侧只回聚合数字（PV/UV/趋势/分布），不含任何原始 IP 明细，因此看板可公开（见 VisitController 注释）。
 *
 * <p>无接口独立 {@code @Service}：与 MonitorMetricsService 同理，@Async/@Scheduled 逻辑不落 JDK 动态代理接口 Bean。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VisitAnalyticsService {

    private final VisitLogMapper visitLogMapper;

    /** 当前在线近似口径：近 N 分钟内有访问即视为在线 */
    private static final int ONLINE_WINDOW_MINUTES = 5;
    /** 趋势曲线默认回看天数 */
    private static final int TREND_DAYS = 14;
    /** 热门页 / 分布统计的「近期」窗口天数 */
    private static final int RECENT_DAYS = 7;
    /** 访问日志保留天数，过期由定时任务物理清理 */
    public static final int RETENTION_DAYS = 90;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM-dd");

    // ==================== 写：记录一次访问 ====================

    /**
     * 埋点入口（拦截器在请求线程内已把入参取成字符串）。异步落库，失败只记日志、不影响页面。
     *
     * <p>关键：入参全部是值类型字符串，不能把 {@code HttpServletRequest} 传进 {@code @Async} 方法 ——
     * 容器线程在请求结束后会回收/复用 request，异步线程再去读 header 会拿到脏数据或抛错。
     */
    @Async("taskExecutor")
    public void recordVisit(String rawPath, String ip, String ua, String referrer) {
        try {
            VisitLog visit = new VisitLog();
            visit.setPath(truncate(rawPath, 200));
            visit.setPageType(classifyPageType(rawPath));
            visit.setIp(truncate(ip, 50));
            visit.setUserAgent(truncate(ua, 400));
            visit.setBrowser(parseBrowser(ua));
            visit.setOs(parseOs(ua));
            visit.setDeviceType(parseDeviceType(ua));
            visit.setReferrer(truncate(referrer, 255));
            visitLogMapper.insert(visit);
        } catch (Exception e) {
            // 埋点是旁路，任何异常都不能回流到用户体验
            log.debug("记录访问日志失败: {}", e.getMessage());
        }
    }

    /**
     * 定时清理过期访问日志（保留 {@link #RETENTION_DAYS} 天）。由 ScheduledTasks 每日调用。
     *
     * @return 本次删除的行数
     */
    public int cleanExpired() {
        LocalDateTime before = LocalDate.now().minusDays(RETENTION_DAYS).atStartOfDay();
        return visitLogMapper.deleteBefore(before);
    }

    // ==================== 读：看板聚合 ====================

    /**
     * 组装看板一次性所需的全部数据。前端一次拉取即可渲染所有卡片。
     */
    public Map<String, Object> summary() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serverTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("metrics", buildMetrics(now));
        data.put("trend", buildTrend(now));
        data.put("hours", buildHours());
        data.put("topPages", buildTopPages(now));
        data.put("browsers", buildDistribution(visitLogMapper.browserDistribution(recentSince(now), 8)));
        data.put("devices", buildDeviceDistribution(visitLogMapper.deviceDistribution(recentSince(now), 6)));
        data.put("referrers", buildDistribution(visitLogMapper.referrerDistribution(recentSince(now), 8)));
        return data;
    }

    private Map<String, Object> buildMetrics(LocalDateTime now) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayPv", visitLogMapper.countTodayPv());
        m.put("todayUv", visitLogMapper.countTodayUv());
        m.put("totalPv", visitLogMapper.countTotalPv());
        m.put("totalUv", visitLogMapper.countTotalUv());
        m.put("online", visitLogMapper.countOnline(now.minusMinutes(ONLINE_WINDOW_MINUTES)));
        m.put("onlineWindowMinutes", ONLINE_WINDOW_MINUTES);
        return m;
    }

    /**
     * 近 {@link #TREND_DAYS} 天的 PV/UV 双线趋势。查询只回有数据的日子，这里补齐缺失日为 0，
     * 保证前端 X 轴等宽、日期连续（否则曲线日期会错位）。
     */
    private Map<String, Object> buildTrend(LocalDateTime now) {
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Long> pv = new ArrayList<>();
        List<Long> uv = new ArrayList<>();
        Map<String, long[]> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : visitLogMapper.trendByDay(today.minusDays(TREND_DAYS - 1).atStartOfDay())) {
            String d = String.valueOf(row.get("d"));
            byDay.put(d, new long[]{toLong(row.get("pv")), toLong(row.get("uv"))});
        }
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.format(DAY_FMT);
            labels.add(day.format(LABEL_FMT));
            long[] vals = byDay.get(key);
            pv.add(vals == null ? 0L : vals[0]);
            uv.add(vals == null ? 0L : vals[1]);
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", labels);
        trend.put("pv", pv);
        trend.put("uv", uv);
        trend.put("days", TREND_DAYS);
        return trend;
    }

    /** 今日 24 小时 PV 分布，固定返回 24 个槽（无数据补 0），前端画柱状图 */
    private List<Long> buildHours() {
        long[] arr = new long[24];
        for (Map<String, Object> row : visitLogMapper.pvByHour()) {
            int h = (int) toLong(row.get("h"));
            if (h >= 0 && h < 24) {
                arr[h] = toLong(row.get("pv"));
            }
        }
        List<Long> list = new ArrayList<>(24);
        for (long v : arr) {
            list.add(v);
        }
        return list;
    }

    private List<Map<String, Object>> buildTopPages(LocalDateTime now) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : visitLogMapper.topPages(recentSince(now), 10)) {
            String path = String.valueOf(row.get("path"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", path);
            item.put("label", humanizePath(path));
            item.put("pv", toLong(row.get("pv")));
            item.put("uv", toLong(row.get("uv")));
            out.add(item);
        }
        return out;
    }

    private List<Map<String, Object>> buildDistribution(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object name = row.get("name");
            item.put("name", name == null ? "未知" : String.valueOf(name));
            item.put("pv", toLong(row.get("pv")));
            out.add(item);
        }
        return out;
    }

    /** 设备分布：把内部英文枚举翻成中文友好名，其余交给通用逻辑 */
    private List<Map<String, Object>> buildDeviceDistribution(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object name = row.get("name");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", deviceLabel(name == null ? null : String.valueOf(name)));
            item.put("pv", toLong(row.get("pv")));
            out.add(item);
        }
        return out;
    }

    private LocalDateTime recentSince(LocalDateTime now) {
        return now.minusDays(RECENT_DAYS).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    // ==================== 辅助：口径 / 解析 ====================

    /** 来源归一化：无 Referer=直接访问；同域=站内跳转；其余保留 host，避免把整条带 query 的 URL 塞进看板 */
    public String normalizeReferrer(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) {
            return "直接访问";
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String host = uri.getHost();
            if (host == null) {
                return "站内跳转";
            }
            String selfHost = request.getServerName();
            if (host.equalsIgnoreCase(selfHost)) {
                return "站内跳转";
            }
            return host;
        } catch (Exception e) {
            return "其他来源";
        }
    }

    private String classifyPageType(String path) {
        if (path == null || path.isBlank()) {
            return "other";
        }
        if (path.equals("/") || path.equals("/index")) {
            return "home";
        }
        if (path.startsWith("/article")) {
            return "article";
        }
        if (path.startsWith("/user")) {
            return "user";
        }
        if (path.startsWith("/monitor") || path.startsWith("/visits")) {
            return "board";
        }
        if (path.startsWith("/login") || path.startsWith("/register")) {
            return "auth";
        }
        return "other";
    }

    /** 路径转友好标签：/→首页，/article/12→文章 #12，/user/3→用户主页 #3，其余原样 */
    private String humanizePath(String path) {
        if (path == null) {
            return "";
        }
        if (path.equals("/") || path.equals("/index")) {
            return "首页";
        }
        if (path.startsWith("/article/")) {
            return "文章 #" + path.substring("/article/".length());
        }
        if (path.startsWith("/user/")) {
            return "用户主页 #" + path.substring("/user/".length());
        }
        return switch (path) {
            case "/monitor" -> "渡劫看板";
            case "/visits" -> "访问看板";
            case "/rank" -> "修仙排行榜";
            case "/login" -> "登录页";
            case "/register" -> "注册页";
            case "/profile" -> "个人中心";
            case "/write" -> "写作页";
            case "/notifications" -> "通知中心";
            default -> path;
        };
    }

    private String deviceLabel(String device) {
        if (device == null) {
            return "未知";
        }
        return switch (device) {
            case "mobile" -> "手机";
            case "tablet" -> "平板";
            case "desktop" -> "桌面电脑";
            case "bot" -> "爬虫/机器人";
            default -> device;
        };
    }

    // ---- 极简 UA 解析（够用即可，不引第三方库；按含优先级顺序命中） ----

    private String parseBrowser(String ua) {
        if (ua == null || ua.isBlank()) {
            return "未知";
        }
        String u = ua.toUpperCase();
        if (u.contains("MICROMessenger") || ua.contains("MicroMessenger")) return "微信";
        if (u.contains("EDG/") || u.contains("EDGE")) return "Edge";
        if (u.contains("OPR/") || u.contains("OPRA")) return "Opera";
        if (u.contains("Firefox")) return "Firefox";
        if (u.contains("SAFARI") && !u.contains("CHROME")) return "Safari";
        if (u.contains("CHROME") || u.contains("CRIM")) return "Chrome";
        if (u.contains("MSIE") || u.contains("TRIDENT")) return "IE";
        return "其他";
    }

    private String parseOs(String ua) {
        if (ua == null || ua.isBlank()) {
            return "未知";
        }
        String u = ua.toUpperCase();
        if (u.contains("WINDOWS NT") || u.contains("WIN")) return "Windows";
        if (u.contains("IPHONE") || u.contains("IPAD") || u.contains("IOS")) return "iOS";
        if (u.contains("ANDROID")) return "Android";
        if (u.contains("MAC OS X") || u.contains("DARWIN")) return "macOS";
        if (u.contains("LINUX") || u.contains("X11")) return "Linux";
        return "其他";
    }

    private String parseDeviceType(String ua) {
        if (ua == null || ua.isBlank()) {
            return "desktop";
        }
        String u = ua.toUpperCase();
        if (u.contains("BOT") || u.contains("CRAWL") || u.contains("SPIDER") || u.contains("SLURP")) {
            return "bot";
        }
        if (u.contains("IPAD") || u.contains("TABLET")) {
            return "tablet";
        }
        if (u.contains("MOBILE") || u.contains("IPHONE") || u.contains("ANDROID")) {
            return "mobile";
        }
        return "desktop";
    }

    private long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
