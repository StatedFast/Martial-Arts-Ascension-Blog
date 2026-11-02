package spring.ai.example.spring_ai_demo.cultivation;

import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.CombatStats;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Cultivator;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Skill;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Style;
import spring.ai.example.spring_ai_demo.cultivation.Cultivation.Trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * 斗法裁决引擎。
 *
 * <p>三个刻意的设计决定：
 * <ol>
 *   <li><b>裁决放服务端。</b>「最终结果要符合小说设定」是内容约束而不是表演约束 ——
 *       放在前端等于谁都能改 JSON 让厉飞雨一拳打死向之礼。服务端算，前端只负责播。</li>
 *   <li><b>确定性。</b>同一对人物必然同一结果：种子由两个 id 推出，不取时钟、不掺随机，
 *       刷新页面不会出现「上一秒韩立赢下一秒韩立输」，也方便把某一局贴给别人复现。</li>
 *   <li><b>原著战绩优先于数值。</b>数值算的是「按设定应当如何」，原著写的是「实际如何」，
 *       冲突时以原著为准 —— 但不会硬改结果：被指定赢的那一方在推演里会被动用护身法宝挡下致命一击，
 *       被指定平手的那一方最后一下收手。页面看到的每一格血条都对得上判词。</li>
 * </ol>
 */
@Component
public class DuelEngine {

    private static final int MAX_ROUNDS = 12;
    private static final double HP_SCALE = 1000;
    /** 战力比压缩指数：1 是线性（境界差会一巴掌把人拍死），0.55 让「一大境界 ≈ 三四击」 */
    private static final double COMPRESS = 0.55;
    /** 原著覆盖局里给胜方的便宜：不是无敌，是「底牌比你多一张」 */
    private static final double CANON_ATK_BIAS = 1.6;
    private static final double CANON_SURVIVE_RATE = 0.06;

    // ==================== 返回结构 ====================

    public record FighterView(Long id, String name, String avatar, String homepage,
                              String realm, String realmLabel, String realmColor, String subText,
                              String title, String origin, String technique,
                              String style, String styleLabel, String styleEssence, List<String> traits,
                              double power, long hpMax, long mpMax,
                              String basis, String basisNote, String quote, List<Skill> skills) {
    }

    public record Event(int seq, int round, Long actorId, Long targetId,
                        String skill, String kind, String vfx, String line,
                        long damage, long heal, boolean crit, boolean ult, boolean skipped,
                        long hpA, long mpA, long hpB, long mpB) {
    }

    /** type：WIN 分胜负 / CRUSH 差两个大境界以上的碾压 / DRAW 平手 / ESCAPE 一方全身而退 */
    public record Outcome(String type, Long winnerId, String headline, String verdict,
                          String method, String basis, int rounds, double powerA, double powerB) {
    }

    public record Duel(long seed, FighterView a, FighterView b, List<Event> events, Outcome outcome) {
    }

    // ==================== 原著战绩覆盖表 ====================

    private record Canon(String type, Long winnerId, String verdict, String canonSource) {
    }

