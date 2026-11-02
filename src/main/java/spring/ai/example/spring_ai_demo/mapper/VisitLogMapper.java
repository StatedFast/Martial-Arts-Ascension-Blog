package spring.ai.example.spring_ai_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import spring.ai.example.spring_ai_demo.entity.VisitLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 访问日志聚合查询：全部走 create_time / path 索引，只回聚合结果，不回明细。
 *
 * <p>约定：返回值一律用列别名，前端/服务层按别名取；DATE_FORMAT 直接返回字符串，
 * 省去 java.sql.Date 的类型转换。自定义 SQL 不经 MyBatis-Plus 逻辑删除拦截，
 * 但本表无逻辑删除字段，无需额外过滤。
 */
@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {

    // ==================== 总量指标 ====================

    @Select("SELECT COUNT(*) FROM blog_visit_log")
    long countTotalPv();

    @Select("SELECT COUNT(DISTINCT ip) FROM blog_visit_log")
    long countTotalUv();

    /** 今日 PV（今天 0 点起） */
    @Select("SELECT COUNT(*) FROM blog_visit_log WHERE create_time >= CURDATE()")
    long countTodayPv();

    /** 今日 UV（今天 0 点起去重 IP） */
    @Select("SELECT COUNT(DISTINCT ip) FROM blog_visit_log WHERE create_time >= CURDATE()")
    long countTodayUv();

    /** 当前在线：近 since 时间点仍有访问的独立 IP 数（近似口径，看板足够用） */
    @Select("SELECT COUNT(DISTINCT ip) FROM blog_visit_log WHERE create_time >= #{since}")
    long countOnline(@Param("since") LocalDateTime since);

    // ==================== 趋势 / 分布 ====================

    /** 按天 PV+UV：d=yyyy-MM-dd，pv=访问数，uv=独立访客数（只返回有数据的日子，缺日由服务层补 0） */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS pv, "
            + "COUNT(DISTINCT ip) AS uv FROM blog_visit_log "
            + "WHERE create_time >= #{since} GROUP BY d ORDER BY d")
    List<Map<String, Object>> trendByDay(@Param("since") LocalDateTime since);

    /** 今日 24 小时 PV 分布：h=0-23，pv=访问数（只返回有数据的小时） */
    @Select("SELECT HOUR(create_time) AS h, COUNT(*) AS pv FROM blog_visit_log "
            + "WHERE create_time >= CURDATE() GROUP BY h ORDER BY h")
    List<Map<String, Object>> pvByHour();

    /** 热门页面 Top：按 PV 降序，附独立访客数 */
    @Select("SELECT path, COUNT(*) AS pv, COUNT(DISTINCT ip) AS uv FROM blog_visit_log "
            + "WHERE create_time >= #{since} GROUP BY path ORDER BY pv DESC LIMIT #{limit}")
    List<Map<String, Object>> topPages(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /** 浏览器分布 */
    @Select("SELECT browser AS name, COUNT(*) AS pv FROM blog_visit_log "
            + "WHERE create_time >= #{since} GROUP BY browser ORDER BY pv DESC LIMIT #{limit}")
    List<Map<String, Object>> browserDistribution(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /** 设备类型分布 */
    @Select("SELECT device_type AS name, COUNT(*) AS pv FROM blog_visit_log "
            + "WHERE create_time >= #{since} GROUP BY device_type ORDER BY pv DESC LIMIT #{limit}")
    List<Map<String, Object>> deviceDistribution(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /** 来源分布 */
    @Select("SELECT referrer AS name, COUNT(*) AS pv FROM blog_visit_log "
            + "WHERE create_time >= #{since} GROUP BY referrer ORDER BY pv DESC LIMIT #{limit}")
    List<Map<String, Object>> referrerDistribution(@Param("since") LocalDateTime since, @Param("limit") int limit);

    // ==================== 保留期清理 ====================

    /** 删除 before 之前的历史访问日志（定时任务按保留期物理清理，防止表无限膨胀） */
    @Delete("DELETE FROM blog_visit_log WHERE create_time < #{before}")
    int deleteBefore(@Param("before") LocalDateTime before);
}
