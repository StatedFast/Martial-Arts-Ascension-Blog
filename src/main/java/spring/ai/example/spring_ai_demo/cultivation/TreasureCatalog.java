package spring.ai.example.spring_ai_demo.cultivation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 混沌万灵榜 · 法宝目录（凡人修仙传 · 人界/灵界篇整理）。
 *
 * <p>数据是小说设定的静态摘录，不含任何真实用户信息，所以整张榜可以公开只读。
 * 品阶从高到低：玄天仙品 &gt; 通天灵宝 &gt; 灵宝 &gt; 法宝 &gt; 灵器 &gt; 法器。
 * 排序先按品阶、再按榜位（rank 为榜内序号，越小越前），前端只吃现成的顺序。
 */
@Component
public class TreasureCatalog {

    /** 品阶顺序：索引越小越高 */
    public static final List<String> TIER_ORDER =
            List.of("玄天仙品", "通天灵宝", "灵宝", "法宝", "灵器", "法器");

    public record Treasure(int rank, String name, String tier, String type,
                           String owner, String origin, String effect) {
    }

    private final List<Treasure> all = build();

    public List<Treasure> all() {
        return all;
    }

    public List<String> tiers() {
        return TIER_ORDER;
    }

    private static Treasure t(int rank, String name, String tier, String type,
                              String owner, String origin, String effect) {
        return new Treasure(rank, name, tier, type, owner, origin, effect);
    }

    private static List<Treasure> build() {
        List<Treasure> list = new ArrayList<>();
        // ---------- 玄天仙品 ----------
        list.add(t(1, "掌天瓶", "玄天仙品", "玉瓶", "韩立", "人界 · 七玄门后山",
                "瓶内自成天地，可催熟灵药、隐匿气息，是韩立立身之本的根脚。"));
        list.add(t(2, "玄天斩灵剑", "玄天仙品", "飞剑", "韩立", "灵界 · 玄天仙域",
                "一剑斩灵，专破法宝灵性，玄天之宝中的攻伐极致。"));
        // ---------- 通天灵宝 ----------
        list.add(t(3, "青竹蜂云剑", "通天灵宝", "飞剑", "韩立", "灵界 · 天渊城",
                "七十二口子剑同源，可结大庚剑阵，剑阵一成同阶无解。"));
        list.add(t(4, "元磁神山", "通天灵宝", "山岳", "韩立", "灵界",
                "元磁之力笼罩百丈，金铁之属尽数失效，遁光亦被定住。"));
        list.add(t(5, "虚天鼎", "通天灵宝", "鼎", "大衍神君", "人界 · 虚天殿",
                "感应天机、蕴养神器，虚天殿一脉的镇殿之宝。"));
        // ---------- 灵宝 ----------
        list.add(t(6, "天雷竹", "灵宝", "灵竹", "韩立", "人界",
                "引天雷淬体诛邪，雷法一脉难得的上品灵材。"));
        list.add(t(7, "大衍宝镜", "灵宝", "宝镜", "大衍神君", "人界",
                "返照神识、勘破幻术，神念一道的克星。"));
        list.add(t(8, "血鬼幡", "灵宝", "幡", "六道极圣", "人界 · 阴罗宗",
                "拘魂炼鬼，血气越盛幡力越强，魔道标志性重宝。"));
        list.add(t(9, "提魂钟", "灵宝", "钟", "极阴祖师", "人界 · 阴罗宗",
                "一响摄魂、二响夺魄，神魂弱者当场失守。"));
        list.add(t(10, "阴阳环", "灵宝", "双环", "冰凤", "人界 · 乱星海",
                "阴阳二气交缠，环过之处虚空生霜。"));
        list.add(t(11, "三焰扇", "灵宝", "宝扇", "红拂", "人界",
                "扇出三色灵焰，一扇成火海，可灼伤元婴。"));
        list.add(t(12, "太乙五烟罗", "灵宝", "烟罗", "万天明", "人界",
                "五色烟罗护体，刀剑难入、法术难侵，守御第一。"));
        list.add(t(13, "金蛟剪", "灵宝", "金剪", "凌啸风", "人界",
                "双蛟化剪，专剪法宝；一旦咬住便难脱身。"));
        // ---------- 法宝 ----------
        list.add(t(14, "金刚罩", "法宝", "护罩", "金魁", "人界",
                "护身法宝，硬抗同阶重击，最是皮糙肉厚。"));
        list.add(t(15, "金砖", "法宝", "砖", "韩立", "人界",
                "近身祭出、势大力沉，砸人从不讲道理。"));
        list.add(t(16, "狼牙棒", "法宝", "棒", "蛮胡子", "人界",
                "一力降十会，棒起处灵光崩碎。"));
        list.add(t(17, "缚灵网", "法宝", "网", "南宫婉", "人界 · 掩月宗",
                "缚敌困兽，连遁光一并锁住。"));
        list.add(t(18, "紫罗极火", "法宝", "灵火", "紫灵", "人界 · 星宫",
                "紫焰灼魂，烧的是生机不是皮肉。"));
        list.add(t(19, "引魂幡", "法宝", "幡", "曲魂", "人界",
                "牵引残魂、御使炼尸，炼尸一脉的看家物件。"));
        list.add(t(20, "灭魔弹", "法宝", "弹丸", "向之礼", "人界",
                "一次性重宝，专破魔功，出手即绝。"));
        list.add(t(21, "青蛟旗", "法宝", "旗", "圭灵", "人界 · 乱星海",
                "旗展蛟出，翻江倒海，妖修一脉的杀伐重器。"));
        list.add(t(22, "万兽牌", "法宝", "令牌", "柳乐儿", "人界 · 御兽",
                "收摄灵兽、号令群妖，御兽宗的传承之物。"));
        // ---------- 灵器 / 法器 ----------
        list.add(t(23, "灵犀佩", "灵器", "玉佩", "慕沛灵", "人界",
                "心意相通、传讯千里，修士行走江湖的保命信物。"));
        list.add(t(24, "聚灵壶", "法器", "壶", "曹梦容", "人界",
                "聚敛灵气、辅助修炼，低阶修士的入门之物。"));
        return list;
    }
}