    /** 只放「原著写过」或「站内自述/互评提过」的对应关系，宁缺毋滥。key 与先后无关 */
    private static final Map<String, Canon> CANON = Map.ofEntries(
            Map.entry(key(101L, 118L), new Canon("WIN", 101L,
                    "这一局取人界篇末：向之礼在空间节点被域外天魔夺舍，最终死在韩立剑下。单论修为他仍是人界顶点，"
                            + "可榜单记的是「最终」—— 而且那一剑本就不该由还清醒的他来挨。",
                    "《凡人修仙传》人界篇末 · 空间节点；站内 1018「我当年没点他——点了反倒害他」")),
            Map.entry(key(101L, 110L), new Canon("DRAW", null,
                    "此战不成。人界篇末韩立与冰凤联手闯空间节点偷渡灵界 —— 这两个人是同路，不是对手。",
                    "《凡人修仙传》人界篇末 · 韩立与冰凤联手破节点")),
            Map.entry(key(101L, 102L), new Canon("DRAW", null,
                    "道侣之间不动真杀。素女元阴功那句「元阴是蓄，不是换」，和青元剑诀那句「慢就是快」，"
                            + "其实是同一条路的两种说法。",
                    "站内 1001 · 1002 功法自述；原著二人为道侣")),
            Map.entry(key(101L, 108L), new Canon("DRAW", null,
                    "打不了。一个在剑里，一个使剑。那句「作为你剑里的人说一句」已经把立场写明白了。",
                    "站内 1004 评论 · 银月自述 1008")),
            Map.entry(key(101L, 112L), new Canon("WIN", 101L,
                    "曲魂自始至终是韩立的身外化身。真动手，这具炼尸抬不起手 —— 所以赢的那一方只能是韩立，"
                            + "而且赢得一点都不光彩。",
                    "站内 1012 自述 · 七玄门张铁")),
            Map.entry(key(101L, 107L), new Canon("WIN", 101L,
                    "大衍神君肉身早没于上古一战，只余残魂留传承；而大衍诀最终落进韩立识海。残魂之身，撑不住三息。",
                    "站内 1007 自述；原著大衍诀传承归韩立")),
            Map.entry(key(111L, 113L), new Canon("DRAW", null,
                    "两条都硬。蛮胡子挨过一记擒婴，极阴祖师被拆了半口鼎 —— 互评里两人都认了账："
                            + "「你擒过我的元婴，我也差点拆了你的鼎，可见谁都没死透。」",
                    "站内 1024 评论 · 蛮胡子 ↔ 极阴祖师")),
            Map.entry(key(104L, 120L), new Canon("DRAW", null,
                    "八极拳打在账本上。两个凡人谁都赢不了谁 —— 而这一局人界篇里没人受伤，这是它最好的地方。",
                    "站内 1004 · 1020 凡俗自述")),
            Map.entry(key(118L, 126L), new Canon("WIN", 118L,
                    "十级妖兽约当元婴后期顶峰，与化神之间还差着一整个「法则」。龟壳挡得住人，挡不住天地之气。",
                    "原著境界对照：十级 ≈ 元婴后期顶峰，十一级方入化神")),
            Map.entry(key(114L, 129L), new Canon("WIN", 129L,
                    "魔道六气再杂，也过不了「守一」这一关。正道第一人压住魔道靠的从来不是更能杀，是更忍得住。",
                    "站内 1014 · 1029 自述；魔道为正道所制系人界篇通例")));

    private static String key(Long x, Long y) {
        return Math.min(x, y) + "-" + Math.max(x, y);
    }

    // ==================== 入口 ====================

    public Optional<Duel> duel(Cultivator ca, Cultivator cb, Map<Long, String> avatars) {
        if (ca == null || cb == null || Objects.equals(ca.userId(), cb.userId())) {
            return Optional.empty();
        }
        Combatant a = new Combatant(ca);
        Combatant b = new Combatant(cb);
        long seed = ca.userId() * 1_000_003L + cb.userId() * 7_919L;
        Sim sim = new Sim(a, b, CANON.get(key(ca.userId(), cb.userId())), new SplittableRandom(seed));
        sim.run();
        return Optional.of(new Duel(seed, view(a, avatars), view(b, avatars), sim.events, sim.outcome()));
    }

    // ==================== 一局推演 ====================

    private static final class Sim {
        private final Combatant a;
        private final Combatant b;
        private final Canon canon;
        private final SplittableRandom rng;
        private final List<Event> events = new ArrayList<>();
        private final Long canonWinnerId;
        private final boolean forceDraw;
        private int seq;
        private int round;
        private String earlyEndReason;
        private Long escapeId;
        private Combatant lastLethalSave;

        Sim(Combatant a, Combatant b, Canon canon, SplittableRandom rng) {
            this.a = a;
            this.b = b;
            this.canon = canon;
            this.rng = rng;
            this.canonWinnerId = canon == null ? null : canon.winnerId();
            this.forceDraw = canon != null && "DRAW".equals(canon.type());
            if (canonWinnerId != null) {
                Combatant fav = Objects.equals(canonWinnerId, a.userId()) ? a : b;
                fav.atk *= CANON_ATK_BIAS;
                fav.def *= 1.28;
                fav.endurance *= 1.4;
            }
        }

