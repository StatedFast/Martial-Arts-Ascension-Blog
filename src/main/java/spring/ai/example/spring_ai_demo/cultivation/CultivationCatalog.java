package spring.ai.example.spring_ai_demo.cultivation;

import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Cultivator;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Realm;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Skill;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Style;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.SubStage;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Vfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spring.ai.example.spring_ai_demo.cultivation.Cultivation.Basis;
import static spring.ai.example.spring_ai_demo.cultivation.Cultivation.Trait;

/**
 * 人界篇设定总表：30 位人物（对应 blog_user 101-130）的最终境界、功法、路数与技能。
 *
 * <p>境界取值口径：一律取「人界篇终局」而非中途，原著明写的一律照写
 * （墨大夫卡炼气后期、冰凤元婴后期、向之礼化神初期巅峰、韩立化神初期后偷渡灵界），
 * 原著只给侧写的按文本可推的范围取值，实在推不出来的标 {@link Basis#BOARD}，
 * 前端会把这三档考据强度分开显示 —— 榜单最忌讳把猜的写成定的。
 */
@Component
public class CultivationCatalog {

    private static final List<Cultivator> ROSTER = build();
    private static final Map<Long, Cultivator> BY_ID = indexById(ROSTER);

    public List<Cultivator> roster() {
        return ROSTER;
    }

    /** 按战力从高到低，用于「人界篇战力榜」 */
    public List<Cultivator> ranked() {
        List<Cultivator> sorted = new ArrayList<>(ROSTER);
        sorted.sort(Comparator.comparingDouble(Cultivator::power).reversed());
        return sorted;
    }

    public Optional<Cultivator> find(Long userId) {
        return Optional.ofNullable(BY_ID.get(userId));
    }

    /** 境界分组：保持 Realm 声明顺序（凡人 → 化神），前端金字塔与分栏共用这一份 */
    public Map<Realm, List<Cultivator>> groupByRealm() {
        Map<Realm, List<Cultivator>> grouped = new LinkedHashMap<>();
        for (Realm realm : Realm.values()) {
            grouped.put(realm, new ArrayList<>());
        }
        for (Cultivator c : ROSTER) {
            grouped.get(c.realm()).add(c);
        }
        grouped.values().forEach(list -> list.sort(Comparator.comparingDouble(Cultivator::power).reversed()));
        return grouped;
    }

    private static Map<Long, Cultivator> indexById(List<Cultivator> roster) {
        Map<Long, Cultivator> map = new LinkedHashMap<>();
        for (Cultivator c : roster) {
            map.put(c.userId(), c);
        }
        return map;
    }

    // ==================== 设定正文 ====================

