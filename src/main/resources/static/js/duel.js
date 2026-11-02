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
                $('fig' + side).innerHTML = figureHtml(side);
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
            fig.innerHTML = figureHtml(side);
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

    /**
     * 立影：先有人体，再穿衣服 —— 上一版是「一个梯形袍子上浮了个头」，这次按真人身架叠：
     *   颈(45~55) → 肩(y30, 宽 40) → 胸(y44) → 腰(y60, 收至 22) → 髋 → 袍摆(y148)
     *   肩宽 : 头宽 ≈ 1.25 : 1，头坐在肩上（颈被领口压住下缘），腿从袍摆开缝里露到靴子。
     * 手臂一律从肩点出发，广袖只盖到腕，腕下一截手掌 + 两根指头 —— 上一版的手埋在袖子里，
     * 看着就是两片翅膀。尺寸仍走 viewBox 0 0 100 160 等比坐标（.dl-body 150x240 = 1.5 倍）。
     * 渐变 id 必须带侧别后缀：A/B 两片 SVG 在同一份文档里，重名会让 B 取到 A 的境界色。
     */
    function figureHtml(side) {
        const gid = name => `dl${name}${side}`;
        const url = name => `url(#${gid(name)})`;
        return `
            <div class="dl-aura"></div>
            <div class="dl-array"></div>
            <div class="dl-shadow"></div>
            <div class="dl-visual">
                <svg class="dl-body" viewBox="0 0 100 160" preserveAspectRatio="xMidYMax meet" aria-hidden="true">
                    <defs>
                        <linearGradient id="${gid('Robe')}" x1="0" y1="0" x2="1" y2="1">
                            <stop class="g-robe-1" offset="0"/>
                            <stop class="g-robe-2" offset=".45"/>
                            <stop class="g-robe-3" offset="1"/>
                        </linearGradient>
                        <linearGradient id="${gid('Skirt')}" x1="0" y1="0" x2="1" y2=".6">
                            <stop class="g-skirt-1" offset="0"/>
                            <stop class="g-skirt-2" offset="1"/>
                        </linearGradient>
                        <linearGradient id="${gid('Sleeve')}" x1="0" y1="0" x2="1" y2=".9">
                            <stop class="g-sleeve-1" offset="0"/>
                            <stop class="g-sleeve-2" offset="1"/>
                        </linearGradient>
                        <linearGradient id="${gid('Sash')}" x1="0" y1="0" x2="0" y2="1">
                            <stop class="g-sash-1" offset="0"/>
                            <stop class="g-sash-2" offset="1"/>
                        </linearGradient>
                    </defs>

                    <!-- 灵光绕体：轮廓光贴着身形走，人物才不像贴在背景上的贴纸 -->
                    <path class="halo" d="M50 16 C36 18 30 26 29 34 C26 48 24 60 22 76 C20 100 18 124 16 148 L84 148 C82 124 80 100 78 76 C76 60 74 48 71 34 C70 26 64 18 50 16 Z"/>

                    <!-- 背后飘带：从肩后垂到腰下，跟着呼吸飘，静止的人才有风 -->
                    <path class="ribbon rb1" d="M33 34 Q17 62 21 100"/>
                    <path class="ribbon rb2" d="M67 36 Q85 64 81 104"/>

                    <g class="torso">
                    <!-- 脑后垂发：绕到肩后，两头都被头发压住才有「长发包着头」的观感 -->
                    <path class="hair" d="M40 14 Q30 34 33 58 L43 54 Q37 34 44 16 Z"/>
                    <path class="hair" d="M60 14 Q70 34 67 58 L57 54 Q63 34 56 16 Z"/>

                    <!-- 颈：头像的下缘正好压住它，肩线在 y30 —— 头坐在肩上，不是浮在袍上 -->
                    <path class="neck" d="M45 14 L45 30 Q50 35 55 30 L55 14 Z"/>
                    <path class="neck-shade" d="M45 14 L55 14 L55 21 Q50 26 45 21 Z"/>

                    <!-- 远侧手臂：肩点出发，袖只到腕，腕下是手掌 -->
                    <g class="arm-b">
                        <path class="limb" fill="${url('Sleeve')}" d="M35 29 Q25 40 23 56 Q22 68 24 76 L37 72 Q34 58 35 46 Q36 37 39 31 Z"/>
                        <path class="cuff" d="M20 68 Q31 64 38 72 Q33 83 22 79 Z"/>
                        <ellipse class="hand" cx="26" cy="85" rx="5" ry="4.6"/>
                        <path class="fingers" d="M23 89 Q26 94 31 92"/>
                    </g>

                    <!-- 裤与靴：袍摆开衩处露出来，脚下才有实地 -->
                    <path class="trouser" d="M43 64 L41 134 L50 134 L49 64 Z"/>
                    <path class="trouser" d="M57 64 L59 134 L50 134 L51 64 Z"/>
                    <path class="boot" d="M39 134 Q36 146 43 150 L51 150 L50 134 Z"/>
                    <path class="boot" d="M61 134 Q64 146 57 150 L49 150 L50 134 Z"/>

                    <!-- 内裳压在最里，从袍摆开缝里露出一条深色中缝 -->
                    <path class="skirt" fill="${url('Skirt')}" d="M38 58 Q25 106 21 150 L47 150 Q45 104 46 58 Z"/>
                    <path class="skirt" fill="${url('Skirt')}" d="M62 58 Q75 106 79 150 L53 150 Q55 104 54 58 Z"/>
                    <path class="under" d="M46 60 L43 148 L57 148 L54 60 Z"/>

                    <!-- 上身两襟：肩→胸→收腰，腰线比头还窄，人才有身形 -->
                    <path class="shoulder" d="M30 33 Q50 23 70 33 Q50 41 30 33 Z"/>
                    <path class="robe" fill="${url('Robe')}" d="M50 26 C41 27 33 31 30 38 C28 48 29 56 31 62 L46 64 L47 40 Z"/>
                    <path class="robe" fill="${url('Robe')}" d="M50 26 C59 27 67 31 70 38 C72 48 71 56 69 62 L54 64 L53 40 Z"/>

                    <!-- 衣褶与受光轮廓：几道细线就够，多了满屏就乱 -->
                    <path class="fold" d="M38 72 Q33 110 29 146"/>
                    <path class="fold" d="M62 74 Q67 112 71 146"/>
                    <path class="fold" d="M46 66 Q45 108 44 146"/>
                    <path class="rim" d="M70 38 C72 58 75 106 79 148"/>

                    <!-- 交领（右衽）：两道襟边在锁骨下交成 V，把颈和胸接上 -->
                    <path class="collar" d="M43 27 Q49 41 58 51"/>
                    <path class="collar in" d="M57 27 Q51 40 44 49"/>

                    <!-- 大带束在腰上（不是腹），玉佩与绦穗垂到胯 —— 视觉上把上身提上去 -->
                    <path class="sash" fill="${url('Sash')}" d="M36 56 Q50 52 64 56 L64 68 Q50 64 36 68 Z"/>
                    <path class="jade" d="M50 68 L55 75 L50 82 L45 75 Z"/>
                    <path class="tassel" d="M50 82 L50 94"/>

                    <!-- 近侧广袖与手：压在最上层，抬袖出手都靠它 -->
                    <g class="arm-f">
                        <path class="limb" fill="${url('Sleeve')}" d="M65 29 Q75 40 77 56 Q78 68 76 76 L63 72 Q66 58 65 46 Q64 37 61 31 Z"/>
                        <path class="cuff" d="M80 68 Q69 64 62 72 Q67 83 78 79 Z"/>
                        <ellipse class="hand" cx="74" cy="85" rx="5.4" ry="4.8"/>
                        <path class="fingers" d="M77 89 Q74 94 69 92"/>
                    </g>
                    </g>
                </svg>
                <div class="dl-headwrap">
                    <svg class="dl-hair dl-hair-back" viewBox="0 0 76 90" aria-hidden="true">
                        <ellipse class="hb" cx="38" cy="32" rx="29" ry="25"/>
                        <path class="hb" d="M38 6 C17 6 5 22 7 44 C8 62 14 78 22 88 L26 44 C26 24 30 12 38 8 Z"/>
                        <path class="hb" d="M38 6 C59 6 71 22 69 44 C68 62 62 78 54 88 L50 44 C50 24 46 12 38 8 Z"/>
                    </svg>
                    <img class="dl-head" src="/images/default-avatar.svg" alt="">
                    <svg class="dl-hair dl-hair-front" viewBox="0 0 76 90" aria-hidden="true">
                        <!-- 刘海压到额头 1/3，鬓发沿脸侧下到下颌：头发贴着脸长，不是扣在头上的黑帽子 -->
                        <path class="hf" d="M12 44 C12 22 23 10 38 10 C53 10 64 22 64 44 C58 27 50 20 38 20 C26 20 18 27 12 44 Z"/>
                        <path class="hf" d="M11 40 C9 56 12 70 18 80 L23 58 C20 50 19 45 19 40 Z"/>
                        <path class="hf" d="M65 40 C67 56 64 70 58 80 L53 58 C56 50 57 45 57 40 Z"/>
                        <path class="sheen" d="M19 30 Q29 15 45 15 Q33 21 24 35 Z"/>
                        <ellipse class="hf" cx="38" cy="8" rx="10" ry="8"/>
                        <rect class="guan" x="29" y="0" width="18" height="7" rx="3"/>
                        <path class="guan-pin" d="M46 4 L60 0"/>
                        <path class="hair-tie" d="M26 14 Q38 20 50 14"/>
                        <path class="ribbon rb3" d="M50 4 Q66 11 62 30"/>
                    </svg>
                </div>
                <div class="dl-weapon">诀</div>
            </div>
            <div class="dl-bubble"></div>
            <div class="dl-nameplate"></div>`;
    }

    // ==================== 报招语音 ====================
    const Speech = {
        ok: typeof window !== 'undefined' && 'speechSynthesis' in window,
        zh: null,
        list: [],
        /* 念话状态：排时间轴要靠它把下一拍按住。上一句没念完就开下一招，两句话会叠成一团糊音。
           busy 由 utterance 的 onend/onerror 归位，另有超时保险丝（有些内核被 cancel 后两个回调都不发）。 */
        busy: false,
        utter: null,
        idleCbs: [],
        fuse: 0,

        init() {
            if (!this.ok) {
                this.state('浏览器不支持语音合成');
                this.warn('这个浏览器没有语音合成接口，报招改用气泡（不影响斗法）。换 Edge / Chrome 正式版即可。');
                return;
            }
            // 音色是异步进来的：先试一次，剩下的交给 onvoiceschanged 与自查重试
            this.refresh();
            window.speechSynthesis.onvoiceschanged = () => this.refresh();
        },

        /* 本机可用的中文音色。只看 lang 会漏：有的系统 lang 只写 "zh"，
           有的 lang 缺失但名字是「Microsoft 晓晓…」，所以名字也参与匹配。 */
        candidates() {
            const vs = window.speechSynthesis.getVoices() || [];
            return vs.filter(v => {
                const lang = (v.lang || '').replace(/_/g, '-');
                return /^zh(-|$)/i.test(lang)
                    || /zh-cn|zh-hans|zh-tw|zh-hk|chinese|mandarin|cantonese/i.test(lang)
                    || /(中文|普通话|国语|粤语|晓晓|云希|云健|云龙|晓伊|Chinese|Mandarin)/i.test(v.name || '');
            });
        },

        /* getVoices() 第一次调用多半返回空数组，而个别内核根本不触发 onvoiceschanged
           → 自己按 250ms 补几次，否则会错怪用户「你没装中文语音包」 */
        refresh(left = 10) {
            this.list = this.candidates();
            if (this.list.length || left <= 0) {
                this.buildList();
                this.report();
                return;
            }
            setTimeout(() => this.refresh(left - 1), 250);
        },

        /* 音色挑选：用户在页面上选过的优先（存 localStorage，跨场保持），
           没选过就本地音色优先（联网音色一卡就没声），其次标准普通话。 */
        pick() {
            if (!this.list.length) { this.zh = null; return null; }
            const want = localStorage.getItem('duel-voice') || '';
            if (want) {
                const hit = this.list.find(v => v.name === want);
                if (hit) { this.zh = hit; return hit; }
            }
            const score = v => (v.localService ? 2 : 0)
                + (/^zh(-cn|-hans)?$/i.test((v.lang || '').replace(/_/g, '-')) ? 1 : 0);
            this.zh = this.list.slice().sort((a, b) => score(b) - score(a))[0];
            return this.zh;
        },

        /* 下拉每次整体重建：音色列表是异步补齐的，追加写法会堆出一堆重复选项，
           而把「列表为空才塞」的判据写在循环里又只会塞进去第一个，等于没得换。 */
        buildList() {
            const sel = $('dlVoiceList');
            if (!sel) return;
            const save = localStorage.getItem('duel-voice') || '';
            sel.innerHTML = '<option value="">自动（本地音色优先）</option>';
            this.list.forEach(v => {
                const o = document.createElement('option');
                o.value = v.name;
                o.textContent = `${v.name} · ${(v.lang || '?').toUpperCase()}${v.localService ? '' : ' · 联网'}`;
                sel.appendChild(o);
            });
            sel.value = this.list.some(v => v.name === save) ? save : '';
            sel.disabled = this.list.length < 2;
        },

        state(msg) { const el = $('dlVoiceState'); if (el) el.textContent = msg; },

        /** 把检测结论写在页面上：有音色就报名字，没有就把装语音包的入口路径写清楚 */
        report() {
            const btn = $('dlVoiceTry');
            if (btn) btn.disabled = !this.list.length;
            if (!this.list.length) {
                this.state('本机没有中文音色');
                this.warn('系统里没有中文语音包 —— 人物仍会「张口」，招名以气泡呈现。'
                    + 'Windows：设置 → 时间和语言 → 语音 → 添加「中文(中国)」并勾选「语音」，'
                    + '或改用 Edge / Chrome 正式版（精简版系统和本 IDE 内置浏览器不带语音）。');
                return;
            }
            const v = this.pick();
            this.state(`音色：${v.name} · 共 ${this.list.length} 个中文音色，下拉可换`);
            const warn = $('dlVoiceWarn');
            if (warn) warn.style.display = 'none';
        },

        warn(msg) { const el = $('dlVoiceWarn'); if (!el) return; el.textContent = msg; el.style.display = 'block'; },

        /** 喝声 = 人物口气前缀 + 招名；招名里的分隔符换成逗号，念着才不像报菜名 */
        line(p, ev) {
            const v = (p && p.voice) || {};
            const skill = String(ev.skill || '').replace(/[·（）()]/g, ' ').replace(/\s+/g, ' ').trim();
            // 喊词自带句号，再接逗号会念成两次停顿，先把结尾标点摘掉
            const head = ev.skipped ? '' : String(v.shout || '').replace(/[。！？!~～]+$/, '');
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
                this.busy = true;
                this.utter = u;
                u.onend = () => this.release(u);
                u.onerror = () => this.release(u);
                /* 兜底：联网音色卡住、或内核一个回调都不发时，按预估时长再给 3 秒就当作念完了。
                   绝不能把整场斗法冻在「等一句话」上 —— 宁可少等一句，也不能不往下打。 */
                clearTimeout(this.fuse);
                this.fuse = setTimeout(() => this.release(u), this.estMs(p, text) + 3000);
                window.speechSynthesis.speak(u);
            } catch (e) {
                // 语音失败不影响演出，气泡与字幕还在，也别把下一拍锁死
                this.release(this.utter);
            }
        },

        /** 这一句结束了（或兜底超时了）：把等着「念完」的下一拍全部放行 */
        release(u) {
            if (u && this.utter !== u) return;   // 被 cancel 掉的旧 utterance 迟到的回调，忽略
            clearTimeout(this.fuse);
            this.busy = false;
            this.utter = null;
            const cbs = this.idleCbs;
            this.idleCbs = [];
            cbs.forEach(fn => {
                try { fn(); } catch (e) { console.warn('[speech] 放行回调异常', e); }
            });
        },

        /** 等这句念完再走下一拍；语音关闭时 busy 恒为 false，等于当场放行 */
        whenIdle(cb) {
            if (!this.busy) { cb(); return; }
            this.idleCbs.push(cb);
        },

        /** 中途关掉语音：停止发声，但必须把排队的下一拍放行，不然斗法会卡在原地 */
        halt() {
            this.cancelSpeak();
            this.release(null);
        },

        /** 清台 / 重新开局：连等待队列一起丢掉，旧链子不该再往下走 */
        reset() {
            clearTimeout(this.fuse);
            this.idleCbs = [];
            this.busy = false;
            this.utter = null;
            this.cancelSpeak();
        },

        cancelSpeak() {
            if (!this.ok) return;
            try { window.speechSynthesis.cancel(); } catch (e) { /* ignore */ }
        },

        /** 试听：用 A 侧人物的音色念一条招名。
            不受「语音开关」约束 —— 它就是用来确认这台机器到底能不能发声的。 */
        trySpeak() {
            if (!this.ok || !this.list.length) return;
            const p = D.people.find(x => x.userId === D.pickA) || D.people[0];
            if (!p) return;
            const sk = (p.skills || []).find(s => s.kind === 'ULT') || (p.skills || [])[0];
            const keep = D.voice;
            D.voice = true;
            this.say(p, this.line(p, { skill: (sk || {}).name || p.styleLabel || p.style, skipped: false }));
            D.voice = keep;
        }
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
        // 音色三件事：试听 / 重新检测 / 换音色（选过的记在本地，下次进来还是他）
        $('dlVoiceTry').addEventListener('click', () => Speech.trySpeak());
        $('dlVoiceRe').addEventListener('click', () => Speech.refresh());
        $('dlVoiceList').addEventListener('change', e => {
            localStorage.setItem('duel-voice', e.target.value);
            Speech.pick();
            Speech.report();
        });
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
        Speech.reset();
        Fx.reset();
        $('dlVerdict').classList.remove('show');
        // 屏幕级演出的残留类要一起清掉，不然清台之后地面还亮着一块
        const el = $('dlImpact'), ground = $('dlGround');
        if (el) el.classList.remove('on');
        if (ground) ground.classList.remove('glow');
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

    /** 逐招串成一条链：上一拍的动画拍子走完、并且这句报招真正念完，才允许下一拍开始。
     *  （原来是先把全部时间轴算好再挂 setTimeout，估时偏短就会两句话叠在一起） */
    function run(d) {
        const events = d.events || [], outcome = d.outcome || {};
        const A = merge(d.a), B = merge(d.b);
        const maxA = A.hpMax, maxB = B.hpMax;

        clearTimers();
        Fx.reset();
        Speech.reset();
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

        const plan = [];
        events.forEach(ev => {
            const actor = sideOf(ev.actorId);
            if (actor) plan.push({ ev, actor, foe: foeOf(actor), p: personOf(actor) });
        });

        const scale = 1 / D.speed;
        let idx = 0;
        function step() {
            // 清台、重新开局、或者链子被丢弃后，旧链子自己断掉，不再往下演
            if (!D.playing) return;
            if (idx >= plan.length) {
                later(() => {
                    if (!D.playing) return;
                    showOutcome(outcome, A, B, sideOf);
                    later(() => { D.playing = false; updateActionState(); }, 1100 * scale);
                }, 300 * scale);
                return;
            }
            const it = plan[idx++];
            const dur = rawDur(it.ev, it.p) * scale;
            playEvent(Object.assign({ maxA, maxB, dur }, it));
            // 先等这一拍的动画拍子，再等这句话念完（语音关掉时 whenIdle 当场放行，节奏不变）
            later(() => Speech.whenIdle(step), dur);
        }
        later(step, 360 * scale);
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

    /** 这一拍留多久：取「动作时长」与「把这句话说完」的较大者；
     *  真正的硬约束在 run() 的链子上（念不完就不开下一拍），这里只是先把拍子给足 */
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
                screenHit(struck, toneOf(ev.vfx, p.realmColor), ult, !!ev.crit);
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
        // 招名跟着这一路的元素色走：剑气青白、火球橘红、冰霜湖蓝 —— 一眼能分清中的是什么
        el.style.setProperty('--sk', toneOf(ev.vfx, '#e2e8f0').main);
        sub.textContent = ev.line || '';
        [el, sub].forEach(x => {
            x.classList.remove('play');
            void x.offsetWidth;
            // 人家用完一句话的时间，字得还在屏上：时长跟这一拍同样缩放
            const dur = (ult ? 3.1 : 1.9) / D.speed;
            x.style.animationDuration = dur + 's';
            x.style.setProperty('--sdur', dur + 's');
            x.classList.add('play');
        });
    }

    /**
     * 屏幕级命中反馈：打实的一刻，落点泛起这一招自己的颜色，同时扩出一圈冲击环。
     * 只靠 canvas 里的粒子的话，画面始终是「平」的 —— 观众需要知道这一下到底打中了没有。
     */
    function screenHit(side, tn, ult, crit) {
        const stage = $('dlStage'), el = $('dlImpact'), ground = $('dlGround');
        if (!el || !stage) return;
        const p = Fx.pos(side);
        el.style.setProperty('--ix', (p.x / stage.clientWidth * 100).toFixed(1) + '%');
        el.style.setProperty('--iy', ((p.y - 46) / stage.clientHeight * 100).toFixed(1) + '%');
        el.style.setProperty('--ic', tn.main);
        el.style.setProperty('--idur', ((ult ? .74 : crit ? .56 : .4) / D.speed) + 's');
        el.style.setProperty('--rdur', ((ult ? 1 : .56) / D.speed) + 's');
        el.classList.remove('on');
        void el.offsetWidth;
        el.classList.add('on');
        later(() => el.classList.remove('on'), 1200);
        // 大招砸下来，脚下整片地面都要亮一下
        if (ground && (ult || crit)) {
            ground.style.setProperty('--gx', (p.x / stage.clientWidth * 100).toFixed(1) + '%');
            ground.style.setProperty('--gc', tn.main);
            ground.classList.remove('glow');
            void ground.offsetWidth;
            ground.classList.add('glow');
        }
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

    /**
     * 元素调色板：每一路招式有自己的颜色（main 主体 / hot 白热芯 / dark 边缘余焰）。
     * 以前所有飞行物与爆点都刷施法者的境界色，所以一场看下来永远是一个色调 ——
     * 剑是青白、火是橘红、冰是湖蓝、音是品红，先靠颜色就能分辨中了什么招。
     * 键与后端 Cultivation.Vfx 对齐；没列上的 vfx 退回境界色，不会没特效。
     */
    const TONE = {
        SLASH:        { main: '#7dd3fc', hot: '#f0f9ff', dark: '#0369a1' },
        SWORD_RAIN:   { main: '#c4b5fd', hot: '#fde68a', dark: '#6d28d9' },
        GOLD_BEAM:    { main: '#fbbf24', hot: '#fffbeb', dark: '#b45309' },
        ICE_FROST:    { main: '#67e8f9', hot: '#ecfeff', dark: '#0e7490' },
        FIRE_ORB:     { main: '#fb923c', hot: '#fef08a', dark: '#b91c1c' },
        SHIELD:       { main: '#38bdf8', hot: '#f0f9ff', dark: '#1d4ed8' },
        MIND_WAVE:    { main: '#a78bfa', hot: '#f5d0fe', dark: '#5b21b6' },
        BLOOD_SHADOW: { main: '#f87171', hot: '#fecaca', dark: '#7f1d1d' },
        SUMMON:       { main: '#4ade80', hot: '#ecfccb', dark: '#15803d' },
        POISON_MIST:  { main: '#a3e635', hot: '#d9f99d', dark: '#3f6212' },
        ARRAY_PRESS:  { main: '#fcd34d', hot: '#fff7ed', dark: '#92400e' },
        MAGNET_BEAM:  { main: '#818cf8', hot: '#c7d2fe', dark: '#312e81' },
        SOUND_RING:   { main: '#e879f9', hot: '#fdf4ff', dark: '#a21caf' },
        TURTLE_SHELL: { main: '#5eead4', hot: '#f0fdfa', dark: '#115e59' },
        MOON_GLOW:    { main: '#bfdbfe', hot: '#ffffff', dark: '#1e40af' },
        QI_PALM:      { main: '#94a3b8', hot: '#f1f5f9', dark: '#334155' },
        MORTAL_PUNCH: { main: '#fda4af', hot: '#ffe4e6', dark: '#9f1239' },
        HEAL_GLOW:    { main: '#4ade80', hot: '#f0fdf4', dark: '#166534' },
        ULT_NOVA:     { main: '#fde68a', hot: '#ffffff', dark: '#f97316' }
    };
    const toneOf = (vfx, fallback) => TONE[vfx] || { main: fallback || '#a78bfa', hot: '#ffffff', dark: '#312e81' };

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

        /** 蓄力：灵气由四面汇向人物，出手前就有前摇；神通额外开一层旋转法阵与灵气漩涡 */
        charge(p, color, ult) {
            const n = ult ? 52 : 26;
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                const r = (ult ? 250 : 150) * (0.5 + Math.random() * 0.7);
                this.parts.push({
                    kind: 'converge', x: p.x + Math.cos(a) * r, y: p.y - 50 + Math.sin(a) * r * 0.7,
                    tx: p.x, ty: p.y - 66, k: 0, speed: 0.02 + Math.random() * 0.02, cx: p.x, cy: p.y,
                    life: 92, max: 92, r: 1.6 + Math.random() * 2.6, color
                });
            }
            this.shapes.push({ kind: 'halo', x: p.x, y: p.y - 54, color, life: ult ? 116 : 76, max: ult ? 116 : 76, r: ult ? 172 : 112 });
            if (ult) {
                this.shapes.push({ kind: 'runeRing', x: p.x, y: p.y - 4, color: '#fde68a', life: 140, max: 140, r: 148, spin: 1 });
                this.shapes.push({ kind: 'vortex', x: p.x, y: p.y - 62, color, life: 128, max: 128, r: 118, arms: 5, spin: -1.5 });
            }
        },

        /**
         * 出手：按 vfx 键给每路招式一套自己的演出（颜色取自 TONE），
         * 一个 case 里至少叠两三层：飞出去的东西 + 落点效果 + 屏幕级反馈。
         */
        projectile(ev, actor, foeSide, color, ult) {
            const from = this.pos(actor), to = this.pos(foeSide);
            const dir = actor === 'A' ? 1 : -1;
            const tn = toneOf(ev.vfx, color);
            const midY = from.y - 62;
            // 起手一道拖光：让「这一招从他身上飞出去」有明确起点
            this.trails.push({ x1: from.x, y1: midY, x2: to.x, y2: to.y - 58, color: tn.main, life: 46, max: 46, w: ult ? 20 : 11 });
            switch (ev.vfx) {
                case 'SLASH':
                    this.shapes.push({ kind: 'arcCut', x: to.x, y: to.y - 66, color: tn.main, life: 46, max: 46, dir, r: 112 });
                    this.shapes.push({ kind: 'arcCut', x: to.x - dir * 26, y: to.y - 34, color: tn.hot, life: 38, max: 38, dir, r: 76 });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 60, color: tn.main, life: 40, max: 40, r: 96, n: 9, rot: dir });
                    this.shards(to, tn, 8, 3.2);
                    break;
                case 'SWORD_RAIN':
                    for (let i = 0; i < (ult ? 58 : 34); i++) {
                        this.parts.push({
                            kind: 'sword', x: to.x + (Math.random() - 0.5) * 240, y: -30 - Math.random() * 200,
                            vx: (Math.random() - 0.5) * 0.9, vy: 5 + Math.random() * 3.6,
                            life: 112, max: 112, color: i % 5 === 0 ? tn.hot : tn.main
                        });
                    }
                    this.shapes.push({ kind: 'runeRing', x: to.x, y: to.y - 74, color: tn.main, life: 116, max: 116, r: 150, spin: 1 });
                    this.shapes.push({ kind: 'arcCut', x: to.x, y: to.y - 62, color: tn.hot, life: 44, max: 44, dir, r: 104 });
                    if (ult) this.shapes.push({ kind: 'flash', color: tn.main, life: 24, max: 24, a: 0.5 });
                    break;
                case 'GOLD_BEAM':
                    this.shapes.push({ kind: 'beam', x1: from.x, y1: midY, x2: to.x, y2: to.y - 56, color: tn.main, life: 76, max: 76, width: ult ? 44 : 20 });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 58, color: tn.hot, life: 54, max: 54, r: 170, n: 14, rot: 0 });
                    this.shapes.push({ kind: 'bloom', x: to.x, y: to.y - 58, color: tn.hot, life: 70, max: 70, r: 120 });
                    this.embers(from, tn, 18);
                    if (ult) this.shapes.push({ kind: 'flash', color: tn.main, life: 26, max: 26, a: 0.55 });
                    break;
                case 'ICE_FROST':
                    this.shapes.push({ kind: 'frostWeb', x: to.x, y: to.y - 46, color: tn.main, life: 124, max: 124, r: ult ? 172 : 122 });
                    this.shapes.push({ kind: 'spike', x: to.x, y: to.y, color: tn.hot, life: 104, max: 104, dir });
                    this.shards(to, tn, 14, 2.6);
                    this.ring(to, tn.main, ult ? 240 : 160, 96);
                    this.mist(to, tn.dark, 12, 0.6);
                    break;
                case 'FIRE_ORB':
                    this.shapes.push({ kind: 'meteor', x1: from.x, y1: midY, x2: to.x, y2: to.y - 54, color: tn.main, life: 60, max: 60, r: ult ? 30 : 20 });
                    this.embers(from, tn, 20);
                    this.shapes.push({ kind: 'bloom', x: to.x, y: to.y - 54, color: tn.hot, life: 66, max: 66, r: 132 });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 54, color: tn.main, life: 48, max: 48, r: 140, n: 12, rot: 0 });
                    this.shapes.push({ kind: 'shock', x: to.x, y: to.y + 8, color: tn.dark, life: 70, max: 70, r: 210 });
                    break;
                case 'MIND_WAVE':
                    this.shapes.push({ kind: 'vortex', x: to.x, y: to.y - 60, color: tn.main, life: 120, max: 120, r: 132, arms: 4, spin: 2.2 });
                    this.ring(from, tn.hot, 300, 100, 2);
                    this.ring(from, tn.main, 210, 84, 2);
                    this.mist(to, tn.dark, 10, 0.5);
                    break;
                case 'SOUND_RING':
                    this.ring(from, tn.main, 360, 108, 4);
                    this.ring(from, tn.hot, 268, 92, 3);
                    this.ring(from, tn.dark, 176, 76, 2);
                    this.petals(to, tn, 10);
                    break;
                case 'BLOOD_SHADOW':
                    this.shapes.push({ kind: 'ghostRun', x1: from.x, y1: from.y, x2: to.x, y2: to.y, color: tn.main, life: 84, max: 84 });
                    this.embers(from, tn, 16);
                    this.shapes.push({ kind: 'crack', x: to.x, y: to.y - 62, color: tn.hot, life: 60, max: 60 });
                    break;
                case 'MAGNET_BEAM':
                    this.shapes.push({ kind: 'pull', x: to.x, y: to.y - 60, color: tn.main, life: 104, max: 104 });
                    this.shapes.push({ kind: 'vortex', x: to.x, y: to.y - 60, color: tn.hot, life: 110, max: 110, r: 120, arms: 3, spin: -2.6 });
                    this.shards(to, tn, 10, 2.2);
                    break;
                case 'ARRAY_PRESS':
                    this.shapes.push({ kind: 'grid', x: to.x, y: to.y - 46, color: tn.main, life: 128, max: 128, r: 232 });
                    this.shapes.push({ kind: 'runeRing', x: to.x, y: to.y - 40, color: tn.hot, life: 132, max: 132, r: 196, spin: -0.8 });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 50, color: tn.main, life: 56, max: 56, r: 150, n: 10, rot: 0 });
                    if (ult) this.shapes.push({ kind: 'flash', color: tn.main, life: 22, max: 22, a: 0.42 });
                    break;
                case 'POISON_MIST':
                    this.mist(to, tn.main, 34, 1.1);
                    this.shapes.push({ kind: 'vortex', x: to.x, y: to.y - 44, color: tn.dark, life: 150, max: 150, r: 150, arms: 6, spin: 0.8 });
                    this.ring(to, tn.main, 190, 130, 2);
                    this.embers(to, tn, 12);
                    break;
                case 'SUMMON':
                    for (let i = 0; i < (ult ? 40 : 24); i++) {
                        this.parts.push({
                            kind: 'rise', x: to.x + (Math.random() - 0.5) * 250, y: this.h,
                            vx: (Math.random() - 0.5) * 0.8, vy: -2.4 - Math.random() * 2.4,
                            life: 134, max: 134, r: 2 + Math.random() * 3.6, color: i % 3 === 0 ? tn.hot : tn.main
                        });
                    }
                    this.shapes.push({ kind: 'runeRing', x: to.x, y: to.y - 2, color: tn.main, life: 140, max: 140, r: 160, spin: 1.2 });
                    this.petals(to, tn, 12);
                    break;
                case 'TURTLE_SHELL':
                case 'SHIELD':
                    this.shapes.push({ kind: 'hex', x: from.x, y: from.y - 62, color: tn.main, life: 128, max: 128, r: ult ? 122 : 98 });
                    this.shapes.push({ kind: 'hex', x: from.x, y: from.y - 62, color: tn.hot, life: 92, max: 92, r: ult ? 86 : 66 });
                    this.ring(from, tn.main, 190, 96, 3);
                    this.shapes.push({ kind: 'halo', x: from.x, y: from.y - 60, color: tn.main, life: 84, max: 84, r: 130 });
                    break;
                case 'MOON_GLOW':
                    this.shapes.push({ kind: 'bloom', x: this.w / 2, y: this.h * 0.2, color: tn.main, life: 132, max: 132, r: 160 });
                    this.shapes.push({ kind: 'rays', x: this.w / 2, y: this.h * 0.22, color: tn.hot, life: 96, max: 96, r: 260, n: 18, rot: 0.4 });
                    this.petals(to, tn, 18);
                    break;
                case 'ULT_NOVA':
                    this.shapes.push({ kind: 'flash', color: tn.hot, life: 30, max: 30, a: 0.66 });
                    this.shapes.push({
                        kind: 'nova', x: to.x, y: to.y - 60, color: tn.main,
                        life: 128, max: 128, r: Math.max(this.w, this.h) * 1.2
                    });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 60, color: tn.hot, life: 84, max: 84, r: 320, n: 22, rot: 0.6 });
                    this.shapes.push({ kind: 'vortex', x: to.x, y: to.y - 60, color: tn.dark, life: 120, max: 120, r: 210, arms: 6, spin: 1.8 });
                    break;
                case 'QI_PALM':
                    this.shapes.push({ kind: 'bloom', x: to.x, y: to.y - 62, color: tn.hot, life: 78, max: 78, r: 118 });
                    this.shapes.push({ kind: 'shock', x: to.x, y: to.y + 8, color: tn.main, life: 72, max: 72, r: 224 });
                    this.ring(to, tn.main, 170, 78, 3);
                    this.shards(to, tn, 8, 2.4);
                    break;
                case 'MORTAL_PUNCH':
                    this.shapes.push({ kind: 'crack', x: to.x, y: to.y - 62, color: tn.hot, life: 60, max: 60 });
                    this.shapes.push({ kind: 'shock', x: to.x, y: to.y + 6, color: tn.main, life: 52, max: 52, r: 128 });
                    this.ring(to, tn.main, 132, 62, 2.4);
                    break;
                default:
                    this.shapes.push({ kind: 'fork', x1: from.x, y1: midY, x2: to.x, y2: to.y - 56, color: tn.main, hot: tn.hot, life: 56, max: 56 });
                    this.shapes.push({ kind: 'bloom', x: to.x, y: to.y - 58, color: tn.hot, life: 58, max: 58, r: 96 });
                    this.shapes.push({ kind: 'rays', x: to.x, y: to.y - 58, color: tn.main, life: 44, max: 44, r: 120, n: 10, rot: -0.5 });
            }
            if (ult) {
                this.ring(from, '#ffffff', 300, 92, 3);
                this.shapes.push({ kind: 'runeRing', x: from.x, y: from.y - 8, color: tn.hot, life: 120, max: 120, r: 132, spin: -1 });
            }
        },

        /** 碎屑四溅：冰晶、气劲、金风都用它，靠颜色区分质感 */
        shards(p, tn, n, speed) {
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                this.parts.push({
                    kind: 'shard', x: p.x, y: p.y - 60,
                    vx: Math.cos(a) * speed * (0.6 + Math.random()), vy: Math.sin(a) * speed - 1.2,
                    life: 96 + Math.random() * 50, max: 140, r: 2 + Math.random() * 3.4,
                    spin: (Math.random() - 0.5) * 0.3, color: i % 3 === 0 ? tn.hot : tn.main
                });
            }
        },

        /** 上升的余烬／火星：火、毒、血都靠它把「余势」拖长 */
        embers(p, tn, n) {
            for (let i = 0; i < n; i++) {
                this.parts.push({
                    kind: 'ember', x: p.x + (Math.random() - 0.5) * 90, y: p.y - Math.random() * 60,
                    vx: (Math.random() - 0.5) * 0.6, vy: -1.1 - Math.random() * 1.4,
                    life: 110 + Math.random() * 60, max: 170, r: 1.4 + Math.random() * 2.4,
                    flick: Math.random() * 6, color: Math.random() < 0.4 ? tn.hot : tn.main
                });
            }
        },

        /** 花瓣／落叶旋落：月华、治愈、召唤这类「柔」的招式用它收尾 */
        petals(p, tn, n) {
            for (let i = 0; i < n; i++) {
                this.parts.push({
                    kind: 'petal', x: p.x + (Math.random() - 0.5) * 220, y: p.y - 150 - Math.random() * 90,
                    vx: (Math.random() - 0.5) * 0.7, vy: 0.7 + Math.random() * 1.1,
                    life: 160 + Math.random() * 70, max: 230, r: 3 + Math.random() * 3.4,
                    spin: (Math.random() - 0.5) * 0.16, sway: Math.random() * 6, color: tn.main
                });
            }
        },

        /** 一团滞空的雾：毒、瘞、寒气用它铺面 */
        mist(p, color, n, scale) {
            for (let i = 0; i < n; i++) {
                this.parts.push({
                    kind: 'fog', x: p.x + (Math.random() - 0.5) * 130, y: p.y - Math.random() * 80,
                    vx: (Math.random() - 0.5) * 0.6 * scale, vy: -0.22 - Math.random() * 0.4,
                    life: 170, max: 170, r: (18 + Math.random() * 30) * scale, color, glow: true
                });
            }
        },

        defense(p, color, vfx) {
            const tn = toneOf(vfx, color);
            this.ring(p, tn.main, 186, 92, 3);
            this.shapes.push({ kind: 'hex', x: p.x, y: p.y - 62, color: tn.main, life: 108, max: 108, r: 96 });
            this.shapes.push({ kind: 'hex', x: p.x, y: p.y - 62, color: tn.hot, life: 78, max: 78, r: 64 });
            this.shapes.push({ kind: 'halo', x: p.x, y: p.y - 58, color: tn.main, life: 70, max: 70, r: 124 });
            if (vfx === 'ARRAY_PRESS') {
                this.shapes.push({ kind: 'grid', x: p.x, y: p.y - 34, color: tn.main, life: 112, max: 112, r: 196 });
                this.shapes.push({ kind: 'runeRing', x: p.x, y: p.y - 4, color: tn.hot, life: 120, max: 120, r: 168, spin: 0.9 });
            }
        },

        heal(p, vfx) {
            const tn = toneOf(vfx, '#4ade80');
            for (let i = 0; i < 30; i++) {
                this.parts.push({
                    kind: 'rise', x: p.x + (Math.random() - 0.5) * 150, y: p.y + 8,
                    vx: (Math.random() - 0.5) * 0.5, vy: -1.6 - Math.random() * 1.8,
                    life: 132, max: 132, r: 2 + Math.random() * 3.2,
                    color: i % 4 === 0 ? tn.hot : tn.main
                });
            }
            this.petals(p, tn, 10);
            this.ring(p, tn.main, 150, 92, 2.4);
            this.shapes.push({ kind: 'runeRing', x: p.x, y: p.y - 2, color: tn.main, life: 128, max: 128, r: 126, spin: -0.7 });
            this.shapes.push({ kind: 'bloom', x: p.x, y: p.y - 56, color: tn.hot, life: 84, max: 84, r: 104 });
        },

        move(p, side, color) {
            const dir = side === 'A' ? -1 : 1;
            for (let i = 0; i < 26; i++) {
                this.parts.push({
                    kind: 'streak', x: p.x + dir * i * 10, y: p.y - 24 - Math.random() * 110,
                    vx: dir * (2.6 + Math.random() * 2.2), vy: 0.15 + Math.random() * 0.4,
                    life: 82, max: 82, color: i % 3 === 0 ? '#ffffff' : color, len: 16 + Math.random() * 26
                });
            }
            this.embers(p, { main: color, hot: '#ffffff' }, 10);
        },

        impact(p, ev, color, ult, crit) {
            const tn = toneOf(ev && ev.vfx, color);
            const n = ult ? 86 : crit ? 46 : 30;
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                const s = (ult ? 7 : crit ? 5.4 : 4) * (0.35 + Math.random() * 0.95);
                this.parts.push({
                    kind: 'spark', x: p.x, y: p.y - 62, vx: Math.cos(a) * s, vy: Math.sin(a) * s - 0.6,
                    life: (ult ? 132 : 92) * (0.6 + Math.random() * 0.7), max: ult ? 132 : 92,
                    r: 1.4 + Math.random() * 2.8,
                    color: i % 3 === 0 ? tn.hot : (i % 3 === 1 ? tn.main : (crit ? '#fbbf24' : tn.main))
                });
            }
            this.ring(p, tn.hot, ult ? 300 : 172, ult ? 92 : 62, ult ? 5 : 2.6);
            this.shapes.push({ kind: 'shock', x: p.x, y: p.y + 8, color: tn.main, life: 84, max: 84, r: ult ? 300 : 168 });
            this.shapes.push({ kind: 'rays', x: p.x, y: p.y - 62, color: tn.main, life: ult ? 70 : 44, max: ult ? 70 : 44, r: ult ? 250 : 132, n: ult ? 20 : 10, rot: 0.3 });
            if (ult) {
                this.shapes.push({ kind: 'flash', color: tn.main, life: 26, max: 26, a: 0.6 });
                this.shapes.push({ kind: 'runeRing', x: p.x, y: p.y - 66, color: tn.hot, life: 120, max: 120, r: 168, spin: 1 });
                this.shapes.push({ kind: 'vortex', x: p.x, y: p.y - 60, color: tn.dark, life: 130, max: 130, r: 190, arms: 5, spin: -1.9 });
                this.shards(p, tn, 16, 4.2);
            } else if (crit) {
                this.shapes.push({ kind: 'flash', color: '#ffffff', life: 16, max: 16, a: 0.34 });
                this.shapes.push({ kind: 'crack', x: p.x, y: p.y - 62, color: tn.hot, life: 78, max: 78 });
            }
        },

        ring(p, color, radius, life, width) {
            this.shapes.push({ kind: 'ring', x: p.x, y: p.y - 54, color, r: 12, maxR: radius, life, max: life, width: width || 3 });
        },

        finale(color) {
            for (let i = 0; i < 96; i++) {
                const a = Math.random() * Math.PI * 2;
                this.parts.push({
                    kind: 'spark', x: this.w / 2 + Math.cos(a) * 40, y: this.h / 2 + Math.sin(a) * 30,
                    vx: Math.cos(a) * (2 + Math.random() * 6), vy: Math.sin(a) * (2 + Math.random() * 5) - 1,
                    life: 176, max: 176, r: 1.6 + Math.random() * 3.4, color: i % 4 === 0 ? '#ffffff' : color
                });
            }
            this.shapes.push({ kind: 'flash', color: '#ffffff', life: 26, max: 26, a: 0.5 });
            this.shapes.push({
                kind: 'nova', x: this.w / 2, y: this.h * 0.5, color: '#fde68a',
                life: 140, max: 140, r: Math.max(this.w, this.h)
            });
            this.shapes.push({ kind: 'rays', x: this.w / 2, y: this.h * 0.5, color: color, life: 120, max: 120, r: Math.max(this.w, this.h) * 0.9, n: 24, rot: 0.5 });
            this.shapes.push({ kind: 'runeRing', x: this.w / 2, y: this.h * 0.55, color: '#fde68a', life: 170, max: 170, r: 260, spin: 0.8 });
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
                    else if (p.kind === 'shard') p.vy += 0.12;
                    else if (p.kind === 'petal') p.x += Math.sin((p.max - p.life) * 0.08 + p.sway) * 0.7;
                    else if (p.kind === 'rise') p.vy += 0.007;
                    else if (p.kind === 'ember') p.vy -= 0.004;
                    else if (p.kind === 'fog') p.r += 0.55;
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
                } else if (p.kind === 'shard') {
                    // 自旋碎晶片：有棱角的粒子看起来才像冰、像碎石、像气劲撞出来的瓷片
                    ctx.save();
                    ctx.translate(x, y);
                    ctx.rotate((p.max - p.life) * (p.spin || 0.12));
                    ctx.globalAlpha = k * 0.9;
                    ctx.beginPath();
                    ctx.moveTo(0, -p.r * 2.1); ctx.lineTo(p.r, p.r); ctx.lineTo(-p.r, p.r * 0.8); ctx.closePath();
                    ctx.fill();
                    ctx.restore();
                } else if (p.kind === 'petal') {
                    ctx.save();
                    ctx.translate(x, y);
                    ctx.rotate((p.max - p.life) * (p.spin || 0.05) + p.sway);
                    ctx.globalAlpha = k * 0.85;
                    ctx.beginPath(); ctx.ellipse(0, 0, p.r * 1.5, p.r * 0.62, 0, 0, Math.PI * 2); ctx.fill();
                    ctx.restore();
                } else if (p.kind === 'ember') {
                    // 余烬：火苗本身会闪，亮度用 sin 抽一抽，不然就是一串匀速上升的圆点
                    ctx.globalAlpha = k * (0.55 + 0.45 * Math.sin(p.life * 0.28 + p.flick));
                    ctx.beginPath(); ctx.arc(x, y, p.r * (0.6 + k * 0.6), 0, Math.PI * 2); ctx.fill();
                } else if (p.kind === 'fog') {
                    const g = ctx.createRadialGradient(x, y, 0, x, y, p.r);
                    g.addColorStop(0, p.color);
                    g.addColorStop(1, 'rgba(0,0,0,0)');
                    ctx.globalAlpha = k * 0.42;
                    ctx.fillStyle = g;
                    ctx.beginPath(); ctx.arc(x, y, p.r, 0, Math.PI * 2); ctx.fill();
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
                    // 全屏白闪：只在头几帧有值，配合震屏就是“被大招正面抽中”的观感
                    case 'flash': {
                        ctx.globalAlpha = Math.pow(k, 2.2) * (s.a || 0.5);
                        ctx.fillStyle = s.color;
                        ctx.fillRect(0, 0, this.w, this.h);
                        break;
                    }
                    // 放射光束：从圆心外刺的星芒，落点开花与神通开场都用它
                    case 'rays': {
                        const n = s.n || 12, rot = (s.rot || 0) * t * 2.6;
                        const len = s.r * (0.42 + 0.58 * Math.sin(k * Math.PI));
                        ctx.lineWidth = 2.6;
                        for (let i = 0; i < n; i++) {
                            const a = i / n * Math.PI * 2 + rot;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * len * 0.18, s.y + Math.sin(a) * len * 0.18);
                            ctx.lineTo(s.x + Math.cos(a) * len, s.y + Math.sin(a) * len);
                            ctx.stroke();
                        }
                        break;
                    }
                    // 灵气漩涡：几臂螺线自转，施法与落点都很抬得起场面
                    case 'vortex': {
                        const arms = s.arms || 5, spin = (s.spin || 1) * t * 5;
                        ctx.lineWidth = 3 * k + 0.8;
                        for (let i = 0; i < arms; i++) {
                            const a0 = i / arms * Math.PI * 2 + spin;
                            ctx.beginPath();
                            for (let j = 0; j <= 12; j++) {
                                const pr = j / 12;
                                const rr = s.r * (0.12 + pr * 0.9) * (0.55 + 0.45 * k);
                                const aa = a0 + pr * 2.2;
                                const px = s.x + Math.cos(aa) * rr, py = s.y + Math.sin(aa) * rr * 0.72;
                                if (j) ctx.lineTo(px, py); else ctx.moveTo(px, py);
                            }
                            ctx.stroke();
                        }
                        break;
                    }
                    // 三层旋转法阵：外环刻度 + 中六芒 + 内环，比原来那个椭圆 rune 像一个真的阵
                    case 'runeRing': {
                        const spin = (s.spin || 1) * t * 3.4;
                        ctx.lineWidth = 2.6;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2); ctx.stroke();
                        ctx.lineWidth = 1.3;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r * 0.82, 0, Math.PI * 2); ctx.stroke();
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r * 0.4, 0, Math.PI * 2); ctx.stroke();
                        for (let i = 0; i < 24; i++) {
                            const a = i / 24 * Math.PI * 2 + spin, long = i % 3 === 0;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * s.r * 0.82, s.y + Math.sin(a) * s.r * 0.82);
                            ctx.lineTo(s.x + Math.cos(a) * s.r * (long ? 1.03 : 0.93), s.y + Math.sin(a) * s.r * (long ? 1.03 : 0.93));
                            ctx.stroke();
                        }
                        ctx.lineWidth = 1.6;
                        for (let i = 0; i < 6; i++) {
                            const a1 = i / 6 * Math.PI * 2 - spin, a2 = (i + 2) / 6 * Math.PI * 2 - spin;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a1) * s.r * 0.62, s.y + Math.sin(a1) * s.r * 0.62);
                            ctx.lineTo(s.x + Math.cos(a2) * s.r * 0.62, s.y + Math.sin(a2) * s.r * 0.62);
                            ctx.stroke();
                        }
                        break;
                    }
                    // 冰晶六角：先长主轴再分小枝，看起来是「结出来」而不是「贴上去」的
                    case 'frostWeb': {
                        const g = Math.min(1, t * 3.4);
                        ctx.lineWidth = 2.2;
                        for (let i = 0; i < 6; i++) {
                            const a = i / 6 * Math.PI * 2 - 0.2;
                            const ex = s.x + Math.cos(a) * s.r * g, ey = s.y + Math.sin(a) * s.r * g * 0.9;
                            ctx.beginPath(); ctx.moveTo(s.x, s.y); ctx.lineTo(ex, ey); ctx.stroke();
                            for (let j = 1; j <= 3; j++) {
                                const pr = j / 4;
                                const bx = s.x + Math.cos(a) * s.r * g * pr, by = s.y + Math.sin(a) * s.r * g * pr * 0.9;
                                const bl = s.r * 0.22 * (1 - pr * 0.4) * g;
                                ctx.beginPath();
                                ctx.moveTo(bx, by); ctx.lineTo(bx + Math.cos(a + 1.1) * bl, by + Math.sin(a + 1.1) * bl);
                                ctx.moveTo(bx, by); ctx.lineTo(bx + Math.cos(a - 1.1) * bl, by + Math.sin(a - 1.1) * bl);
                                ctx.stroke();
                            }
                        }
                        break;
                    }
                    // 火流星：头亮尾透，一路拖到落点，比一个固定圆球有轨迹得多
                    case 'meteor': {
                        const pr = 1 - k;
                        const x = s.x1 + (s.x2 - s.x1) * pr, y = s.y1 + (s.y2 - s.y1) * pr;
                        const bx = s.x1 + (s.x2 - s.x1) * Math.max(0, pr - 0.26), by = s.y1 + (s.y2 - s.y1) * Math.max(0, pr - 0.26);
                        const g = ctx.createLinearGradient(bx, by, x, y);
                        g.addColorStop(0, 'rgba(255,255,255,0)');
                        g.addColorStop(1, s.color);
                        ctx.strokeStyle = g; ctx.lineWidth = s.r * 1.2;
                        ctx.beginPath(); ctx.moveTo(bx, by); ctx.lineTo(x, y); ctx.stroke();
                        const rg = ctx.createRadialGradient(x, y, 0, x, y, s.r * 1.8);
                        rg.addColorStop(0, '#fff'); rg.addColorStop(.4, s.color); rg.addColorStop(1, 'rgba(0,0,0,0)');
                        ctx.fillStyle = rg;
                        ctx.beginPath(); ctx.arc(x, y, s.r * 1.8, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    // 弧形斩击：三层错位 + 白芯，从中间往两头扫开
                    case 'arcCut': {
                        const sweep = Math.min(1, t * 2.6);
                        for (let i = 0; i < 3; i++) {
                            ctx.lineWidth = (7 - i * 2) * k + 0.6;
                            ctx.strokeStyle = i === 0 ? '#ffffff' : s.color;
                            ctx.beginPath();
                            ctx.arc(s.x - i * 9 * (s.dir || 1), s.y, s.r - i * 15,
                                    -1.2 * sweep + i * 0.18, 1.3 * sweep + i * 0.18);
                            ctx.stroke();
                        }
                        break;
                    }
                    // 多叉雷电：主链 + 三道旁叉，每帧重新抖一次，所以看着是“活”的
                    case 'fork': {
                        const zig = (x1, y1, x2, y2, w, jit) => {
                            ctx.lineWidth = w;
                            ctx.beginPath(); ctx.moveTo(x1, y1);
                            for (let i = 1; i <= 6; i++) {
                                const pr = i / 6;
                                ctx.lineTo(x1 + (x2 - x1) * pr + (i === 6 ? 0 : (Math.random() - 0.5) * jit),
                                            y1 + (y2 - y1) * pr + (i === 6 ? 0 : (Math.random() - 0.5) * jit * 0.8));
                            }
                            ctx.stroke();
                        };
                        zig(s.x1, s.y1, s.x2, s.y2, 4 * k + 1.2, 34);
                        for (let i = 0; i < 3; i++) {
                            const pr = 0.3 + i * 0.2;
                            const bx = s.x1 + (s.x2 - s.x1) * pr, by = s.y1 + (s.y2 - s.y1) * pr;
                            zig(bx, by, bx + (Math.random() - 0.5) * 170, by + (Math.random() - 0.2) * 130, 1.8, 26);
                        }
                        ctx.fillStyle = s.hot || '#ffffff';
                        const hx = s.x1 + (s.x2 - s.x1) * (1 - k), hy = s.y1 + (s.y2 - s.y1) * (1 - k);
                        ctx.beginPath(); ctx.arc(hx, hy, 9, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
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