        void run() {
            while (round < MAX_ROUNDS && a.hp > 0 && b.hp > 0 && earlyEndReason == null) {
                round++;
                regen(a);
                regen(b);

                double firstScore = a.spd * (0.85 + rng.nextDouble(0.3)) + a.sense * 0.25;
                double secondScore = b.spd * (0.85 + rng.nextDouble(0.3)) + b.sense * 0.25;
                Combatant first = firstScore >= secondScore ? a : b;
                Combatant second = first == a ? b : a;
                for (Combatant actor : List.of(first, second)) {
                    if (a.hp <= 0 || b.hp <= 0 || earlyEndReason != null) {
                        break;
                    }
                    act(actor, actor == a ? b : a);
                }
                checkEscape();
                checkDrawProgression();
            }
        }

        /** 原著指定平手的局里，谁都不许真的被打死：最后一击收手，留一线血底 */
        private long applyDamage(Combatant actor, Combatant foe, long dmg) {
            long real = Math.min(dmg, foe.hp);
            boolean lethal = real >= foe.hp;
            if (lethal) {
                long spare = Math.max(1, Math.round(foe.hpMax * CANON_SURVIVE_RATE));
                if (forceDraw) {
                    real = Math.max(0, foe.hp - spare);
                    earlyEndReason = "DRAW";
                } else if (canonWinnerId != null && Objects.equals(canonWinnerId, foe.userId())) {
                    real = Math.max(0, foe.hp - spare);
                    lastLethalSave = foe;
                }
            }
            foe.hp -= real;
            return real;
        }

        private void act(Combatant actor, Combatant foe) {
            if (actor.stunTurns > 0) {
                actor.stunTurns--;
                push(actor, foe, "一时失神", "SKIPPED", "MIND_WAVE",
                        "神识被人按住，这一息连法诀都起不来。", 0, 0, false, false, true);
                return;
            }
            Skill skill = pick(actor, foe);
            actor.mp = Math.max(0, actor.mp - skill.cost());
            actor.lastSkill = skill;
            String line = skill.desc();
            long damage = 0;
            long heal = 0;
            boolean crit = false;

            switch (skill.kind()) {
                case "ATTACK", "ULT" -> {
                    double counter = Style.counter(actor.c.style(), foe.c.style());
                    double raw = (actor.atk * skill.power()) / Math.max(1, foe.def * (foe.shieldTurns > 0 ? 1.6 : 1.0));
                    double dmg = HP_SCALE * 0.115 * Math.pow(Math.max(0.0005, raw), COMPRESS) * counter;
                    dmg *= 0.88 + rng.nextDouble(0.24);
                    crit = rng.nextDouble() < clamp(0.06 + (actor.sense - foe.sense) / 1200.0, 0.02, 0.3);
                    if (crit) {
                        dmg *= 1.55;
                    }
                    if (foe.evadeTurns > 0) {
                        dmg *= 0.4;
                        foe.evadeTurns--;
                    }
                    if (actor.atkDebuffTurns > 0) {
                        dmg *= 0.72;
                        actor.atkDebuffTurns--;
                    }
                    if (foe.shieldTurns > 0) {
                        dmg *= 0.5;
                        foe.shieldTurns--;
                    }
                    damage = applyDamage(actor, foe, Math.max(1, Math.round(dmg)));
                    boolean mindGame = actor.c.style() == Style.MIND || actor.c.style() == Style.ILLUSION
                            || actor.c.style() == Style.YIN || actor.c.style() == Style.SOUND;
                    if (mindGame && actor.sense > foe.sense * 1.15 && foe.stunTurns == 0
                            && rng.nextDouble() < 0.34) {
                        foe.stunTurns = 1;
                        line = line + " —— 你识海一滞，下一息起不来。";
                    }
                }
                case "DEFENSE" -> {
                    actor.shieldTurns = 2;
                    actor.mp = Math.min(actor.mpMax, actor.mp + 60);
                }
                case "CONTROL" -> {
                    foe.atkDebuffTurns = 2;
                    long chip = Math.max(1, Math.round(HP_SCALE * 0.05 * Math.pow(Math.max(0.0005,
                            (actor.atk * skill.power()) / Math.max(1, foe.def)), COMPRESS)));
                    damage = applyDamage(actor, foe, chip);
                }
                case "HEAL" -> {
                    heal = Math.min(Math.round(140 * skill.power() * (actor.endurance / 100.0)), actor.hpMax - actor.hp);
                    actor.hp += heal;
                    actor.mp = Math.min(actor.mpMax, actor.mp + Math.round(150 * skill.power()));
                }
                case "MOBILITY" -> actor.evadeTurns = 2;
                default -> damage = applyDamage(actor, foe, Math.max(1, Math.round(HP_SCALE * 0.08
                        * Math.pow(Math.max(0.0005, actor.atk / Math.max(1, foe.def)), COMPRESS))));
            }
            push(actor, foe, skill.name(), skill.kind(), skill.vfx(), line,
                    damage, heal, crit, "ULT".equals(skill.kind()), false);
            if (lastLethalSave != null) {
                Combatant saved = lastLethalSave;
                lastLethalSave = null;
                push(saved, actor, "护身法宝 · 挡下致命一击", "DEFENSE", "SHIELD",
                        "那一下本该要命。是身上那件平日不显的旧物替它挡了 —— 人界篇里活到高处的人，"
                                + "靠的从来不只是打得过。", 0, 0, false, false, false);
            }
        }