    private static List<Cultivator> build() {
        List<Cultivator> list = new ArrayList<>(30);

        // ---------- 化神：人界天花板，只有两个人 ----------
        list.add(new Cultivator(101L, "韩立", Realm.SOUL_TRANSFORMATION, SubStage.EARLY, "初期",
                "掌天瓶主 · 人界最后一步", "越国嘉元郡 · 七玄门 → 黄枫谷 → 落云宗",
                "青元剑诀（长春功打底 · 大衍诀修神念）", Style.SWORD,
                List.of(Trait.SWORD, Trait.MIND, Trait.ESCAPE, Trait.TREASURE, Trait.PERSIST),
                Basis.CANON, "原著人界篇末晋化神，与冰凤联手闯空间节点偷渡灵界",
                "资质差不是绝路，路线选错才是。慢，就是快。",
                List.of(
                        new Skill("青竹蜂云剑 · 万剑齐发", "ATTACK", 1.62, 170, Vfx.SWORD_RAIN,
                                "本命飞剑走绵密一路，一剑不快，万剑齐至。"),
                        new Skill("大衍诀 · 分识探破绽", "CONTROL", 0.72, 150, Vfx.MIND_WAVE,
                                "神念先一步探明对方法诀破绽，比多一件法宝都管用。"),
                        new Skill("长春功 · 养气回元", "HEAL", 0.55, 0, Vfx.HEAL_GLOW,
                                "进境极慢但根基扎实，一路养气至今没丢。"),
                        new Skill("血影遁", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "打不过就走。这条不算神通，但救过我最多回。"),
                        new Skill("青元剑诀 · 剑光化虹", "ULT", 2.55, 390, Vfx.GOLD_BEAM,
                                "木属性剑气生发、绵绵不尽，收束成一线时最快。"))));

        list.add(new Cultivator(118L, "向之礼", Realm.SOUL_TRANSFORMATION, SubStage.EARLY_PEAK, "初期巅峰",
                "人界顶点 · 三千年没等到飞升", "上古遗老 · 独行于天南",
                "化神法则（天地之气不必从身上取）", Style.LAW,
                List.of(Trait.LAW, Trait.MIND, Trait.PERSIST),
                Basis.CANON, "本项目 bio 明写「化神初期，人界顶点」，原著为人界化神初期巅峰",
                "化神打元婴，从不斗久。天地之气站在谁那边，这一战就完了。",
                List.of(
                        new Skill("一呼 · 山河皆应", "ATTACK", 1.95, 210, Vfx.ARRAY_PRESS,
                                "真元不必自用，一呼一吸山河替你出手。"),
                        new Skill("天地之气 · 压", "DEFENSE", 1.0, 160, Vfx.SHIELD,
                                "不是挡下你这一击，是这一击到我身前就自己散了。"),
                        new Skill("三千年所记 · 知你根底", "CONTROL", 0.8, 150, Vfx.MIND_WAVE,
                                "上古如何、灵气如何、飞升如何，我都记着。你练的那门功我也见过。"),
                        new Skill("化神叫令 · 天地为我所用", "ULT", 3.1, 430, Vfx.ULT_NOVA,
                                "叫得动天地者，不与人斗，与势斗。"))));

        // ---------- 元婴：人界明面上的顶尖战力 ----------
        list.add(new Cultivator(102L, "南宫婉", Realm.NASCENT_SOUL, SubStage.LATE, "后期（距化神一步）",
                "掩月宗 · 素女元阴功传人", "越国七大派掩月宗 · 后为韩立道侣",
                "素女元阴功（元阴为本，不在双修）", Style.ICE,
                List.of(Trait.MIND, Trait.PERSIST, Trait.ALCHEMY),
                Basis.CANON, "原著人界篇末为元婴后期大修士，离化神只一步之差",
                "元阴是蓄，不是换。守不住清静的人，练得越快，反噬越早。",
                List.of(
                        new Skill("凝气成刃 · 寒气一刀", "ATTACK", 1.58, 160, Vfx.ICE_FROST,
                                "元阴功的杀伐只有一式，出手极冷极净，非必要不出。"),
                        new Skill("月华洗髓", "HEAL", 0.9, 200, Vfx.MOON_GLOW,
                                "每半月取一次月满之夜，灵气随潮信入体。"),
                        new Skill("素女元阴护体", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "真元损耗慢、伤势不易恶化，对邪毒天然有几分压制。"),
                        new Skill("元阴本源 · 冰封千里", "ULT", 2.4, 380, Vfx.ICE_FROST,
                                "至寒一线铺开，方圆之内水气与灵气的牵连先冻住。"))));

        list.add(new Cultivator(110L, "冰凤", Realm.NASCENT_SOUL, SubStage.LATE, "后期（卡关十一年）",
                "天南冰凤一族 · 玄冰真火", "天南冰凤氏",
                "玄冰真火（本命神通 · 名里有火实则至寒）", Style.ICE,
                List.of(Trait.MIND, Trait.PERSIST, Trait.TREASURE),
                Basis.CANON, "本项目 bio 明写「元婴后期，卡在化神已十一年」",
                "化神不是冲过去的，是你不再想冲的时候，它自己开的。",
                List.of(
                        new Skill("玄冰真火 · 冻灵", "ATTACK", 1.78, 190, Vfx.ICE_FROST,
                                "被它沾上，真元自己就冻住，运不开了。"),
                        new Skill("凤翎冰羽", "ATTACK", 1.42, 150, Vfx.SWORD_RAIN,
                                "一族血脉所授，旁人学不去。"),
                        new Skill("雪止 · 心静", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "在雪地里坐过两年的人，不容易被你的杀气带乱。"),
                        new Skill("化神一问 · 天地同频", "ULT", 2.5, 400, Vfx.ULT_NOVA,
                                "那道坎我只推开一条缝 —— 但缝里的光照出来，够你受用。"))));

        list.add(new Cultivator(126L, "圭灵", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖兽）",
                "昆吾山 · 玄岩龟", "昆吾山守山妖王 · 十级妖兽",
                "玄岩龟妖体（不修功法，只修省、慢、扛）", Style.TURTLE,
                List.of(Trait.TURTLE, Trait.GUARD, Trait.BODY),
                Basis.CANON, "本项目 bio 明写「十级妖兽」；人界十级约当元婴后期顶峰、无限近化神初期",
                "防御不是怯。是知道自己要守什么。",
                List.of(
                        new Skill("八百年头槌", "ATTACK", 1.9, 200, Vfx.QI_PALM,
                                "不会飞，不擅追，神通只有一个：扛。扛够了就还你一下。"),
                        new Skill("缩进壳里", "DEFENSE", 1.0, 110, Vfx.TURTLE_SHELL,
                                "能不见就不见。有人上山，我缩进壳里。"),
                        new Skill("能不动就不动", "HEAL", 0.7, 0, Vfx.HEAL_GLOW,
                                "灵气在壳里自己转，动一次要养三年。"),
                        new Skill("守山八百年 · 同阶全力一击不动", "ULT", 2.25, 360, Vfx.TURTLE_SHELL,
                                "这口壳是八百年长成的，你的重手是我身上最不值钱的东西。"))));

        list.add(new Cultivator(107L, "大衍神君", Realm.NASCENT_SOUL, SubStage.LATE, "后期（残魂）",
                "上古神念一脉 · 只余残魂", "上古大衍氏 · 肉身没于古魔之乱",
                "大衍诀（定识 · 分识 · 千识齐动）", Style.MIND,
                List.of(Trait.MIND, Trait.RESIDUAL, Trait.GLASS),
                Basis.CANON, "原著为上古元婴后期修士，人界篇只以残魂传承现世；本榜按残魂状态取战力",
                "神念是刀，能开法诀破绽，也能剔去自己的神魂。",
                List.of(
                        new Skill("分识 · 二念并攻", "ATTACK", 1.85, 180, Vfx.MIND_WAVE,
                                "一念化为二，如两枝同生一干，各算一处破绽。"),
                        new Skill("定识 · 按住念头", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "多数人连十息都定不住，就想着分识，那是自寻死路。"),
                        new Skill("摄识锁神", "CONTROL", 0.95, 170, Vfx.MIND_WAVE,
                                "多探一寸，自己的念头就多染一分脏 —— 可我还是要探。"),
                        new Skill("千识齐动", "ULT", 2.45, 420, Vfx.ULT_NOVA,
                                "一人同御数十件法器，同时在斗法里算住你三处破绽。"))));

        list.add(new Cultivator(114L, "六道极圣", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "魔道六宗 · 底牌从不嫌多", "乱星海魔道",
                "六道轮回诀（六道养六气，一气不得独大）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.SWORD),
                Basis.INFERRED, "原著魔道巨枭级存在，人界篇元婴后期；bio 未点明小层，本榜取后期",
                "境界越高，越不是靠天赋，是靠手里还有多少底牌。",
                List.of(
                        new Skill("修罗道 · 见战", "ATTACK", 1.92, 200, Vfx.SLASH,
                                "六气之中只放修罗一道出来，见人就战。"),
                        new Skill("饿鬼道 · 吞灵", "CONTROL", 0.85, 170, Vfx.POISON_MIST,
                                "不伤你身，先吃你的灵力。"),
                        new Skill("人道 · 权宜", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "六气并立，攻守进退各有其主，从不一气独大。"),
                        new Skill("六道并立 · 天、人、修罗、畜生、饿鬼、地狱", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "六门同开的那一刻，一身之中六气各使其力。"))));

        list.add(new Cultivator(117L, "凌啸风", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天星双圣之一 · 元磁神光", "乱星海天星双圣",
                "元磁神光（两极分修，合之则成）", Style.MAGNET,
                List.of(Trait.MAGNET, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "本项目 1017 明写「元婴后期二人联手可短暂抗住化神」",
                "不与你斗法，先收你兵。",
                List.of(
                        new Skill("元磁神光 · 吸你法器", "CONTROL", 0.95, 190, Vfx.MAGNET_BEAM,
                                "方圆百丈之内一切金铁之物，听调不听宣。"),
                        new Skill("两极同频", "ATTACK", 1.82, 190, Vfx.MAGNET_BEAM,
                                "一阴一阳须同时起、同时收 —— 三百年练的就是这个。"),
                        new Skill("星宫旧甲", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "一副用惯的旧甲，不名贵，但贴身。"),
                        new Skill("百丈金铁 · 尽听我调", "ULT", 2.5, 410, Vfx.ULT_NOVA,
                                "你祭出去的那些东西，此刻在空中转了个方向。"))));

        list.add(new Cultivator(113L, "极阴祖师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "玄阴一脉 · 在你看不见的地方动手", "乱星海极阴岛",
                "极阴真解（摄魂 · 擒婴 · 玄阴护体）", Style.YIN,
                List.of(Trait.MIND, Trait.GLASS, Trait.TREASURE),
                Basis.INFERRED, "bio 只写「乱星海元婴」，虚天殿一役的规格按元婴后期取值",
                "你能锁住别人的元婴，锁不住自己的因果。",
                List.of(
                        new Skill("摄魂 · 入你识海", "CONTROL", 1.0, 200, Vfx.MIND_WAVE,
                                "轻则乱你记忆，重则代你做主。"),
                        new Skill("擒婴", "ATTACK", 2.05, 240, Vfx.BLOOD_SHADOW,
                                "杀不了人，但能把人变成一具还会走路的空壳。"),
                        new Skill("玄阴护体", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "寒气结膜，法术近身先自衰三分。"),
                        new Skill("至阴锁婴 · 一饮一啄", "ULT", 2.45, 420, Vfx.ULT_NOVA,
                                "斗法时我不喜硬拼，宁愿多等三日 —— 等就是你露破绽的时候。"))));

        list.add(new Cultivator(129L, "万三姑", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "乱星海正道第一人", "乱星海正道万氏",
                "太素元真诀（真元厚而不烈，最善守最不善杀）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "bio 明写「正道第一人」，人界篇正道翘楚按元婴后期取值",
                "神通是你能打过多强的人。正道是你能对自己人下多重的手。",
                List.of(
                        new Skill("太素守一", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "厚而不烈，这一手我练了千年，专门用来不杀人。"),
                        new Skill("定是非 · 折你兵锋", "CONTROL", 0.9, 170, Vfx.QI_PALM,
                                "能讲例的不讲情，能讲情的不讲力。"),
                        new Skill("绵长一击", "ATTACK", 1.6, 170, Vfx.QI_PALM,
                                "我这一生亲手杀人极少，不是不能，是不能随便。"),
                        new Skill("对自己人下重手", "ULT", 2.65, 420, Vfx.ULT_NOVA,
                                "百余年忍着手的那一下，今日放开。"))));

        list.add(new Cultivator(116L, "金魁", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "星宫大长老 · 执律令镇海疆", "乱星海星宫",
                "星宫镇府功 + 镇海大阵（半边阵眼常年轮值）", Style.ARRAY,
                List.of(Trait.GUARD, Trait.NO_BURST, Trait.TREASURE),
                Basis.CANON, "bio 明写「元婴中期巅峰」",
                "阵这东西，十年不出事就没人记得它有用。",
                List.of(
                        new Skill("星宫律令 · 禁你一手", "CONTROL", 0.95, 180, Vfx.ARRAY_PRESS,
                                "律令写在纸上，不是写在心里。"),
                        new Skill("镇府厚土", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "与人对掌不吃亏，独缺一锤定音的手段。"),
                        new Skill("阵眼调用 · 三十六岛同压", "ATTACK", 1.85, 210, Vfx.ARRAY_PRESS,
                                "我的实权不在斗法，在阵。"),
                        new Skill("镇海大阵 · 百年无案", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "我守了百余年，就是为了今天能用上一次。"))));

        list.add(new Cultivator(111L, "蛮胡子", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "乱星海第一防御魔功 · 体修", "乱星海魔道散修",
                "托塔天魔功（扛 · 忍 · 反）", Style.BODY,
                List.of(Trait.BODY, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "bio 明写「乱星海元婴中期，托塔天魔功，体修」",
                "我这一身不是无敌，是耐打。",
                List.of(
                        new Skill("扛 · 硬接你全力一击", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "真元不走经脉走筋骨，一层层压进皮肉里。"),
                        new Skill("忍 · 空档反手", "ATTACK", 1.72, 170, Vfx.QI_PALM,
                                "忍得住一击之后的空档，才轮得到你出手。"),
                        new Skill("反 · 天魔还力", "ATTACK", 1.98, 210, Vfx.QI_PALM,
                                "把挨下的那一分力，原封还给你。"),
                        new Skill("托塔 · 一肩扛下", "ULT", 2.3, 390, Vfx.ULT_NOVA,
                                "压到最后一息之间，周身如浇铁汁。"))));

        list.add(new Cultivator(119L, "云露老魔", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "魔道老怪 · 雾里藏刀", "魔道散修 · 有一女不认他",
                "云露魔光（先迷神，后取物）", Style.ILLUSION,
                List.of(Trait.MIND, Trait.GLASS, Trait.ESCAPE),
                Basis.INFERRED, "bio 未点小层，魔道老怪规格取元婴中期",
                "魔道路快，是因为它把代价往后推。推到什么时候、推给谁，你事先从来不知道。",
                List.of(
                        new Skill("魔光迷神 · 三息", "CONTROL", 1.05, 200, Vfx.POISON_MIST,
                                "人在自己最想要的幻象里待上三息，三息之后身上有什么就不是自己的了。"),
                        new Skill("雾里藏刀", "ATTACK", 1.95, 190, Vfx.SLASH,
                                "不伤人，先迷神 —— 刀是在你分神那一下递进来的。"),
                        new Skill("摸腕自证", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "用多了自己也分不清哪一重是真的，夜里坐起摸一摸手腕，确认还在。"),
                        new Skill("一重幻界 · 你最想要的那一重", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "我不打你。我陪你待在你最想留的地方。"))));

        list.add(new Cultivator(115L, "万天明", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "正道万氏 · 鉴宝三十年", "乱星海万氏",
                "照灵诀（以神识贴物，看灵气走向匀不匀）", Style.MIND,
                List.of(Trait.MIND, Trait.TREASURE, Trait.GLASS),
                Basis.INFERRED, "虚天殿一役的世家子弟规格，取元婴初期",
                "我算对了物，没算人。",
                List.of(
                        new Skill("照灵诀 · 破妄", "CONTROL", 0.85, 160, Vfx.MIND_WAVE,
                                "真器灵气内敛如一，伪器外润内乱 —— 人也是一样的看法。"),
                        new Skill("补天丹 · 含一粒", "HEAL", 0.95, 0, Vfx.HEAL_GLOW,
                                "拿到了，然后发现对自己几乎没用。可它到底还是颗好丹。"),
                        new Skill("三十年眼力 · 点你三处破绽", "ATTACK", 1.95, 210, Vfx.SLASH,
                                "我不一定打赢你，但我先知道你下一剑从哪儿来。"),
                        new Skill("万氏家学 · 一件不留", "ULT", 2.15, 380, Vfx.ULT_NOVA,
                                "看物先看其主。今日我把你这一身东西的来历全说破。"))));

        list.add(new Cultivator(130L, "红拂", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "红拂三剑 · 为走不为胜", "天南剑修 · 董萱儿之母",
                "红拂三剑（一斩禁制，二断神识，三不为胜）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "bio 与 1030 只写剑不写阶，能以剑破阵带人走，取元婴初期",
                "能走，比能胜值钱。",
                List.of(
                        new Skill("一剑斩禁制", "CONTROL", 0.95, 170, Vfx.SLASH,
                                "我不与他们对掌，只斩他们手里的禁制旗。"),
                        new Skill("二剑断神识相接", "CONTROL", 1.0, 190, Vfx.SLASH,
                                "你和你的法宝之间，那一根线我先断给你看。"),
                        new Skill("三剑不为胜 · 为走", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "年轻时把它放最后，如今放最前。"),
                        new Skill("不回头", "ULT", 2.45, 390, Vfx.SWORD_RAIN,
                                "孩子哭，我走我的。回头一次，就走不了。"))));

        list.add(new Cultivator(109L, "紫灵", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "乱星海妙音门 · 行走多年", "乱星海妙音门",
                "音律功法（潮信为号，惑心乱神）", Style.SOUND,
                List.of(Trait.SUMMON, Trait.ESCAPE, Trait.PERSIST),
                Basis.CANON, "原著人界篇末凭坠魔谷灵烛果入元婴初期，境界低于南宫婉",
                "这里的人笑起来都很客气。你要看的不是他笑得暖不暖，是他把手放在哪里。",
                List.of(
                        new Skill("妙音惑心", "CONTROL", 0.95, 170, Vfx.SOUND_RING,
                                "一句话拐三道弯，听着像夸你，其实是在算你。"),
                        new Skill("潮信引退", "MOBILITY", 0.0, 120, Vfx.SOUND_RING,
                                "风色一变、海鸟低飞、水下泛凉，三条中两条就必须走。"),
                        new Skill("海兽令", "ATTACK", 1.78, 200, Vfx.SUMMON,
                                "每逢潮信，深海兽群上涌 —— 看着是劫，也是钱。"),
                        new Skill("乱星海 · 万兽朝音", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "这片海不认宗门辈分，只认两件事：背后的势力，手上的货。"))));

        list.add(new Cultivator(128L, "白凤仙子", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗白凤峰峰主 · 三式剑", "天南落云宗",
                "白凤三剑（照 · 断 · 归）", Style.SWORD,
                List.of(Trait.SWORD, Trait.PERSIST),
                Basis.CANON, "bio 明写「元婴初期」，1028 自述三式剑",
                "剑不是用来打赢的，是用来叫你不必多说话的。",
                List.of(
                        new Skill("照 · 剑光滞身", "ATTACK", 1.5, 150, Vfx.SLASH,
                                "剑光亮处，对手身形先滞一分。"),
                        new Skill("断 · 破护身灵光", "ATTACK", 2.05, 210, Vfx.SLASH,
                                "一气斩下，不取巧，专破护身灵光。"),
                        new Skill("归 · 收剑即止", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "这一式最难，因为它要求你在能杀的时候停。"),
                        new Skill("白凤三式齐出", "ULT", 2.5, 400, Vfx.SWORD_RAIN,
                                "十七种剑招我全丢了，只留这三式，磨了一百年。"))));

        // ---------- 结丹 ----------
        list.add(new Cultivator(124L, "慕沛灵", Realm.CORE_FORMING, SubStage.LATE, "后期（冲元婴未果）",
                "落云宗 · 静明诀", "天南落云宗 · 药园执掌",
                "静明诀（专养神识清明）", Style.MIND,
                List.of(Trait.MIND, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "原著人界篇最高结丹后期，后冲击元婴陨落",
                "我修不成大道，就把家族里那几个孩子教出来。人不能只靠一段关系活着。",
                List.of(
                        new Skill("静明 · 不失方寸", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "答应过的事、欠下的账、说过的话，我记得极清楚。"),
                        new Skill("记账分明 · 识破", "CONTROL", 0.85, 150, Vfx.MIND_WAVE,
                                "神识清明到极处，就是你说谎之前我已经听见了。"),
                        new Skill("颠凤培元功 · 短时提气", "ATTACK", 1.9, 200, Vfx.QI_PALM,
                                "借来的力也是力 —— 只是要还。"),
                        new Skill("分寸二字", "ULT", 2.2, 360, Vfx.ULT_NOVA,
                                "什么时候该退一步，什么时候一步都不能退。这一手我练得比功法熟。"))));

        list.add(new Cultivator(108L, "银月", Realm.CORE_FORMING, SubStage.LATE, "后期（器灵）",
                "银月剑 · 看过的修士比看过的剑多", "剑中器灵 · 后为九尾一脉旧识",
                "祭炼三段（认主 · 通灵 · 化形）", Style.SWORD,
                List.of(Trait.SPIRIT, Trait.SWORD, Trait.MIND),
                Basis.BOARD, "原著器灵无境界，本榜按「离剑即衰」的规格折算到结丹后期",
                "法宝是修士的另一条命。你把命交出去之前，先想清楚自己配不配拿回来。",
                List.of(
                        new Skill("剑鸣 · 滞你一分", "ATTACK", 1.55, 140, Vfx.SLASH,
                                "你心虚的一刻，剑锋会慢上半分 —— 那半分就是生死之别。"),
                        new Skill("认主 · 护主", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "你拿它护身，它就稳；你拿它逞凶，它就躁。"),
                        new Skill("化形", "ATTACK", 1.95, 210, Vfx.SWORD_RAIN,
                                "到这一步，法宝开始挑主人，谁祭炼谁都难说。"),
                        new Skill("一剑不留情", "ULT", 2.35, 380, Vfx.ULT_NOVA,
                                "器灵出剑，不替主人省力气 —— 因为我自己没有第二条命。"))));

        list.add(new Cultivator(127L, "柳乐儿", Realm.CORE_FORMING, SubStage.MID, "中期（九尾三尾）",
                "灵寰界 · 九尾一脉如今归她管", "九尾遗族 · 灵寰界",
                "凝月诀（取月华入体，一尾一境界）", Style.BEAST,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.GLASS),
                Basis.BOARD, "原著人界篇尚在成长期，本榜按「三尾映月」折算结丹中期",
                "活得久不算本事，活得像自己才算。",
                List.of(
                        new Skill("月华一层尾", "HEAL", 0.8, 0, Vfx.MOON_GLOW,
                                "尾越长天劫越重，所以先长的那一尾要省着用。"),
                        new Skill("族中幼狐齐啸", "ATTACK", 1.72, 190, Vfx.SUMMON,
                                "没几个孩子，老的全是伤 —— 可它们肯替我看方向。"),
                        new Skill("不许吃人 · 族训", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "这一条写在祠堂门口。吃了人，就再也回不了人形。"),
                        new Skill("三尾映月", "ULT", 2.15, 360, Vfx.MOON_GLOW,
                                "九尾圆满可敌化神 —— 我现在有三尾，还差六次天劫。"))));

        list.add(new Cultivator(105L, "董萱儿", Realm.CORE_FORMING, SubStage.EARLY, "初期",
                "黄枫谷灵植夫 · 与药草打交道比与人多", "天南黄枫谷药园",
                "青木吐纳诀（把木灵气渡进根须）", Style.WOOD,
                List.of(Trait.SUMMON, Trait.ALCHEMY, Trait.GLASS),
                Basis.INFERRED, "自述只提灵植六年与木属性慢功，按结丹初期取值",
                "山里的药是养出来的，不是抢出来的。",
                List.of(
                        new Skill("青木缠藤", "CONTROL", 0.95, 160, Vfx.POISON_MIST,
                                "催芽、接骨、救一株半枯的老参，靠的都是这一口气。"),
                        new Skill("灵草毒雾", "ATTACK", 1.62, 170, Vfx.POISON_MIST,
                                "药园一天要转三遍，少一遍第二天就给你脸色看。"),
                        new Skill("三年老参 · 一掷", "DEFENSE", 1.0, 120, Vfx.HEAL_GLOW,
                                "一株百年青芝：育苗四月、养土九月、看火十一月。"),
                        new Skill("潮气没弄对 · 全废", "ULT", 2.1, 350, Vfx.ULT_NOVA,
                                "中间一次潮气没弄对就全废 —— 我拿这炉药试过你。"))));

        list.add(new Cultivator(123L, "菡云芝", Realm.CORE_FORMING, SubStage.EARLY, "初期",
                "灵兽山 → 御灵宗 · 御兽不驭兽", "天南灵兽山一脉",
                "百灵谐鸣诀（以神识与兽念相接，不压不锁只讲合）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.ESCAPE, Trait.ALCHEMY),
                Basis.CANON, "1023 自述「结丹我成了。慢，但成了」",
                "御兽这门功夫教给我的，是怎么叫一个比你活得久的东西愿意陪你。",
                List.of(
                        new Skill("二阶灵禽 · 替我看方向", "CONTROL", 0.8, 140, Vfx.SUMMON,
                                "跟了我一百多年，如今通人性。"),
                        new Skill("不与兽硬拼，先绕", "MOBILITY", 0.0, 110, Vfx.BLOOD_SHADOW,
                                "我的兽帮我找路，不帮我拼命。"),
                        new Skill("百灵齐鸣", "ATTACK", 1.75, 185, Vfx.SUMMON,
                                "它觉得跟你一起划算，所以它来了。"),
                        new Skill("血色试炼求生录", "ULT", 2.1, 350, Vfx.ULT_NOVA,
                                "天黑前一定停下。夜里那片林子的东西，不是给人留的。"))));

        // ---------- 筑基 ----------
        list.add(new Cultivator(106L, "李化元", Realm.FOUNDATION, SubStage.LATE, "后期",
                "黄枫谷执事长老 · 管过资源分配", "天南黄枫谷",
                "宗门功法（不擅杀伐，擅算账）", Style.QI,
                List.of(Trait.GUARD, Trait.ALCHEMY, Trait.NO_BURST),
                Basis.CANON, "bio 明写「筑基期，管过宗门资源分配」",
                "筑基丹从来不是商品，是分配权。",
                List.of(
                        new Skill("四十枚灵石 · 布一个局", "DEFENSE", 1.0, 120, Vfx.ARRAY_PRESS,
                                "宗门弟子每年灵石四十枚、引气丹六瓶 —— 我今天把它们全摆出来。"),
                        new Skill("引气丹 · 当场提气", "HEAL", 0.75, 0, Vfx.HEAL_GLOW,
                                "丹药不是商品，是分配权。现在它在你我之间易手。"),
                        new Skill("黄枫谷剑光", "ATTACK", 1.55, 150, Vfx.SLASH,
                                "执事长老也拔剑，只是拔得慢。"),
                        new Skill("宗门资源 · 压你一年份额", "ULT", 2.05, 340, Vfx.ARRAY_PRESS,
                                "我年轻时也不服。如今执了这杆笔，也只是把同样的事往下传。"))));

        list.add(new Cultivator(112L, "曲魂", Realm.FOUNDATION, SubStage.MID, "中期（炼尸之身）",
                "本名张铁 · 不人不尸", "七玄门 → 身外化身",
                "象甲功（刀枪不入）+ 血炼神光（以血养气）", Style.CORPSE,
                List.of(Trait.BODY, Trait.GLASS, Trait.PERSIST),
                Basis.BOARD, "原著张铁以炼尸之身长随韩立，无正式境界，本榜按战力折算筑基中期",
                "法器再利，也换不回一副能尝出肉干咸淡的身子。",
                List.of(
                        new Skill("象甲硬扛", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "凡人练它能打赢十个凡人；修士练它，只能多挨修士一下。"),
                        new Skill("血炼神光", "ATTACK", 1.88, 190, Vfx.BLOOD_SHADOW,
                                "出手带煞，同阶不敢硬接。代价是每用一次，我离人远一步。"),
                        new Skill("不知痛", "HEAL", 0.5, 0, Vfx.QI_PALM,
                                "我记得「高兴」的定义，记不起感觉。痛也一样。"),
                        new Skill("身外化身 · 不留余地", "ULT", 2.2, 370, Vfx.ULT_NOVA,
                                "这具身子已经是法器了，用坏就用坏吧。"))));

        list.add(new Cultivator(125L, "曹梦容", Realm.FOUNDATION, SubStage.LATE, "后期",
                "大晋坊市丹药铺 · 真假一眼能分", "大晋坊市",
                "辨药要录（真丹香气沉，假丹香浮）", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.MIND, Trait.GLASS),
                Basis.BOARD, "原著无明确境界记载，按坊市管事水准推定筑基后期",
                "修士买丹，多半不是买药，是买一个「我还能再进一步」的念头。",
                List.of(
                        new Skill("一眼辨破 · 你那颗是假的", "CONTROL", 0.9, 150, Vfx.MIND_WAVE,
                                "药性偏激那种最骗急着突破的人 —— 你身上那瓶我闻过。"),
                        new Skill("猛药催丹相", "ATTACK", 1.9, 200, Vfx.POISON_MIST,
                                "服下当时灵力大涨，半年之后经脉自己就脆了。今日我先用在你身上。"),
                        new Skill("铺子规矩 · 卖错认赔", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "当日赔，不拖。这条规矩我在铺子里立了三十年。"),
                        new Skill("一炉三假", "ULT", 2.05, 340, Vfx.POISON_MIST,
                                "以次充好、药性偏激、借名 —— 我见过的三种假丹，今日都摆给你看。"))));

        list.add(new Cultivator(121L, "陈巧倩", Realm.FOUNDATION, SubStage.EARLY, "初期",
                "黄枫谷弟子 · 翠云诀", "天南黄枫谷",
                "翠云诀（木属性慢功，养人不出重手）", Style.QI,
                List.of(Trait.ALCHEMY, Trait.PERSIST, Trait.NO_BURST),
                Basis.CANON, "1021 自述「筑基初期」；原著终生未结丹",
                "被人误会时不哭，去查档。哭一次，旁人便觉得你理亏。",
                List.of(
                        new Skill("翠云护体", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "受了伤比别人好得快 —— 这是我这一门唯一的长处。"),
                        new Skill("慢火长熬", "HEAL", 0.7, 0, Vfx.HEAL_GLOW,
                                "灵力不烈，胜在能耗。同阶斗法我多半是拖到对方没耐性的那个。"),
                        new Skill("人情当场还清", "ATTACK", 1.5, 140, Vfx.QI_PALM,
                                "灵石、丹药、人情，能当日还的当日还。"),
                        new Skill("查档", "ULT", 1.95, 320, Vfx.ULT_NOVA,
                                "把出入、丹药、值夜的日子摆出来 —— 一次就够。"))));

        // ---------- 炼气 ----------
        list.add(new Cultivator(103L, "墨大夫", Realm.QI_REFINING, SubStage.LATE, "后期（卡二十年）",
                "七玄门太医 · 长春功传人", "越国七玄门",
                "长春功（养气延年）+ 夺舍三忌", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "1003 自述「卡在炼气后期二十年，寿数将尽」",
                "功法无罪，用功法的人心术不正，才是祸。",
                List.of(
                        new Skill("长春功 · 养气一指", "ATTACK", 1.35, 90, Vfx.QI_PALM,
                                "进境慢得看不出，但能压住伤病、拖住大限。"),
                        new Skill("行医四十年的银针", "ATTACK", 1.62, 120, Vfx.POISON_MIST,
                                "凡俗人的病我治，修士的伤我也见过。"),
                        new Skill("夺舍三忌 · 护住本神", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "一忌根器不合，二忌因果不清，三忌心气不平。"),
                        new Skill("寿数将尽 · 最后一炉", "ULT", 1.85, 260, Vfx.POISON_MIST,
                                "老朽这一生就剩这一炉了 —— 它不一定打得赢你，但一定能毒到你。"))));

        // ---------- 凡人 · 不在仙途之内 ----------
        list.add(new Cultivator(104L, "厉飞雨", Realm.MORTAL, SubStage.EARLY, "凡俗武徒",
                "七玄门凡俗武徒 · 不懂法术但活得明白", "越国七玄门",
                "八极拳（崩 · 撼 · 突 · 击，一寸之内发力）", Style.MORTAL,
                List.of(Trait.BODY, Trait.MORALE),
                Basis.CANON, "bio 明写「凡俗武徒，没有灵根」，原著终生为凡人",
                "能活着走到最后的，都不是最能打的。",
                List.of(
                        new Skill("八极拳 · 崩拳", "ATTACK", 1.0, 40, Vfx.MORTAL_PUNCH,
                                "一寸之内发力。放在修士面前，一掌就能被护身灵光弹开。"),
                        new Skill("跑得快、扛得住", "MOBILITY", 0.0, 30, Vfx.MORTAL_PUNCH,
                                "修士靠遁光，我只能靠这两条腿。"),
                        new Skill("凡人的骨气", "DEFENSE", 1.0, 20, Vfx.SHIELD,
                                "受伤了能自己走回去 —— 这一条我练了十年。"))));

        list.add(new Cultivator(122L, "墨彩环", Realm.MORTAL, SubStage.EARLY, "凡俗医道",
                "墨家三女 · 没有灵根", "越国七玄门 · 墨家",
                "医道（凡俗人的病我治，修士的伤我也见过）", Style.MORTAL,
                List.of(Trait.ALCHEMY, Trait.MORALE),
                Basis.CANON, "bio 与 1022 明写「没有灵根」；硬引气轻则伤身重则瘫床",
                "把心思放在「叫他也看我一眼」上，是把自己活成了一件事。我不干。",
                List.of(
                        new Skill("下针 · 先吊住一口气", "HEAL", 0.9, 30, Vfx.HEAL_GLOW,
                                "爹爹的手艺如今是我的。这一手救过的人，比仙师记得清楚。"),
                        new Skill("认药 · 辨你丹药", "CONTROL", 0.7, 25, Vfx.MIND_WAVE,
                                "凝气散那种东西，凡人吃下去七窍流血 —— 你身上这瓶我给你收了。"),
                        new Skill("把家撑住", "DEFENSE", 1.0, 20, Vfx.SHIELD,
                                "门里门外的事总得有人管。修士不管这些，他们觉得这些不算是本事。"))));

        list.add(new Cultivator(120L, "孙二狗", Realm.MORTAL, SubStage.EARLY, "凡俗帮主",
                "四平帮帮主 · 替仙师看着一具炼尸", "越国凡俗江湖",
                "不会法术，斗大的字认得一百多个", Style.MORTAL,
                List.of(Trait.MORALE, Trait.SUMMON),
                Basis.CANON, "bio 与 1020 明写「凡俗之人，不会法术」",
                "别拿他的东西，别问，账要记清。",
                List.of(
                        new Skill("账本一挡", "DEFENSE", 1.0, 15, Vfx.SHIELD,
                                "仙师赏的灵石我一枚没动，收支都记在本子上，什么时候都可以来查。"),
                        new Skill("院里那位 · 叫他一声", "ATTACK", 1.3, 40, Vfx.SUMMON,
                                "他白天站在院里不动，晚上会自己走到门口站着。我不确定他听不听我的。"),
                        new Skill("认个错，拱手", "MOBILITY", 0.0, 10, Vfx.MORTAL_PUNCH,
                                "打不过就认。凡人在仙师脚边活了这么多年，靠的不是能打的。"))));

        return List.copyOf(list);
    }
}
