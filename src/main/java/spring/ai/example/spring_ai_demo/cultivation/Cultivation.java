package spring.ai.example.spring_ai_demo.cultivation;

import java.util.List;

/**
 * 凡人修仙传·人界篇 修仙排行榜的领域模型。
 *
 * <p>刻意不落库：这份数据是「小说设定」而不是「用户数据」，它会随考据修订而不随业务变化，
 * 建表反而带来 schema 迁移与脏数据风险。人物身份（昵称/头像/主页）仍从 blog_user 取，
 * 这里只按 userId 挂设定，改设定等于改代码，走正常发布流程。
 *
 * <p>数值口径（人界篇）：上古古魔入侵后灵气稀薄，人界修士最高只能到化神初期；
 * 炼气十三层，其上分初/中/后/圆满，寿元约 80 / 150 / 200 / 500 / 1000 / 2000。
 */
public final class Cultivation {

    private Cultivation() {
    }

    /** 大境界：base 是战力基数，相邻大境界约 5 倍，用来体现「境界即天堑」 */
    public enum Realm {
        MORTAL("凡人", "无灵根，不引气", 8, "#9aa5b1", "约八十", "不在仙途之内 —— 但榜单上留着，因为人界篇大半的人一生在这一格。"),
        QI_REFINING("炼气", "一至十三层", 45, "#7dd3fc", "约一百五", "引气入体。十三层是极限，多数人卡在后期一辈子。"),
        FOUNDATION("筑基", "初 · 中 · 后 · 圆满", 260, "#38bdf8", "二百", "真正踏入修仙门槛。一名筑基执事足以镇一宗。"),
        CORE_FORMING("结丹", "初 · 中 · 后 · 圆满", 1300, "#34d399", "五百", "结丹有成，便是一方人物；宗门老祖多半停在这一格。"),
        NASCENT_SOUL("元婴", "初 · 中 · 后 · 巅峰", 6500, "#a78bfa", "一千", "元婴老怪，人界明面上的顶尖战力。"),
        SOUL_TRANSFORMATION("化神", "人界天花板 · 灵气已薄", 32000, "#f59e0b", "二千", "叫得动天地之气。可人界灵气不够施展太久 —— 化神在这里是「多余」的。");

        private final String label;
        private final String rule;
        private final double base;
        private final String color;
        private final String lifespan;
        private final String note;

        Realm(String label, String rule, double base, String color, String lifespan, String note) {
            this.label = label;
            this.rule = rule;
            this.base = base;
            this.color = color;
            this.lifespan = lifespan;
            this.note = note;
        }

        public String getLabel() {
            return label;
        }

        public String getRule() {
            return rule;
        }

        public double getBase() {
            return base;
        }

        public String getColor() {
            return color;
        }

        public String getLifespan() {
            return lifespan;
        }

        public String getNote() {
            return note;
        }

        public int idx() {
            return ordinal();
        }
    }

    /** 小层倍率：subText 由人物自己给（各境界叫法不同：圆满 / 巅峰 / 十三层） */
    public enum SubStage {
        EARLY(1.0), EARLY_PEAK(1.55), MID(1.3), LATE(1.7), PEAK(2.2);

        private final double mul;

        SubStage(double mul) {
            this.mul = mul;
        }

        public double getMul() {
            return mul;
        }
    }

    /** 考据强度：叫得出口和叫不出口，是两张不同的脸 */
    public enum Basis {
        CANON("原著明载"), INFERRED("原著可推"), BOARD("本榜推定");

        private final String label;