        /** 出招选择：先保命、再掀底牌、最后按权重普攻。同种子同序列，完全可复现 */
        private Skill pick(Combatant actor, Combatant foe) {
            List<Skill> usable = new ArrayList<>();
            for (Skill s : actor.c.skills()) {
                if (s.cost() <= actor.mp && (actor.lastSkill == null || !Objects.equals(s.name(), actor.lastSkill.name()))) {
                    usable.add(s);
                }
            }
            if (usable.isEmpty()) {
                return cheapest(actor);
            }
            double hpRate = (double) actor.hp / actor.hpMax;

            if (hpRate < 0.42 && rng.nextDouble() < 0.72) {
                Optional<Skill> save = usable.stream()
                        .filter(s -> "DEFENSE".equals(s.kind()) || "HEAL".equals(s.kind())).findFirst();
                if (save.isPresent()) {
                    return save.get();
                }
            }
            if (hpRate < 0.35 && actor.escape >= 300 && rng.nextDouble() < 0.6) {
                Optional<Skill> flee = usable.stream().filter(s -> "MOBILITY".equals(s.kind())).findFirst();
                if (flee.isPresent()) {
                    return flee.get();
                }
            }
            if (!actor.ultUsed) {
                Optional<Skill> ult = usable.stream().filter(s -> "ULT".equals(s.kind())).findFirst();
                if (ult.isPresent() && (hpRate < 0.55 || (double) foe.hp / foe.hpMax < 0.5 || round >= 3)) {
                    actor.ultUsed = true;
                    return ult.get();
                }
            }
            List<Skill> offense = usable.stream().filter(s -> "ATTACK".equals(s.kind())).toList();
            if (!offense.isEmpty()) {
                return offense.get(rng.nextInt(offense.size()));
            }
            return usable.get(rng.nextInt(usable.size()));
        }

        private Skill cheapest(Combatant actor) {
            Skill best = actor.c.skills().get(0);
            for (Skill s : actor.c.skills()) {
                if (s.cost() < best.cost()) {
                    best = s;
                }
            }
            return best;
        }

        private void regen(Combatant c) {
            long gain = Math.round(34 * (c.endurance / 100.0) * (0.9 + rng.nextDouble(0.2)));
            c.mp = Math.min(c.mpMax, c.mp + gain);
        }

