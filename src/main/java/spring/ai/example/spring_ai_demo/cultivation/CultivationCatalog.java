package spring.ai.example.spring_ai_demo.cultivation;

import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Cultivator;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Realm;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Skill;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Style;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.SubStage;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Vfx;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Voice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spring.ai.example.spring_ai_demo.cultivation.Cultivation.Basis;
import static spring.ai.example.spring_ai_demo.cultivation.Cultivation.Trait;

/**
 * 人界篇设定总表：七十三位人物（对应 blog_user 101-173）的最终境界、功法、路数与技能。
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
    private static final Map<Long, Voice> VOICES = buildVoices();

    /** 拿不到就给一套中性人声：斗法台不能因为某个角色没配音色就不开口 */
    private static final Voice DEFAULT_VOICE = new Voice(1.0, 0.95, "未单独配音色", "");

    public Voice voice(Long userId) {
        return VOICES.getOrDefault(userId, DEFAULT_VOICE);
    }

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

    /**
     * 报招音色：只写“听起来像谁”，不写数值，也不影响裁决。
     * 取值的根据是原著里这个人的说话方式：韩立少话、蛮胡子粗豪、曲魂是炼尸几乎不开口、
     * 银月与柳乐儿是个姑娘腔、向之礼三千年没跟人吵过架。女修普遍 pitch 高、rate 快，
     * 老魔 pitch 低、rate 慢 —— 一听就知道是谁在出手。
     */
    private static Map<Long, Voice> buildVoices() {
        Map<Long, Voice> m = new LinkedHashMap<>();
        m.put(101L, new Voice(0.82, 0.92, "低而稳，话不多", "去。"));
        m.put(118L, new Voice(0.68, 0.82, "苍老悠远", "起。"));
        m.put(102L, new Voice(1.22, 0.92, "清冷女声", "凝。"));
        m.put(110L, new Voice(1.14, 0.90, "傲冷、字字结霜", "冻。"));
        m.put(126L, new Voice(1.02, 0.78, "兽语沉缓", "扛。"));
        m.put(107L, new Voice(0.74, 0.86, "残魂空响", "识。"));
        m.put(114L, new Voice(0.70, 0.88, "六气重叠", "六道。"));
        m.put(117L, new Voice(0.86, 0.94, "风急刀鸣", "收。"));
        m.put(113L, new Voice(0.64, 0.80, "阴寒沙哑", "来鼎。"));
        m.put(129L, new Voice(1.16, 0.90, "端凝持重", "正法。"));
        m.put(116L, new Voice(0.78, 0.88, "金铁之声", "砸。"));
        m.put(111L, new Voice(0.58, 0.80, "粗豪炸裂", "哈！"));
        m.put(119L, new Voice(0.70, 0.84, "迷幻慵懒", "睡吧。"));
        m.put(115L, new Voice(0.88, 0.95, "灵光清越", "照。"));
        m.put(130L, new Voice(1.08, 1.00, "三剑急啸", "剑去。"));
        m.put(109L, new Voice(1.20, 0.95, "柔中带韧", "紫焰。"));
        m.put(128L, new Voice(1.26, 0.94, "清亮傲气", "凤鸣。"));
        m.put(124L, new Voice(1.12, 1.00, "急切清越", "我还没输。"));
        m.put(108L, new Voice(1.30, 0.98, "少女器灵", "我在剑里。"));
        m.put(127L, new Voice(1.34, 1.04, "娇俏狐音", "看尾巴。"));
        m.put(105L, new Voice(1.28, 1.00, "娇声带刺", "花里有剑。"));
        m.put(123L, new Voice(1.16, 0.94, "温婉清淡", "云芝在此。"));
        m.put(106L, new Voice(0.90, 0.96, "沉郁书生", "师命不敢辞。"));
        m.put(112L, new Voice(0.55, 0.76, "炼尸不开口，只当招自己到了", ""));
        m.put(125L, new Voice(1.06, 1.02, "市井爽利", "讲规矩。"));
        m.put(121L, new Voice(1.24, 0.98, "少女脆声", "师兄小心。"));
        m.put(103L, new Voice(0.76, 0.90, "阴恻恻老医", "别动。"));
        m.put(104L, new Voice(0.84, 1.00, "少年清亮", "八极拳。"));
        m.put(122L, new Voice(1.18, 0.96, "温软医女", "我帮你。"));
        m.put(120L, new Voice(0.96, 1.04, "市井粗嗓", "先走一步。"));

        // 人界篇元婴补位（天南、乱星海、大晋与妖修）与结丹第一人温天仁的音色：
        // 老魔一律压低放慢，女修提上去；不会说话的两头妖兽一个只给吼、一个直接不开口。
        m.put(131L, new Voice(0.72, 0.84, "沙哑含毒", "毒。"));
        m.put(132L, new Voice(0.88, 0.92, "道士清朗", "焚。"));
        m.put(133L, new Voice(0.66, 0.86, "低缓黏人", "合。"));
        m.put(134L, new Voice(0.70, 0.90, "怨气冲顶", "恨。"));
        m.put(135L, new Voice(0.84, 0.98, "清亮带哨", "鸣。"));
        m.put(136L, new Voice(1.12, 0.92, "冷女声带霜", "封。"));
        m.put(137L, new Voice(0.76, 0.96, "细密剑音", "丝。"));
        m.put(138L, new Voice(0.92, 0.94, "镜音清冷", "收。"));
        m.put(139L, new Voice(0.68, 0.82, "苍老拖沓", "守。"));
        m.put(140L, new Voice(0.80, 1.00, "急而轻", "走。"));
        m.put(141L, new Voice(0.66, 0.84, "空落摄魂", "碎。"));
        m.put(142L, new Voice(0.86, 0.88, "厚重老声", "护。"));
        m.put(143L, new Voice(0.74, 0.90, "呼哨唤兽", "来。"));
        m.put(144L, new Voice(1.30, 1.02, "童声老腔", "烧。"));
        m.put(145L, new Voice(1.18, 0.98, "清越女声", "虹。"));
        m.put(146L, new Voice(0.80, 0.88, "沉稳掌门腔", "镇。"));
        m.put(147L, new Voice(0.90, 0.96, "连绵如水", "浪。"));
        m.put(148L, new Voice(1.14, 0.90, "端凝女声", "听调。"));
        m.put(149L, new Voice(0.72, 0.86, "低沉含尸气", "上。"));
        m.put(150L, new Voice(0.62, 0.80, "枯哑无情", "绝。"));
        m.put(151L, new Voice(0.84, 0.86, "厚重道音", "平。"));
        m.put(152L, new Voice(0.78, 0.94, "鼓震嗓音", "鼓。"));
        m.put(153L, new Voice(0.70, 0.88, "血哑低喘", "祭。"));
        m.put(154L, new Voice(0.64, 0.82, "梵音洪亮", "唵。"));
        m.put(155L, new Voice(0.74, 0.80, "禅定缓声", "静。"));
        m.put(156L, new Voice(0.86, 1.00, "散修快剑", "斩。"));
        m.put(157L, new Voice(1.08, 0.88, "寒到发慢", "蓝。"));
        m.put(158L, new Voice(1.10, 0.86, "宫音威严", "令。"));
        m.put(159L, new Voice(1.02, 0.76, "鬼语重影", "来。"));
        m.put(160L, new Voice(0.70, 0.90, "嘶哑狠声", "见血。"));
        m.put(161L, new Voice(0.82, 0.88, "平日和气、出手转沉", "露一手。"));
        m.put(162L, new Voice(0.76, 0.86, "执法平铺", "按。"));
        m.put(163L, new Voice(0.68, 0.84, "祭坛回响", "起烟。"));
        m.put(164L, new Voice(0.78, 0.94, "巫祝嘶喊", "神来。"));
        m.put(165L, new Voice(0.60, 0.82, "龙吟压嗓", "量。"));
        m.put(166L, new Voice(0.56, 0.76, "兽吼闷雷", "抱。"));
        m.put(167L, new Voice(0.72, 0.92, "学人说话、尾音带哨", "撒。"));
        m.put(168L, new Voice(0.66, 0.72, "木石摩擦般迟缓", "长。"));
        m.put(169L, new Voice(0.68, 0.90, "尸音带笑", "猜。"));
        m.put(170L, new Voice(0.58, 0.70, "不会说话，只一声吼", ""));
        m.put(171L, new Voice(0.70, 0.86, "老妖尖缓", "放。"));
        m.put(172L, new Voice(0.66, 0.84, "骨节摩擦声", "吃一口。"));
        m.put(173L, new Voice(0.96, 1.00, "清秀少年音、尾音带傲", "金光。"));
        return m;
    }

    // ==================== 设定正文 ====================

    private static List<Cultivator> build() {
        List<Cultivator> list = new ArrayList<>(80);

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

        // ---------- 天南三大修士：元婴战力的天花板 ----------
        list.add(new Cultivator(131L, "魏无涯", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "天南三大修士之首 · 毒道通神", "天南九国盟化意门太上长老",
                "万毒混元功（十绝毒 · 腹尸之毒 · 绿毒化蛇）", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.SUMMON, Trait.TREASURE),
                Basis.CANON, "原著明载：化意门太上长老，天南三大修士之一，入元婴后期最早，毒道冠绝天南",
                "毒是我用的，名声是别人怕的。我这一辈子没在背后算计过谁 —— 毒摆在你面前，你也要喝。",
                List.of(
                        new Skill("十绝毒 · 尸之毒", "ATTACK", 1.95, 215, Vfx.POISON_MIST,
                                "十绝之中最不值钱的一味，因为它只要沾上就够同阶喝一壶。"),
                        new Skill("腹尸之毒 · 从中烂起", "CONTROL", 1.0, 200, Vfx.POISON_MIST,
                                "不打你的身，打你吞下去的那口气。等你觉得不对，已经晚了半息。"),
                        new Skill("数亩绿云 · 化蛇万条", "ATTACK", 1.82, 205, Vfx.SUMMON,
                                "一片绿云铺开，云中自己生出蛇来 —— 这些蛇不是我的兽，是我的毒。"),
                        new Skill("万毒混元身", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "修毒的人先把自己泡成药。你打我一分，我自己也要受三分。"),
                        new Skill("破魂蛇 · 一出魂不留", "ULT", 2.58, 428, Vfx.ULT_NOVA,
                                "这一味要养六十年，只放一次。放出去的不是毒，是一条有名字的蛇。"))));

        list.add(new Cultivator(132L, "至阳上人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天南正道第一人 · 太真门太上长老", "天南正道盟太真门",
                "静心诀 + 至阳真火（至阳至刚，专破魔器）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.TREASURE),
                Basis.CANON, "原著明载：太真门太上长老，正道盟领袖，三人中修为稍弱，曾负于天恨老怪",
                "三人里我最弱，这话我认。可我守的那道门、布的那座阵，也是三人里最结实的。",
                List.of(
                        new Skill("至阳真火 · 破魔", "ATTACK", 1.86, 200, Vfx.GOLD_BEAM,
                                "不烧人，专烧你身上那些阴寒法器。魔道修士最怕的不是我，是我这一口火。"),
                        new Skill("静心诀 · 一寸不乱", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "魔道擅长先乱人心。我这门功从二十岁起就在练一件事：不上头。"),
                        new Skill("太真门阵 · 借地势", "CONTROL", 0.92, 180, Vfx.ARRAY_PRESS,
                                "我不擅斗久，所以起手先布阵 —— 阵成了，才轮得到讲道理。"),
                        new Skill("焚天阵 · 金光万道", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "四十多岁的身子背一柄长剑站在阵心，那一刻天南没人敢进阵。"))));

        list.add(new Cultivator(133L, "合欢老魔", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "天南魔道第一人 · 合欢宗大长老", "天南魔道合欢宗",
                "合欢魔功（阴姹女 · 阳婴儿，采补而不伤根）", Style.DEMON,
                List.of(Trait.MIND, Trait.TREASURE, Trait.PERSIST),
                Basis.CANON, "原著明载：合欢宗大长老，天南第一魔修，与房宗主切磋不分胜负",
                "正道讲规矩，我们讲效果。这话我说得不害臊 —— 因为我活到今天，靠的就是不害臊。",
                List.of(
                        new Skill("阴姹女 · 采人元气", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "采补之术最难的不是吸，是吸完还要叫你的经脉看不出被吸过。"),
                        new Skill("阳婴儿 · 婴前杀人", "ATTACK", 1.98, 215, Vfx.BLOOD_SHADOW,
                                "你元婴未炼成之前，我这一手是白用的；炼成了，才值一趟。"),
                        new Skill("魔障 · 先讲人情", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "我从不与人拼第一击。前几句是客气，客气里已经算完了。"),
                        new Skill("喜合擅暗 · 补上那一下", "ATTACK", 2.12, 230, Vfx.SLASH,
                                "天南骂我爱偷袭。骂对了 —— 正面打不赢的人，我凭什么要正面打。"),
                        new Skill("合欢魔功 · 一气化三", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "三具化身同使一门功。你杀了我一次，剩下两次我还要跟你讲人情。"))));

        // ---------- 天南元中一线：原著有名有姓的老怪 ----------
        list.add(new Cultivator(134L, "天恨老怪", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "三大修士之下第一人 · 曾轻伤至阳上人", "天南魔道散修",
                "天恨魔光（先怨后杀，光不蚀肉先蚀神）", Style.ILLUSION,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.GLASS),
                Basis.CANON, "原著明载：天南元中之下第一，曾将至阳上人打成轻伤",
                "我打伤过三个比我名头大的人。此事天南知道的人不多，所以我现在还活着。",
                List.of(
                        new Skill("天恨魔光 · 三息", "CONTROL", 1.05, 195, Vfx.POISON_MIST,
                                "魔光不伤人，先叫你想起一生最不甘的那件事。三息足够我走，也够我出剑。"),
                        new Skill("怨念一击", "ATTACK", 1.92, 200, Vfx.SLASH,
                                "我这门功的力气全在气头上 —— 所以谁叫我冷静，谁就是在削我。"),
                        new Skill("不能力敌就走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "散修的老命是靠腿挣的。名头越大，越该带两条好腿。"),
                        new Skill("一恨到底 · 光尽人废", "ULT", 2.42, 408, Vfx.ULT_NOVA,
                                "我这一恨记了三百年。今日原样还你。"))));

        list.add(new Cultivator(135L, "龙晗", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "鸾鸣宗太上长老 · 天道盟领军", "天南鸾鸣宗",
                "鸾鸣九响（以音为号，联阵攻坚）", Style.SOUND,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "原著明载其为天道盟领军、鸾鸣宗太上长老，小层未点，按与凤冰并列的元中巅峰取值",
                "领军不是最能打的那个，是最后退的那个。",
                List.of(
                        new Skill("鸾鸣三响 · 乱你法诀", "CONTROL", 1.0, 185, Vfx.SOUND_RING,
                                "一声起，二声断指挥，三声你身后的阵自己就乱了。"),
                        new Skill("联军齐攻 · 不独斗", "ATTACK", 1.86, 205, Vfx.SUMMON,
                                "天道盟的仗从来不是两个人打。我一声令下，七位元婴各安其位。"),
                        new Skill("为后队断声", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "这一手不杀人，专门用来把追兵的声音压住。"),
                        new Skill("凤龙合鸣 · 媲美元后", "ULT", 2.48, 418, Vfx.ULT_NOVA,
                                "与道侣同使的一击。两个人中期，打出一个后期的数 —— 这一手盟里最怕。"))));

        list.add(new Cultivator(136L, "凤冰", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "鸾鸣宗 · 龙晗道侣 · 独力牵住一位神师", "天南鸾鸣宗",
                "寒凰真气（不冻人身，先冻灵气之间那道牵连）", Style.ICE,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其与龙晗并肩、可独力牵制大晋慕兰第四神师；小层按元中巅峰取",
                "盟里的事他出头，海上的账我算。两件事本来就得有人分开做。",
                List.of(
                        new Skill("寒凰封冻", "ATTACK", 1.82, 195, Vfx.ICE_FROST,
                                "冻住的是你法诀里那一处转弯。转不过来，一击就散了。"),
                        new Skill("冰面留身 · 且战且走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "牵制不是硬扛。我拖住一个后期，靠的是不跟他打。"),
                        new Skill("一尺冰 · 挡你全力", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "我这门功的护身只有一尺厚，多一寸要省三年。"),
                        new Skill("冰封一角阵", "ULT", 2.44, 410, Vfx.ULT_NOVA,
                                "不冻全场，只冻一角 —— 角里那位今日就别想支援任何人。"))));

        list.add(new Cultivator(137L, "金武环", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "古剑门大长老 · 化剑为丝", "天南古剑门",
                "化剑为丝（一剑分丝，万千不穷）", Style.SWORD,
                List.of(Trait.SWORD, Trait.TREASURE, Trait.GLASS),
                Basis.CANON, "原著明载：古剑门大长老，与韩立切磋，败于风雷翅与乾蓝冰焰",
                "剑修到我这年纪，比的不是快，是敢不敢把自己也炼成剑。",
                List.of(
                        new Skill("化剑为丝 · 万千", "CONTROL", 1.05, 200, Vfx.SWORD_RAIN,
                                "一剑散成三千丝，丝不伤人，先把人困住。困住了才谈剑。"),
                        new Skill("剑丝收口", "ATTACK", 2.0, 215, Vfx.SLASH,
                                "丝阵一成，收口只在一息。这一息里护身灵光跟没有一样。"),
                        new Skill("古剑门传剑 · 单片不留", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "我把本命剑也炼成丝绕在身上。守的时候，我自己就是剑。"),
                        new Skill("丝尽剑亡", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "三千丝全数并成一线。这一线出去，我三百年道行就只剩一半。"))));

        list.add(new Cultivator(138L, "南宫阙", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "掩月宗元中剑修 · 凝光镜", "天南掩月宗",
                "凝光剑诀（以镜收光，光尽剑至）", Style.SWORD,
                List.of(Trait.SWORD, Trait.TREASURE, Trait.MIND),
                Basis.INFERRED, "原著明载掩月宗元中剑修、手持凝光镜，强于令狐老怪；小层取中期",
                "同为南宫，我与婉妹不熟。掩月宗的剑不是拿来讲亲缘的。",
                List.of(
                        new Skill("凝光镜 · 收你剑光", "CONTROL", 1.0, 185, Vfx.MIND_WAVE,
                                "镜不伤人，只把你那一剑的光收了进去 —— 下一剑是你的光，我的剑。"),
                        new Skill("掩月一剑", "ATTACK", 1.95, 205, Vfx.SLASH,
                                "月缺时出剑。这一式不取名，因为它只要一息。"),
                        new Skill("镜光护身", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "镜里存着三段别人的剑光，挨打的时候放一段出去顶。"),
                        new Skill("镜光齐放 · 还你三年", "ULT", 2.4, 405, Vfx.SWORD_RAIN,
                                "这面镜子里攒了我半生的对手。今日全部还给你们。"))));

        list.add(new Cultivator(139L, "令狐老怪", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "黄枫谷唯一的元婴", "天南黄枫谷",
                "黄枫谷旧法（不擅杀伐，擅守谷）", Style.QI,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载黄枫谷只有令狐一人元中，小层按谷中撑得起场面那个取中期",
                "谷里弟子叫我老祖。这点修为摆出去天南没人当回事 —— 可我守的是黄枫谷。",
                List.of(
                        new Skill("守谷一击", "ATTACK", 1.7, 185, Vfx.QI_PALM,
                                "我这一辈子出手多数在谷口，不在别处。"),
                        new Skill("老怪的脸 · 先吓退", "CONTROL", 0.82, 150, Vfx.MIND_WAVE,
                                "名头这个东西不值钱，但能把一场仗推到明日再打。"),
                        new Skill("黄枫谷护山", "DEFENSE", 1.0, 125, Vfx.SHIELD,
                                "谷中弟子都躲在后面，我这一口真气就不能散。"),
                        new Skill("一门之气 · 尽付我手", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "一座小峰三百年不出名，靠的就是有人肯站在门口。"))));

        list.add(new Cultivator(140L, "南陇侯", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "万尺一线 · 天南数一数二的遁术", "天南散修",
                "万尺一线（苍坤上人独创遁术，一线之间人已千里）", Style.QI,
                List.of(Trait.ESCAPE, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著明载其修万尺一线、以遁术闻名，小层无载取元中",
                "散修没有宗门，退路就是我的宗门。",
                List.of(
                        new Skill("万尺一线", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "不是一步千里，是把自己压成一条线 —— 线比你先到位。"),
                        new Skill("存了一辈子的一击", "ATTACK", 1.75, 190, Vfx.QI_PALM,
                                "我不与人换手。攒了三十招才舍得出一次。"),
                        new Skill("见势不对先退", "DEFENSE", 1.0, 125, Vfx.SHIELD,
                                "这道护身不是为了挡住你，是为了叫我多活那半息去起遁。"),
                        new Skill("一线千里 · 你追不上", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "苍坤上人创这一式时想明白的事：先活下来，再谈道。"))));

        list.add(new Cultivator(141L, "碎魂真人", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "碎魂一脉 · 不击身直取魂", "天南散修",
                "碎魂大法（先碎你魂魄，再谈胜负）", Style.MIND,
                List.of(Trait.MIND, Trait.GLASS, Trait.NO_BURST),
                Basis.INFERRED, "原著只存其名与一脉功法，小层按元中取",
                "人身好护，魂难护。这一句我写在帖子上，不是写在心里。",
                List.of(
                        new Skill("碎魂一指", "ATTACK", 1.95, 200, Vfx.MIND_WAVE,
                                "不伤皮肉，直进识海。你身上有多少护身法宝都没用。"),
                        new Skill("乱神摄魂", "CONTROL", 1.0, 185, Vfx.MIND_WAVE,
                                "先叫你对不上法诀里那一口气，后打。"),
                        new Skill("护魂守一", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "修碎魂的人自己最怕被碎。我这一手比功法练得久。"),
                        new Skill("魂飞魄散", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "你元婴出窍那一刻，就是我最快的一刻。"))));

        list.add(new Cultivator(142L, "鲁卫英", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "天极门长老 · 死于坠魔谷", "天南天极门",
                "天极玄光（不显不抢，先护同行的人）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为天极门长老、元中，坠魔谷一役殁于古魔；小层取中期",
                "谷里那东西不是我们杀得死的。我能做到的，是叫三个晚辈先出去。",
                List.of(
                        new Skill("天极玄光 · 厚壁", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "这一门光不出奇，长处是密。密到能替人挡一息。"),
                        new Skill("护住后撤", "CONTROL", 0.85, 160, Vfx.ARRAY_PRESS,
                                "不求胜不求债，先把谷口给你堵住。"),
                        new Skill("长老的一剑", "ATTACK", 1.72, 185, Vfx.QI_PALM,
                                "我出手极老，看着不像元婴。可它到位。"),
                        new Skill("一门之气 · 不尽不止", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "天极门这次进去的人只回去几个。我算那一个没回去的。"))));

        list.add(new Cultivator(143L, "东门图", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "御灵宗大长老 · 以灵压兽", "天南御灵宗",
                "御灵真诀（灵虫猛兽皆受我调）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载御灵宗大长老东门图为元中，小层取中期",
                "兽不听我的，只听划算。我这一生做的事就是一件：把它们算明白。",
                List.of(
                        new Skill("灵虫群 · 噬灵", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "不咬人，先吃你法器上那层灵光。吃完了再谈。"),
                        new Skill("唤兽上山", "ATTACK", 1.85, 200, Vfx.SUMMON,
                                "我不动手，我吹一声。山上那些东西替我动。"),
                        new Skill("御灵印 · 护主", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "印在身上，三阶以内的东西近不了。"),
                        new Skill("万灵齐至", "ULT", 2.45, 415, Vfx.ULT_NOVA,
                                "御灵宗不靠人多。今日叫你看看，我这一山的规矩。"))));

        list.add(new Cultivator(144L, "火龙童子", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "火龙一路 · 童身老怪", "天南魔道散修",
                "火龙真炎（以火养气，气尽火不灭）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著由其元初巅峰一路至元中，本榜取人界篇终局的中期",
                "我叫童子，因为我这张脸不会老。这一条比火龙好用。",
                List.of(
                        new Skill("火龙吐息", "ATTACK", 1.9, 200, Vfx.FIRE_ORB,
                                "一气化龙，龙过成灰。看着热闹，其实烧的是你自己的水气。"),
                        new Skill("童子拜火 · 乱你心神", "CONTROL", 0.95, 180, Vfx.FIRE_ORB,
                                "一张小孩的脸站在火前 —— 你那一剑递不递得下去，就这三息里见。"),
                        new Skill("火铠", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "以身为炉。法术近身先被烧去一半。"),
                        new Skill("火龙焚天", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "一口气养了三百年的火。它出窍那一刻，我自己也要少活十年。"))));

        list.add(new Cultivator(145L, "白瑶怡", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "天南顶尖元中女修 · 独斗青背苍狼", "天南散修",
                "白虹剑诀（一人一剑，不进任何一盟）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其为顶尖元中女修、小极宫一役独对十级妖狼游刃有余；小层取中期",
                "跟妖物打过的人都知道：它不跟你讲规矩，所以你也不必讲体面。",
                List.of(
                        new Skill("白虹一剑", "ATTACK", 1.95, 200, Vfx.SLASH,
                                "剑光起时像一道虹。虹短，够用。"),
                        new Skill("剑光分影", "CONTROL", 0.95, 180, Vfx.SWORD_RAIN,
                                "三道虚影一张网 —— 那张网不是它撒的，是我撒的。"),
                        new Skill("不硬拼 · 且战且走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "十级的爪子擦身而过，靠的不是挡，是让它抓偏。"),
                        new Skill("虹化", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "把自己炼成一线光。这一线过去，我与剑只剩一个还能站着。"))));

        list.add(new Cultivator(146L, "程天坤", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗掌门 · 一家之主", "天南落云宗",
                "落云宗镇宗功法（不求胜人，只求不丢宗门）", Style.ARRAY,
                List.of(Trait.GUARD, Trait.TREASURE, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为落云宗掌门、元婴修士，小层按一宗之主撑场面取初期",
                "掌门不是宗里最强的，是最后一个走的。",
                List.of(
                        new Skill("掌门印 · 借阵", "ATTACK", 1.8, 200, Vfx.ARRAY_PRESS,
                                "这一击不是我打的，是落云宗三座峰一起打的。"),
                        new Skill("护山禁制 · 封一门", "CONTROL", 0.9, 175, Vfx.ARRAY_PRESS,
                                "峰主们在里头议事，我在外头把门关上。"),
                        new Skill("镇宗厚气", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "这门功出不了重手，长处是站着不倒。"),
                        new Skill("宗门之重", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "我修为在几位峰主之下。可今日这一下，吃的是一宗的灵气。"))));

        list.add(new Cultivator(147L, "吕洛", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗长老 · 千浪决", "天南落云宗",
                "千浪决（一波未平一波又起，长于遁走）", Style.QI,
                List.of(Trait.ESCAPE, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为落云宗元婴长老、修千浪决；小层取初期",
                "浪多不代表浪大。我这一门靠的是打不完。",
                List.of(
                        new Skill("千浪叠涌", "ATTACK", 1.7, 185, Vfx.QI_PALM,
                                "一击之后还有九击，每一击都不重 —— 加起来就重了。"),
                        new Skill("浪里抽身", "MOBILITY", 0.0, 125, Vfx.BLOOD_SHADOW,
                                "水气一乱，人已经在三十丈外。"),
                        new Skill("一层水幕", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "不挡重手，只把你那一击偏出去半尺。"),
                        new Skill("千浪齐碎", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "叠了百年的浪一起碎在你身上。声音很大，威力其实一般 —— 但你得先扛住那声音。"))));

        // ---------- 乱星海补：天星双圣的另一位 ----------
        list.add(new Cultivator(148L, "温青", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天星双圣之一 · 天灵根", "乱星海天星双圣",
                "元磁神光（两极之中她养阳）+ 天灵根", Style.MAGNET,
                List.of(Trait.MAGNET, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "原著明载天星双圣之一、天灵根、元磁神光，与凌啸风并列元后",
                "天灵根不是我选的，双修也不是。可这一身两极，我认。",
                List.of(
                        new Skill("元磁神光 · 摄你兵", "CONTROL", 1.0, 195, Vfx.MAGNET_BEAM,
                                "你祭出的那件东西，此刻在半空中偏了一线。一线就够。"),
                        new Skill("阳极一放", "ATTACK", 1.85, 195, Vfx.MAGNET_BEAM,
                                "两极须同时起、同时收。三百年练的就是这一口气。"),
                        new Skill("星宫旧阵 · 半边阵眼", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "阵眼在我们两人身上各一半。我这一半今日不许丢。"),
                        new Skill("两极齐鸣 · 短暂抗化神", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一击要两个人同使。今日他不在，我一个人也能叫这片海听调。"))));

        // ---------- 大晋：元后多如麻，只收有名有姓、出手有据的 ----------
        list.add(new Cultivator(149L, "房宗主", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "阴罗宗宗主 · 三具尸王随身", "大晋阴罗宗",
                "阴罗宗炼尸大法（铜甲、乌甲尸王各是其主）", Style.CORPSE,
                List.of(Trait.TREASURE, Trait.SUMMON, Trait.PERSIST),
                Basis.INFERRED, "原著明载阴罗宗宗主为元后、随身三具尸王（约当元婴初期）；小层取后期",
                "我门下最听话的是这三具。它们不讨价，也不告老。",
                List.of(
                        new Skill("铜甲尸王 · 上前", "ATTACK", 1.9, 210, Vfx.SUMMON,
                                "不知痛、不惧死，生前也是元婴。死后比我听话。"),
                        new Skill("乌甲尸王 · 夹击", "ATTACK", 1.95, 215, Vfx.SUMMON,
                                "两具同使，一正一侧。你顾哪一头？"),
                        new Skill("尸毒郁积 · 一闻就倒", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "炼尸身上那点毒积了几百年，开盖就散。"),
                        new Skill("金色尖锥 · 遁速极快", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "宗主不与人斗久。要么尸王上，要么我这一锥先走。"),
                        new Skill("灭仙珠 · 一颗可惊化神", "ULT", 2.7, 435, Vfx.ULT_NOVA,
                                "此珠一发，境界这个东西今日不算。代价是三具尸王都要替我按住它。"))));

        list.add(new Cultivator(150L, "乾老魔", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "阴罗宗大长老 · 五子同心魔", "大晋阴罗宗",
                "逆情断天大法（先绝情绝性，方敢入门）", Style.DEMON,
                List.of(Trait.MIND, Trait.SUMMON, Trait.PERSIST),
                Basis.CANON, "原著明载阴罗宗大长老，五子同心魔合体可达元后巅峰且不灭",
                "逆情断天这四个字不是形容。它是一条门槛：先把你心里那点东西砍了。",
                List.of(
                        new Skill("五子同心魔 · 合体", "ATTACK", 2.25, 235, Vfx.SUMMON,
                                "五个元婴精魂祭炼二十年，合起来是一具不死的魔。"),
                        new Skill("鬼罗幡 · 镇宗之宝", "CONTROL", 1.05, 205, Vfx.BLOOD_SHADOW,
                                "幡一展开，里头那些精魂都认识我 —— 因为它们全是我放进去的。"),
                        new Skill("血魔珠 · 寄附元婴", "HEAL", 0.95, 195, Vfx.BLOOD_SHADOW,
                                "这具身子坏了不要紧，珠里还养着一个元婴，那是我的第二副肉躯。"),
                        new Skill("逆情 · 断你六亲", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "我不怕你威胁我。我这一身已经没有可以被威胁的地方了。"),
                        new Skill("绝情一击", "ULT", 2.62, 432, Vfx.ULT_NOVA,
                                "砍掉的那部分会疼三百年。疼完了，力气都是它的。"))));

        list.add(new Cultivator(151L, "玄青子", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "太一门话事人 · 平山印", "大晋太一门",
                "太一玄功 + 平山印（仿制通天灵宝）", Style.GUARD,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载太一门话事人持仿制通天灵宝平山印；小层取元后",
                "灵宝不是我们的。我们只是替它看门。",
                List.of(
                        new Skill("平山印 · 一印镇山", "ATTACK", 2.15, 225, Vfx.ARRAY_PRESS,
                                "印落下来不带风声。山不是被打倒的，是被按住的。"),
                        new Skill("太一护身", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "大晋的宗门不擅斗狠，擅的是不被斗狠。"),
                        new Skill("仿宝之威 · 先压器", "CONTROL", 0.95, 185, Vfx.QI_PALM,
                                "印未落，你手上那件先颤 —— 它认得比它高的东西。"),
                        new Skill("山不至 · 人已平", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一印是照着真正的平山印仿的。仿到几分，今日叫你量一量。"))));

        list.add(new Cultivator(152L, "七妙真人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天魔宗执法长老 · 赤鸣鼓", "大晋天魔宗",
                "赤鸣鼓音（一鼓破胆，再鼓散魂）", Style.SOUND,
                List.of(Trait.TREASURE, Trait.MIND, Trait.GUARD),
                Basis.INFERRED, "原著明载天魔宗执法长老七妙真人持仿制灵宝赤鸣鼓；小层取元后",
                "执法这件事不能讲情面，也不能讲力气。讲力气就轮不到我。",
                List.of(
                        new Skill("赤鸣一鼓 · 破胆", "CONTROL", 1.05, 195, Vfx.SOUND_RING,
                                "鼓声不进耳，进心脉。你听见那一下，胆气已经短了三分。"),
                        new Skill("再鼓散魂", "ATTACK", 1.95, 205, Vfx.SOUND_RING,
                                "两鼓之间没有第三鼓的余地。这一门讲的就是这个。"),
                        new Skill("执法令 · 令众不动", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "宗里有人要动手，我先敲一鼓。鼓响之后还动手的，才叫执法。"),
                        new Skill("三鼓 · 万军自乱", "ULT", 2.5, 415, Vfx.ULT_NOVA,
                                "赤鸣鼓一响，仿制灵宝这四个字就没人敢提了。"))));

        list.add(new Cultivator(153L, "坤无极", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "血凄门大长老 · 以血换力", "大晋血凄门",
                "血凄真解（以自身精血换一时之强）", Style.YIN,
                List.of(Trait.PERSIST, Trait.TREASURE, Trait.GLASS),
                Basis.BOARD, "原著只列其名与元后之阶，出手记载极少，本榜按大晋元后通则取值",
                "别人炼气，我炼血。血少一分，力气就多一分 —— 这笔账我自己算得清。",
                List.of(
                        new Skill("血凄一祭", "ATTACK", 1.9, 200, Vfx.BLOOD_SHADOW,
                                "一口精血喷在法器上，那一击立刻重三成。重的这三成是从寿元里借的。"),
                        new Skill("以血养气", "HEAL", 0.9, 0, Vfx.HEAL_GLOW,
                                "这一门不养气养血。血回来了，人才算回来。"),
                        new Skill("摄血成幕", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "血幕不挡法，挡的是你靠近的那一步。"),
                        new Skill("一门之血 · 尽付此击", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "今日这一击之后，我要养六十年。养不养得回来，看你死没死。"))));

        list.add(new Cultivator(154L, "元智大师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "雷音宗 · 明王诀 + 四层金刚诀", "大晋雷音宗",
                "明王诀（金身四相 · 肉身堪比法宝）", Style.BODY,
                List.of(Trait.BODY, Trait.GUARD, Trait.TREASURE),
                Basis.INFERRED, "原著明载雷音宗元智大师修明王诀与四层金刚诀，肉身堪比法宝；小层取元后",
                "禅杖上那颗万年火珊瑚不是我供的，是我拿来敲人的。",
                List.of(
                        new Skill("四层金刚 · 身如法宝", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "金诀练到第四层，刀砍上去是砍在自己法器上。"),
                        new Skill("明王忿怒相", "ATTACK", 2.0, 215, Vfx.QI_PALM,
                                "一念忿怒，一身金相。雷音宗不动这相，动这相就是不讲佛法了。"),
                        new Skill("法力金刚 · 虚影落地", "ATTACK", 1.9, 205, Vfx.SUMMON,
                                "召出的不是佛，是我这些年攒下的那点法力。它比我耐打。"),
                        new Skill("禅杖 · 火珊瑚一敲", "CONTROL", 0.95, 185, Vfx.FIRE_ORB,
                                "万年火珊瑚嵌在杖头。敲下去先烧你护身灵光那一层。"),
                        new Skill("明王现身", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "金身一现，法不辩、理不辩。老衲这一生只辩这一次。"))));

        list.add(new Cultivator(155L, "碧月禅师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "净火宗 · 四百年入元后的万年一遇", "大晋净火宗",
                "净火禅定（火不焚人，先焚自心）", Style.QI,
                List.of(Trait.PERSIST, Trait.GUARD, Trait.NO_BURST),
                Basis.BOARD, "原著只载其才名与四百年入元后，人界篇未正面出手，本榜按元后通则取值",
                "没人见过我出手，所以人人都说我最强。这一句我受着，受了几百年。",
                List.of(
                        new Skill("净火一焚", "ATTACK", 1.8, 195, Vfx.FIRE_ORB,
                                "此火不烧皮肉，先烧你身上那些不干净的东西。魔道修士最怕这一味。"),
                        new Skill("禅定不动", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "四百年的静坐不是白坐的。你那一击到我身前，我先不动心。"),
                        new Skill("火中生莲", "HEAL", 0.85, 170, Vfx.HEAL_GLOW,
                                "伤势用自家真火养。疼是真疼，好也是真好。"),
                        new Skill("四百年只做这一件事", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "今日若出这一手，往后四百年大约又要重来。所以我一直没用。"))));

        list.add(new Cultivator(156L, "易洗天", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "大晋四大散修之一 · 曾轻斩八级妖兽", "大晋散修",
                "洗天剑诀（一人一剑，不属任何宗门）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其为大晋四大散修之一、曾轻斩八级妖兽；本榜依富成那句「战力不宜高估」取元后而非元后巅峰",
                "散修没有靠山，我的靠山是那一剑。斩过八级妖兽，所以还能站着讲这句话。",
                List.of(
                        new Skill("一剑斩妖", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "对皮粗肉厚的东西，剑要走在它眨眼之前。八级妖兽我斩过，它也没眨眼。"),
                        new Skill("洗天 · 剑气如虹", "ATTACK", 1.9, 200, Vfx.SWORD_RAIN,
                                "剑气铺开像下雨。散修的剑不讲究漂亮，讲究覆盖。"),
                        new Skill("散修的警觉", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "我睡觉时护身灵光也不散。这一条比剑诀值钱。"),
                        new Skill("四大散修之名", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这个名号不是封的，是一路活下来别人叫的。叫的人多，就得应。"))));

        list.add(new Cultivator(157L, "寒骊上人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "北夜小极宫 · 乾蓝冰焰修到极致", "大晋北夜小极宫",
                "乾蓝冰焰 + 两口乾蓝鼎", Style.ICE,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载小极宫寒骊上人两口乾蓝鼎、乾蓝冰焰已修到极致，能轻接三焰一扇；小层取元后",
                "北夜的冰不是寒，是慢。慢到你身上那点灵气忘了自己是活的。",
                List.of(
                        new Skill("乾蓝冰焰 · 一扇", "ATTACK", 2.05, 215, Vfx.ICE_FROST,
                                "这火焰看着蓝，其实不烫。它冻的是你运功的那条线。"),
                        new Skill("两口乾蓝鼎 · 同压", "CONTROL", 1.0, 190, Vfx.ARRAY_PRESS,
                                "两口鼎一起祭出，一口压你身，一口装你那些没了用的东西。"),
                        new Skill("冰焰护体", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "近我三尺者先僵。僵那一息，就够我把鼎收回来。"),
                        new Skill("北夜极寒 · 三焰不足惧", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "这一场我只当是替小极宫看门。门里的东西，你不能带走。"))));

        list.add(new Cultivator(158L, "小极宫宫主", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "小极宫主 · 手里有一把黄泉的钥匙", "大晋北夜小极宫",
                "小极宫主功法 + 制约黄泉鬼母之法", Style.MIND,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.NO_BURST),
                Basis.BOARD, "原著未点其名与境界，按其为一宫之主、能制鬼母的规格取元后",
                "制得住她的不是我的功法，是我手里那把钥匙。这一句我从不避。",
                List.of(
                        new Skill("钥令 · 唤鬼母", "CONTROL", 1.05, 200, Vfx.MIND_WAVE,
                                "我不出手。我把令一亮，自有人替我出手 —— 那个不是人。"),
                        new Skill("小极寒冰阵", "ATTACK", 1.85, 200, Vfx.ARRAY_PRESS,
                                "北夜的阵不靠阵旗，靠这一山的寒气。我站在阵心就是阵眼。"),
                        new Skill("宫主之位", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "这一宫的人都在我身后，所以我不能散。"),
                        new Skill("一宫之权", "ULT", 2.4, 410, Vfx.ULT_NOVA,
                                "钥匙只有一把。今日用它，明日宫里就要少一个人。"))));

        list.add(new Cultivator(159L, "黄泉鬼母", Realm.NASCENT_SOUL, SubStage.MID, "中期（白板鬼修）",
                "鬼道白板 · 与车老妖化身不分胜负", "北夜黄泉 · 鬼道",
                "黄泉鬼道（无肉身，纯以阴魂作战）", Style.CORPSE,
                List.of(Trait.RESIDUAL, Trait.MIND, Trait.ESCAPE),
                Basis.INFERRED, "原著明载其为白板鬼修、与车老妖化身斗个不分胜负；白板之身按元中折算",
                "他们叫我鬼母。我这一身最不值钱的就是那具身体 —— 它早烂了。",
                List.of(
                        new Skill("阴魂夺舍", "ATTACK", 1.95, 205, Vfx.BLOOD_SHADOW,
                                "我不杀你。我进你那具身子，试试合不合穿。"),
                        new Skill("黄泉雾 · 掩身", "CONTROL", 1.0, 185, Vfx.POISON_MIST,
                                "雾里那些脸都是我的旧识。它们在帮你选一边站。"),
                        new Skill("无身可伤", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "你砍了我三剑，我少了一层雾。这话听着像认输，其实是认我这具身子不算。"),
                        new Skill("鬼母现身 · 阴魂不散", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "被制了三百年。今日谁把钥匙给我，我替他做一件事。"))));

        list.add(new Cultivator(160L, "叶家大长老", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "叶家大长老 · 黑血刃砍伤狮禽兽", "大晋叶家",
                "黑血刃（以精血激发，一击见骨）", Style.SWORD,
                List.of(Trait.SWORD, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "原著明载叶家大长老持黑血刃、曾砍伤十级妖禽并接过古魔一击；小层取元后",
                "世家不是功法好，是肯把精血拿出来的人多。",
                List.of(
                        new Skill("黑血刃 · 精血激发", "ATTACK", 2.15, 225, Vfx.SLASH,
                                "这口刃要吃血才亮。血是自己的，亮一分就是一分。"),
                        new Skill("接古魔一击", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "那一击我接了半边身子。半边够了 —— 身后那些孩子还没跑远。"),
                        new Skill("喝退妖禽", "CONTROL", 0.95, 190, Vfx.QI_PALM,
                                "四翅的畜生不怕人，怕的是敢拿刀跟它讲价的人。"),
                        new Skill("一门之血 · 一刀换一生", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "叶家这一代能站着说话，是因为上一代有人肯把血放完。"))));

        list.add(new Cultivator(161L, "叶月圣", Realm.NASCENT_SOUL, SubStage.LATE, "后期（隐藏修为）",
                "叶家七叔 · 藏到最后一刻才动手", "大晋叶家",
                "借古魔之力炼体（藏得最深的那一个）", Style.BODY,
                List.of(Trait.BODY, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载叶家七叔隐藏元婴后期修为、终投古魔；本榜按其暴露后的规格取元后",
                "族里都当我修为平平。藏了三百年，就为今日这一下。",
                List.of(
                        new Skill("藏了三百年的一拳", "ATTACK", 2.1, 220, Vfx.QI_PALM,
                                "这一拳不出去，我还是那个帮着看孩子的七叔。"),
                        new Skill("古魔之力 · 炼体", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "借来的力要先把自己改成一具能用它的身子。这一步最疼，也最值。"),
                        new Skill("血祭借力", "ATTACK", 1.9, 200, Vfx.BLOOD_SHADOW,
                                "每借一分，我就少记得一点自己原来的名字。"),
                        new Skill("七叔的名分", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "今日之后，叶家没有七叔了。这一句我等了很久，也怕了很久。"))));

        list.add(new Cultivator(162L, "富成", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "九幽宗执法长老 · 把话放在心里说", "大晋九幽宗",
                "九幽玄功（不显山不露水，专办宗门的事）", Style.YIN,
                List.of(Trait.GUARD, Trait.MIND, Trait.NO_BURST),
                Basis.INFERRED, "原著明载九幽宗执法长老富成为元中；他点评易洗天那句「战力不宜高估」是本榜取值口径的根据",
                "宗门里的事，办完了不必说。说了就还得再办一次。",
                List.of(
                        new Skill("九幽拿禁", "CONTROL", 1.0, 190, Vfx.MIND_WAVE,
                                "不伤人，先把你两只手借走。宗里办事讲个规矩。"),
                        new Skill("执法一击", "ATTACK", 1.78, 190, Vfx.SLASH,
                                "这一击不重。重的是它背后的那条文。"),
                        new Skill("护住本心", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "九幽一路最会拿人心。我这一手专门防自家的人心。"),
                        new Skill("宗门律 · 也是我律", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "我办过的人里有一半今日会替我说话。这就是执法长老的本钱。"))));

        list.add(new Cultivator(163L, "徐大仙师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天澜草原大仙师 · 与至阳、乾老魔齐名", "天澜草原",
                "草原祭天大法（借地势祭坛，不借人身）", Style.ARRAY,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载天澜草原大仙师与至阳上人、乾老魔齐名，后被万年尸熊砍去躯体；小层取元后",
                "草原不认宗门，认祭坛。坛上那点烟一升，万帐的人都站我这边。",
                List.of(
                        new Skill("祭坛召力", "ATTACK", 1.9, 205, Vfx.ARRAY_PRESS,
                                "我这一击不是从手上来的，是从脚下那片地来的。"),
                        new Skill("大仙师之名 · 压场", "CONTROL", 1.0, 185, Vfx.ARRAY_PRESS,
                                "名号这个东西在草原上有分量。分量本身就能压住一半人。"),
                        new Skill("躯体不全 · 硬撑", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "少了一半身子的老怪还在撑阵 —— 因为我一散，万帐就乱。"),
                        new Skill("草原万帐同祭", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一座坛是草原几百年的账。今日拿它跟你结一次。"))));

        list.add(new Cultivator(164L, "田锤", Realm.NASCENT_SOUL, SubStage.EARLY_PEAK, "刚入后期",
                "慕兰第四神师 · 神来人不来", "慕兰草原",
                "慕兰巫神功（以血祭神，神力附体）", Style.MIND,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著明载其为慕兰第四神师、刚进阶元后，曾被凤冰牵制；小层取刚入后期",
                "神不来说我不在。可血一放出去，我就得在坛下挨着。",
                List.of(
                        new Skill("血祭请神", "CONTROL", 1.0, 195, Vfx.MIND_WAVE,
                                "神来那一瞬，你的法诀会自己乱一拍。一拍够了。"),
                        new Skill("神师一击", "ATTACK", 1.85, 200, Vfx.QI_PALM,
                                "这一击我使不出自己的力气 —— 使的是附在我身上的那一份。"),
                        new Skill("被冰牵住 · 硬拖", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "冻到手臂上我就不换刀。这一门神功教的头一条是不喊疼。"),
                        new Skill("神来一瞬", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "只借一瞬。那一瞬里我不是我 —— 这大约是最好的事，也是最坏的。"))));

        // ---------- 妖修：十级约当元婴后期，人界明面上的顶尖战力 ----------
        list.add(new Cultivator(165L, "金蛟王", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级妖兽）",
                "外海蛟龙族长 · 一枪杀星宫桂长老", "乱星海外海蛟龙族",
                "蛟龙族炼体 + 金甲长枪", Style.BEAST,
                List.of(Trait.BODY, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载外海蛟龙族长金蛟王为十级妖修（约当元后巅峰）、强势击杀星宫桂长老；本榜按其规格取元后巅峰",
                "外海不认人界的规矩，只认鳞片的硬度。",
                List.of(
                        new Skill("蛟枪 · 一刺穿金甲", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "蛟筋做杆、龙牙做尖。人界那些法器在它面前像纸糊的。"),
                        new Skill("龙鳞护身", "DEFENSE", 1.0, 150, Vfx.TURTLE_SHELL,
                                "一片鳞就是一件护身法宝。我身上有几千片。"),
                        new Skill("水府号令 · 召群妖", "ATTACK", 1.9, 205, Vfx.SUMMON,
                                "我一枪不出，先叫。海里有的是愿意替我出枪的。"),
                        new Skill("蛟龙之躯 · 金身", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "现出原身那一刻，人界的斗法就变成一个词：量。"))));

        list.add(new Cultivator(166L, "万年尸熊", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖谷副谷主 · 昆吾山大杀四方", "天南万妖谷",
                "尸熊之躯 + 血刃（仿制魔龙刃）", Style.BEAST,
                List.of(Trait.BODY, Trait.TREASURE, Trait.SWORD),
                Basis.INFERRED, "原著明载万妖谷副谷主尸熊持仿制魔龙刃血刃，昆吾山中砍去徐大仙师躯体；按十级妖修取元后",
                "谷里叫我副谷主。这个位子不是打来的，是大家伙都挨过我一刀之后让出来的。",
                List.of(
                        new Skill("血刃 · 砍去躯体", "ATTACK", 2.15, 225, Vfx.BLOOD_SHADOW,
                                "仿制的魔龙刃，砍人不用第二下。仙师的躯体也是一刀。"),
                        new Skill("熊抱 · 骨头自己会断", "CONTROL", 1.0, 195, Vfx.QI_PALM,
                                "法器我都嫌麻烦。抱住之后你自己会散。"),
                        new Skill("万年皮肉", "DEFENSE", 1.0, 150, Vfx.TURTLE_SHELL,
                                "这一身不是甲，是万年慢慢长出来的肉。它自己会夹住你的剑。"),
                        new Skill("万妖谷二当家", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "昆吾山上那一夜，我没让过路。不让路是谷里的规矩。"))));

        list.add(new Cultivator(167L, "青背苍狼", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖谷副谷主 · 一张网困两名元中", "天南万妖谷",
                "苍狼血脉 + 皂袍藏网", Style.BEAST,
                List.of(Trait.TREASURE, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载万妖谷副谷主青背苍狼作皂袍老者，持网独斗叶家大长老与白瑶怡而游刃有余；按十级取元后",
                "我穿人衣，说人话。不是为了像人，是为了叫人先不想动手。",
                List.of(
                        new Skill("撒网 · 困敌", "CONTROL", 1.05, 195, Vfx.ARRAY_PRESS,
                                "这张网不勒人，勒法器。网住的那一位今日只能用自己的手。"),
                        new Skill("苍狼扑食", "ATTACK", 1.95, 205, Vfx.SLASH,
                                "狼的打法只有一式：先绕，后扑。绕的那三圈你也得算进去。"),
                        new Skill("且战且走", "MOBILITY", 0.0, 135, Vfx.BLOOD_SHADOW,
                                "两个元中一起上来，我自然得分开打 —— 分开打靠腿。"),
                        new Skill("谷中狼群 · 齐至", "ULT", 2.45, 415, Vfx.ULT_NOVA,
                                "我不是最能打的。可我叫得动的比他们多。"))));

        list.add(new Cultivator(168L, "木魁", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级妖修）",
                "昆吾山四妖 · 一株占了半座山的古木", "昆吾山",
                "草木大阵（根须延数十里，木雾含剧毒）", Style.WOOD,
                List.of(Trait.PERSIST, Trait.SUMMON, Trait.GUARD),
                Basis.INFERRED, "原著明载昆吾山木魁为万年古木，枝干堪比极品法宝、扎根无尽；最终被三老残留神念驱法宝灭杀，按十级取元后巅峰",
                "我不追人。我长得比你跑得远。",
                List.of(
                        new Skill("草木大阵 · 困", "CONTROL", 1.05, 200, Vfx.ARRAY_PRESS,
                                "山上那些草都是我的根。你踩了一路，其实一直在我身上。"),
                        new Skill("木牢 · 枝干如极品法宝", "ATTACK", 1.95, 210, Vfx.SLASH,
                                "一根枝条就是一件法宝。我身上有几万根。"),
                        new Skill("剧毒木雾", "ATTACK", 1.85, 200, Vfx.POISON_MIST,
                                "雾不难看，就是不能呼吸。山中的兽都绕着走。"),
                        new Skill("扎根 · 不尽", "HEAL", 1.0, 0, Vfx.HEAL_GLOW,
                                "烧了我一半不要紧。根在，半座山替我长回来。"),
                        new Skill("半座山都是我的", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "这一式不是法术，是年岁。一万年的东西压下来，你拿什么挡？"))));

        list.add(new Cultivator(169L, "银翅夜叉", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级飞天尸）",
                "昆吾山四妖 · 会算计的一具尸", "昆吾山阴阳窟",
                "飞天尸神通（煞魂丝 · 风土二遁 · 兼修幻术）", Style.CORPSE,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.BODY),
                Basis.INFERRED, "原著明载银翅夜叉为昆吾山四妖之一、实是仅次于金身月尸的十级飞天尸，在阴阳窟秒杀两名元中；按十级取元后巅峰",
                "他们以为尸不会想事。想事的尸，才是真正难杀的那一种。",
                List.of(
                        new Skill("煞魂丝 · 缚婴", "CONTROL", 1.05, 200, Vfx.BLOOD_SHADOW,
                                "此丝不缚身，缚你元婴。婴被缚住，你那具身子就只剩一层皮。"),
                        new Skill("阴阳窟 · 秒杀两元中", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "那一窟里我没用第二招。不是省力气，是叫他们以为还有第三人在洞内。"),
                        new Skill("风土二遁 · 倏忽", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "一遁走风，一遁走土。尸身不需要呼吸，所以土遁我能多待半日。"),
                        new Skill("尸身不知痛", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "你砍了我一臂，我用另一半接着打 —— 因为那一臂本来就不算我的。"),
                        new Skill("银翅遮天", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "翅一开，窟中尽是银光。那里面哪一只是真的，你得自己猜。"))));

        list.add(new Cultivator(170L, "狮禽兽", Realm.NASCENT_SOUL, SubStage.LATE, "后期（未化形）",
                "昆吾山四妖 · 一身爆发碾压多数元后", "昆吾山",
                "狮首鹰身四翅（雷遁 + 金色波纹，不修神识只修力）", Style.BEAST,
                List.of(Trait.BODY, Trait.ESCAPE, Trait.NO_BURST),
                Basis.INFERRED, "原著明载狮禽兽为昆吾山四妖之一、狮首鹰身四翅、未化形而肉身爆发极强；智不足，按十级取元后",
                "我不会说话。这一条山上的人都知道，所以从不劝我讲道理。",
                List.of(
                        new Skill("四翅雷遁 · 扑", "ATTACK", 2.2, 225, Vfx.QI_PALM,
                                "四翅一齐，雷在爪上。它不快到看不见，只是挡不住。"),
                        new Skill("金色波纹 · 声波", "ATTACK", 1.95, 205, Vfx.SOUND_RING,
                                "一声下去，山石齐碎。法器在里面的人耳朵里先什么也听不见了。"),
                        new Skill("紫光翅影", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "它不使遁术。它就是飞，可四翅一展你已经算不出它下一步落在哪儿。"),
                        new Skill("未化形之身 · 一击不讲理", "ULT", 2.55, 430, Vfx.ULT_NOVA,
                                "它不懂法诀，也不懂克制。它只会把全身力气放在一下里 —— 这一下比多数元后的神通都重。"))));

        list.add(new Cultivator(171L, "车老妖", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖幡主 · 化身与鬼母不分胜负", "天南万妖谷",
                "万妖幡（幡上诸妖，皆我所炼）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载车老妖持万妖幡、其化身与黄泉鬼母斗个不分胜负；按十级妖修取元后",
                "幡里那些东西都欠我。欠得久了，它们就肯替我死。",
                List.of(
                        new Skill("万妖幡 · 放妖", "ATTACK", 2.0, 215, Vfx.SUMMON,
                                "一幡在手，里头几百头。它们不听话，但它们怕我比怕你多。"),
                        new Skill("化身代战", "CONTROL", 1.0, 190, Vfx.MIND_WAVE,
                                "你打赢的那个不是我。这一条我从不隐瞒 —— 因为说出来你也没办法。"),
                        new Skill("妖气遮身", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "妖气一裹，你那些探人的本事全部落空。"),
                        new Skill("幡中百妖齐出", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "今日这一幡抖完，往后的年月里我得重新收一批。"))));

        list.add(new Cultivator(172L, "玄骨上人", Realm.NASCENT_SOUL, SubStage.LATE, "后期（被擒复出身）",
                "虚天殿旧怪 · 死过一次的老怪", "乱星海",
                "玄骨魔功（骨为器，魂为引）", Style.YIN,
                List.of(Trait.MIND, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "原著虚天殿一役的老怪：肉身被极阴祖师打爆、元婴被擒，多年后复出；按复出后的规格取元后",
                "死过一次以后，什么面子、什么势，都可以不要了。",
                List.of(
                        new Skill("玄骨化兵", "ATTACK", 1.95, 205, Vfx.BLOOD_SHADOW,
                                "骨头是自己的，法器也是自己的。这一门里这两件事没分别。"),
                        new Skill("擒婴 · 反手", "CONTROL", 1.0, 195, Vfx.MIND_WAVE,
                                "他擒过我的婴。如今我学了这一手，拿来还他 —— 这话我不对人讲，只对自己讲。"),
                        new Skill("死过一次 · 不怕", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "你吓不住一个已经死过一回的人。这一条比护身法术好用。"),
                        new Skill("复出 · 当席吞你", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "在那座殿里被关了多年。今日重见天日，我先吃一口。"))));

        // ---------- 结丹补：乱星海结丹第一人 ----------
        list.add(new Cultivator(173L, "温天仁", Realm.CORE_FORMING, SubStage.PEAK, "巅峰（乱星海结丹第一人）",
                "六道极圣传人 · 一身宝物一身劫", "乱星海 · 魔道六道极圣门下",
                "六极真魔功（另有化劫大法、颠凤培元功、引龙诀）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "原著明载：乱星海结丹修士第一人，六道极圣门下；后被鬼雾卷入阴冥之地法力尽废，死于暴风山山腰",
                "身上宝物最多的人，最后死在一条不会法术的手臂上。这话不是说我，是说那一夜的我。",
                List.of(
                        new Skill("六极真魔功 · 六气齐出", "ATTACK", 1.95, 210, Vfx.BLOOD_SHADOW,
                                "师父那一门的功我使了个皮。遇上辟邪神雷，六气就只够挨一下。"),
                        new Skill("天阳鎏金针 · 克制噬金虫", "CONTROL", 1.0, 185, Vfx.GOLD_BEAM,
                                "虫群一到，金针先放。这一件宝是为那东西专门炼的 —— 我身上全是这种专门。"),
                        new Skill("化劫大法 · 断肢化劫", "DEFENSE", 1.0, 160, Vfx.SHIELD,
                                "劫来了就切一条手臂给它。切过一次，下次还敢切。"),
                        new Skill("引龙诀 · 借一口气", "HEAL", 0.9, 0, Vfx.HEAL_GLOW,
                                "师父自创的双修秘术。借来的力也是力 —— 只是要还。"),
                        new Skill("八门金光镜（仿品）· 神焰封门", "ULT", 2.35, 380, Vfx.ULT_NOVA,
                                "八门齐开那一刻，我真以为自己已经是元婴。每一道金光都要先耗我自己一口元气。"))));

        return List.copyOf(list);
    }
}