        Basis(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * 功法路数。克制关系写在 {@link Style#beatsRaw} / {@link Style#weakRaw}，
     * 逻辑尽量贴着小说里说得通的句子：剑修破护身灵光、元磁先收你兵、体修不吃法术、
     * 神念探破绽、化神叫动天地 —— 而不是随手编一张五行表。
     * 两栏里出现的每一个词都必须是本枚举内的路数名，"ALL" 表示无解。
     */
    public enum Style {
        SWORD("剑修", "剑气生发，专破护身灵光", "MIND,TURTLE,WOOD,ILLUSION,POISON,TRAIN,SOUND,BEAST", "MAGNET,ARRAY"),
        QI("养气", "长春功一路，根基扎实却不擅杀伐", "POISON,ARRAY", "SWORD,DEMON,BODY"),
        ICE("至寒", "冻的是水气与灵气之间的牵连", "CORPSE,DEMON,WOOD,QI,TURTLE", "BODY,GUARD"),
        MIND("神念", "在你看不见的地方动手", "CORPSE,MORTAL,ILLUSION,SOUND,TURTLE,GUARD", "SWORD,BODY"),
        YIN("至阴", "摄魂、擒婴，寒气结膜", "CORPSE,WOOD,QI", "GUARD,BODY,MIND"),
        DEMON("魔道", "六气并立，底牌多过道理", "QI,WOOD,ICE,SOUND,TRAIN", "GUARD,BODY,TURTLE"),
        BODY("体修", "真元不走经脉走筋骨，挨打就是修行", "MIND,ICE,YIN,SOUND,MORTAL", "SWORD,LAW"),
        CORPSE("炼尸", "不知痛、不惧死，魂魄早没了", "MORTAL,WOOD", "SWORD,YIN,MIND,POISON,LAW"),
        SOUND("音律", "惑心乱神，潮信为号", "CORPSE,BEAST,TRAIN,MORTAL", "MIND,GUARD,ILLUSION"),
        ARRAY("阵法", "不与你斗法，先借地势", "TRAIN,MORTAL,BEAST,POISON", "MAGNET,MIND,LAW"),
        MAGNET("元磁", "方圆百丈金铁听调 —— 先收你兵", "SWORD,ARRAY", "BODY,LAW,MORTAL"),
        LAW("法则", "一呼一吸山河皆为你用", "ALL", "MAGNET"),
        TURTLE("妖盾", "八百年长成的壳，同阶全力一击不动", "SWORD,ICE,DEMON,QI,SOUND,TRAIN", "MIND,POISON,LAW"),
        BEAST("妖体", "血脉为力，天生比人耐活", "QI,MORTAL", "SWORD,ICE,MIND,LAW"),
        TRAIN("御兽", "不压不服，只讲一个合字", "MORTAL,POISON", "SOUND,SWORD,ARRAY,LAW"),
        WOOD("灵植", "催芽接骨，药与藤都是兵器", "MORTAL,TRAIN,BEAST", "SWORD,ICE,POISON,DEMON"),
        ILLUSION("迷神", "魔光不伤人，先迷神三息", "CORPSE,SOUND,QI,BODY", "MIND,GUARD,SWORD,TURTLE"),
        POISON("药毒", "针与丹，救人也杀人", "CORPSE,BEAST,BODY", "MIND,GUARD,ICE"),
        GUARD("守一", "真元厚而不烈，最善守最不善杀", "DEMON,ILLUSION,YIN,MIND,POISON", "LAW,SWORD"),
        MORTAL("凡人", "没有灵根，只有两条腿和一副骨头", "", "ALL");

        private final String label;
        private final String essence;
        private final String beatsRaw;
        private final String weakRaw;

        Style(String label, String essence, String beatsRaw, String weakRaw) {
            this.label = label;
            this.essence = essence;
            this.beatsRaw = beatsRaw;
            this.weakRaw = weakRaw;
        }

        public String getLabel() {
            return label;
        }

        public String getEssence() {
            return essence;
        }

        public String getBeatsRaw() {
            return beatsRaw;
        }

        public String getWeakRaw() {
            return weakRaw;
        }

        /**
         * a 打 b 的克制倍率。四个方向各查一次：a 明写克制 b、b 明写畏惧 a 都算占优；
         * 反过来 a 畏惧 b、b 明写克制 a 都算吃亏。含 "ALL" 表示这一栏无解（凡人之于仙长）。
         */
        public static double counter(Style a, Style b) {
            if (a == b) {
                return 1.0;
            }
            double mul = 1.0;
            if (hits(a.beatsRaw, b)) {
                mul *= 1.22;
            }
            if (hits(b.weakRaw, a)) {
                mul *= 1.22;
            }
            if (hits(a.weakRaw, b)) {
                mul /= 1.18;
            }
            if (hits(b.beatsRaw, a)) {
                mul /= 1.18;
            }
            return Math.max(0.4, Math.min(2.1, mul));
        }

        private static boolean hits(String csv, Style target) {
            if (csv.contains("ALL")) {
                return true;
            }
            for (String token : csv.split(",")) {
                if (token.trim().equals(target.name())) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 特性：全部以「倍率」形式累乘到 CombatStats 上，避免为每个人手填七个数字 */
    public enum Trait {
        SWORD("剑修", 1.12, s -> { s.atkMul *= 1.25; s.spdMul *= 1.15; }),
        BODY("体修", 1.06, s -> { s.hpMul *= 1.9; s.defMul *= 1.35; s.atkMul *= 0.95; s.spdMul *= 0.85; }),
        GUARD("善守", 1.02, s -> { s.defMul *= 1.5; s.hpMul *= 1.2; s.atkMul *= 0.85; s.enduranceMul *= 1.3; }),
        TURTLE("万年妖盾", 1.12, s -> {
            s.defMul *= 1.9; s.hpMul *= 2.4; s.atkMul *= 1.05; s.spdMul *= 0.5;
            s.escapeMul *= 0.2; s.enduranceMul *= 1.5;
        }),
        MIND("神念极强", 1.10, s -> { s.senseMul *= 2.6; s.atkMul *= 1.1; s.mpMul *= 1.15; }),
        RESIDUAL("残魂寄物", 0.62, s -> { s.hpMul *= 0.5; s.mpMul *= 0.6; s.senseMul *= 1.6; s.enduranceMul *= 0.7; }),
        SPIRIT("器灵之身", 0.92, s -> { s.hpMul *= 0.7; s.mpMul *= 0.65; s.atkMul *= 1.18; s.enduranceMul *= 0.7; }),
        ESCAPE("遁术冠绝", 1.05, s -> { s.escapeMul *= 4.5; s.spdMul *= 1.5; s.enduranceMul *= 1.15; }),
        TREASURE("重宝随身", 1.14, s -> { s.atkMul *= 1.15; s.defMul *= 1.22; s.mpMul *= 1.3; }),
        PERSIST("心性续航", 1.06, s -> { s.enduranceMul *= 1.7; s.mpMul *= 1.3; }),
        SUMMON("御兽 / 依众", 1.08, s -> { s.atkMul *= 1.2; s.hpMul *= 1.1; s.mpMul *= 0.9; }),
        GLASS("出手轻、身子薄", 0.94, s -> { s.hpMul *= 0.78; s.defMul *= 0.82; }),
        ALCHEMY("丹药医道", 1.0, s -> { s.hpMul *= 1.25; s.enduranceMul *= 1.45; }),
        MAGNET("收金铁", 1.10, s -> { s.atkMul *= 1.18; s.defMul *= 1.2; s.senseMul *= 1.2; }),
        NO_BURST("缺一锤定音", 0.95, s -> { s.atkMul *= 0.82; s.enduranceMul *= 1.2; }),
        /** 人界灵气薄：化神的代价是有叫动天地的本事，可这点灵气不够你施展太久 */
        LAW("天地法则", 1.45, s -> {
            s.atkMul *= 2.3; s.senseMul *= 1.8; s.defMul *= 1.3;
            s.mpMul *= 0.55; s.enduranceMul *= 0.6;
        }),
        MORALE("凡人的骨气", 1.0, s -> { s.enduranceMul *= 1.6; s.escapeMul *= 1.2; });

        private final String label;
        private final double powerMul;
        private final java.util.function.Consumer<CombatStats> modifier;

        Trait(String label, double powerMul, java.util.function.Consumer<CombatStats> modifier) {
            this.label = label;
            this.powerMul = powerMul;
            this.modifier = modifier;
        }

        public String getLabel() {
            return label;
        }

        public double getPowerMul() {
            return powerMul;
        }

        public void apply(CombatStats stats) {
            modifier.accept(stats);
        }
    }

    /** 斗法数值容器：只存倍率，最终绝对值由 CultivationCatalog 统一算 */
    public static final class CombatStats {
        public double hpMul = 1, mpMul = 1, atkMul = 1, defMul = 1;
        public double spdMul = 1, senseMul = 1, escapeMul = 1, enduranceMul = 1;
    }

    /** kind：ATTACK 输出 / ULT 底牌大招 / DEFENSE 护体 / CONTROL 控场 / HEAL 疗养 / MOBILITY 走 */
    public record Skill(String name, String kind, double power, int cost, String vfx, String desc) {
    }

    /**
     * 斗法台报招配音：前端用浏览器语音合成把招名念出来，这里只定“听起来像谁”。
     * pitch/rate 直接给 Web Speech 用（1.0 为正常人声），timbre 是人设描述，
     * shout 是出手前那一声喊招 —— 短，否则一拍里光说话就打不完仗了。
     */
    public record Voice(double pitch, double rate, String timbre, String shout) {
    }

    /** 特效键，前端 duel.js 按键分派画法 */
    public static final class Vfx {
        public static final String SLASH = "SLASH";
        public static final String SWORD_RAIN = "SWORD_RAIN";
        public static final String GOLD_BEAM = "GOLD_BEAM";
        public static final String ICE_FROST = "ICE_FROST";
        public static final String FIRE_ORB = "FIRE_ORB";
        public static final String SHIELD = "SHIELD";
        public static final String MIND_WAVE = "MIND_WAVE";
        public static final String BLOOD_SHADOW = "BLOOD_SHADOW";
        public static final String SUMMON = "SUMMON";
        public static final String POISON_MIST = "POISON_MIST";
        public static final String ARRAY_PRESS = "ARRAY_PRESS";
        public static final String MAGNET_BEAM = "MAGNET_BEAM";
        public static final String SOUND_RING = "SOUND_RING";
        public static final String TURTLE_SHELL = "TURTLE_SHELL";
        public static final String MOON_GLOW = "MOON_GLOW";
        public static final String QI_PALM = "QI_PALM";
        public static final String MORTAL_PUNCH = "MORTAL_PUNCH";
        public static final String HEAL_GLOW = "HEAL_GLOW";
        public static final String ULT_NOVA = "ULT_NOVA";

        private Vfx() {
        }
    }

    /**
     * 一个人物的一条设定。userId 对应 blog_user 里的角色账号（101-173，其中 131 以后只建账号不写文章），
     * name 只作兜底：万一角色账号被删了，榜单仍然说得清这是谁。
     */
    public record Cultivator(
            Long userId,
            String name,
            Realm realm,
            SubStage sub,
            String subText,
            String title,
            String origin,
            String technique,
            Style style,
            List<Trait> traits,
            Basis basis,
            String basisNote,
            String quote,
            List<Skill> skills) {

        /** 战力：境界基数 × 小层 × 特性累乘，前端横条按 log 压缩后展示 */
        public double power() {
            double mul = 1.0;
            for (Trait t : traits) {
                mul *= t.getPowerMul();
            }
            return realm.getBase() * sub.getMul() * mul;
        }

        public CombatStats stats() {
            CombatStats stats = new CombatStats();
            for (Trait t : traits) {
                t.apply(stats);
            }
            return stats;
        }
    }
}