        /** 遁术高、境界吃亏、血线见底的，走。这在人界篇不是懦弱，是活下来的方法论 */
        private void checkEscape() {
            if (canon != null) {
                return;
            }
            for (Combatant loser : new Combatant[]{a, b}) {
                Combatant winner = loser == a ? b : a;
                int gap = winner.c.realm().idx() - loser.c.realm().idx();
                if (gap >= 1 && loser.escape >= 300 && loser.hp > 0
                        && (double) loser.hp / loser.hpMax < 0.3) {
                    // 遁法名取本人技能表里的 MOBILITY，没配的就用通用说法 —— 不能把谁的招式按到别人身上
                    Skill flee = loser.c.skills().stream()
                            .filter(s -> "MOBILITY".equals(s.kind())).findFirst().orElse(null);
                    push(loser, winner, (flee == null ? "遁光" : flee.name()) + " · 全身而退", "ESCAPE",
                            flee == null ? "BLOOD_SHADOW" : flee.vfx(),
                            loser.c.quote(), 0, 0, false, false, false);
                    earlyEndReason = "ESCAPE";
                    escapeId = loser.userId();
                    return;
                }
            }
        }

        /** 指定平手的一局：双方都打掉四成血以上就各自收手，不必真打满十二回合 */
        private void checkDrawProgression() {
            if (!forceDraw || earlyEndReason != null || round < 3) {
                return;
            }
            if ((double) a.hp / a.hpMax < 0.62 && (double) b.hp / b.hpMax < 0.62) {
                earlyEndReason = "DRAW";
            }
        }

        private Outcome outcome() {
            if ("ESCAPE".equals(earlyEndReason)) {
                Combatant fleeing = Objects.equals(escapeId, a.userId()) ? a : b;
                Combatant other = fleeing == a ? b : a;
                return new Outcome("ESCAPE", other.userId(),
                        "无人落败 · " + fleeing.c.name() + " 全身而退",
                        String.format("%s没有硬拼那一记，一道遁光退到百丈之外。%s要追，人已经没了影子 —— "
                                        + "人界篇里活到最后的人，多半不是最能打的那一个。",
                                fleeing.c.name(), other.c.name()),
                        "境界差一级 + 遁术冠绝：按「打不过就走」收场",
                        fleeing.c.basisNote(), round, round2(a.power), round2(b.power));
            }
            if (canon != null) {
                if (forceDraw) {
                    return new Outcome("DRAW", null, "平手 · 此战不成", canon.verdict(),
                            "原著战绩覆盖", canon.canonSource(), round, round2(a.power), round2(b.power));
                }
                Combatant winner = Objects.equals(canonWinnerId, a.userId()) ? a : b;
                Combatant loser = winner == a ? b : a;
                return new Outcome("WIN", winner.userId(), winner.c.name() + " 胜", canon.verdict(),
                        "原著战绩覆盖", canon.canonSource(), round, round2(a.power), round2(b.power));
            }
            if (a.hp <= 0 || b.hp <= 0) {
                Combatant winner = a.hp > 0 ? a : b;
                return win(winner, winner == a ? b : a, "数值推演");
            }
            double ra = (double) a.hp / a.hpMax;
            double rb = (double) b.hp / b.hpMax;
            if (Math.abs(ra - rb) < 0.12) {
                return new Outcome("DRAW", null, "平手 · 各自收手",
                        String.format("%d 回合打完，谁也没能把谁留下。%s真元气血剩 %d%%，%s剩 %d%% —— "
                                        + "再打下去就不是斗法，是比谁先把寿元烧完。",
                                round, a.c.name(), Math.round(ra * 100), b.c.name(), Math.round(rb * 100)),
                        "同境界相持，按剩余气血与续航判平",
                        a.c.basisNote() + " / " + b.c.basisNote(), round, round2(a.power), round2(b.power));
            }
            return win(ra > rb ? a : b, ra > rb ? b : a, "回合耗尽 · 按剩余气血判定");
        }

