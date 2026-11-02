/**
 * 斗法台（人界篇）—— 选人 → 服务端推演 → 前端演播
 *
 * 职责边界：谁赢、怎么赢，全部由服务端 DuelEngine 裁决，本文件只负责「演得像」：
 *   1) 人物立影：SVG 道影 + 头像为首，会蓄力、扑招、受击、格挡、遁走、倒地；
 *   2) 报招：施法时人物自己念招名（Web Speech 逐人物配音调语速，系统没中文语音就退化为气泡）；
 *   3) 特效：Canvas 粒子 / 剑气 / 法阵 / 冲击波，时长按「看得清」调，不放完就散；
 *   4) 音效：WebAudio 现场合成，零外部素材。
 * 数据只吃 /api/rank/roster 与 /api/rank/duel，这里不写任何修仙设定数字。
 */
(function () {
    'use strict';

    const D = {
        realms: [], people: [], byId: new Map(), styles: {}, realmIdx: {},
        pickA: null, pickB: null,
        speed: 1, voice: true, sfx: true,
        playing: false, timers: []
    };

    const $ = id => document.getElementById(id);
    const later = (fn, ms) => { const t = setTimeout(fn, ms); D.timers.push(t); return t; };
    const clearTimers = () => { D.timers.forEach(clearTimeout); D.timers = []; };
    const avatarOf = p => (p && p.avatar) ? p.avatar : '/images/default-avatar.svg';

    // 一拍时长（毫秒）：普通招式、神通大招、自守疗伤，再乘语音长度，保证「说完看完」才收
    const BASE_MS = 1900;      // 攻招
    const ULT_MS = 3300;       // 神通：自带慢镜
    const SOFT_MS = 1600;      // 守 / 疗 / 遁
    const CHARGE_MS = 640;     // 起手蓄力（报招在这一步开始）
    const FLIGHT_MS = 330;     // 出手到命中
    const TAIL_MS = 620;       // 命中后的收势余韵

    // 武器字由路数决定；用汉字而不是 emoji，各系统渲染一致、不会变成彩色图标
    const GLYPH = {
        SWORD: '剑', QI: '气', ICE: '冰', MIND: '神', YIN: '阴', DEMON: '魔', BODY: '骨',
        CORPSE: '尸', SOUND: '音', ARRAY: '阵', MAGNET: '磁', LAW: '法', TURTLE: '盾',
        BEAST: '妖', WOOD: '木', ILLUSION: '迷', POISON: '毒', GUARD: '守', MORTAL: '拳'
    };

    document.addEventListener('DOMContentLoaded', () => {
        Fx.init();
        Stage.init();
        Speech.init();
        bindControls();
        loadRoster();
    });

    // ==================== 数据 ====================
    async function loadRoster() {
        try {
            const data = await http.get('/rank/roster');
            D.realms = data.realms || [];
            D.people = data.people || [];
            D.styles = data.styles || {};
            D.realmIdx = {};
            D.realms.forEach((r, i) => { D.realmIdx[r.realm] = i; });
            D.byId = new Map(D.people.map(p => [p.userId, p]));
            $('dlUpdated').textContent = `${data.scope || '人界篇'} · ${data.total || D.people.length} 人`;
            renderChips('rosterA', '');
            renderChips('rosterB', '');
            renderStyleGuide();
            renderRealmLegend();
            // 默认给一对同境界的化神：这两个人打得起十几合，动作、报招、特效一次看全
            // （配「差两级」的那对只有一合，一上来看不出演出效果）
            autoPick(101, 118);
        } catch (e) {
            $('dlUpdated').textContent = '名录加载失败：' + e.message;
            $('dlStageHint').textContent = '名录加载失败：' + e.message;
        }
    }

    function autoPick(a, b) {
        if (D.byId.has(a) && D.byId.has(b)) {
            setPick('A', a);
            setPick('B', b);
        }
    }

    // ==================== 选人 ====================
    function renderChips(boxId, keyword) {
        const box = $(boxId);
        const kw = (keyword || '').trim().toLowerCase();
        const list = D.people.filter(p => !kw
            || (p.name + p.realmLabel + p.technique + (p.origin || '') + (p.styleLabel || '')).toLowerCase().includes(kw));
        box.innerHTML = list.map(p => `
            <span class="dl-chip${isPicked(p.userId) ? ' picked' : ''}" data-id="${p.userId}"
                  title="${p.realmLabel}${p.subText || ''} · ${p.technique}">
                <i class="dl-dot" style="background:${p.realmColor}"></i>${p.name}</span>`).join('')
            || '<span class="dl-chip">没有匹配的人</span>';
        box.querySelectorAll('.dl-chip[data-id]').forEach(el =>
            el.addEventListener('click', () => setPick(boxId.endsWith('A') ? 'A' : 'B', +el.dataset.id)));
    }

    function isPicked(id) { return D.pickA === id || D.pickB === id; }

    function setPick(side, id) {
        if (D.playing) return;
        if (side === 'A' && D.pickB === id) D.pickB = null;
        if (side === 'B' && D.pickA === id) D.pickA = null;
        D['pick' + side] = id;
        paintPick(side);
        Stage.paint(side);
        updateActionState();
    }

    function paintPick(side) {
        const p = D.byId.get(D['pick' + side]);
        const box = $('picked' + side), wrap = $('pick' + side);
        wrap.classList.toggle('ready', !!p);
        wrap.style.setProperty('--pc', p ? p.realmColor : '#6366f1');
        box.className = 'dl-picked' + (p ? '' : ' empty');
        box.innerHTML = p ? `
            <img src="${avatarOf(p)}" alt="${p.name}"
                 onerror="this.onerror=null;this.src='/images/default-avatar.svg'">
            <div>
                <div class="dl-pk-name">${p.name}</div>
                <div class="dl-pk-realm">${p.realmLabel}${p.subText || ''} · ${p.styleLabel || ''} · 战力 ${p.power}</div>
            </div>` : '尚未选人';
        const tip = $('dlMatchTip');
        if (D.pickA && D.pickB) {
            const a = D.byId.get(D.pickA), b = D.byId.get(D.pickB);
            const gap = Math.abs((D.realmIdx[a.realm] ?? 0) - (D.realmIdx[b.realm] ?? 0));
            tip.textContent = gap >= 2
                ? `差 ${gap} 个大境界 —— 这不是对决，是「顺手清了」；想看以弱胜强，去点榜单里原著明载那几对。`
                : gap === 1
                    ? '差一个大境界 —— 有底牌、遁术高的人才有得谈。'
                    : '同境界 —— 拼小层、功法克制与底牌，最能看出谁把日子花在哪。';
        } else {
            tip.textContent = '先选两位 —— 同境界拼小层与底牌，差两级基本无解。';
        }
    }

    function updateActionState() {
        const btn = $('dlFight');
        const ready = !!(D.pickA && D.pickB);
        btn.disabled = !ready || D.playing;
        btn.textContent = ready
            ? `⚔ ${D.byId.get(D.pickA).name} vs ${D.byId.get(D.pickB).name}`
            : '⚔ 开始斗法';
    }

    // ==================== 人物立影 ====================
    const Stage = {
        init() {
            ['A', 'B'].forEach(side => {
                $('fig' + side).innerHTML = figureHtml();
                $('fg' + side).innerHTML = `
                    <div class="dl-f-name"><span id="dlName${side}">未选人</span>
                        <em class="dl-f-realm" id="dlRealm${side}"></em></div>
                    <div class="dl-bar hp" id="dlHp${side}"><i style="width:100%"></i><span></span></div>
                    <div class="dl-bar mp" id="dlMp${side}"><i style="width:100%"></i></div>`;
            });
        },

        /** 手臂必须用 fill-box 以「自己的」包围盒算轴心，否则 transform-origin 会按整个 svg 的框定位，抬臂会甩飞 */
        pose(side, cls, ms) {
            const fig = $('fig' + side);
            if (!fig) return;
            fig.classList.remove('cast', 'attack', 'hurt', 'guard', 'flee', 'down', 'win');
            void fig.offsetWidth;
            if (cls) fig.classList.add(cls);
            if (cls && ms) later(() => fig.classList.remove(cls), ms);
        },

        array(side, on) {
            const el = $('fig' + side).querySelector('.dl-array');
            if (el) el.classList.toggle('on', on);
        },

        bubble(side, html, ult, ms) {
            const el = $('fig' + side).querySelector('.dl-bubble');
            if (!el) return;
            el.innerHTML = html;
            el.classList.toggle('ult', !!ult);
            el.classList.add('on');
            if (ms) later(() => el.classList.remove('on'), ms);
        },

        talk(side, on) { $('fig' + side).classList.toggle('talk', on); },

        /** 血条在命中那一刻才更新，提前更新就看不出这一刀的分量 */
        bars(ev, maxA, maxB) {
            set('A', ev.hpA, ev.mpA, maxA);
            set('B', ev.hpB, ev.mpB, maxB);
            function set(side, hp, mp, max) {
                const hpBar = $('dlHp' + side), mpBar = $('dlMp' + side);
                if (!hpBar || !mpBar) return;
                const pct = Math.max(0, Math.min(100, hp / max * 100));
                hpBar.querySelector('i').style.width = pct + '%';
                hpBar.querySelector('span').textContent = Math.round(hp) + ' / ' + max;
                hpBar.classList.toggle('low', pct < 32);
                mpBar.querySelector('i').style.width = Math.max(0, Math.min(100, mp / Math.max(1, max * 0.45) * 100)) + '%';
            }
        },

        paint(side) {
            const p = D.byId.get(D['pick' + side]);
            const fig = $('fig' + side);
            fig.innerHTML = figureHtml();
            fig.style.setProperty('--pc', p ? p.realmColor : '#64748b');
            fig.style.setProperty('--flip', side === 'A' ? '1' : '-1');
            if (!p) return;
            const head = fig.querySelector('.dl-head');
            head.src = avatarOf(p);
            head.onerror = () => { head.onerror = null; head.src = '/images/default-avatar.svg'; };
            fig.querySelector('.dl-weapon').textContent = GLYPH[p.style] || '诀';
            fig.querySelector('.dl-nameplate').innerHTML =
                `${p.name}<br><span>${p.realmLabel}${p.subText || ''}</span>`;
        },

        header(side, p, hpMax, mpMax) {
            $('dlName' + side).textContent = p.name;
            $('dlName' + side).style.color = p.realmColor || '#e2e8f0';
            $('dlRealm' + side).textContent = `${p.styleLabel || ''} · 气血 ${hpMax} · 灵力 ${mpMax}`;
        }
    };

    function figureHtml() {
        return `
            <div class="dl-aura"></div>
            <div class="dl-array"></div>
            <div class="dl-shadow"></div>
            <div class="dl-visual">
                <svg class="dl-body" viewBox="0 0 120 200" aria-hidden="true">
                    <g class="torso">
                        <g class="arm-b"><path class="limb" d="M50 92 q-12 20 -12 44 l16 -1 q1 -22 6 -40 z"/></g>
                        <path class="robe2" d="M42 94 Q30 148 20 196 L100 196 Q88 148 78 94 Z"/>
                        <path class="robe" d="M47 94 Q41 148 33 192 L87 192 Q79 148 73 94 Z"/>
                        <path class="sash" d="M44 122 h32 v10 h-32 z"/>
                        <path class="limb" d="M52 74 q8 -10 16 0 l-2 22 h-12 z"/>
                        <path class="ribbon rb1" d="M80 112 Q108 132 98 166"/>
                        <path class="ribbon rb2" d="M40 114 Q12 134 24 168"/>
                        <g class="arm-f"><path class="limb" d="M66 92 q16 14 22 38 l-16 6 q-6 -22 -16 -34 z"/></g>
                    </g>
                </svg>
                <div class="dl-headwrap"><img class="dl-head" src="/images/default-avatar.svg" alt=""></div>
                <div class="dl-weapon">诀</div>
            </div>
            <div class="dl-bubble"></div>
            <div class="dl-nameplate"></div>`;
    }

    // ==================== 报招语音 ====================
    const Speech = {
        ok: typeof window !== 'undefined' && 'speechSynthesis' in window,
        zh: null,

        init() {
            if (!this.ok) {
                this.warn('这个浏览器没有语音合成，报招改用气泡（不影响斗法）。');
                return;
            }
            const pick = () => {
                const vs = window.speechSynthesis.getVoices() || [];
                this.zh = vs.find(v => /^zh([-_]|$)/i.test(v.lang)) || null;
                $('dlVoiceState').textContent = this.zh ? '语音 · ' + this.zh.name : '未装中文语音包，报招走气泡';
                if (!this.zh) this.warn('系统里没有中文语音包 —— 人物仍会「张口」，招名以气泡呈现。');
            };
            pick();
            window.speechSynthesis.onvoiceschanged = pick;
        },

        warn(msg) { const el = $('dlVoiceWarn'); el.textContent = msg; el.style.display = 'block'; },

        /** 喝声 = 人物口气前缀 + 招名；招名里的分隔符换成逗号，念着才不像报菜名 */
        line(p, ev) {
            const v = (p && p.voice) || {};
            const skill = String(ev.skill || '').replace(/[·（）()]/g, ' ').replace(/\s+/g, ' ').trim();
            const head = ev.skipped ? '' : (v.shout || '');
            return (head ? head + '，' : '') + skill;
        },

        /** 返回预计说话时长（毫秒），排时间轴时用它把这一拍拉长 */
        estMs(p, text) {
            if (!this.usable()) return 0;
            const rate = ((p.voice || {}).rate || 0.95);
            return Math.round(text.length * 250 / rate);
        },

        usable() { return D.voice && this.ok && !!this.zh; },

        say(p, text) {
            if (!this.usable()) return;
            try {
                window.speechSynthesis.cancel();
                const u = new SpeechSynthesisUtterance(text);
                const v = (p && p.voice) || {};
                u.voice = this.zh;
                u.lang = this.zh.lang;
                u.pitch = clampNum(v.pitch || 1, 0.4, 1.8);
                u.rate = clampNum((v.rate || 0.95) * (D.speed >= 1.7 ? 1.12 : 1), 0.6, 1.6);
                u.volume = 0.95;
                window.speechSynthesis.speak(u);
            } catch (e) { /* 语音失败不影响演出，气泡与字幕还在 */ }
        },

        halt() { if (this.ok) { try { window.speechSynthesis.cancel(); } catch (e) { /* ignore */ } } }
    };

    function clampNum(v, min, max) { return Math.max(min, Math.min(max, v)); }

    // ==================== 音效（WebAudio 现场合成） ====================
    const Sfx = {
        ctx: null,

        ready() {
            if (!D.sfx) return false;
            if (!this.ctx) {
                const AC = window.AudioContext || window.webkitAudioContext;
                if (!AC) return false;
                this.ctx = new AC();
            }
            if (this.ctx.state === 'suspended') this.ctx.resume();
            return true;
        },

        noise(dur, from, to, gain) {
            if (!this.ready()) return;
            const c = this.ctx, n = Math.floor(c.sampleRate * dur), buf = c.createBuffer(1, n, c.sampleRate);
            const d = buf.getChannelData(0);
            for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / n);
            const src = c.createBufferSource(); src.buffer = buf;
            const f = c.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 1.1;
            f.frequency.setValueAtTime(from, c.currentTime);
            f.frequency.exponentialRampToValueAtTime(Math.max(60, to), c.currentTime + dur);
            const g = c.createGain();
            g.gain.setValueAtTime(gain, c.currentTime);
            g.gain.exponentialRampToValueAtTime(0.0008, c.currentTime + dur);
            src.connect(f); f.connect(g); g.connect(c.destination); src.start();
        },

        tone(freq, dur, type, gain, slideTo) {
            if (!this.ready()) return;
            const c = this.ctx, o = c.createOscillator(), g = c.createGain();
            o.type = type || 'sine';
            o.frequency.setValueAtTime(freq, c.currentTime);
            if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, c.currentTime + dur);
            g.gain.setValueAtTime(gain, c.currentTime);
            g.gain.exponentialRampToValueAtTime(0.0008, c.currentTime + dur);
            o.connect(g); g.connect(c.destination); o.start(); o.stop(c.currentTime + dur);
        },

        charge() { this.tone(210, 0.52, 'triangle', 0.045, 640); },
        swing(ult) { this.noise(ult ? 0.52 : 0.32, 1900, 320, ult ? 0.2 : 0.13); },
        hit(ult, crit) {
            this.noise(ult ? 0.58 : 0.32, 900, 120, ult ? 0.28 : 0.18);
            this.tone(ult ? 82 : 128, ult ? 0.5 : 0.3, 'square', crit ? 0.11 : 0.075, 44);
        },
        guard() { this.tone(520, 0.34, 'sine', 0.09, 300); this.noise(0.18, 2600, 900, 0.07); },
        heal() { this.tone(660, 0.46, 'sine', 0.075, 990); },
        flee() { this.noise(0.55, 2600, 480, 0.11); },
        stall() { this.tone(150, 0.4, 'sawtooth', 0.05, 96); },
        verdict(win) { this.tone(win ? 392 : 196, 0.75, 'triangle', 0.1, win ? 588 : 132); }
    };

    // ==================== 控件 ====================
    function bindControls() {
        $('dlFight').addEventListener('click', start);
        $('dlReset').addEventListener('click', teardown);
        $('dlRandom').addEventListener('click', randomPair);
        $('dlVoice').addEventListener('change', e => { D.voice = e.target.checked; if (!D.voice) Speech.halt(); });
        $('dlSfx').addEventListener('change', e => D.sfx = e.target.checked);
        $('dlSpeed').addEventListener('click', e => {
            const b = e.target.closest('button');
            if (!b) return;
            D.speed = +b.dataset.speed;
            $('dlSpeed').querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b));
        });
        ['A', 'B'].forEach(side =>
            $('search' + side).addEventListener('input', e => renderChips('roster' + side, e.target.value)));
        // 点判词遮罩即可关掉，方便接着看立影
        $('dlVerdict').addEventListener('click', () => $('dlVerdict').classList.remove('show'));
    }

    function randomPair() {
        if (D.playing || D.people.length < 2) return;
        const a = D.people[Math.floor(Math.random() * D.people.length)];
        // 三成概率配同境界对手，否则总在看化神屠凡人
        const same = Math.random() < 0.3;
        const pool = same
            ? D.people.filter(p => (D.realmIdx[p.realm] ?? -1) === D.realmIdx[a.realm] && p.userId !== a.userId)
            : D.people.filter(p => p.userId !== a.userId);
        const b = pool[Math.floor(Math.random() * pool.length)];
        if (!b) return;
        setPick('A', a.userId);
        setPick('B', b.userId);
    }

    // ==================== 主流程 ====================
    async function start() {
        if (!D.pickA || !D.pickB || D.playing) return;
        const btn = $('dlFight');
        D.playing = true;
        btn.disabled = true;
        btn.textContent = '推演中…';
        try {
            const d = await http.get('/rank/duel', { a: D.pickA, b: D.pickB });
            run(d);
        } catch (e) {
            D.playing = false;
            updateActionState();
            $('dlStageHint').style.display = '';
            $('dlStageHint').textContent = '推演失败：' + e.message;
        }
    }

    function teardown() {
        D.playing = false;
        clearTimers();
        Speech.halt();
        Fx.reset();
        $('dlVerdict').classList.remove('show');
        $('dlLog').innerHTML = '';
        $('dlRoundTag').textContent = '待 手';
        $('dlSeedTip').textContent = '';
        $('dlStageHint').style.display = '';
        $('dlStageHint').textContent = '选好两位，点下方「开始斗法」。同一对人种子相同，招式序列必然相同。';
        ['A', 'B'].forEach(s => {
            Stage.pose(s, null);
            Stage.array(s, false);
            Stage.talk(s, false);
            const hp = $('dlHp' + s), mp = $('dlMp' + s);
            if (hp) {
                hp.querySelector('i').style.width = '100%';
                hp.querySelector('span').textContent = '';
                hp.classList.remove('low');
            }
            if (mp) mp.querySelector('i').style.width = '100%';
        });
        updateActionState();
    }

    /** 逐招累加时长排成时间轴，再按倍速缩放；演出与调度都在这里，不在 Fx 里 */
    function run(d) {
        const events = d.events || [], outcome = d.outcome || {};
        const A = merge(d.a), B = merge(d.b);
        const maxA = A.hpMax, maxB = B.hpMax;

        clearTimers();
        Fx.reset();
        Speech.halt();
        $('dlLog').innerHTML = '';
        $('dlVerdict').classList.remove('show');
        $('dlStageHint').style.display = 'none';
        $('dlRoundTag').textContent = '第 1 合';
        const gap = Math.abs((D.realmIdx[A.realm] ?? 0) - (D.realmIdx[B.realm] ?? 0));
        $('dlSeedTip').textContent = `seed ${d.seed} · ${gap === 0 ? '同境界' : '差 ' + gap + ' 级'} · `
            + (outcome.method === '原著战绩覆盖' ? '原著战绩覆盖' : '本榜推定');

        Stage.header('A', A, maxA, A.mpMax);
        Stage.header('B', B, maxB, B.mpMax);
        ['A', 'B'].forEach(s => { Stage.pose(s, null); Stage.array(s, false); });
        Stage.bars({ hpA: maxA, mpA: A.mpMax, hpB: maxB, mpB: B.mpMax }, maxA, maxB);

        const sideOf = id => (id === A.userId ? 'A' : id === B.userId ? 'B' : null);
        const personOf = side => (side === 'A' ? A : B);
        const foeOf = side => (side === 'A' ? 'B' : 'A');

        const scale = 1 / D.speed;
        let t = 360 * scale;
        events.forEach(ev => {
            const actor = sideOf(ev.actorId);
            if (!actor) return;
            const dur = rawDur(ev, personOf(actor)) * scale;
            later(() => playEvent({
                ev, actor, foe: foeOf(actor), p: personOf(actor), maxA, maxB, dur
            }), t);
            t += dur;
        });
        later(() => showOutcome(outcome, A, B, sideOf), t + 300 * scale);
        later(() => { D.playing = false; updateActionState(); }, t + 1100 * scale);
    }

    /** 名录里带昵称、头像与配音设定，推演结果里才有的气血上限 —— 合成一个完整的打斗对象 */
    function merge(fighter) {
        const base = D.byId.get(fighter.id) || {};
        return Object.assign({}, base, fighter, {
            userId: fighter.id,
            avatar: fighter.avatar || base.avatar || null,
            voice: base.voice || null
        });
    }

    /** 这一拍留多久：取「动作时长」与「把这句话说完」的较大者，招式名就不会说一半就没了 */
    function rawDur(ev, p) {
        const spoken = Speech.usable() ? Speech.estMs(p, Speech.line(p, ev)) : 0;
        const base = ev.ult ? ULT_MS : (ev.kind === 'ATTACK' || ev.kind === 'ULT' ? BASE_MS : SOFT_MS);
        return Math.max(base, spoken + 520) + (ev.crit ? 320 : 0) + (ev.skipped ? -300 : 0);
    }

    /** 一招四段：蓄力报招 → 出手 → 命中 → 收势 */
    function playEvent(ctx) {
        const { ev, actor, foe, p, maxA, maxB, dur } = ctx;
        const ult = !!ev.ult;
        const stage = $('dlStage');
        const offense = ev.kind === 'ATTACK' || ev.kind === 'ULT' || ev.kind === 'CONTROL';
        const defense = ev.kind === 'DEFENSE';
        const heal = ev.kind === 'HEAL';
        const move = ev.kind === 'MOBILITY' || ev.kind === 'ESCAPE';
        const inv = 1 / D.speed;

        // ① 蓄力：抬手上抬、脚底起阵，人物先喝出声
        $('dlRoundTag').textContent = `第 ${ev.round} 合 · ${ult ? '神通' : ev.skipped ? '失神' : '招式'}`;
        Stage.pose(actor, 'cast');
        Stage.array(actor, true);
        const shout = Speech.line(p, ev);
        if (ev.skipped) {
            Stage.bubble(actor, '<b>……真元逆乱</b><small>这一手慢了半拍</small>', false, Math.max(900, dur * 0.6));
            Sfx.stall();
        } else {
            Stage.bubble(actor, `<b>${trimSkill(ev.skill)}</b><small>${p.name} · ${(p.voice || {}).timbre || '喝声'}</small>`,
                ult, Math.max(1000, dur * 0.78));
            Speech.say(p, shout);
            Stage.talk(actor, true);
            Sfx.charge();
            Fx.charge(Fx.pos(actor), p.realmColor, ult);
        }
        showSkillName(ev, ult);
        if (ult) {
            stage.classList.add('zoom');
            later(() => stage.classList.remove('zoom'), Math.min(1200, dur * 0.7));
        }

        // ② 出手：扑身 + 飞行物
        later(() => {
            if (!ev.skipped) {
                Stage.pose(actor, 'attack');
                Sfx.swing(ult);
                if (offense) Fx.projectile(ev, actor, foe, p.realmColor, ult);
                else if (defense) { Fx.defense(Fx.pos(actor), p.realmColor, ev.vfx); Sfx.guard(); }
                else if (heal) { Fx.heal(Fx.pos(actor), ev.vfx); Sfx.heal(); }
                else if (move) { Fx.move(Fx.pos(actor), actor, p.realmColor); Sfx.flee(); }
                else Fx.projectile(ev, actor, foe, p.realmColor, ult);
            }
        }, CHARGE_MS * inv);

        // ③ 命中：血条在这一刻才动
        later(() => {
            const struck = ev.damage > 0 && ev.targetId && ev.targetId !== p.userId ? foe : null;
            if (struck) {
                Stage.pose(struck, 'hurt');
                Fx.impact(Fx.pos(struck), ev, personOfSide(struck).realmColor, ult, !!ev.crit);
                Sfx.hit(ult, ev.crit);
                floatText(struck, '-' + Math.round(ev.damage), ev.crit ? 'crit' : '');
                shake(struck, ult, ev.crit);
            }
            if (ev.heal > 0) {
                floatText(actor, '+' + Math.round(ev.heal), 'heal');
                Fx.heal(Fx.pos(actor), ev.vfx);
            }
            if (defense) Stage.pose(actor, 'guard');
            if (move) Stage.pose(actor, 'flee');
            if (ev.kind === 'CONTROL' && !ev.damage) floatText(foe, ev.line ? '受制' : '乱了', '');
            Stage.bars(ev, maxA, maxB);
            pushLog(ev, p);
        }, (CHARGE_MS + FLIGHT_MS) * inv);

        // ④ 收势：回到待手，留一段静默给人看清战况
        later(() => {
            ['A', 'B'].forEach(s => {
                $('fig' + s).classList.remove('cast', 'attack', 'hurt', 'guard', 'talk');
            });
            Stage.talk(actor, false);
            Stage.array(actor, false);
        }, Math.max(600, dur - TAIL_MS * inv));
    }

    function personOfSide(side) {
        return side === 'A' ? (D.byId.get(D.pickA) || {}) : (D.byId.get(D.pickB) || {});
    }

    function trimSkill(skill) {
        const s = String(skill || '');
        return s.length > 16 ? s.slice(0, 15) + '…' : s;
    }

    function showSkillName(ev, ult) {
        const el = $('dlSkillName'), sub = $('dlSkillSub');
        el.textContent = ev.skipped ? '……失神' : trimSkill(ev.skill);
        el.classList.toggle('ult', !!ult);
        sub.textContent = ev.line || '';
        [el, sub].forEach(x => {
            x.classList.remove('play');
            void x.offsetWidth;
            // 人家用完一句话的时间，字得还在屏上：时长跟这一拍同样缩放
            x.style.animationDuration = ((ult ? 3.1 : 1.9) / D.speed) + 's';
            x.classList.add('play');
        });
    }

    function shake(struckSide, ult, crit) {
        const stage = $('dlStage');
        const cls = ult ? 'shake-hard' : (crit ? 'shake-hard' : 'shake');
        stage.classList.remove('shake', 'shake-hard');
        void stage.offsetWidth;
        stage.classList.add(cls);
        if (ult || crit) {
            stage.style.setProperty('--tc', struckSide === 'A' ? '#60a5fa' : '#f87171');
            stage.classList.add('tint');
            later(() => stage.classList.remove('tint'), 620);
        }
        later(() => stage.classList.remove(cls), ult ? 1100 : 620);
    }

    function floatText(side, text, cls) {
        const p = Fx.pos(side);
        const el = document.createElement('div');
        el.className = 'dl-dmg ' + (cls || '');
        el.textContent = text;
        el.style.left = (p.x + (Math.random() - 0.5) * 76) + 'px';
        el.style.top = (p.y - 130) + 'px';
        $('dlStage').appendChild(el);
        later(() => el.remove(), 1700);
    }

    function pushLog(ev, p) {
        const box = $('dlLog');
        const div = document.createElement('div');
        div.className = 'dl-log-item' + (ev.ult ? ' ult' : '');
        div.innerHTML = `
            <span class="dl-lg-r">${ev.round}</span>
            <span class="dl-lg-n" style="color:${p.realmColor}">${p.name}</span>
            <span class="dl-lg-t">${ev.skipped ? '真元逆乱 · 失神' : trimSkill(ev.skill)}
                ${ev.damage ? ` —— ${Math.round(ev.damage)} 点${ev.crit ? '（要害）' : ''}` : ''}
                ${ev.heal ? ` —— 回 ${Math.round(ev.heal)}` : ''}
                ${ev.kind === 'ESCAPE' ? ' —— 遁走' : ''}
                <br><i style="opacity:.62">${ev.line || ''}</i></span>`;
        box.prepend(div);
        box.scrollTop = 0;
    }

    // ==================== 落幕判词 ====================
    function showOutcome(o, A, B, sideOf) {
        const head = $('dlVerdictHead'), body = $('dlVerdictBody'), src = $('dlVerdictSrc');
        const winSide = o.winnerId ? sideOf(o.winnerId) : null;
        const loseSide = winSide ? (winSide === 'A' ? 'B' : 'A') : null;
        const tag = { CRUSH: '碾压', WIN: '胜', ESCAPE: '全身而退', DRAW: '平手' }[o.type] || '';
        if (o.type === 'DRAW') {
            head.textContent = o.headline || '平手 · 各自收手';
            ['A', 'B'].forEach(s => Stage.pose(s, 'hurt'));
        } else {
            head.textContent = `${o.headline || ''} ｜ ${tag}`;
            if (winSide) { Stage.pose(winSide, 'win'); Stage.array(winSide, true); }
            if (loseSide && o.type !== 'ESCAPE') Stage.pose(loseSide, 'down');
            if (loseSide && o.type === 'ESCAPE') Stage.pose(loseSide, 'flee');
        }
        body.innerHTML = `<b>${o.verdict || ''}</b>`;
        src.innerHTML = `${o.method || ''} · 共 ${o.rounds} 合<br>
            <span style="opacity:.72">${o.basis || ''}</span>`;
        $('dlVerdict').classList.add('show');
        $('dlRoundTag').textContent = '已 分 胜 负';
        Sfx.verdict(o.type !== 'DRAW');
        Fx.finale((winSide === 'A' ? A : winSide === 'B' ? B : A).realmColor || '#a78bfa');
        $('dlStageHint').style.display = '';
        $('dlStageHint').textContent = '换个对手再来一局，或去「修仙排行榜」看所有人的境界与排名。';
        later(() => ['A', 'B'].forEach(s => Stage.array(s, false)), 1800);
    }

    // ==================== 说明区 ====================
    function renderStyleGuide() {
        const entries = Object.entries(D.styles || {});
        if (!entries.length) return;
        $('styleGuide').innerHTML = entries.map(([key, s]) => `
            <div class="dl-style">
                <b>${s.label || key}</b>
                <p>${s.essence || ''}</p>
                <div class="dl-line">克制 ${(s.beats || []).join('、') || '——'} <i>▲</i></div>
                <div class="dl-line bad">受制 ${(s.weak || []).join('、') || '——'} <i>▼</i></div>
            </div>`).join('');
    }

    function renderRealmLegend() {
        $('realmLegend').innerHTML = (D.realms || []).map(r =>
            `<span class="dl-realm" style="background:${r.color};opacity:${r.realm === 'MORTAL' ? 0.6 : 1}">
                ${r.label} · ${r.lifespan} · ${r.count} 人</span>`).join('');
    }

    // ==================== 特效引擎 ====================
    // 所有 life 都比「一闪而过」长一倍以上：得让人看清这一招是什么、从哪来到哪去
    const Fx = {
        canvas: null, ctx: null, w: 0, h: 0, parts: [], shapes: [], trails: [], running: false,

        init() {
            this.canvas = $('dlFx');
            this.ctx = this.canvas.getContext('2d');
            this.resize();
            window.addEventListener('resize', () => this.resize());
            this.seedMist();
            this.loop();
        },

        resize() {
            const stage = $('dlStage'), dpr = window.devicePixelRatio || 1;
            this.w = stage.clientWidth;
            this.h = stage.clientHeight;
            this.canvas.width = Math.round(this.w * dpr);
            this.canvas.height = Math.round(this.h * dpr);
            this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        },

        seedMist() {
            for (let i = 0; i < 34; i++) {
                this.parts.push({
                    kind: 'mist', x: Math.random() * (this.w || 800), y: Math.random() * (this.h || 460),
                    vx: (Math.random() - 0.5) * 0.2, vy: -0.09 - Math.random() * 0.15,
                    life: 1e9, max: 1e9, r: 14 + Math.random() * 34, hue: 232 + Math.random() * 44
                });
            }
        },

        pos(side) {
            return { x: (side === 'A' ? 0.2 : 0.8) * this.w, y: this.h - 92 };  // 与立影脚底齐平
        },

        reset() {
            this.parts = this.parts.filter(p => p.kind === 'mist');
            this.shapes = [];
            this.trails = [];
        },

        /** 蓄力：灵气由四面汇向人物，出手前就有前摇 */
        charge(p, color, ult) {
            const n = ult ? 36 : 22;
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                const r = (ult ? 210 : 140) * (0.5 + Math.random() * 0.7);
                this.parts.push({
                    kind: 'converge', x: p.x + Math.cos(a) * r, y: p.y - 50 + Math.sin(a) * r * 0.7,
                    tx: p.x, ty: p.y - 66, k: 0, speed: 0.02 + Math.random() * 0.02, cx: p.x, cy: p.y,
                    life: 86, max: 86, r: 1.6 + Math.random() * 2.4, color
                });
            }
            this.shapes.push({ kind: 'halo', x: p.x, y: p.y - 54, color, life: ult ? 110 : 76, max: ult ? 110 : 76, r: ult ? 158 : 108 });
            if (ult) this.shapes.push({ kind: 'rune', x: p.x, y: p.y - 6, color: '#fde68a', life: 120, max: 120, r: 120 });
        },

        projectile(ev, actor, foeSide, color, ult) {
            const from = this.pos(actor), to = this.pos(foeSide);
            const dir = actor === 'A' ? 1 : -1;
            this.trails.push({ x1: from.x, y1: from.y - 62, x2: to.x, y2: to.y - 58, color: color || '#cbd5e1', life: 40, max: 40 });
            switch (ev.vfx) {
                case 'SWORD_RAIN':
                    for (let i = 0; i < (ult ? 44 : 24); i++) {
                        this.parts.push({
                            kind: 'sword', x: to.x + (Math.random() - 0.5) * 200, y: -30 - Math.random() * 160,
                            vx: (Math.random() - 0.5) * 0.5, vy: 4.4 + Math.random() * 3.2,
                            life: 104, max: 104, color: '#c7d2fe'
                        });
                    }
                    this.shapes.push({ kind: 'rune', x: to.x, y: to.y - 70, color: '#a5b4fc', life: 96, max: 96, r: 132 });
                    break;
                case 'SLASH':
                    this.shapes.push({ kind: 'slash', x: to.x, y: to.y - 66, color: '#e0f2fe', life: 52, max: 52, dir, r: 88 });
                    this.shapes.push({ kind: 'slash', x: to.x - dir * 28, y: to.y - 36, color: '#bae6fd', life: 44, max: 44, dir, r: 64 });
                    break;
                case 'GOLD_BEAM':
                    this.shapes.push({
                        kind: 'beam', x1: from.x, y1: from.y - 62, x2: to.x, y2: to.y - 56,
                        color: '#fcd34d', life: 70, max: 70, width: ult ? 36 : 18
                    });
                    break;
                case 'FIRE_ORB':
                    this.shapes.push({ kind: 'orb', x1: from.x, y1: from.y - 62, x2: to.x, y2: to.y - 56, color: '#fb923c', life: 64, max: 64, r: 22 });
                    break;
                case 'ICE_FROST':
                    this.shapes.push({ kind: 'spike', x: to.x, y: to.y, color: '#a5f3fc', life: 96, max: 96, dir });
                    this.ring(to, '#67e8f9', ult ? 220 : 150, 92);
                    break;
                case 'MIND_WAVE':
                    this.ring(from, '#c4b5fd', 340, 104, 2);
                    this.ring(from, '#a78bfa', 250, 86, 2);
                    break;
                case 'SOUND_RING':
                    this.ring(from, '#f0abfc', 310, 100, 3);
                    this.ring(from, '#e879f9', 220, 84, 2);
                    this.ring(from, '#f5d0fe', 140, 68, 2);
                    break;
                case 'BLOOD_SHADOW':
                    this.shapes.push({ kind: 'ghostRun', x1: from.x, y1: from.y, x2: to.x, y2: to.y, color: '#f87171', life: 80, max: 80 });
                    break;
                case 'MAGNET_BEAM':
                    this.shapes.push({ kind: 'pull', x: to.x, y: to.y - 60, color: '#93c5fd', life: 96, max: 96 });
                    break;
                case 'ARRAY_PRESS':
                    this.shapes.push({ kind: 'grid', x: to.x, y: to.y - 46, color: '#fcd34d', life: 116, max: 116, r: 216 });
                    break;
                case 'POISON_MIST':
                    for (let i = 0; i < 28; i++) {
                        this.parts.push({
                            kind: 'mist', x: to.x + (Math.random() - 0.5) * 110, y: to.y - Math.random() * 70,
                            vx: (Math.random() - 0.5) * 0.55, vy: -0.26 - Math.random() * 0.4,
                            life: 150, max: 150, r: 18 + Math.random() * 26, hue: 108 + Math.random() * 32
                        });
                    }
                    break;
                case 'SUMMON':
                    for (let i = 0; i < (ult ? 32 : 20); i++) {
                        this.parts.push({
                            kind: 'rise', x: to.x + (Math.random() - 0.5) * 230, y: this.h,
                            vx: (Math.random() - 0.5) * 0.7, vy: -2.2 - Math.random() * 2.2,
                            life: 128, max: 128, r: 2 + Math.random() * 3.4, color: '#86efac'
                        });
                    }
                    break;
                case 'TURTLE_SHELL':
                    this.shapes.push({ kind: 'hex', x: from.x, y: from.y - 62, color: '#a3a380', life: 116, max: 116, r: 98 });
                    break;
                case 'SHIELD':
                    this.shapes.push({ kind: 'hex', x: from.x, y: from.y - 62, color: '#7dd3fc', life: 92, max: 92, r: 84 });
                    break;
                case 'MOON_GLOW':
                    this.shapes.push({ kind: 'bloom', x: this.w / 2, y: this.h * 0.2, color: '#e0f2fe', life: 120, max: 120, r: 128 });
                    break;
                case 'ULT_NOVA':
                    this.shapes.push({
                        kind: 'nova', x: to.x, y: to.y - 60, color: '#fef3c7',
                        life: 118, max: 118, r: Math.max(this.w, this.h) * 1.15
                    });
                    break;
                case 'QI_PALM':
                    // 肉掌推出去是一团压实的气，不是闪电：落点开花 + 地面涟漪
                    this.shapes.push({ kind: 'bloom', x: to.x, y: to.y - 62, color: color || '#e2e8f0', life: 74, max: 74, r: 98 });
                    this.shapes.push({ kind: 'shock', x: to.x, y: to.y + 8, color: color || '#cbd5e1', life: 66, max: 66, r: 196 });
                    break;
                case 'MORTAL_PUNCH':
                    // 凡人一拳：没有灵光，只有砸中的那一下裂意
                    this.shapes.push({ kind: 'crack', x: to.x, y: to.y - 62, color: '#fda4af', life: 56, max: 56 });
                    this.ring(to, '#fecaca', 126, 62, 2.4);
                    break;
                default:
                    this.shapes.push({
                        kind: 'bolt', x1: from.x, y1: from.y - 62, x2: to.x, y2: to.y - 56,
                        color: color || '#cbd5e1', life: 48, max: 48
                    });
            }
            if (ult) this.ring(from, '#ffffff', 270, 84, 2);
        },

        defense(p, color, vfx) {
            this.ring(p, color, 158, 86, 3);
            this.shapes.push({ kind: 'hex', x: p.x, y: p.y - 62, color, life: 96, max: 96, r: 92 });
            if (vfx === 'ARRAY_PRESS') this.shapes.push({ kind: 'grid', x: p.x, y: p.y - 34, color, life: 104, max: 104, r: 176 });
        },

        heal(p, vfx) {
            for (let i = 0; i < 24; i++) {
                this.parts.push({
                    kind: 'rise', x: p.x + (Math.random() - 0.5) * 130, y: p.y + 8,
                    vx: (Math.random() - 0.5) * 0.45, vy: -1.5 - Math.random() * 1.6,
                    life: 122, max: 122, r: 2 + Math.random() * 3,
                    color: vfx === 'HEAL_GLOW' ? '#4ade80' : '#a7f3d0'
                });
            }
            this.ring(p, '#86efac', 126, 84, 2);
        },

        move(p, side, color) {
            const dir = side === 'A' ? -1 : 1;
            for (let i = 0; i < 22; i++) {
                this.parts.push({
                    kind: 'streak', x: p.x + dir * i * 10, y: p.y - 24 - Math.random() * 100,
                    vx: dir * (2.2 + Math.random() * 1.8), vy: 0.15 + Math.random() * 0.35,
                    life: 76, max: 76, color, len: 14 + Math.random() * 22
                });
            }
        },

        impact(p, ev, color, ult, crit) {
            const n = ult ? 62 : crit ? 36 : 24;
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                const s = (ult ? 6 : crit ? 5 : 3.8) * (0.35 + Math.random() * 0.9);
                this.parts.push({
                    kind: 'spark', x: p.x, y: p.y - 62, vx: Math.cos(a) * s, vy: Math.sin(a) * s - 0.6,
                    life: (ult ? 124 : 86) * (0.6 + Math.random() * 0.7), max: ult ? 124 : 86,
                    r: 1.4 + Math.random() * 2.6, color: crit ? '#fbbf24' : (color || '#e2e8f0')
                });
            }
            this.ring(p, '#ffffff', ult ? 260 : 156, ult ? 86 : 60, ult ? 4 : 2.4);
            this.shapes.push({ kind: 'shock', x: p.x, y: p.y + 8, color: color || '#cbd5e1', life: 78, max: 78, r: ult ? 270 : 158 });
            if (ult) this.shapes.push({ kind: 'rune', x: p.x, y: p.y - 70, color: '#fde68a', life: 108, max: 108, r: 152 });
            if (crit) this.shapes.push({ kind: 'crack', x: p.x, y: p.y - 62, color: '#fecaca', life: 78, max: 78 });
        },

        ring(p, color, radius, life, width) {
            this.shapes.push({ kind: 'ring', x: p.x, y: p.y - 54, color, r: 12, maxR: radius, life, max: life, width: width || 3 });
        },

        finale(color) {
            for (let i = 0; i < 64; i++) {
                const a = Math.random() * Math.PI * 2;
                this.parts.push({
                    kind: 'spark', x: this.w / 2 + Math.cos(a) * 40, y: this.h / 2 + Math.sin(a) * 30,
                    vx: Math.cos(a) * (2 + Math.random() * 5), vy: Math.sin(a) * (2 + Math.random() * 4) - 1,
                    life: 160, max: 160, r: 1.6 + Math.random() * 3, color
                });
            }
            this.shapes.push({
                kind: 'nova', x: this.w / 2, y: this.h * 0.5, color: '#fde68a',
                life: 130, max: 130, r: Math.max(this.w, this.h)
            });
        },

        loop() {
            if (this.running) return;
            this.running = true;
            const ctx = this.ctx;
            const draw = () => {
                if (document.hidden) { this.running = false; setTimeout(() => this.loop(), 500); return; }
                ctx.clearRect(0, 0, this.w, this.h);
                this.stepMist(ctx);
                this.stepTrails(ctx);
                this.stepParts(ctx);
                this.stepShapes(ctx);
                requestAnimationFrame(draw);
            };
            requestAnimationFrame(draw);
        },

        stepMist(ctx) {
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.parts.filter(p => p.kind === 'mist').forEach(p => {
                p.x += p.vx; p.y += p.vy;
                if (p.y < -50) { p.y = this.h + 24; p.x = Math.random() * this.w; }
                if (p.x < -50) p.x = this.w + 24; else if (p.x > this.w + 50) p.x = -24;
                const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r);
                g.addColorStop(0, `hsla(${p.hue},80%,72%,0.15)`);
                g.addColorStop(1, 'hsla(230,60%,60%,0)');
                ctx.fillStyle = g;
                ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
            });
            ctx.restore();
        },

        stepTrails(ctx) {
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.trails = this.trails.filter(t => {
                t.life--;
                const k = Math.max(0, t.life / t.max);
                ctx.globalAlpha = k * 0.42;
                ctx.strokeStyle = t.color;
                ctx.lineWidth = 12 * k;
                ctx.beginPath(); ctx.moveTo(t.x1, t.y1); ctx.lineTo(t.x2, t.y2); ctx.stroke();
                return t.life > 0;
            });
            ctx.restore();
        },

        stepParts(ctx) {
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.parts.forEach(p => {
                if (p.kind === 'mist') return;
                p.life--;
                if (p.kind === 'converge') {
                    p.k = Math.min(1, p.k + p.speed);
                    const e = 1 - Math.pow(1 - p.k, 3);
                    p.cx = p.x + (p.tx - p.x) * e;
                    p.cy = p.y + (p.ty - p.y) * e;
                } else {
                    p.x += p.vx || 0; p.y += p.vy || 0;
                    if (p.kind === 'sword') p.vy += 0.09;
                    else if (p.kind === 'spark' || p.kind === 'streak') p.vy += 0.04;
                    else if (p.kind === 'rise') p.vy += 0.007;
                }
                const k = Math.max(0, p.life / p.max);
                ctx.globalAlpha = k;
                ctx.fillStyle = p.color; ctx.strokeStyle = p.color;
                const x = p.kind === 'converge' ? p.cx : p.x, y = p.kind === 'converge' ? p.cy : p.y;
                if (p.kind === 'streak') {
                    ctx.lineWidth = 2.4;
                    ctx.beginPath(); ctx.moveTo(x, y);
                    ctx.lineTo(x - (p.vx || 0) * p.len / 4, y - (p.vy || 0) * p.len / 4); ctx.stroke();
                } else if (p.kind === 'sword') {
                    ctx.lineWidth = 2.6;
                    ctx.beginPath(); ctx.moveTo(x, y);
                    ctx.lineTo(x - (p.vx || 0) * 3, y - (p.vy || 0) * 3.2); ctx.stroke();
                    ctx.beginPath(); ctx.arc(x, y, 2.2, 0, Math.PI * 2); ctx.fill();
                } else if (p.kind === 'spark') {
                    ctx.lineWidth = 1.8;
                    ctx.beginPath(); ctx.moveTo(x, y);
                    ctx.lineTo(x - (p.vx || 0) * 1.6, y - (p.vy || 0) * 1.6); ctx.stroke();
                    ctx.beginPath(); ctx.arc(x, y, p.r * k, 0, Math.PI * 2); ctx.fill();
                } else {
                    ctx.beginPath(); ctx.arc(x, y, p.r * (0.5 + k * 0.7), 0, Math.PI * 2); ctx.fill();
                }
            });
            this.parts = this.parts.filter(p => p.kind === 'mist' || p.life > 0);
            ctx.restore();
        },

        stepShapes(ctx) {
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.shapes = this.shapes.filter(s => {
                s.life--;
                const k = Math.max(0, s.life / s.max), t = 1 - k;
                ctx.globalAlpha = k;
                ctx.strokeStyle = s.color; ctx.fillStyle = s.color;
                switch (s.kind) {
                    case 'ring': {
                        ctx.lineWidth = s.width * (0.5 + k * 0.8);
                        ctx.beginPath();
                        ctx.arc(s.x, s.y, s.r + (s.maxR - s.r) * (1 - Math.pow(k, 1.6)), 0, Math.PI * 2);
                        ctx.stroke();
                        break;
                    }
                    case 'halo': {
                        const g = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, s.r * (1 - t * 0.15));
                        g.addColorStop(0, 'rgba(255,255,255,0.32)');
                        g.addColorStop(0.5, s.color);
                        g.addColorStop(1, 'rgba(0,0,0,0)');
                        ctx.fillStyle = g;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'beam': {
                        const w = s.width * (0.35 + k * 0.9);
                        const g = ctx.createLinearGradient(s.x1, s.y1, s.x2, s.y2);
                        g.addColorStop(0, 'rgba(255,255,255,0.05)');
                        g.addColorStop(0.45, s.color);
                        g.addColorStop(1, 'rgba(255,255,255,0.95)');
                        ctx.strokeStyle = g; ctx.lineWidth = w;
                        ctx.beginPath(); ctx.moveTo(s.x1, s.y1); ctx.lineTo(s.x2, s.y2); ctx.stroke();
                        ctx.lineWidth = w * 0.26; ctx.strokeStyle = '#fff'; ctx.stroke();
                        break;
                    }
                    case 'bolt': {
                        ctx.lineWidth = 3;
                        ctx.beginPath(); ctx.moveTo(s.x1, s.y1);
                        for (let i = 1; i <= 5; i++) {
                            const pr = i / 5;
                            ctx.lineTo(
                                s.x1 + (s.x2 - s.x1) * pr + (Math.random() - 0.5) * 26,
                                s.y1 + (s.y2 - s.y1) * pr + (Math.random() - 0.5) * 20);
                        }
                        ctx.stroke();
                        const x = s.x1 + (s.x2 - s.x1) * (1 - k), y = s.y1 + (s.y2 - s.y1) * (1 - k);
                        ctx.beginPath(); ctx.arc(x, y, 7, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'orb': {
                        const x = s.x1 + (s.x2 - s.x1) * (1 - k), y = s.y1 + (s.y2 - s.y1) * (1 - k);
                        const g = ctx.createRadialGradient(x, y, 0, x, y, s.r * 2.4);
                        g.addColorStop(0, '#fff'); g.addColorStop(0.35, s.color); g.addColorStop(1, 'rgba(249,115,22,0)');
                        ctx.fillStyle = g;
                        ctx.beginPath(); ctx.arc(x, y, s.r * 2.4, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'slash': {
                        ctx.lineWidth = 5 * k + 1;
                        for (let i = 0; i < 3; i++) {
                            ctx.beginPath();
                            ctx.arc(s.x, s.y, s.r - i * 16, -1.0 + i * 0.22, 1.15 + i * 0.22);
                            ctx.stroke();
                        }
                        break;
                    }
                    case 'spike': {
                        ctx.lineWidth = 3;
                        for (let i = -3; i <= 3; i++) {
                            const bx = s.x + i * 26, hh = 66 * k * (1 - Math.abs(i) / 5);
                            ctx.beginPath();
                            ctx.moveTo(bx, s.y); ctx.lineTo(bx + s.dir * 9, s.y - hh); ctx.stroke();
                        }
                        break;
                    }
                    case 'hex': {
                        ctx.lineWidth = 3;
                        for (let i = 0; i < 6; i++) {
                            const a = i / 6 * Math.PI * 2 + t * 1.4;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * s.r * 0.55, s.y + Math.sin(a) * s.r * 0.55);
                            ctx.lineTo(s.x + Math.cos(a) * s.r, s.y + Math.sin(a) * s.r);
                            ctx.stroke();
                        }
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r * 0.55, 0, Math.PI * 2); ctx.stroke();
                        break;
                    }
                    case 'rune': {
                        ctx.lineWidth = 2.2;
                        ctx.beginPath(); ctx.ellipse(s.x, s.y, s.r, s.r * 0.34, 0, 0, Math.PI * 2); ctx.stroke();
                        ctx.beginPath(); ctx.ellipse(s.x, s.y, s.r * 0.66, s.r * 0.22, 0, 0, Math.PI * 2); ctx.stroke();
                        for (let i = 0; i < 12; i++) {
                            const a = i / 12 * Math.PI * 2 + t * 2.2;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * s.r * 0.66, s.y + Math.sin(a) * s.r * 0.22);
                            ctx.lineTo(s.x + Math.cos(a) * s.r, s.y + Math.sin(a) * s.r * 0.34);
                            ctx.stroke();
                        }
                        break;
                    }
                    case 'grid': {
                        ctx.lineWidth = 1.6;
                        const n = 6, size = s.r;
                        for (let i = 0; i <= n; i++) {
                            const o = -size / 2 + i * size / n;
                            ctx.beginPath(); ctx.moveTo(s.x + o, s.y - size / 2); ctx.lineTo(s.x + o, s.y + size / 2); ctx.stroke();
                            ctx.beginPath(); ctx.moveTo(s.x - size / 2, s.y + o); ctx.lineTo(s.x + size / 2, s.y + o); ctx.stroke();
                        }
                        break;
                    }
                    case 'pull': {
                        ctx.lineWidth = 2.4;
                        for (let i = 0; i < 6; i++) {
                            const a = i / 6 * Math.PI * 2 + t * 2.6, rr = 178 * k;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * rr, s.y + Math.sin(a) * rr * 0.7);
                            ctx.lineTo(s.x, s.y); ctx.stroke();
                        }
                        break;
                    }
                    case 'bloom': {
                        const g = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, s.r);
                        g.addColorStop(0, 'rgba(255,255,255,0.78)'); g.addColorStop(1, 'rgba(224,242,254,0)');
                        ctx.fillStyle = g;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'ghostRun': {
                        ctx.globalAlpha = k * 0.55;
                        for (let i = 0; i < 5; i++) {
                            const pr = Math.min(1, t + i * 0.11);
                            const x = s.x1 + (s.x2 - s.x1) * pr, y = s.y1 + (s.y2 - s.y1) * pr;
                            ctx.fillStyle = s.color;
                            ctx.fillRect(x - 13, y - 148, 26, 148);
                        }
                        break;
                    }
                    case 'shock': {
                        ctx.lineWidth = 4 * k + 1;
                        const rr = s.r * (1 - Math.pow(k, 2));
                        ctx.beginPath();
                        ctx.ellipse(s.x, s.y, rr, rr * 0.24, 0, 0, Math.PI * 2);
                        ctx.stroke();
                        break;
                    }
                    case 'crack': {
                        ctx.lineWidth = 2.6;
                        for (let i = 0; i < 7; i++) {
                            const a = i / 7 * Math.PI * 2 + 0.4, len = 74 * (0.5 + Math.random() * 0.6) * (1 - t * 0.3);
                            ctx.beginPath(); ctx.moveTo(s.x, s.y);
                            ctx.lineTo(s.x + Math.cos(a) * len, s.y + Math.sin(a) * len * 0.8); ctx.stroke();
                        }
                        break;
                    }
                    case 'nova': {
                        const rr = s.r * (1 - Math.pow(k, 2.4));
                        const g = ctx.createRadialGradient(s.x, s.y, Math.max(0, rr * 0.5), s.x, s.y, rr);
                        g.addColorStop(0, 'rgba(255,255,255,0)');
                        g.addColorStop(0.7, s.color);
                        g.addColorStop(1, 'rgba(254,243,199,0)');
                        ctx.fillStyle = g;
                        ctx.beginPath(); ctx.arc(s.x, s.y, rr, 0, Math.PI * 2); ctx.fill();
                        ctx.lineWidth = 3.4; ctx.strokeStyle = '#fff';
                        for (let i = 0; i < 16; i++) {
                            const a = i / 16 * Math.PI * 2;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * rr * 0.35, s.y + Math.sin(a) * rr * 0.35);
                            ctx.lineTo(s.x + Math.cos(a) * rr, s.y + Math.sin(a) * rr);
                            ctx.stroke();
                        }
                        break;
                    }
                }
                return s.life > 0;
            });
            ctx.restore();
        }
    };
})();
