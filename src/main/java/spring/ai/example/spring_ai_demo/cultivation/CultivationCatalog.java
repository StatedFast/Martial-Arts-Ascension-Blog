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
                                "我一剑从不指望快——快不过你。可万剑齐至那一刻，你已经没处可躲。"),
                        new Skill("大衍诀 · 分识探破绽", "CONTROL", 0.72, 150, Vfx.MIND_WAVE,
                                "你法诀里那道破绽，三招前我就看见了。留着，是等你自己撞上来。"),
                        new Skill("长春功 · 养气回元", "HEAL", 0.55, 0, Vfx.HEAL_GLOW,
                                "慢有什么要紧。我这一身真气一分没丢——先耗不住的，是你。"),
                        new Skill("血影遁", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "打不过就走，这不丢人。活得够久的人，才轮得到开口说话。"),
                        new Skill("青元剑诀 · 剑光化虹", "ULT", 2.55, 390, Vfx.GOLD_BEAM,
                                "剑气生发绵绵不尽，收束成一线的那一刹——我这一辈子，只赌这一剑。"))));

        list.add(new Cultivator(118L, "向之礼", Realm.SOUL_TRANSFORMATION, SubStage.EARLY_PEAK, "初期巅峰",
                "人界顶点 · 三千年没等到飞升", "上古遗老 · 独行于天南",
                "化神法则（天地之气不必从身上取）", Style.LAW,
                List.of(Trait.LAW, Trait.MIND, Trait.PERSIST),
                Basis.CANON, "本项目 bio 明写「化神初期，人界顶点」，原著为人界化神初期巅峰",
                "化神打元婴，从不斗久。天地之气站在谁那边，这一战就完了。",
                List.of(
                        new Skill("一呼 · 山河皆应", "ATTACK", 1.95, 210, Vfx.ARRAY_PRESS,
                                "不必我自己动手。一呼一吸，这万里山河替我落子。"),
                        new Skill("天地之气 · 压", "DEFENSE", 1.0, 160, Vfx.SHIELD,
                                "不是我挡下你这一击——是你这一击到了我身前，自己就散了。"),
                        new Skill("三千年所记 · 知你根底", "CONTROL", 0.8, 150, Vfx.MIND_WAVE,
                                "你这套功夫，三千年前我见过你师父使。你出的每一手，我都记得。"),
                        new Skill("化神叫令 · 天地为我所用", "ULT", 3.1, 430, Vfx.ULT_NOVA,
                                "叫得动天地的人，从不与人斗，只与势斗。势站在我这边——你，拿什么赢？"))));

        // ---------- 元婴：人界明面上的顶尖战力 ----------
        list.add(new Cultivator(102L, "南宫婉", Realm.NASCENT_SOUL, SubStage.LATE, "后期（距化神一步）",
                "掩月宗 · 素女元阴功传人", "越国七大派掩月宗 · 后为韩立道侣",
                "素女元阴功（元阴为本，不在双修）", Style.ICE,
                List.of(Trait.MIND, Trait.PERSIST, Trait.ALCHEMY),
                Basis.CANON, "原著人界篇末为元婴后期大修士，离化神只一步之差",
                "元阴是蓄，不是换。守不住清静的人，练得越快，反噬越早。",
                List.of(
                        new Skill("凝气成刃 · 寒气一刀", "ATTACK", 1.58, 160, Vfx.ICE_FROST,
                                "元阴的杀伐只此一式。出手极冷极净——不逼到最后，我不出。"),
                        new Skill("月华洗髓", "HEAL", 0.9, 200, Vfx.MOON_GLOW,
                                "每逢月满，潮信替我洗髓。这一口清气，我守了不知多少年月。"),
                        new Skill("素女元阴护体", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "我这一身元阴，邪毒近身先退三分。你再阴的手段，也脏不了我。"),
                        new Skill("元阴本源 · 冰封千里", "ULT", 2.4, 380, Vfx.ICE_FROST,
                                "至寒一线铺开千里。方圆之内水气与灵气的牵连，先被我冻住。"))));

        list.add(new Cultivator(110L, "冰凤", Realm.NASCENT_SOUL, SubStage.LATE, "后期（卡关十一年）",
                "天南冰凤一族 · 玄冰真火", "天南冰凤氏",
                "玄冰真火（本命神通 · 名里有火实则至寒）", Style.ICE,
                List.of(Trait.MIND, Trait.PERSIST, Trait.TREASURE),
                Basis.CANON, "本项目 bio 明写「元婴后期，卡在化神已十一年」",
                "化神不是冲过去的，是你不再想冲的时候，它自己开的。",
                List.of(
                        new Skill("玄冰真火 · 冻灵", "ATTACK", 1.78, 190, Vfx.ICE_FROST,
                                "被它沾上，你自己的真元当场冻住，运都运不开。"),
                        new Skill("凤翎冰羽", "ATTACK", 1.42, 150, Vfx.SWORD_RAIN,
                                "一族血脉所授的冰羽，旁人穷尽一生也学不去。"),
                        new Skill("雪止 · 心静", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "在雪地里坐足两年的人，不会被你这点杀气带乱半分。"),
                        new Skill("化神一问 · 天地同频", "ULT", 2.5, 400, Vfx.ULT_NOVA,
                                "那道坎我只推开过一条缝 —— 但缝里的光照出来，够你受用一世。"))));

        list.add(new Cultivator(126L, "圭灵", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖兽）",
                "昆吾山 · 玄岩龟", "昆吾山守山妖王 · 十级妖兽",
                "玄岩龟妖体（不修功法，只修省、慢、扛）", Style.TURTLE,
                List.of(Trait.TURTLE, Trait.GUARD, Trait.BODY),
                Basis.CANON, "本项目 bio 明写「十级妖兽」；人界十级约当元婴后期顶峰、无限近化神初期",
                "防御不是怯。是知道自己要守什么。",
                List.of(
                        new Skill("八百年头槌", "ATTACK", 1.9, 200, Vfx.QI_PALM,
                                "不会飞，不擅追，我通身只一个字：扛。扛够了，就还你一下。"),
                        new Skill("缩进壳里", "DEFENSE", 1.0, 110, Vfx.TURTLE_SHELL,
                                "能不见就不见。有人上山，我把头一缩——这山，我守得比你久。"),
                        new Skill("能不动就不动", "HEAL", 0.7, 0, Vfx.HEAL_GLOW,
                                "灵气在壳里自己周转。我动一次要养三年，你，等得起么？"),
                        new Skill("守山八百年 · 同阶全力一击不动", "ULT", 2.25, 360, Vfx.TURTLE_SHELL,
                                "这口壳是八百年长成的。你那点重手，落在我身上最不值钱。"))));

        list.add(new Cultivator(107L, "大衍神君", Realm.NASCENT_SOUL, SubStage.LATE, "后期（残魂）",
                "上古神念一脉 · 只余残魂", "上古大衍氏 · 肉身没于古魔之乱",
                "大衍诀（定识 · 分识 · 千识齐动）", Style.MIND,
                List.of(Trait.MIND, Trait.RESIDUAL, Trait.GLASS),
                Basis.CANON, "原著为上古元婴后期修士，人界篇只以残魂传承现世；本榜按残魂状态取战力",
                "神念是刀，能开法诀破绽，也能剔去自己的神魂。",
                List.of(
                        new Skill("分识 · 二念并攻", "ATTACK", 1.85, 180, Vfx.MIND_WAVE,
                                "一念化二，两枝同干，各算你一处破绽——你顾得住哪一头？"),
                        new Skill("定识 · 按住念头", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "十息都定不住就妄想分识，那是自寻死路。我这一念，按得住你的念头。"),
                        new Skill("摄识锁神", "CONTROL", 0.95, 170, Vfx.MIND_WAVE,
                                "多探一寸，我多染一分脏——可你的破绽，我探定了。"),
                        new Skill("千识齐动", "ULT", 2.45, 420, Vfx.ULT_NOVA,
                                "一人同御数十件法器，斗法里同时算死你三处破绽。"))));

        list.add(new Cultivator(114L, "六道极圣", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "魔道六宗 · 底牌从不嫌多", "乱星海魔道",
                "六道轮回诀（六道养六气，一气不得独大）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.SWORD),
                Basis.INFERRED, "原著魔道巨枭级存在，人界篇元婴后期；bio 未点明小层，本榜取后期",
                "境界越高，越不是靠天赋，是靠手里还有多少底牌。",
                List.of(
                        new Skill("修罗道 · 见战", "ATTACK", 1.92, 200, Vfx.SLASH,
                                "六道我只放修罗一道出来——见你，便战。"),
                        new Skill("饿鬼道 · 吞灵", "CONTROL", 0.85, 170, Vfx.POISON_MIST,
                                "不伤你身，先吞你灵力。等你空了，再跟你谈生死。"),
                        new Skill("人道 · 权宜", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "六气并立，攻守进退各有其主。你想寻我破绽？我没有单面的破绽。"),
                        new Skill("六道并立 · 天、人、修罗、畜生、饿鬼、地狱", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "六门同开，一身六气各使其力——这一日，我底牌尽出。"))));

        list.add(new Cultivator(117L, "凌啸风", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天星双圣之一 · 元磁神光", "乱星海天星双圣",
                "元磁神光（两极分修，合之则成）", Style.MAGNET,
                List.of(Trait.MAGNET, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "本项目 1017 明写「元婴后期二人联手可短暂抗住化神」",
                "不与你斗法，先收你兵。",
                List.of(
                        new Skill("元磁神光 · 吸你法器", "CONTROL", 0.95, 190, Vfx.MAGNET_BEAM,
                                "方圆百丈，一切金铁听调不听宣——包括你手里那件。"),
                        new Skill("两极同频", "ATTACK", 1.82, 190, Vfx.MAGNET_BEAM,
                                "一阴一阳同时起、同时收。三百年，我练的就是这一口气。"),
                        new Skill("星宫旧甲", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "一副旧甲不名贵，可它贴身三百年，比你的新宝可靠。"),
                        new Skill("百丈金铁 · 尽听我调", "ULT", 2.5, 410, Vfx.ULT_NOVA,
                                "你祭出去的那些东西，此刻在空中——调转了方向。"))));

        list.add(new Cultivator(113L, "极阴祖师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "玄阴一脉 · 在你看不见的地方动手", "乱星海极阴岛",
                "极阴真解（摄魂 · 擒婴 · 玄阴护体）", Style.YIN,
                List.of(Trait.MIND, Trait.GLASS, Trait.TREASURE),
                Basis.INFERRED, "bio 只写「乱星海元婴」，虚天殿一役的规格按元婴后期取值",
                "你能锁住别人的元婴，锁不住自己的因果。",
                List.of(
                        new Skill("摄魂 · 入你识海", "CONTROL", 1.0, 200, Vfx.MIND_WAVE,
                                "入你识海：轻则乱你记忆，重则——替你做个主。"),
                        new Skill("擒婴", "ATTACK", 2.05, 240, Vfx.BLOOD_SHADOW,
                                "擒婴不杀你，只把你变成一具还会走路的空壳。"),
                        new Skill("玄阴护体", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "寒气结膜，你的法术近身先衰三分——剩下的，打不动我。"),
                        new Skill("至阴锁婴 · 一饮一啄", "ULT", 2.45, 420, Vfx.ULT_NOVA,
                                "我不喜硬拼，宁愿多等三日。等——就是你露破绽的那一刻。"))));

        list.add(new Cultivator(129L, "万三姑", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "乱星海正道第一人", "乱星海正道万氏",
                "太素元真诀（真元厚而不烈，最善守最不善杀）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "bio 明写「正道第一人」，人界篇正道翘楚按元婴后期取值",
                "神通是你能打过多强的人。正道是你能对自己人下多重的手。",
                List.of(
                        new Skill("太素守一", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "厚而不烈，这一手我练了千年——专门用来不杀人，也专门用来扛住你。"),
                        new Skill("定是非 · 折你兵锋", "CONTROL", 0.9, 170, Vfx.QI_PALM,
                                "能讲例的不讲情，能讲情的不讲力。这三序，你今日一样都讨不走。"),
                        new Skill("绵长一击", "ATTACK", 1.6, 170, Vfx.QI_PALM,
                                "我这一生亲手杀人极少。不是不能——是不肯随便。今日，你值得我认真一次。"),
                        new Skill("对自己人下重手", "ULT", 2.65, 420, Vfx.ULT_NOVA,
                                "百余年忍着的那一下，今日——为你放开。"))));

        list.add(new Cultivator(116L, "金魁", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "星宫大长老 · 执律令镇海疆", "乱星海星宫",
                "星宫镇府功 + 镇海大阵（半边阵眼常年轮值）", Style.ARRAY,
                List.of(Trait.GUARD, Trait.NO_BURST, Trait.TREASURE),
                Basis.CANON, "bio 明写「元婴中期巅峰」",
                "阵这东西，十年不出事就没人记得它有用。",
                List.of(
                        new Skill("星宫律令 · 禁你一手", "CONTROL", 0.95, 180, Vfx.ARRAY_PRESS,
                                "星宫的律令写在纸上，不写在心里——所以禁你这一手，不讲情面。"),
                        new Skill("镇府厚土", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "我与人对掌从没吃过亏。只缺一锤定音——那一锤，今日给你补上。"),
                        new Skill("阵眼调用 · 三十六岛同压", "ATTACK", 1.85, 210, Vfx.ARRAY_PRESS,
                                "我的实权不在斗法，在阵。三十六岛的力，此刻一并压向你。"),
                        new Skill("镇海大阵 · 百年无案", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "这座阵我守了百余年，为的就是今天——能名正言顺地用它一次。"))));

        list.add(new Cultivator(111L, "蛮胡子", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "乱星海第一防御魔功 · 体修", "乱星海魔道散修",
                "托塔天魔功（扛 · 忍 · 反）", Style.BODY,
                List.of(Trait.BODY, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "bio 明写「乱星海元婴中期，托塔天魔功，体修」",
                "我这一身不是无敌，是耐打。",
                List.of(
                        new Skill("扛 · 硬接你全力一击", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "我的真元不走经脉走筋骨，一层层压进皮肉——你打的就是这身铁。"),
                        new Skill("忍 · 空档反手", "ATTACK", 1.72, 170, Vfx.QI_PALM,
                                "忍得住一击之后的空档，才轮得到我出手。你，忍住了么？"),
                        new Skill("反 · 天魔还力", "ATTACK", 1.98, 210, Vfx.QI_PALM,
                                "你刚才打我的那一分力，我原封——还给你。"),
                        new Skill("托塔 · 一肩扛下", "ULT", 2.3, 390, Vfx.ULT_NOVA,
                                "压到最后一息，周身如浇铁汁。这一肩，天塌下来我扛着。"))));

        list.add(new Cultivator(119L, "云露老魔", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "魔道老怪 · 雾里藏刀", "魔道散修 · 有一女不认他",
                "云露魔光（先迷神，后取物）", Style.ILLUSION,
                List.of(Trait.MIND, Trait.GLASS, Trait.ESCAPE),
                Basis.INFERRED, "bio 未点小层，魔道老怪规格取元婴中期",
                "魔道路快，是因为它把代价往后推。推到什么时候、推给谁，你事先从来不知道。",
                List.of(
                        new Skill("魔光迷神 · 三息", "CONTROL", 1.05, 200, Vfx.POISON_MIST,
                                "让你在最想要的幻象里待三息。三息之后，你身上还有什么，就不是你的了。"),
                        new Skill("雾里藏刀", "ATTACK", 1.95, 190, Vfx.SLASH,
                                "魔光不伤人，先迷神。刀——是在你分神那一刹递进来的。"),
                        new Skill("摸腕自证", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "用多了我也分不清哪重是真。可今夜我摸着手腕——这一刀，是真的。"),
                        new Skill("一重幻界 · 你最想要的那一重", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "我不打你。我送你进你最想留下的那一重——然后，从里面拆了你。"))));

        list.add(new Cultivator(115L, "万天明", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "正道万氏 · 鉴宝三十年", "乱星海万氏",
                "照灵诀（以神识贴物，看灵气走向匀不匀）", Style.MIND,
                List.of(Trait.MIND, Trait.TREASURE, Trait.GLASS),
                Basis.INFERRED, "虚天殿一役的世家子弟规格，取元婴初期",
                "我算对了物，没算人。",
                List.of(
                        new Skill("照灵诀 · 破妄", "CONTROL", 0.85, 160, Vfx.MIND_WAVE,
                                "真器内敛如一，伪器外润内乱。看物如此——看你，也如此。"),
                        new Skill("补天丹 · 含一粒", "HEAL", 0.95, 0, Vfx.HEAL_GLOW,
                                "这颗丹补我不成，可它到底是颗好丹。今日含一粒，气力照涨。"),
                        new Skill("三十年眼力 · 点你三处破绽", "ATTACK", 1.95, 210, Vfx.SLASH,
                                "我不一定打得赢你。可你下一剑从哪儿来——我先知道。"),
                        new Skill("万氏家学 · 一件不留", "ULT", 2.15, 380, Vfx.ULT_NOVA,
                                "看物先看其主。今日我把你这一身法宝的来历，一件件说破给你听。"))));

        list.add(new Cultivator(130L, "红拂", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "红拂三剑 · 为走不为胜", "天南剑修 · 董萱儿之母",
                "红拂三剑（一斩禁制，二断神识，三不为胜）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "bio 与 1030 只写剑不写阶，能以剑破阵带人走，取元婴初期",
                "能走，比能胜值钱。",
                List.of(
                        new Skill("一剑斩禁制", "CONTROL", 0.95, 170, Vfx.SLASH,
                                "我不与他们对掌。我只斩他们手里的禁制旗——旗一碎，谁还拦我？"),
                        new Skill("二剑断神识相接", "CONTROL", 1.0, 190, Vfx.SLASH,
                                "你和你法宝之间那一根线，我先断给你看。断了，你就是空手。"),
                        new Skill("三剑不为胜 · 为走", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "这一式年轻时我放最后。如今放最前——能走，比能胜值钱。"),
                        new Skill("不回头", "ULT", 2.45, 390, Vfx.SWORD_RAIN,
                                "孩子哭，我走我的。回头一次，就再也走不了。这一剑，不回头。"))));

        list.add(new Cultivator(109L, "紫灵", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "乱星海妙音门 · 行走多年", "乱星海妙音门",
                "音律功法（潮信为号，惑心乱神）", Style.SOUND,
                List.of(Trait.SUMMON, Trait.ESCAPE, Trait.PERSIST),
                Basis.CANON, "原著人界篇末凭坠魔谷灵烛果入元婴初期，境界低于南宫婉",
                "这里的人笑起来都很客气。你要看的不是他笑得暖不暖，是他把手放在哪里。",
                List.of(
                        new Skill("妙音惑心", "CONTROL", 0.95, 170, Vfx.SOUND_RING,
                                "我一句话拐三道弯。听着像夸你——其实是在算你值多少。"),
                        new Skill("潮信引退", "MOBILITY", 0.0, 120, Vfx.SOUND_RING,
                                "风色一变、海鸟低飞、水下泛凉——占了两条，我转身就走。你拦不住一个会走的人。"),
                        new Skill("海兽令", "ATTACK", 1.78, 200, Vfx.SUMMON,
                                "每逢潮信，深海兽群上涌。看着是劫——在我眼里，那都是钱。"),
                        new Skill("乱星海 · 万兽朝音", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "这片海不认辈分，只认两件事：背后的势力，手上的货。今日两样，我都要。"))));

        list.add(new Cultivator(128L, "白凤仙子", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗白凤峰峰主 · 三式剑", "天南落云宗",
                "白凤三剑（照 · 断 · 归）", Style.SWORD,
                List.of(Trait.SWORD, Trait.PERSIST),
                Basis.CANON, "bio 明写「元婴初期」，1028 自述三式剑",
                "剑不是用来打赢的，是用来叫你不必多说话的。",
                List.of(
                        new Skill("照 · 剑光滞身", "ATTACK", 1.5, 150, Vfx.SLASH,
                                "剑光亮起那一处，你的身形先滞一分——一分，就够了。"),
                        new Skill("断 · 破护身灵光", "ATTACK", 2.05, 210, Vfx.SLASH,
                                "一气斩下，不取半分巧，专破你护身灵光。"),
                        new Skill("归 · 收剑即止", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "这一式最难——它要你在能杀的时候，停手。"),
                        new Skill("白凤三式齐出", "ULT", 2.5, 400, Vfx.SWORD_RAIN,
                                "十七种剑招我全丢了，只留这三式磨一百年。今日，够割你一个虚名。"))));

        // ---------- 结丹 ----------
        list.add(new Cultivator(124L, "慕沛灵", Realm.CORE_FORMING, SubStage.LATE, "后期（冲元婴未果）",
                "落云宗 · 静明诀", "天南落云宗 · 药园执掌",
                "静明诀（专养神识清明）", Style.MIND,
                List.of(Trait.MIND, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "原著人界篇最高结丹后期，后冲击元婴陨落",
                "我修不成大道，就把家族里那几个孩子教出来。人不能只靠一段关系活着。",
                List.of(
                        new Skill("静明 · 不失方寸", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "答应过的事、欠下的账、说过的话——我一笔不漏。你赖不掉。"),
                        new Skill("记账分明 · 识破", "CONTROL", 0.85, 150, Vfx.MIND_WAVE,
                                "神识清明到极处，就是你还没开口——我已经听见了你说谎。"),
                        new Skill("颠凤培元功 · 短时提气", "ATTACK", 1.9, 200, Vfx.QI_PALM,
                                "借来的力也是力。此刻砸在你身上——至于还，是我之后的事。"),
                        new Skill("分寸二字", "ULT", 2.2, 360, Vfx.ULT_NOVA,
                                "什么时候退一步，什么时候一步都不退。这一手——我练得比功法还熟。"))));

        list.add(new Cultivator(108L, "银月", Realm.CORE_FORMING, SubStage.LATE, "后期（器灵）",
                "银月剑 · 看过的修士比看过的剑多", "剑中器灵 · 后为九尾一脉旧识",
                "祭炼三段（认主 · 通灵 · 化形）", Style.SWORD,
                List.of(Trait.SPIRIT, Trait.SWORD, Trait.MIND),
                Basis.BOARD, "原著器灵无境界，本榜按「离剑即衰」的规格折算到结丹后期",
                "法宝是修士的另一条命。你把命交出去之前，先想清楚自己配不配拿回来。",
                List.of(
                        new Skill("剑鸣 · 滞你一分", "ATTACK", 1.55, 140, Vfx.SLASH,
                                "你心虚那一瞬，剑锋慢了半分——那半分，就是你的生死之别。"),
                        new Skill("认主 · 护主", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "你拿它护身，它便稳；你拿它逞凶，它便躁。器有主——更有脾气。"),
                        new Skill("化形", "ATTACK", 1.95, 210, Vfx.SWORD_RAIN,
                                "到这一步，是法宝开始挑主人。今日——它挑了我。"),
                        new Skill("一剑不留情", "ULT", 2.35, 380, Vfx.ULT_NOVA,
                                "器灵出剑，从不替主人省力气。因为我自己——没有第二条命。"))));

        list.add(new Cultivator(127L, "柳乐儿", Realm.CORE_FORMING, SubStage.MID, "中期（九尾三尾）",
                "灵寰界 · 九尾一脉如今归她管", "九尾遗族 · 灵寰界",
                "凝月诀（取月华入体，一尾一境界）", Style.BEAST,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.GLASS),
                Basis.BOARD, "原著人界篇尚在成长期，本榜按「三尾映月」折算结丹中期",
                "活得久不算本事，活得像自己才算。",
                List.of(
                        new Skill("月华一层尾", "HEAL", 0.8, 0, Vfx.MOON_GLOW,
                                "尾越长，天劫越重。所以这一尾我省着用——今日，不省了。"),
                        new Skill("族中幼狐齐啸", "ATTACK", 1.72, 190, Vfx.SUMMON,
                                "族里没几个孩子，老的全是伤。可这一声啸，它们肯替我拼命。"),
                        new Skill("不许吃人 · 族训", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "「不许吃人」写在祠堂门口。谁坏这条——先问过我这几条尾。"),
                        new Skill("三尾映月", "ULT", 2.15, 360, Vfx.MOON_GLOW,
                                "九尾圆满可敌化神。我如今三尾——今日先叫你知道，这三尾的分量。"))));

        list.add(new Cultivator(105L, "董萱儿", Realm.CORE_FORMING, SubStage.EARLY, "初期",
                "黄枫谷灵植夫 · 与药草打交道比与人多", "天南黄枫谷药园",
                "青木吐纳诀（把木灵气渡进根须）", Style.WOOD,
                List.of(Trait.SUMMON, Trait.ALCHEMY, Trait.GLASS),
                Basis.INFERRED, "自述只提灵植六年与木属性慢功，按结丹初期取值",
                "山里的药是养出来的，不是抢出来的。",
                List.of(
                        new Skill("青木缠藤", "CONTROL", 0.95, 160, Vfx.POISON_MIST,
                                "催芽、接骨、救一株半枯老参，靠的都是这一口气。缠住你——也一样够。"),
                        new Skill("灵草毒雾", "ATTACK", 1.62, 170, Vfx.POISON_MIST,
                                "药园一天转三遍，少一遍它就给你脸色。我这毒雾——也一样不认人。"),
                        new Skill("三年老参 · 一掷", "DEFENSE", 1.0, 120, Vfx.HEAL_GLOW,
                                "一株百年青芝，育苗养土看火，我熬了三年。今日拿它——给你陪葬。"),
                        new Skill("潮气没弄对 · 全废", "ULT", 2.1, 350, Vfx.ULT_NOVA,
                                "差一次潮气就全废。这一炉我守了三十年——今日先拿你试药。"))));

        list.add(new Cultivator(123L, "菡云芝", Realm.CORE_FORMING, SubStage.EARLY, "初期",
                "灵兽山 → 御灵宗 · 御兽不驭兽", "天南灵兽山一脉",
                "百灵谐鸣诀（以神识与兽念相接，不压不锁只讲合）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.ESCAPE, Trait.ALCHEMY),
                Basis.CANON, "1023 自述「结丹我成了。慢，但成了」",
                "御兽这门功夫教给我的，是怎么叫一个比你活得久的东西愿意陪你。",
                List.of(
                        new Skill("二阶灵禽 · 替我看方向", "CONTROL", 0.8, 140, Vfx.SUMMON,
                                "这只灵禽跟了我一百多年。它不替我拼命——它替我，看你的死路。"),
                        new Skill("不与兽硬拼，先绕", "MOBILITY", 0.0, 110, Vfx.BLOOD_SHADOW,
                                "我的兽替我找路，不替我拼命。可真要拼命时——它们也敢。"),
                        new Skill("百灵齐鸣", "ATTACK", 1.75, 185, Vfx.SUMMON,
                                "它觉得与我同战划算，所以它来了。你——不在它划算的那一边。"),
                        new Skill("血色试炼求生录", "ULT", 2.1, 350, Vfx.ULT_NOVA,
                                "血色试炼头一条：天黑前必停。夜里那片林子——不是给人留的。"))));

        // ---------- 筑基 ----------
        list.add(new Cultivator(106L, "李化元", Realm.FOUNDATION, SubStage.LATE, "后期",
                "黄枫谷执事长老 · 管过资源分配", "天南黄枫谷",
                "宗门功法（不擅杀伐，擅算账）", Style.QI,
                List.of(Trait.GUARD, Trait.ALCHEMY, Trait.NO_BURST),
                Basis.CANON, "bio 明写「筑基期，管过宗门资源分配」",
                "筑基丹从来不是商品，是分配权。",
                List.of(
                        new Skill("四十枚灵石 · 布一个局", "DEFENSE", 1.0, 120, Vfx.ARRAY_PRESS,
                                "弟子每年灵石四十枚、引气丹六瓶——我今天全摆桌上。这笔账，你也得算。"),
                        new Skill("引气丹 · 当场提气", "HEAL", 0.75, 0, Vfx.HEAL_GLOW,
                                "丹药从来不是商品，是分配权。现在——它在你我之间易手。"),
                        new Skill("黄枫谷剑光", "ATTACK", 1.55, 150, Vfx.SLASH,
                                "执事长老也拔剑。只是我拔得慢——慢，不代表不割人。"),
                        new Skill("宗门资源 · 压你一年份额", "ULT", 2.05, 340, Vfx.ARRAY_PRESS,
                                "我年轻时也不服这规矩。如今执这杆笔——压你这份份额，我压得住。"))));

        list.add(new Cultivator(112L, "曲魂", Realm.FOUNDATION, SubStage.MID, "中期（炼尸之身）",
                "本名张铁 · 不人不尸", "七玄门 → 身外化身",
                "象甲功（刀枪不入）+ 血炼神光（以血养气）", Style.CORPSE,
                List.of(Trait.BODY, Trait.GLASS, Trait.PERSIST),
                Basis.BOARD, "原著张铁以炼尸之身长随韩立，无正式境界，本榜按战力折算筑基中期",
                "法器再利，也换不回一副能尝出肉干咸淡的身子。",
                List.of(
                        new Skill("象甲硬扛", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "凡人练它打赢十个凡人。我练它——为多挨修士那一下。够我近身了。"),
                        new Skill("血炼神光", "ATTACK", 1.88, 190, Vfx.BLOOD_SHADOW,
                                "血炼出手带煞，同阶不敢硬接。每用一次我离人远一步——可你已经来不及了。"),
                        new Skill("不知痛", "HEAL", 0.5, 0, Vfx.QI_PALM,
                                "我记得「高兴」的定义，记不起感觉。痛也一样——所以我不躲。"),
                        new Skill("身外化身 · 不留余地", "ULT", 2.2, 370, Vfx.ULT_NOVA,
                                "这具身子早就是件法器了。用坏——就用坏吧。反正它不会喊疼。"))));

        list.add(new Cultivator(125L, "曹梦容", Realm.FOUNDATION, SubStage.LATE, "后期",
                "大晋坊市丹药铺 · 真假一眼能分", "大晋坊市",
                "辨药要录（真丹香气沉，假丹香浮）", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.MIND, Trait.GLASS),
                Basis.BOARD, "原著无明确境界记载，按坊市管事水准推定筑基后期",
                "修士买丹，多半不是买药，是买一个「我还能再进一步」的念头。",
                List.of(
                        new Skill("一眼辨破 · 你那颗是假的", "CONTROL", 0.9, 150, Vfx.MIND_WAVE,
                                "药性偏激那种最骗急着突破的人。你身上那瓶——我隔着三丈就闻出来了。"),
                        new Skill("猛药催丹相", "ATTACK", 1.9, 200, Vfx.POISON_MIST,
                                "猛药下去灵力当场大涨，半年之后经脉自脆。今日——我先还给你。"),
                        new Skill("铺子规矩 · 卖错认赔", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "卖错当日赔，不拖。这条规矩我在铺子里立了三十年，今天照样灵。"),
                        new Skill("一炉三假", "ULT", 2.05, 340, Vfx.POISON_MIST,
                                "以次充好、药性偏激、借名——三种假丹我见尽了。今日一并，摆给你尝。"))));

        list.add(new Cultivator(121L, "陈巧倩", Realm.FOUNDATION, SubStage.EARLY, "初期",
                "黄枫谷弟子 · 翠云诀", "天南黄枫谷",
                "翠云诀（木属性慢功，养人不出重手）", Style.QI,
                List.of(Trait.ALCHEMY, Trait.PERSIST, Trait.NO_BURST),
                Basis.CANON, "1021 自述「筑基初期」；原著终生未结丹",
                "被人误会时不哭，去查档。哭一次，旁人便觉得你理亏。",
                List.of(
                        new Skill("翠云护体", "DEFENSE", 1.0, 100, Vfx.SHIELD,
                                "我受了伤总比别人好得快。这是我这一门唯一的长处——也够耗死你了。"),
                        new Skill("慢火长熬", "HEAL", 0.7, 0, Vfx.HEAL_GLOW,
                                "灵力不烈，胜在能耗。同阶斗法——我多半是把对手拖没耐性的那个。"),
                        new Skill("人情当场还清", "ATTACK", 1.5, 140, Vfx.QI_PALM,
                                "灵石、丹药、人情——我当日还清。今日这一掌，也一样当场还你。"),
                        new Skill("查档", "ULT", 1.95, 320, Vfx.ULT_NOVA,
                                "把出入、丹药、值夜的日子全摆出来。我不哭——我查档，一次就叫你哑口。"))));

        // ---------- 炼气 ----------
        list.add(new Cultivator(103L, "墨大夫", Realm.QI_REFINING, SubStage.LATE, "后期（卡二十年）",
                "七玄门太医 · 长春功传人", "越国七玄门",
                "长春功（养气延年）+ 夺舍三忌", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "1003 自述「卡在炼气后期二十年，寿数将尽」",
                "功法无罪，用功法的人心术不正，才是祸。",
                List.of(
                        new Skill("长春功 · 养气一指", "ATTACK", 1.35, 90, Vfx.QI_PALM,
                                "长春功进境慢得看不出。可它压得住伤病、拖得住大限——也拖得动你。"),
                        new Skill("行医四十年的银针", "ATTACK", 1.62, 120, Vfx.POISON_MIST,
                                "凡俗人的病我治了四十年，修士的伤我也见过。这针——扎谁都一样见血。"),
                        new Skill("夺舍三忌 · 护住本神", "DEFENSE", 1.0, 110, Vfx.SHIELD,
                                "夺舍三忌：根器不合、因果不清、心气不平。我守了三忌——你犯的是第几条？"),
                        new Skill("寿数将尽 · 最后一炉", "ULT", 1.85, 260, Vfx.POISON_MIST,
                                "老朽这一生就剩这一炉了。它不一定打得赢你——但一定能，毒到你。"))));

        // ---------- 凡人 · 不在仙途之内 ----------
        list.add(new Cultivator(104L, "厉飞雨", Realm.MORTAL, SubStage.EARLY, "凡俗武徒",
                "七玄门凡俗武徒 · 不懂法术但活得明白", "越国七玄门",
                "八极拳（崩 · 撼 · 突 · 击，一寸之内发力）", Style.MORTAL,
                List.of(Trait.BODY, Trait.MORALE),
                Basis.CANON, "bio 明写「凡俗武徒，没有灵根」，原著终生为凡人",
                "能活着走到最后的，都不是最能打的。",
                List.of(
                        new Skill("八极拳 · 崩拳", "ATTACK", 1.0, 40, Vfx.MORTAL_PUNCH,
                                "八极崩拳，一寸之内发力。修士一掌就被灵光弹开——可我这一拳，打的是你肉胎凡身。"),
                        new Skill("跑得快、扛得住", "MOBILITY", 0.0, 30, Vfx.MORTAL_PUNCH,
                                "修士靠遁光，我靠这两条腿。跑得快、扛得住——凡人活命，全靠它。"),
                        new Skill("凡人的骨气", "DEFENSE", 1.0, 20, Vfx.SHIELD,
                                "受了伤我能自己走回去。这一条我练了十年——骨气这个东西，不靠灵根。"))));

        list.add(new Cultivator(122L, "墨彩环", Realm.MORTAL, SubStage.EARLY, "凡俗医道",
                "墨家三女 · 没有灵根", "越国七玄门 · 墨家",
                "医道（凡俗人的病我治，修士的伤我也见过）", Style.MORTAL,
                List.of(Trait.ALCHEMY, Trait.MORALE),
                Basis.CANON, "bio 与 1022 明写「没有灵根」；硬引气轻则伤身重则瘫床",
                "把心思放在「叫他也看我一眼」上，是把自己活成了一件事。我不干。",
                List.of(
                        new Skill("下针 · 先吊住一口气", "HEAL", 0.9, 30, Vfx.HEAL_GLOW,
                                "爹爹的手艺如今在我手里。这一针救过的人——比仙师记得还清楚。"),
                        new Skill("认药 · 辨你丹药", "CONTROL", 0.7, 25, Vfx.MIND_WAVE,
                                "凝气散凡人吃了七窍流血。你身上这瓶——我给你收了。"),
                        new Skill("把家撑住", "DEFENSE", 1.0, 20, Vfx.SHIELD,
                                "门里门外总得有人管。修士嫌这些不算本事——可撑着这个家的，是我。"))));

        list.add(new Cultivator(120L, "孙二狗", Realm.MORTAL, SubStage.EARLY, "凡俗帮主",
                "四平帮帮主 · 替仙师看着一具炼尸", "越国凡俗江湖",
                "不会法术，斗大的字认得一百多个", Style.MORTAL,
                List.of(Trait.MORALE, Trait.SUMMON),
                Basis.CANON, "bio 与 1020 明写「凡俗之人，不会法术」",
                "别拿他的东西，别问，账要记清。",
                List.of(
                        new Skill("账本一挡", "DEFENSE", 1.0, 15, Vfx.SHIELD,
                                "仙师赏的灵石我一枚没动，收支全记在本上。你什么时候来查——都对得上。"),
                        new Skill("院里那位 · 叫他一声", "ATTACK", 1.3, 40, Vfx.SUMMON,
                                "院里那位白天不动，夜里自己站门口。我叫一声——他不听我的，可你，也别想动他。"),
                        new Skill("认个错，拱手", "MOBILITY", 0.0, 10, Vfx.MORTAL_PUNCH,
                                "打不过就认，拱手。凡人在仙师脚边活这么多年——靠的，从来不是能打的。"))));

        // ---------- 天南三大修士：元婴战力的天花板 ----------
        list.add(new Cultivator(131L, "魏无涯", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "天南三大修士之首 · 毒道通神", "天南九国盟化意门太上长老",
                "万毒混元功（十绝毒 · 腹尸之毒 · 绿毒化蛇）", Style.POISON,
                List.of(Trait.ALCHEMY, Trait.SUMMON, Trait.TREASURE),
                Basis.CANON, "原著明载：化意门太上长老，天南三大修士之一，入元婴后期最早，毒道冠绝天南",
                "毒是我用的，名声是别人怕的。我这一辈子没在背后算计过谁 —— 毒摆在你面前，你也要喝。",
                List.of(
                        new Skill("十绝毒 · 尸之毒", "ATTACK", 1.95, 215, Vfx.POISON_MIST,
                                "十绝里这味最不值钱——只因你只要沾上，就够同阶喝一壶。"),
                        new Skill("腹尸之毒 · 从中烂起", "CONTROL", 1.0, 200, Vfx.POISON_MIST,
                                "腹尸之毒不打身，打你吞下去那口气。等你觉出不对——已经晚了半息。"),
                        new Skill("数亩绿云 · 化蛇万条", "ATTACK", 1.82, 205, Vfx.SUMMON,
                                "数亩绿云铺开，云中自生出蛇来。这些蛇不是我的兽——是我的毒。"),
                        new Skill("万毒混元身", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "修毒先修己，我早把自己泡成药。你打我一分，自己先中三分毒。"),
                        new Skill("破魂蛇 · 一出魂不留", "ULT", 2.58, 428, Vfx.ULT_NOVA,
                                "破魂蛇养六十年，只放这一次。放出去的不是毒——是一条有名字的蛇。"))));

        list.add(new Cultivator(132L, "至阳上人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天南正道第一人 · 太真门太上长老", "天南正道盟太真门",
                "静心诀 + 至阳真火（至阳至刚，专破魔器）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.TREASURE),
                Basis.CANON, "原著明载：太真门太上长老，正道盟领袖，三人中修为稍弱，曾负于天恨老怪",
                "三人里我最弱，这话我认。可我守的那道门、布的那座阵，也是三人里最结实的。",
                List.of(
                        new Skill("至阳真火 · 破魔", "ATTACK", 1.86, 200, Vfx.GOLD_BEAM,
                                "我这口火不烧人，专烧你那些阴寒法器。魔道怕的不是我——是我这一口火。"),
                        new Skill("静心诀 · 一寸不乱", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "魔道擅先乱人心。我这门功从二十岁起只练一件事：不上头。你乱得了我？"),
                        new Skill("太真门阵 · 借地势", "CONTROL", 0.92, 180, Vfx.ARRAY_PRESS,
                                "我不擅斗久，所以起手先借阵。阵一成——才轮得到跟你讲道理。"),
                        new Skill("焚天阵 · 金光万道", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "焚天阵金光万道那一刻，天南——没人敢踏进阵来。"))));

        list.add(new Cultivator(133L, "合欢老魔", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "天南魔道第一人 · 合欢宗大长老", "天南魔道合欢宗",
                "合欢魔功（阴姹女 · 阳婴儿，采补而不伤根）", Style.DEMON,
                List.of(Trait.MIND, Trait.TREASURE, Trait.PERSIST),
                Basis.CANON, "原著明载：合欢宗大长老，天南第一魔修，与房宗主切磋不分胜负",
                "正道讲规矩，我们讲效果。这话我说得不害臊 —— 因为我活到今天，靠的就是不害臊。",
                List.of(
                        new Skill("阴姹女 · 采人元气", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "采补最难的不是吸——是吸完还要叫你的经脉，看不出被吸过。"),
                        new Skill("阳婴儿 · 婴前杀人", "ATTACK", 1.98, 215, Vfx.BLOOD_SHADOW,
                                "你元婴未成前我这手白用；一旦炼成——才值我走这一趟。"),
                        new Skill("魔障 · 先讲人情", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "我从不在第一击上跟你拼。前几句客气——客气里已经把你算完了。"),
                        new Skill("喜合擅暗 · 补上那一下", "ATTACK", 2.12, 230, Vfx.SLASH,
                                "天南骂我爱偷袭，骂对了。正面打不赢的人——我凭什么要正面打？"),
                        new Skill("合欢魔功 · 一气化三", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "三具化身同使一门功。你杀得了我一次——剩下两次，我还要跟你讲人情。"))));

        // ---------- 天南元中一线：原著有名有姓的老怪 ----------
        list.add(new Cultivator(134L, "天恨老怪", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "三大修士之下第一人 · 曾轻伤至阳上人", "天南魔道散修",
                "天恨魔光（先怨后杀，光不蚀肉先蚀神）", Style.ILLUSION,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.GLASS),
                Basis.CANON, "原著明载：天南元中之下第一，曾将至阳上人打成轻伤",
                "我打伤过三个比我名头大的人。此事天南知道的人不多，所以我现在还活着。",
                List.of(
                        new Skill("天恨魔光 · 三息", "CONTROL", 1.05, 195, Vfx.POISON_MIST,
                                "天恨魔光不伤人，先叫你想起一生最不甘那件事。三息——够我走，也够我出剑。"),
                        new Skill("怨念一击", "ATTACK", 1.92, 200, Vfx.SLASH,
                                "我这门功的力气全在气头上。谁叫我冷静——谁就是在削我。"),
                        new Skill("不能力敌就走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "散修的老命靠腿挣。名头越大，越该带两条好腿——你也别想留下我。"),
                        new Skill("一恨到底 · 光尽人废", "ULT", 2.42, 408, Vfx.ULT_NOVA,
                                "我这一恨记了三百年。今日——原样还你。"))));

        list.add(new Cultivator(135L, "龙晗", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "鸾鸣宗太上长老 · 天道盟领军", "天南鸾鸣宗",
                "鸾鸣九响（以音为号，联阵攻坚）", Style.SOUND,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "原著明载其为天道盟领军、鸾鸣宗太上长老，小层未点，按与凤冰并列的元中巅峰取值",
                "领军不是最能打的那个，是最后退的那个。",
                List.of(
                        new Skill("鸾鸣三响 · 乱你法诀", "CONTROL", 1.0, 185, Vfx.SOUND_RING,
                                "鸾鸣九响：一声起，二声断你指挥，三声你身后那座阵——自己就乱。"),
                        new Skill("联军齐攻 · 不独斗", "ATTACK", 1.86, 205, Vfx.SUMMON,
                                "天道盟的仗从来不是两个人打。我一声令下——七位元婴各安其位。"),
                        new Skill("为后队断声", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "为后队断声这一手不杀人——专把追兵的声音，死死压住。"),
                        new Skill("凤龙合鸣 · 媲美元后", "ULT", 2.48, 418, Vfx.ULT_NOVA,
                                "与道侣同使的一击。两个中期——打出一个后期来。盟里最怕的就是这声合鸣。"))));

        list.add(new Cultivator(136L, "凤冰", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "鸾鸣宗 · 龙晗道侣 · 独力牵住一位神师", "天南鸾鸣宗",
                "寒凰真气（不冻人身，先冻灵气之间那道牵连）", Style.ICE,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其与龙晗并肩、可独力牵制大晋慕兰第四神师；小层按元中巅峰取",
                "盟里的事他出头，海上的账我算。两件事本来就得有人分开做。",
                List.of(
                        new Skill("寒凰封冻", "ATTACK", 1.82, 195, Vfx.ICE_FROST,
                                "寒凰冻的不是你身，是你法诀里那处转弯。转不过来——一击就散。"),
                        new Skill("冰面留身 · 且战且走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "牵制不是硬扛。我拖住一个后期——靠的，就是不跟他打。"),
                        new Skill("一尺冰 · 挡你全力", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "我这护身冰只一尺厚。多一寸我要省三年——可挡你这一击，一尺够了。"),
                        new Skill("冰封一角阵", "ULT", 2.44, 410, Vfx.ULT_NOVA,
                                "冰封一角阵，不冻全场只冻一角。角里那位——今日别想再支援任何人。"))));

        list.add(new Cultivator(137L, "金武环", Realm.NASCENT_SOUL, SubStage.PEAK, "中期巅峰",
                "古剑门大长老 · 化剑为丝", "天南古剑门",
                "化剑为丝（一剑分丝，万千不穷）", Style.SWORD,
                List.of(Trait.SWORD, Trait.TREASURE, Trait.GLASS),
                Basis.CANON, "原著明载：古剑门大长老，与韩立切磋，败于风雷翅与乾蓝冰焰",
                "剑修到我这年纪，比的不是快，是敢不敢把自己也炼成剑。",
                List.of(
                        new Skill("化剑为丝 · 万千", "CONTROL", 1.05, 200, Vfx.SWORD_RAIN,
                                "一剑散成三千丝。丝不伤人——先把你困住。困住了，才轮得到谈剑。"),
                        new Skill("剑丝收口", "ATTACK", 2.0, 215, Vfx.SLASH,
                                "丝阵一成，收口只在一息。这一息里——你的护身灵光跟没有一样。"),
                        new Skill("古剑门传剑 · 单片不留", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "我把本命剑也炼成丝绕在身上。守的时候——我自己，就是那口剑。"),
                        new Skill("丝尽剑亡", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "三千丝尽并成一线。这一线出去——我三百年道行只剩一半。可我，不悔。"))));

        list.add(new Cultivator(138L, "南宫阙", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "掩月宗元中剑修 · 凝光镜", "天南掩月宗",
                "凝光剑诀（以镜收光，光尽剑至）", Style.SWORD,
                List.of(Trait.SWORD, Trait.TREASURE, Trait.MIND),
                Basis.INFERRED, "原著明载掩月宗元中剑修、手持凝光镜，强于令狐老怪；小层取中期",
                "同为南宫，我与婉妹不熟。掩月宗的剑不是拿来讲亲缘的。",
                List.of(
                        new Skill("凝光镜 · 收你剑光", "CONTROL", 1.0, 185, Vfx.MIND_WAVE,
                                "凝光镜不伤人，只把你那一剑的光收进去。下一剑——是你的光，我的剑。"),
                        new Skill("掩月一剑", "ATTACK", 1.95, 205, Vfx.SLASH,
                                "掩月一剑，月缺时出。这一式不取名——因为它，只要一息。"),
                        new Skill("镜光护身", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "镜里存着三段别人的旧剑光。挨打时放一段出去——替我，挡下你。"),
                        new Skill("镜光齐放 · 还你三年", "ULT", 2.4, 405, Vfx.SWORD_RAIN,
                                "这面镜子里攒了我半生的对手。今日——一并还给你们。"))));

        list.add(new Cultivator(139L, "令狐老怪", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "黄枫谷唯一的元婴", "天南黄枫谷",
                "黄枫谷旧法（不擅杀伐，擅守谷）", Style.QI,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载黄枫谷只有令狐一人元中，小层按谷中撑得起场面那个取中期",
                "谷里弟子叫我老祖。这点修为摆出去天南没人当回事 —— 可我守的是黄枫谷。",
                List.of(
                        new Skill("守谷一击", "ATTACK", 1.7, 185, Vfx.QI_PALM,
                                "我这一辈子出手，多半就在谷口。今日——也在。别处，你去不成。"),
                        new Skill("老怪的脸 · 先吓退", "CONTROL", 0.82, 150, Vfx.MIND_WAVE,
                                "我这张老脸不值钱，可它能把你这一场仗——推到明日再打。"),
                        new Skill("黄枫谷护山", "DEFENSE", 1.0, 125, Vfx.SHIELD,
                                "谷中弟子都躲在我身后。我这一口真气——散了，他们就没命了。"),
                        new Skill("一门之气 · 尽付我手", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "黄枫谷三百年不出名，靠的就是有人肯站在门口。今日这门之气——尽在我手。"))));

        list.add(new Cultivator(140L, "南陇侯", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "万尺一线 · 天南数一数二的遁术", "天南散修",
                "万尺一线（苍坤上人独创遁术，一线之间人已千里）", Style.QI,
                List.of(Trait.ESCAPE, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著明载其修万尺一线、以遁术闻名，小层无载取元中",
                "散修没有宗门，退路就是我的宗门。",
                List.of(
                        new Skill("万尺一线", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "万尺一线不是一步千里——是把自己压成一条线。这条线，比你先到位。"),
                        new Skill("存了一辈子的一击", "ATTACK", 1.75, 190, Vfx.QI_PALM,
                                "我不与人换手。攒够三十招才舍得出这一次——你，接得住么？"),
                        new Skill("见势不对先退", "DEFENSE", 1.0, 125, Vfx.SHIELD,
                                "这道护身不为挡住你——为叫我多活那半息，好起遁。半息，就是生死。"),
                        new Skill("一线千里 · 你追不上", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "苍坤创这一式时想明白了：先活下来，再谈道。你追不上一个只想活的人。"))));

        list.add(new Cultivator(141L, "碎魂真人", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "碎魂一脉 · 不击身直取魂", "天南散修",
                "碎魂大法（先碎你魂魄，再谈胜负）", Style.MIND,
                List.of(Trait.MIND, Trait.GLASS, Trait.NO_BURST),
                Basis.INFERRED, "原著只存其名与一脉功法，小层按元中取",
                "人身好护，魂难护。这一句我写在帖子上，不是写在心里。",
                List.of(
                        new Skill("碎魂一指", "ATTACK", 1.95, 200, Vfx.MIND_WAVE,
                                "碎魂一指不伤皮肉，直进识海。你身上有多少护身法宝——都没用。"),
                        new Skill("乱神摄魂", "CONTROL", 1.0, 185, Vfx.MIND_WAVE,
                                "先叫你对不上法诀里那一口气。乱了——再打，你已无还手。"),
                        new Skill("护魂守一", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "修碎魂的人自己最怕被碎。这一手护魂——比我那功法练得久得多。"),
                        new Skill("魂飞魄散", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "你元婴出窍那一刻——就是我最快的一刻。魂飞，魄散，别怪我。"))));

        list.add(new Cultivator(142L, "鲁卫英", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "天极门长老 · 死于坠魔谷", "天南天极门",
                "天极玄光（不显不抢，先护同行的人）", Style.GUARD,
                List.of(Trait.GUARD, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为天极门长老、元中，坠魔谷一役殁于古魔；小层取中期",
                "谷里那东西不是我们杀得死的。我能做到的，是叫三个晚辈先出去。",
                List.of(
                        new Skill("天极玄光 · 厚壁", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "天极玄光不出奇，长处是密。密到——能替身后的人，挡下你一息。"),
                        new Skill("护住后撤", "CONTROL", 0.85, 160, Vfx.ARRAY_PRESS,
                                "我不求胜、不求债。先把这谷口给你堵住——让晚辈们，先走。"),
                        new Skill("长老的一剑", "ATTACK", 1.72, 185, Vfx.QI_PALM,
                                "我这一剑出手极老，看着不像元婴。可它——到位。到位的剑，就要见血。"),
                        new Skill("一门之气 · 不尽不止", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "天极门进去的人只回去几个。那没回去的其中一个——今日，拉你垫账。"))));

        list.add(new Cultivator(143L, "东门图", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "御灵宗大长老 · 以灵压兽", "天南御灵宗",
                "御灵真诀（灵虫猛兽皆受我调）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载御灵宗大长老东门图为元中，小层取中期",
                "兽不听我的，只听划算。我这一生做的事就是一件：把它们算明白。",
                List.of(
                        new Skill("灵虫群 · 噬灵", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "它不咬人——先啃你法器上那层灵光。灵光啃尽了，你我再坐下谈。"),
                        new Skill("唤兽上山", "ATTACK", 1.85, 200, Vfx.SUMMON,
                                "我不动手。我只管吹一声——满山那些东西，自替我动手。"),
                        new Skill("御灵印 · 护主", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "印落身上。三阶以内的东西，休想近我三尺。"),
                        new Skill("万灵齐至", "ULT", 2.45, 415, Vfx.ULT_NOVA,
                                "御灵宗从不靠人多。今日就叫你认清——这一山，是谁的规矩。"))));

        list.add(new Cultivator(144L, "火龙童子", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "火龙一路 · 童身老怪", "天南魔道散修",
                "火龙真炎（以火养气，气尽火不灭）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著由其元初巅峰一路至元中，本榜取人界篇终局的中期",
                "我叫童子，因为我这张脸不会老。这一条比火龙好用。",
                List.of(
                        new Skill("火龙吐息", "ATTACK", 1.9, 200, Vfx.FIRE_ORB,
                                "一气化龙，龙过成灰。看着是热闹——它烧的，是你身上那点水气。"),
                        new Skill("童子拜火 · 乱你心神", "CONTROL", 0.95, 180, Vfx.FIRE_ORB,
                                "一张孩童的脸立在火前。你那一剑递是不递——三息之内，见分晓。"),
                        new Skill("火铠", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "以身为炉。法术才到近前，先给我烧去一半。"),
                        new Skill("火龙焚天", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这口火我养了三百年。它出窍那一刻——我自己，也要少活十年。"))));

        list.add(new Cultivator(145L, "白瑶怡", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "天南顶尖元中女修 · 独斗青背苍狼", "天南散修",
                "白虹剑诀（一人一剑，不进任何一盟）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其为顶尖元中女修、小极宫一役独对十级妖狼游刃有余；小层取中期",
                "跟妖物打过的人都知道：它不跟你讲规矩，所以你也不必讲体面。",
                List.of(
                        new Skill("白虹一剑", "ATTACK", 1.95, 200, Vfx.SLASH,
                                "剑光起时，像一道虹。虹是短——可足够贯穿你。"),
                        new Skill("剑光分影", "CONTROL", 0.95, 180, Vfx.SWORD_RAIN,
                                "三道虚影，一张网。看清楚了——网是我撒的，不是它。"),
                        new Skill("不硬拼 · 且战且走", "MOBILITY", 0.0, 130, Vfx.BLOOD_SHADOW,
                                "十级的爪子擦身而过。我靠的不是挡——是叫它抓偏半寸。"),
                        new Skill("虹化", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "把自己炼成一线光。这一线过去——我与剑，只余一个还能站着。"))));

        list.add(new Cultivator(146L, "程天坤", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗掌门 · 一家之主", "天南落云宗",
                "落云宗镇宗功法（不求胜人，只求不丢宗门）", Style.ARRAY,
                List.of(Trait.GUARD, Trait.TREASURE, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为落云宗掌门、元婴修士，小层按一宗之主撑场面取初期",
                "掌门不是宗里最强的，是最后一个走的。",
                List.of(
                        new Skill("掌门印 · 借阵", "ATTACK", 1.8, 200, Vfx.ARRAY_PRESS,
                                "这一击不是我出的——是落云宗三座峰，一起出的。"),
                        new Skill("护山禁制 · 封一门", "CONTROL", 0.9, 175, Vfx.ARRAY_PRESS,
                                "峰主们在里头议事。我在外头——把这门，替你关上。"),
                        new Skill("镇宗厚气", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "这门功出不了重手。它的长处只有一个——站着，不倒。"),
                        new Skill("宗门之重", "ULT", 2.3, 400, Vfx.ULT_NOVA,
                                "我修为确在几位峰主之下。可今日这一下——吃的是一整宗的灵气。"))));

        list.add(new Cultivator(147L, "吕洛", Realm.NASCENT_SOUL, SubStage.EARLY, "初期",
                "落云宗长老 · 千浪决", "天南落云宗",
                "千浪决（一波未平一波又起，长于遁走）", Style.QI,
                List.of(Trait.ESCAPE, Trait.PERSIST, Trait.NO_BURST),
                Basis.INFERRED, "原著明载其为落云宗元婴长老、修千浪决；小层取初期",
                "浪多不代表浪大。我这一门靠的是打不完。",
                List.of(
                        new Skill("千浪叠涌", "ATTACK", 1.7, 185, Vfx.QI_PALM,
                                "一击之后还有九击。每一击都不重——叠起来，你还扛得住么？"),
                        new Skill("浪里抽身", "MOBILITY", 0.0, 125, Vfx.BLOOD_SHADOW,
                                "水气一乱。你再抬眼——人已在三十丈外。"),
                        new Skill("一层水幕", "DEFENSE", 1.0, 120, Vfx.SHIELD,
                                "重手我不硬挡。只把你那一击——偏出去半尺。"),
                        new Skill("千浪齐碎", "ULT", 2.3, 395, Vfx.ULT_NOVA,
                                "叠了百年的浪一起碎在你身上。声势极大，威力其实寻常——可你，得先扛住那声势。"))));

        // ---------- 乱星海补：天星双圣的另一位 ----------
        list.add(new Cultivator(148L, "温青", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天星双圣之一 · 天灵根", "乱星海天星双圣",
                "元磁神光（两极之中她养阳）+ 天灵根", Style.MAGNET,
                List.of(Trait.MAGNET, Trait.GUARD, Trait.PERSIST),
                Basis.CANON, "原著明载天星双圣之一、天灵根、元磁神光，与凌啸风并列元后",
                "天灵根不是我选的，双修也不是。可这一身两极，我认。",
                List.of(
                        new Skill("元磁神光 · 摄你兵", "CONTROL", 1.0, 195, Vfx.MAGNET_BEAM,
                                "你祭出的那件东西——此刻已在半空偏了一线。一线，就够要你的命。"),
                        new Skill("阳极一放", "ATTACK", 1.85, 195, Vfx.MAGNET_BEAM,
                                "两极须同起、同收。三百年，我练的就是这一口气。"),
                        new Skill("星宫旧阵 · 半边阵眼", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "阵眼分在两人身上，各一半。我这一半——今日，谁也别想夺。"),
                        new Skill("两极齐鸣 · 短暂抗化神", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一击本该两人同使。今日他不在——我一个人，也叫得动这片海听调。"))));

        // ---------- 大晋：元后多如麻，只收有名有姓、出手有据的 ----------
        list.add(new Cultivator(149L, "房宗主", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "阴罗宗宗主 · 三具尸王随身", "大晋阴罗宗",
                "阴罗宗炼尸大法（铜甲、乌甲尸王各是其主）", Style.CORPSE,
                List.of(Trait.TREASURE, Trait.SUMMON, Trait.PERSIST),
                Basis.INFERRED, "原著明载阴罗宗宗主为元后、随身三具尸王（约当元婴初期）；小层取后期",
                "我门下最听话的是这三具。它们不讨价，也不告老。",
                List.of(
                        new Skill("铜甲尸王 · 上前", "ATTACK", 1.9, 210, Vfx.SUMMON,
                                "不知痛，不惧死，生前也是元婴。死后——比活时更听我的话。"),
                        new Skill("乌甲尸王 · 夹击", "ATTACK", 1.95, 215, Vfx.SUMMON,
                                "两具同出，一正一侧。你倒是说说——先顾哪一头？"),
                        new Skill("尸毒郁积 · 一闻就倒", "CONTROL", 1.0, 190, Vfx.POISON_MIST,
                                "炼尸身上那点毒，积了几百年。盖一开——你自己吸。"),
                        new Skill("金色尖锥 · 遁速极快", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "宗主从不与人斗久。要么尸王上前——要么，我这一锥先取你性命。"),
                        new Skill("灭仙珠 · 一颗可惊化神", "ULT", 2.7, 435, Vfx.ULT_NOVA,
                                "此珠一发，境界三个字今日作废。代价是——三具尸王，齐替我按住它。"))));

        list.add(new Cultivator(150L, "乾老魔", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰",
                "阴罗宗大长老 · 五子同心魔", "大晋阴罗宗",
                "逆情断天大法（先绝情绝性，方敢入门）", Style.DEMON,
                List.of(Trait.MIND, Trait.SUMMON, Trait.PERSIST),
                Basis.CANON, "原著明载阴罗宗大长老，五子同心魔合体可达元后巅峰且不灭",
                "逆情断天这四个字不是形容。它是一条门槛：先把你心里那点东西砍了。",
                List.of(
                        new Skill("五子同心魔 · 合体", "ATTACK", 2.25, 235, Vfx.SUMMON,
                                "五个元婴精魂，祭炼二十年。合起来——是一具不死的魔。"),
                        new Skill("鬼罗幡 · 镇宗之宝", "CONTROL", 1.05, 205, Vfx.BLOOD_SHADOW,
                                "幡一展开，里头那些精魂都认得我。为何？——因为它们，全是我亲手放进去的。"),
                        new Skill("血魔珠 · 寄附元婴", "HEAL", 0.95, 195, Vfx.BLOOD_SHADOW,
                                "这具身子坏了何妨。珠里还养着一个元婴——那才是我的第二副肉躯。"),
                        new Skill("逆情 · 断你六亲", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "威胁我？你且寻寻看——我这一身，还有哪处在乎得失。"),
                        new Skill("绝情一击", "ULT", 2.62, 432, Vfx.ULT_NOVA,
                                "砍掉的那部分会疼三百年。疼过之后——这一身力气，全是它的。"))));

        list.add(new Cultivator(151L, "玄青子", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "太一门话事人 · 平山印", "大晋太一门",
                "太一玄功 + 平山印（仿制通天灵宝）", Style.GUARD,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载太一门话事人持仿制通天灵宝平山印；小层取元后",
                "灵宝不是我们的。我们只是替它看门。",
                List.of(
                        new Skill("平山印 · 一印镇山", "ATTACK", 2.15, 225, Vfx.ARRAY_PRESS,
                                "印落下来，不带半点风声。山不是被推倒的——是被生生按平的。"),
                        new Skill("太一护身", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "大晋的宗门不擅斗狠。擅的是——叫你的斗狠，落不到我身上。"),
                        new Skill("仿宝之威 · 先压器", "CONTROL", 0.95, 185, Vfx.QI_PALM,
                                "印还未落，你手上那件先自颤——它认得，比它高的东西。"),
                        new Skill("山不至 · 人已平", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一印照着真正的平山印仿成。仿到了几分——今日，你来亲手量一量。"))));

        list.add(new Cultivator(152L, "七妙真人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天魔宗执法长老 · 赤鸣鼓", "大晋天魔宗",
                "赤鸣鼓音（一鼓破胆，再鼓散魂）", Style.SOUND,
                List.of(Trait.TREASURE, Trait.MIND, Trait.GUARD),
                Basis.INFERRED, "原著明载天魔宗执法长老七妙真人持仿制灵宝赤鸣鼓；小层取元后",
                "执法这件事不能讲情面，也不能讲力气。讲力气就轮不到我。",
                List.of(
                        new Skill("赤鸣一鼓 · 破胆", "CONTROL", 1.05, 195, Vfx.SOUND_RING,
                                "鼓声不入耳——直入心脉。你听见那一下时，胆气已短了三分。"),
                        new Skill("再鼓散魂", "ATTACK", 1.95, 205, Vfx.SOUND_RING,
                                "两鼓之间，从不留第三鼓的余地。这一门的规矩，就是这个。"),
                        new Skill("执法令 · 令众不动", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "宗里有人要动手，我先敲一鼓。鼓响之后还敢动手的——那时，才轮到执法。"),
                        new Skill("三鼓 · 万军自乱", "ULT", 2.5, 415, Vfx.ULT_NOVA,
                                "赤鸣鼓一响——仿制灵宝这四个字，再没人敢当面提起。"))));

        list.add(new Cultivator(153L, "坤无极", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "血凄门大长老 · 以血换力", "大晋血凄门",
                "血凄真解（以自身精血换一时之强）", Style.YIN,
                List.of(Trait.PERSIST, Trait.TREASURE, Trait.GLASS),
                Basis.BOARD, "原著只列其名与元后之阶，出手记载极少，本榜按大晋元后通则取值",
                "别人炼气，我炼血。血少一分，力气就多一分 —— 这笔账我自己算得清。",
                List.of(
                        new Skill("血凄一祭", "ATTACK", 1.9, 200, Vfx.BLOOD_SHADOW,
                                "一口精血喷上法器，那一击立重三成。这三成——是我从寿元里借来的。"),
                        new Skill("以血养气", "HEAL", 0.9, 0, Vfx.HEAL_GLOW,
                                "这一门不养气，只养血。血回来了——人才算回来了。"),
                        new Skill("摄血成幕", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "血幕不挡你的法术。它挡的——是你敢往前迈的那一步。"),
                        new Skill("一门之血 · 尽付此击", "ULT", 2.45, 410, Vfx.ULT_NOVA,
                                "今日这一击之后，我要养六十年。养不养得回来——看你今日死不死。"))));

        list.add(new Cultivator(154L, "元智大师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "雷音宗 · 明王诀 + 四层金刚诀", "大晋雷音宗",
                "明王诀（金身四相 · 肉身堪比法宝）", Style.BODY,
                List.of(Trait.BODY, Trait.GUARD, Trait.TREASURE),
                Basis.INFERRED, "原著明载雷音宗元智大师修明王诀与四层金刚诀，肉身堪比法宝；小层取元后",
                "禅杖上那颗万年火珊瑚不是我供的，是我拿来敲人的。",
                List.of(
                        new Skill("四层金刚 · 身如法宝", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "金刚诀练到第四层。刀砍上来——是砍在我自己那件法器上。"),
                        new Skill("明王忿怒相", "ATTACK", 2.0, 215, Vfx.QI_PALM,
                                "一念忿怒，一身金相。雷音宗轻易不动这相——动了，便再不讲佛法。"),
                        new Skill("法力金刚 · 虚影落地", "ATTACK", 1.9, 205, Vfx.SUMMON,
                                "召出的不是佛——是我这一生攒下的那点法力。它，比我耐打。"),
                        new Skill("禅杖 · 火珊瑚一敲", "CONTROL", 0.95, 185, Vfx.FIRE_ORB,
                                "万年火珊瑚嵌在杖头。一杖敲下——先烧穿你那层护身灵光。"),
                        new Skill("明王现身", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "金身一现，法也不辩、理也不辩。老衲这一生——只辩这最后一次。"))));

        list.add(new Cultivator(155L, "碧月禅师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "净火宗 · 四百年入元后的万年一遇", "大晋净火宗",
                "净火禅定（火不焚人，先焚自心）", Style.QI,
                List.of(Trait.PERSIST, Trait.GUARD, Trait.NO_BURST),
                Basis.BOARD, "原著只载其才名与四百年入元后，人界篇未正面出手，本榜按元后通则取值",
                "没人见过我出手，所以人人都说我最强。这一句我受着，受了几百年。",
                List.of(
                        new Skill("净火一焚", "ATTACK", 1.8, 195, Vfx.FIRE_ORB,
                                "此火不烧皮肉——先烧你身上那些不干净的东西。魔道修士，最怕这一味。"),
                        new Skill("禅定不动", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "四百年的静坐不是白坐的。你那一击到我身前——老衲先不动心。"),
                        new Skill("火中生莲", "HEAL", 0.85, 170, Vfx.HEAL_GLOW,
                                "伤势用自家真火来养。疼，是真疼；好，也是真好。"),
                        new Skill("四百年只做这一件事", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "今日若出这一手，往后四百年大约要从头再来。所以我——几百年都不曾动用。"))));

        list.add(new Cultivator(156L, "易洗天", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "大晋四大散修之一 · 曾轻斩八级妖兽", "大晋散修",
                "洗天剑诀（一人一剑，不属任何宗门）", Style.SWORD,
                List.of(Trait.SWORD, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载其为大晋四大散修之一、曾轻斩八级妖兽；本榜依富成那句「战力不宜高估」取元后而非元后巅峰",
                "散修没有靠山，我的靠山是那一剑。斩过八级妖兽，所以还能站着讲这句话。",
                List.of(
                        new Skill("一剑斩妖", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "对付皮粗肉厚的东西，剑要走在它眨眼之前。八级妖兽我亲手斩过——它，也没来得及眨眼。"),
                        new Skill("洗天 · 剑气如虹", "ATTACK", 1.9, 200, Vfx.SWORD_RAIN,
                                "剑气铺开，像落了一场的雨。散修的剑不求漂亮——求的是，无处可躲。"),
                        new Skill("散修的警觉", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "我连睡觉时护身灵光都不散。这一条——比剑诀值钱。"),
                        new Skill("四大散修之名", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这名号不是谁封的——是一路活下来，别人一口口叫出来的。叫的人多了，你就得应。"))));

        list.add(new Cultivator(157L, "寒骊上人", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "北夜小极宫 · 乾蓝冰焰修到极致", "大晋北夜小极宫",
                "乾蓝冰焰 + 两口乾蓝鼎", Style.ICE,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载小极宫寒骊上人两口乾蓝鼎、乾蓝冰焰已修到极致，能轻接三焰一扇；小层取元后",
                "北夜的冰不是寒，是慢。慢到你身上那点灵气忘了自己是活的。",
                List.of(
                        new Skill("乾蓝冰焰 · 一扇", "ATTACK", 2.05, 215, Vfx.ICE_FROST,
                                "这火焰看着是蓝，其实不烫。它冻住的——是你运功的那一条线。"),
                        new Skill("两口乾蓝鼎 · 同压", "CONTROL", 1.0, 190, Vfx.ARRAY_PRESS,
                                "两口鼎一齐祭出。一口压你一身——一口，装你那些没了用的残骸。"),
                        new Skill("冰焰护体", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "近我三尺者先僵。就僵那一息——已够我把鼎，稳稳收回。"),
                        new Skill("北夜极寒 · 三焰不足惧", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "这一场，我只当是替小极宫看一回门。门里的东西——你一件也带不走。"))));

        list.add(new Cultivator(158L, "小极宫宫主", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "小极宫主 · 手里有一把黄泉的钥匙", "大晋北夜小极宫",
                "小极宫主功法 + 制约黄泉鬼母之法", Style.MIND,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.NO_BURST),
                Basis.BOARD, "原著未点其名与境界，按其为一宫之主、能制鬼母的规格取元后",
                "制得住她的不是我的功法，是我手里那把钥匙。这一句我从不避。",
                List.of(
                        new Skill("钥令 · 唤鬼母", "CONTROL", 1.05, 200, Vfx.MIND_WAVE,
                                "我不出手。令一亮——自有人替我动手。只那一个，已经不是人了。"),
                        new Skill("小极寒冰阵", "ATTACK", 1.85, 200, Vfx.ARRAY_PRESS,
                                "北夜的阵不靠阵旗——靠的是这一山的寒气。我往阵心一站，我便是阵眼。"),
                        new Skill("宫主之位", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "这一宫的人，都立在我身后。所以我——不能散。"),
                        new Skill("一宫之权", "ULT", 2.4, 410, Vfx.ULT_NOVA,
                                "钥匙，只有一把。今日用它——明日，宫里就要少一个人。"))));

        list.add(new Cultivator(159L, "黄泉鬼母", Realm.NASCENT_SOUL, SubStage.MID, "中期（白板鬼修）",
                "鬼道白板 · 与车老妖化身不分胜负", "北夜黄泉 · 鬼道",
                "黄泉鬼道（无肉身，纯以阴魂作战）", Style.CORPSE,
                List.of(Trait.RESIDUAL, Trait.MIND, Trait.ESCAPE),
                Basis.INFERRED, "原著明载其为白板鬼修、与车老妖化身斗个不分胜负；白板之身按元中折算",
                "他们叫我鬼母。我这一身最不值钱的就是那具身体 —— 它早烂了。",
                List.of(
                        new Skill("阴魂夺舍", "ATTACK", 1.95, 205, Vfx.BLOOD_SHADOW,
                                "我不杀你。我进你那具身子——试一试，合不合我穿。"),
                        new Skill("黄泉雾 · 掩身", "CONTROL", 1.0, 185, Vfx.POISON_MIST,
                                "雾里那些脸，都是我的旧识。它们此刻——正替你看该站哪一边。"),
                        new Skill("无身可伤", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "你砍我三剑，我只少了一层雾。这话像认输——其实是告诉你，我这具身子，压根不算什么。"),
                        new Skill("鬼母现身 · 阴魂不散", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "被人制了三百年。今日谁把钥匙递到我手里——我替他，做一件天大的事。"))));

        list.add(new Cultivator(160L, "叶家大长老", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "叶家大长老 · 黑血刃砍伤狮禽兽", "大晋叶家",
                "黑血刃（以精血激发，一击见骨）", Style.SWORD,
                List.of(Trait.SWORD, Trait.PERSIST, Trait.TREASURE),
                Basis.INFERRED, "原著明载叶家大长老持黑血刃、曾砍伤十级妖禽并接过古魔一击；小层取元后",
                "世家不是功法好，是肯把精血拿出来的人多。",
                List.of(
                        new Skill("黑血刃 · 精血激发", "ATTACK", 2.15, 225, Vfx.SLASH,
                                "这口刃要吃血才肯亮。血是自己的——亮一分，便是我一命里的一分。"),
                        new Skill("接古魔一击", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "那一击，我拿半边身子接下了。半边，够了——身后那些孩子，还没跑远。"),
                        new Skill("喝退妖禽", "CONTROL", 0.95, 190, Vfx.QI_PALM,
                                "四翅的畜生不怕人。它怕的——是敢提刀跟它讲价的人。"),
                        new Skill("一门之血 · 一刀换一生", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "叶家这一代能站着说这句话——是因为上一代，有人肯把血流干。"))));

        list.add(new Cultivator(161L, "叶月圣", Realm.NASCENT_SOUL, SubStage.LATE, "后期（隐藏修为）",
                "叶家七叔 · 藏到最后一刻才动手", "大晋叶家",
                "借古魔之力炼体（藏得最深的那一个）", Style.BODY,
                List.of(Trait.BODY, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载叶家七叔隐藏元婴后期修为、终投古魔；本榜按其暴露后的规格取元后",
                "族里都当我修为平平。藏了三百年，就为今日这一下。",
                List.of(
                        new Skill("藏了三百年的一拳", "ATTACK", 2.1, 220, Vfx.QI_PALM,
                                "这一拳不出去——我便还是那个替家族看孩子的七叔。"),
                        new Skill("古魔之力 · 炼体", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "借来的力，先得把自己改成一副能用它的身子。这一步最疼——也，最值。"),
                        new Skill("血祭借力", "ATTACK", 1.9, 200, Vfx.BLOOD_SHADOW,
                                "每借一分力——我便少记得一分，自己原来的名字。"),
                        new Skill("七叔的名分", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "今日之后，叶家再没有七叔了。这一句——我等了很久，也怕了很久。"))));

        list.add(new Cultivator(162L, "富成", Realm.NASCENT_SOUL, SubStage.MID, "中期",
                "九幽宗执法长老 · 把话放在心里说", "大晋九幽宗",
                "九幽玄功（不显山不露水，专办宗门的事）", Style.YIN,
                List.of(Trait.GUARD, Trait.MIND, Trait.NO_BURST),
                Basis.INFERRED, "原著明载九幽宗执法长老富成为元中；他点评易洗天那句「战力不宜高估」是本榜取值口径的根据",
                "宗门里的事，办完了不必说。说了就还得再办一次。",
                List.of(
                        new Skill("九幽拿禁", "CONTROL", 1.0, 190, Vfx.MIND_WAVE,
                                "不伤你性命。先把你那两只手——借走。宗里办事，讲的是规矩。"),
                        new Skill("执法一击", "ATTACK", 1.78, 190, Vfx.SLASH,
                                "这一击本身不重。重的——是它背后压着的那条文。"),
                        new Skill("护住本心", "DEFENSE", 1.0, 130, Vfx.SHIELD,
                                "九幽一路，最会拿捏人心。我这一手——专防自家那点人心。"),
                        new Skill("宗门律 · 也是我律", "ULT", 2.35, 400, Vfx.ULT_NOVA,
                                "我办过的人里，有一半今日会站出来替我说话。这——就是执法长老的本钱。"))));

        list.add(new Cultivator(163L, "徐大仙师", Realm.NASCENT_SOUL, SubStage.LATE, "后期",
                "天澜草原大仙师 · 与至阳、乾老魔齐名", "天澜草原",
                "草原祭天大法（借地势祭坛，不借人身）", Style.ARRAY,
                List.of(Trait.TREASURE, Trait.GUARD, Trait.PERSIST),
                Basis.INFERRED, "原著明载天澜草原大仙师与至阳上人、乾老魔齐名，后被万年尸熊砍去躯体；小层取元后",
                "草原不认宗门，认祭坛。坛上那点烟一升，万帐的人都站我这边。",
                List.of(
                        new Skill("祭坛召力", "ATTACK", 1.9, 205, Vfx.ARRAY_PRESS,
                                "我这一击，不是从手上来的——是从你脚下那片地，一起涌上来的。"),
                        new Skill("大仙师之名 · 压场", "CONTROL", 1.0, 185, Vfx.ARRAY_PRESS,
                                "名号这东西，在草原上是有分量的。这分量本身——就能压住你一半人。"),
                        new Skill("躯体不全 · 硬撑", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "少了半边身子的老怪，还在硬撑这座阵。为什么？——因为我一散，万帐立刻就乱。"),
                        new Skill("草原万帐同祭", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "这一座坛，是草原几百年的旧账。今日——就拿它，跟你结上一回。"))));

        list.add(new Cultivator(164L, "田锤", Realm.NASCENT_SOUL, SubStage.EARLY_PEAK, "刚入后期",
                "慕兰第四神师 · 神来人不来", "慕兰草原",
                "慕兰巫神功（以血祭神，神力附体）", Style.MIND,
                List.of(Trait.SUMMON, Trait.PERSIST, Trait.GLASS),
                Basis.INFERRED, "原著明载其为慕兰第四神师、刚进阶元后，曾被凤冰牵制；小层取刚入后期",
                "神不来说我不在。可血一放出去，我就得在坛下挨着。",
                List.of(
                        new Skill("血祭请神", "CONTROL", 1.0, 195, Vfx.MIND_WAVE,
                                "神降临那一瞬——你的法诀会自己乱上一拍。就一拍，足够了。"),
                        new Skill("神师一击", "ATTACK", 1.85, 200, Vfx.QI_PALM,
                                "这一击里，没有我自己的力气——用的，是附在我身上那一位的。"),
                        new Skill("被冰牵住 · 硬拖", "DEFENSE", 1.0, 135, Vfx.SHIELD,
                                "冻到了手臂，我也不换刀。这门神功头一条教我的——就是不喊疼。"),
                        new Skill("神来一瞬", "ULT", 2.4, 405, Vfx.ULT_NOVA,
                                "只借一瞬。那一瞬里，我不再是我——这大约是天大的好事，也是天大的坏事。"))));

        // ---------- 妖修：十级约当元婴后期，人界明面上的顶尖战力 ----------
        list.add(new Cultivator(165L, "金蛟王", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级妖兽）",
                "外海蛟龙族长 · 一枪杀星宫桂长老", "乱星海外海蛟龙族",
                "蛟龙族炼体 + 金甲长枪", Style.BEAST,
                List.of(Trait.BODY, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载外海蛟龙族长金蛟王为十级妖修（约当元后巅峰）、强势击杀星宫桂长老；本榜按其规格取元后巅峰",
                "外海不认人界的规矩，只认鳞片的硬度。",
                List.of(
                        new Skill("蛟枪 · 一刺穿金甲", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "蛟筋做杆，龙牙做尖。人界那些法器在它跟前——跟纸糊的一样。"),
                        new Skill("龙鳞护身", "DEFENSE", 1.0, 150, Vfx.TURTLE_SHELL,
                                "一片鳞，便是一件护身法宝。而我身上——有几千片。"),
                        new Skill("水府号令 · 召群妖", "ATTACK", 1.9, 205, Vfx.SUMMON,
                                "我一枪不出，先长啸一声。这海里——多的是愿意替我出枪的。"),
                        new Skill("蛟龙之躯 · 金身", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "现出原身那一刻，人界所谓斗法就只剩一个字——量。"))));

        list.add(new Cultivator(166L, "万年尸熊", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖谷副谷主 · 昆吾山大杀四方", "天南万妖谷",
                "尸熊之躯 + 血刃（仿制魔龙刃）", Style.BEAST,
                List.of(Trait.BODY, Trait.TREASURE, Trait.SWORD),
                Basis.INFERRED, "原著明载万妖谷副谷主尸熊持仿制魔龙刃血刃，昆吾山中砍去徐大仙师躯体；按十级妖修取元后",
                "谷里叫我副谷主。这个位子不是打来的，是大家伙都挨过我一刀之后让出来的。",
                List.of(
                        new Skill("血刃 · 砍去躯体", "ATTACK", 2.15, 225, Vfx.BLOOD_SHADOW,
                                "仿制的魔龙刃，砍人从不落第二下。那位仙师的躯体——也不过一刀。"),
                        new Skill("熊抱 · 骨头自己会断", "CONTROL", 1.0, 195, Vfx.QI_PALM,
                                "法器我都嫌麻烦。被我抱住——你自己就会散架。"),
                        new Skill("万年皮肉", "DEFENSE", 1.0, 150, Vfx.TURTLE_SHELL,
                                "这一身不是甲，是万年一寸寸长出来的肉。它自己——就会夹住你的剑。"),
                        new Skill("万妖谷二当家", "ULT", 2.55, 425, Vfx.ULT_NOVA,
                                "昆吾山上那一夜，我没有让过一次路。不让路——就是我万妖谷的规矩。"))));

        list.add(new Cultivator(167L, "青背苍狼", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖谷副谷主 · 一张网困两名元中", "天南万妖谷",
                "苍狼血脉 + 皂袍藏网", Style.BEAST,
                List.of(Trait.TREASURE, Trait.ESCAPE, Trait.PERSIST),
                Basis.INFERRED, "原著明载万妖谷副谷主青背苍狼作皂袍老者，持网独斗叶家大长老与白瑶怡而游刃有余；按十级取元后",
                "我穿人衣，说人话。不是为了像人，是为了叫人先不想动手。",
                List.of(
                        new Skill("撒网 · 困敌", "CONTROL", 1.05, 195, Vfx.ARRAY_PRESS,
                                "这张网不缠人——只缠法器。被网住的那位，今日只剩下自己一双手。"),
                        new Skill("苍狼扑食", "ATTACK", 1.95, 205, Vfx.SLASH,
                                "狼的打法只有一式：先绕，后扑。绕的那三圈——你也一并算进去了么？"),
                        new Skill("且战且走", "MOBILITY", 0.0, 135, Vfx.BLOOD_SHADOW,
                                "两个元中一齐上来，我自然要分开来打。而分开打——靠的是腿。"),
                        new Skill("谷中狼群 · 齐至", "ULT", 2.45, 415, Vfx.ULT_NOVA,
                                "论最能打，轮不到我。可论叫得动的——比他们，都多。"))));

        list.add(new Cultivator(168L, "木魁", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级妖修）",
                "昆吾山四妖 · 一株占了半座山的古木", "昆吾山",
                "草木大阵（根须延数十里，木雾含剧毒）", Style.WOOD,
                List.of(Trait.PERSIST, Trait.SUMMON, Trait.GUARD),
                Basis.INFERRED, "原著明载昆吾山木魁为万年古木，枝干堪比极品法宝、扎根无尽；最终被三老残留神念驱法宝灭杀，按十级取元后巅峰",
                "我不追人。我长得比你跑得远。",
                List.of(
                        new Skill("草木大阵 · 困", "CONTROL", 1.05, 200, Vfx.ARRAY_PRESS,
                                "山上那些草，都是我的根。你这一路踩过来——其实，一直踩在我身上。"),
                        new Skill("木牢 · 枝干如极品法宝", "ATTACK", 1.95, 210, Vfx.SLASH,
                                "一根枝条，便是一件法宝。而我身上——有几万根。"),
                        new Skill("剧毒木雾", "ATTACK", 1.85, 200, Vfx.POISON_MIST,
                                "这雾不难看，只是吸一口就不能呼吸。山里的兽——都绕着它走。"),
                        new Skill("扎根 · 不尽", "HEAL", 1.0, 0, Vfx.HEAL_GLOW,
                                "烧掉我一半，何妨。根还在——半座山，自会替我长回来。"),
                        new Skill("半座山都是我的", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "这一式不是法术，是年岁。一万年的东西压下来——你，拿什么去挡？"))));

        list.add(new Cultivator(169L, "银翅夜叉", Realm.NASCENT_SOUL, SubStage.PEAK, "后期巅峰（十级飞天尸）",
                "昆吾山四妖 · 会算计的一具尸", "昆吾山阴阳窟",
                "飞天尸神通（煞魂丝 · 风土二遁 · 兼修幻术）", Style.CORPSE,
                List.of(Trait.MIND, Trait.ESCAPE, Trait.BODY),
                Basis.INFERRED, "原著明载银翅夜叉为昆吾山四妖之一、实是仅次于金身月尸的十级飞天尸，在阴阳窟秒杀两名元中；按十级取元后巅峰",
                "他们以为尸不会想事。想事的尸，才是真正难杀的那一种。",
                List.of(
                        new Skill("煞魂丝 · 缚婴", "CONTROL", 1.05, 200, Vfx.BLOOD_SHADOW,
                                "此丝不缠你的身——缠的是你的元婴。婴一旦被缠，你那具身子，就只剩一层空皮。"),
                        new Skill("阴阳窟 · 秒杀两元中", "ATTACK", 2.1, 220, Vfx.SLASH,
                                "那一窟里，我没用第二招。不是省力气——是要他们到死都以为，洞内还藏着第三人。"),
                        new Skill("风土二遁 · 倏忽", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "一遁走风，一遁走土。尸身本不必呼吸——所以那土遁里，我能多潜半日。"),
                        new Skill("尸身不知痛", "DEFENSE", 1.0, 145, Vfx.SHIELD,
                                "你砍了我一臂，我用另一半接着打。为什么？——那一臂，本来就不算我的。"),
                        new Skill("银翅遮天", "ULT", 2.6, 430, Vfx.ULT_NOVA,
                                "双翅一开，满窟尽是银光。哪一只是真的——你自己，慢慢猜。"))));

        list.add(new Cultivator(170L, "狮禽兽", Realm.NASCENT_SOUL, SubStage.LATE, "后期（未化形）",
                "昆吾山四妖 · 一身爆发碾压多数元后", "昆吾山",
                "狮首鹰身四翅（雷遁 + 金色波纹，不修神识只修力）", Style.BEAST,
                List.of(Trait.BODY, Trait.ESCAPE, Trait.NO_BURST),
                Basis.INFERRED, "原著明载狮禽兽为昆吾山四妖之一、狮首鹰身四翅、未化形而肉身爆发极强；智不足，按十级取元后",
                "我不会说话。这一条山上的人都知道，所以从不劝我讲道理。",
                List.of(
                        new Skill("四翅雷遁 · 扑", "ATTACK", 2.2, 225, Vfx.QI_PALM,
                                "四翅齐张，雷就聚在爪上。它并非快到看不见——是你，根本挡不住。"),
                        new Skill("金色波纹 · 声波", "ATTACK", 1.95, 205, Vfx.SOUND_RING,
                                "一声长啸，山石齐碎。法器里那位——耳朵先就什么也听不见了。"),
                        new Skill("紫光翅影", "MOBILITY", 0.0, 140, Vfx.BLOOD_SHADOW,
                                "它不使遁术，它只是飞。可四翅一展——你已算不出它下一爪落在何处。"),
                        new Skill("未化形之身 · 一击不讲理", "ULT", 2.55, 430, Vfx.ULT_NOVA,
                                "它不懂法诀，也不懂克制。它只会把一身力气全压在一下里——这一下，比多数元后的神通都要重。"))));

        list.add(new Cultivator(171L, "车老妖", Realm.NASCENT_SOUL, SubStage.LATE, "后期（十级妖修）",
                "万妖幡主 · 化身与鬼母不分胜负", "天南万妖谷",
                "万妖幡（幡上诸妖，皆我所炼）", Style.TRAIN,
                List.of(Trait.SUMMON, Trait.TREASURE, Trait.PERSIST),
                Basis.INFERRED, "原著明载车老妖持万妖幡、其化身与黄泉鬼母斗个不分胜负；按十级妖修取元后",
                "幡里那些东西都欠我。欠得久了，它们就肯替我死。",
                List.of(
                        new Skill("万妖幡 · 放妖", "ATTACK", 2.0, 215, Vfx.SUMMON,
                                "一幡在手，里头几百头。它们是不听话——可它们怕我，远胜过怕你。"),
                        new Skill("化身代战", "CONTROL", 1.0, 190, Vfx.MIND_WAVE,
                                "你打赢的那一个，不是我。这一条我从不隐瞒——因为，说出来你也无可奈何。"),
                        new Skill("妖气遮身", "DEFENSE", 1.0, 140, Vfx.SHIELD,
                                "妖气一裹——你那些探查人的本事，尽数落空。"),
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
                                "骨头是自己的，法器也是自己的。在我这一门里——这两件事，本没有分别。"),
                        new Skill("擒婴 · 反手", "CONTROL", 1.0, 195, Vfx.MIND_WAVE,
                                "他擒过我的婴。如今我学会了这一手——就是拿来，还他的。这话我不对人讲，只对自己讲。"),
                        new Skill("死过一次 · 不怕", "DEFENSE", 1.0, 150, Vfx.SHIELD,
                                "你吓不住一个已经死过一回的人。这一条比护身法术好用。"),
                        new Skill("复出 · 当席吞你", "ULT", 2.5, 420, Vfx.ULT_NOVA,
                                "在那座殿里被关了太多年。今日重见天日——我，要先吃上一口。"))));

        // ---------- 结丹补：乱星海结丹第一人 ----------
        list.add(new Cultivator(173L, "温天仁", Realm.CORE_FORMING, SubStage.PEAK, "巅峰（乱星海结丹第一人）",
                "六道极圣传人 · 一身宝物一身劫", "乱星海 · 魔道六道极圣门下",
                "六极真魔功（另有化劫大法、颠凤培元功、引龙诀）", Style.DEMON,
                List.of(Trait.TREASURE, Trait.PERSIST, Trait.GLASS),
                Basis.CANON, "原著明载：乱星海结丹修士第一人，六道极圣门下；后被鬼雾卷入阴冥之地法力尽废，死于暴风山山腰",
                "身上宝物最多的人，最后死在一条不会法术的手臂上。这话不是说我，是说那一夜的我。",
                List.of(
                        new Skill("六极真魔功 · 六气齐出", "ATTACK", 1.95, 210, Vfx.BLOOD_SHADOW,
                                "师父那一门的功，我只学到了点皮毛。一遇上辟邪神雷——这六气，就只够挨一下。"),
                        new Skill("天阳鎏金针 · 克制噬金虫", "CONTROL", 1.0, 185, Vfx.GOLD_BEAM,
                                "虫群一到，金针先发。这件宝，就是为那东西专门炼的——我这一身，全是这样的专门。"),
                        new Skill("化劫大法 · 断肢化劫", "DEFENSE", 1.0, 160, Vfx.SHIELD,
                                "劫来了，就切一条手臂喂它。切过一次——下一次，我照样敢切。"),
                        new Skill("引龙诀 · 借一口气", "HEAL", 0.9, 0, Vfx.HEAL_GLOW,
                                "师父自创的双修秘术。借来的力，到底也是力——只是，终究要还。"),
                        new Skill("八门金光镜（仿品）· 神焰封门", "ULT", 2.35, 380, Vfx.ULT_NOVA,
                                "八门齐开那一刻——我真当自己已是元婴。可每一道金光，都得先耗尽我口里的一缕元气。"))));

        return List.copyOf(list);
    }
}