        private Outcome win(Combatant winner, Combatant loser, String method) {
            int gap = winner.c.realm().idx() - loser.c.realm().idx();
            String verdict;
            if (loser.c.realm().idx() == 0 && winner.c.realm().idx() >= 2) {
                verdict = String.format("%s是凡人之躯，站在%s面前不是输在勇气上 —— 没有灵根这件事，"
                        + "人界篇里没人能替他改。", loser.c.name(), winner.c.name());
            } else if (gap >= 2) {
                verdict = String.format("%s（%s）对%s（%s）差着两个大境界。这不是法力量的差别，是换了种活法："
                                + "%s一出手，%s得把「撑过三息」当成一件正经事来打算。",
                        winner.c.name(), winner.c.realm().getLabel(), loser.c.name(), loser.c.realm().getLabel(),
                        winner.c.name(), loser.c.name());
            } else if (gap == 1) {
                verdict = String.format("%s（%s）压%s（%s）一整级。一个的路数是「%s」，另一个是「%s」—— "
                                + "境界这道天堑，从来不是靠勇气填的。",
                        winner.c.name(), winner.c.realm().getLabel(), loser.c.name(), loser.c.realm().getLabel(),
                        winner.c.style().getEssence(), loser.c.style().getEssence());
            } else {
                verdict = String.format("同一境界比的是小层、法宝、神识，以及谁先沉不住气。%s（%s）以「%s」压过"
                                + "%s（%s）。%s自己那句话倒是说准了：%s",
                        winner.c.name(), winner.subLabel(), winner.c.style().getLabel(),
                        loser.c.name(), loser.subLabel(), loser.c.name(), loser.c.quote());
            }
            return new Outcome(gap >= 2 ? "CRUSH" : "WIN", winner.userId(),
                    winner.c.name() + " 胜", verdict, method,
                    winner.c.basisNote() + " / " + loser.c.basisNote(), round,
                    round2(winner.power), round2(loser.power));
        }

        private void push(Combatant actor, Combatant foe, String name, String kind, String vfx, String line,
                          long damage, long heal, boolean crit, boolean ult, boolean skipped) {
            events.add(new Event(++seq, round, actor.userId(), foe.userId(), name, kind, vfx, line,
                    damage, heal, crit, ult, skipped, a.hp, a.mp, b.hp, b.mp));
        }
    }

    // ==================== 视图 ====================

    private FighterView view(Combatant c, Map<Long, String> avatars) {
        Cultivator x = c.c;
        return new FighterView(x.userId(), x.name(),
                avatars == null ? null : avatars.get(x.userId()), "/user/" + x.userId(),
                x.realm().name(), x.realm().getLabel(), x.realm().getColor(), x.subText(),
                x.title(), x.origin(), x.technique(),
                x.style().name(), x.style().getLabel(), x.style().getEssence(),
                x.traits().stream().map(Trait::getLabel).toList(),
                round2(x.power()), c.hpMax, c.mpMax,
                x.basis().name(), x.basisNote(), x.quote(), x.skills());
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 运行时状态：气血灵力由倍率折算成绝对值，整局在上面加减 */
    private static final class Combatant {
        final Cultivator c;
        long hp, hpMax, mp, mpMax;
        double atk, def, spd, sense, escape, endurance, power;
        int shieldTurns, stunTurns, atkDebuffTurns, evadeTurns;
        boolean ultUsed;
        Skill lastSkill;

        Combatant(Cultivator c) {
            CombatStats s = c.stats();
            this.c = c;
            this.power = c.power();
            this.hpMax = Math.round(HP_SCALE * s.hpMul);
            this.mpMax = Math.round(1000 * s.mpMul);
            this.hp = hpMax;
            this.mp = mpMax;
            this.atk = power * s.atkMul;
            this.def = power * s.defMul;
            this.spd = 100 * s.spdMul;
            this.sense = 100 * s.senseMul;
            this.escape = 100 * s.escapeMul;
            this.endurance = 100 * s.enduranceMul;
        }

        Long userId() {
            return c.userId();
        }

        String subLabel() {
            return c.realm().getLabel() + c.subText();
        }
    }
}
