/* ==================== 修仙排行榜（人界篇）· 页面内核 ====================
 * 职责边界：
 *  - 榜单渲染：一次 /api/rank/roster 铺满境界塔、人物墙、战力榜、路数表；
 *  - 斗法播放：结果由服务端 DuelEngine 裁决（境界压制 + 功法克制 + 原著战绩覆盖），
 *    这里只把返回的招式时间轴「演」出来 —— 前端不参与任何胜负判断，改不动设定。
 * 全部样式走 rk- 前缀，特效画在一张 canvas 上，不往全局 CSS 里塞东西。
 */
(function () {
    'use strict';

    const RK = {
        realms: [],
        people: [],
        byId: new Map(),
        pickA: null,
        pickB: null,
        speed: 1,
        playing: false,
        timers: []
    };

    // 舞台坐标：两个人物的锚点，特效都从这里起落
    const ANCHOR = { A: 0.2, B: 0.8 };
    const BASE_MS = 1150;

    const $ = id => document.getElementById(id);
    const clearTimers = () => { RK.timers.forEach(t => clearTimeout(t)); RK.timers = []; };
    const later = (fn, ms) => { const t = setTimeout(fn, ms); RK.timers.push(t); return t; };

    document.addEventListener('DOMContentLoaded', loadRoster);
    $('rkRefresh').addEventListener('click', loadRoster);

    // ==================== 一、取榜单 ====================

    async function loadRoster() {
        $('rkUpdated').textContent = '加载中...';
        try {
            const data = await http.get('/rank/roster');
            RK.realms = data.realms || [];
            RK.people = data.people || [];
            RK.byId = new Map(RK.people.map(p => [p.userId, p]));
            renderTally(data);
            renderPyramid();
            renderStyleGuide(data.styles || {});
            renderTierBoard();
            renderPowerBoard();
            renderChips('rosterA', '');
            renderChips('rosterB', '');
            $('rkUpdated').textContent = '共 ' + data.total + ' 位 · ' + new Date().toLocaleTimeString('zh-CN');
            selectTier(RK.realms[RK.realms.length - 1]);
        } catch (e) {
            $('pyramidBox').innerHTML = '<div class="rk-empty">榜单取不到：' + escapeHtml(e.message || '接口异常') + '</div>';
            $('rkUpdated').textContent = '取数失败';
        }
    }

    function renderTally(data) {
        const spirit = RK.realms.find(r => r.realm === 'SOUL_TRANSFORMATION');
        $('rkTally').textContent = `${data.total} 位 · 化神 ${spirit ? spirit.count : 0} 位 · 凡人 ${data.realms[0].count} 位`;
    }

    // ==================== 二、境界塔 ====================

    /* 六层倒三角：化神在最窄的塔尖，凡人在最宽的塔底。
       塔尖之上再留一道虚线，标「人界天花板」—— 这一格 above 才是这部小说最残酷的地方：
       不是没人能到化神，是到了化神也没地方再往上走。 */
    function renderPyramid() {
        const order = [...RK.realms].reverse();          // 化神 → 凡人
        const W = 680, rowH = 58, top = 26, gap = 6;
        const minW = 130, maxW = 640;
        const H = top + order.length * (rowH + gap) + 54;
        let svg = `<svg id="pyramidSvg" viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="境界金字塔">`;
        svg += `<defs><filter id="rkSoft" x="-30%" y="-30%" width="160%" height="160%">
                  <feDropShadow dx="0" dy="3" stdDeviation="4" flood-color="#0f172a" flood-opacity="0.14"/>
                </filter></defs>`;
        svg += `<line x1="60" y1="${top - 8}" x2="${W - 60}" y2="${top - 8}" stroke="#f59e0b" stroke-width="1.5" stroke-dasharray="7 6" opacity=".75"/>`;
        svg += `<text x="${W - 58}" y="${top - 14}" text-anchor="end" font-size="11" fill="#b45309">人界天花板 · 化神之上无路由（灵气已薄）</text>`;

        order.forEach((r, i) => {
            const y = top + i * (rowH + gap);
            const wTop = minW + (maxW - minW) * (i / order.length);
            const wBot = minW + (maxW - minW) * ((i + 1) / order.length);
            const x1 = (W - wTop) / 2, x2 = (W + wTop) / 2, x3 = (W + wBot) / 2, x4 = (W - wBot) / 2;
            const points = `${x1},${y} ${x2},${y} ${x3},${y + rowH} ${x4},${y + rowH}`;
            const power = r.count ? '' : ' opacity=".42"';
            svg += `<g class="rk-py-row" data-realm="${r.realm}"${power} tabindex="0">
                      <polygon points="${points}" fill="${r.color}" fill-opacity=".88" filter="url(#rkSoft)"/>
                      <text class="rk-py-label" x="${W / 2}" y="${y + 26}" text-anchor="middle">${r.label}</text>
                      <text class="rk-py-count" x="${W / 2}" y="${y + 44}" text-anchor="middle">${r.count} 位 · 寿元${r.lifespan}</text>
                      <text class="rk-py-count" x="${x4 - 12}" y="${y + 34}" text-anchor="end">${escapeHtml(r.names.slice(0, 3).join('、'))}${r.names.length > 3 ? ' 等' : ''}</text>
                    </g>`;
        });
        svg += `<text x="${W / 2}" y="${H - 26}" text-anchor="middle" font-size="11.5" fill="#94a3b8">
                  ${RK.people.length} 位角色 · 塔底宽是因为人界绝大多数人一生走不出前两层</text></svg>`;
        $('pyramidBox').innerHTML = svg;
        $('pyramidBox').querySelectorAll('.rk-py-row').forEach(g => {
            g.addEventListener('click', () => {
                const realm = RK.realms.find(r => r.realm === g.dataset.realm);
                if (realm && realm.count) selectTier(realm);
            });
        });
    }

    function selectTier(realm) {
        if (!realm) return;
        $('pyramidBox').querySelectorAll('.rk-py-row').forEach(g =>
            g.classList.toggle('active', g.dataset.realm === realm.realm));
        const list = RK.people.filter(p => p.realm === realm.realm);
        const rows = list.slice(0, 6).map(p => `
            <a class="rk-tier-item" href="${p.homepage}" title="看 TA 的心得帖">
                <img src="${escapeAttr(p.avatar)}" alt="${escapeHtml(p.name)}" onerror="this.src='/images/default-avatar.svg'">
                <div>
                    <div class="rk-ti-name">${escapeHtml(p.name)} <span class="rk-badge ${p.basis.toLowerCase()}">${p.basisLabel}</span></div>
                    <div class="rk-ti-sub">${escapeHtml(p.subText)} · ${escapeHtml(p.styleLabel)} · ${escapeHtml(p.title)}</div>
                </div>
                <div class="rk-ti-power">战力<br><b>${p.power.toLocaleString('zh-CN')}</b></div>
            </a>`).join('');
        $('tierTop').innerHTML = `
            <div class="rk-tier-name"><span class="rk-tier-dot" style="background:${realm.color}"></span>${realm.label}
                <small style="font-weight:500;color:var(--text-light);font-size:12px;">${realm.rule}</small></div>
            <div class="rk-tier-meta">寿元 ${realm.lifespan} · 战力基数 ${realm.base.toLocaleString('zh-CN')} · 在榜 ${realm.count} 位<br>${escapeHtml(realm.note)}</div>
            <div class="rk-tier-list">${rows || '<div class="rk-empty">这一格本站没有收录人物</div>'}</div>`;
    }

    // ==================== 三、人物墙 ====================

    function renderTierBoard() {
        const order = [...RK.realms].slice().reverse();
        $('tierBoard').innerHTML = order.filter(r => r.count).map(r => `
            <div class="rk-tier-block">
                <div class="rk-tier-head"><span class="rk-tier-dot" style="background:${r.color}"></span>
                    ${r.label} · ${r.count} 位<span class="rk-rule">${r.rule}</span></div>
                <div class="rk-people">${RK.people.filter(p => p.realm === r.realm).map(personCard).join('')}</div>
            </div>`).join('');
    }

    function personCard(p) {
        const chips = p.skills.map(s => `
            <span class="rk-skill-chip ${s.kind.toLowerCase()}" title="${escapeAttr(s.desc)}">${escapeHtml(s.name)}</span>`).join('');
        return `
        <article class="rk-person" style="--pc:${p.realmColor}">
            <div class="rk-person-top">
                <img src="${escapeAttr(p.avatar)}" alt="${escapeHtml(p.name)}" onerror="this.src='/images/default-avatar.svg'">
                <div>
                    <div class="rk-person-name"><a href="${p.homepage}" style="color:inherit;text-decoration:none">${escapeHtml(p.name)}</a>
                        <span class="rk-badge ${p.basis.toLowerCase()}">${p.basisLabel}</span></div>
                    <div class="rk-person-title">${escapeHtml(p.realmLabel)}${escapeHtml(p.subText)} · ${escapeHtml(p.title)}</div>
                </div>
                <div class="rk-rank-no">#${p.rank}</div>
            </div>
            <div class="rk-person-meta">
                <b>功法</b> ${escapeHtml(p.technique)}<br>
                <b>路数</b> ${escapeHtml(p.styleLabel)} · ${escapeHtml(p.styleEssence)}<br>
                <b>出身</b> ${escapeHtml(p.origin)}<br>
                <b>标签</b> ${p.traits.map(escapeHtml).join(' / ')}<br>
                <b>考据</b> ${escapeHtml(p.basisNote)}
            </div>
            <div class="rk-power-track"><i class="rk-power-fill" style="background:${p.realmColor}" data-w="${p.powerBar}"></i></div>
            <div class="rk-quote">${escapeHtml(p.quote)}</div>
            <div class="rk-skills">${chips}</div>
        </article>`;
    }

    // ==================== 四、战力榜 ====================

    function renderPowerBoard() {
        $('powerBoard').innerHTML = RK.people.slice(0, 14).map(p => `
            <div class="rk-bar-row">
                <div class="rk-bar-rank ${p.rank <= 3 ? 't' + p.rank : ''}">${p.rank}</div>
                <div class="rk-bar-name" title="${escapeAttr(p.technique)}">${escapeHtml(p.name)}</div>
                <div class="rk-bar-track"><i class="rk-bar-fill" data-w="${p.powerBar}" style="background:linear-gradient(90deg,${p.realmColor},#1e293b)"></i></div>
                <div class="rk-bar-foot"><span>${p.power.toLocaleString('zh-CN')}</span> · ${escapeHtml(p.realmLabel)}</div>
            </div>`).join('') + '<p class="rk-sec-note">条长按对数压缩，否则凡人那一格会细到看不见 —— 这倒也符合人界篇的本意。</p>';
    }

    // ==================== 五、路数表 ====================

    function renderStyleGuide(styles) {
        $('styleGuide').innerHTML = Object.keys(styles).map(k => {
            const s = styles[k];
            return `<div class="rk-style"><b>${escapeHtml(s.label)}</b>
                <p>${escapeHtml(s.essence)}</p>
                <div class="rk-line">克 <i>${s.beats.map(escapeHtml).join('、') || '—'}</i></div>
                <div class="rk-line bad">畏 <i>${s.weak.map(escapeHtml).join('、') || '—'}</i></div>
            </div>`;
        }).join('');
    }

    // ==================== 六、选人 ====================

    function renderChips(boxId, keyword) {
        const kw = (keyword || '').trim().toLowerCase();
        const hit = RK.people.filter(p => !kw || [p.name, p.technique, p.realmLabel, p.styleLabel, p.title, p.origin]
            .join(' ').toLowerCase().includes(kw));
        const other = boxId === 'rosterA' ? RK.pickB : RK.pickA;
        $(boxId).innerHTML = hit.length
            ? hit.map(p => `<button class="rk-chip ${other === p.userId ? 'picked' : ''}" data-id="${p.userId}">
                    <span class="rk-dot" style="background:${p.realmColor}"></span>${escapeHtml(p.name)} · ${escapeHtml(p.realmLabel)}</button>`).join('')
            : '<div class="rk-empty">没人对得上这个词</div>';
        $(boxId).querySelectorAll('.rk-chip').forEach(btn =>
            btn.addEventListener('click', () => pick(boxId === 'rosterA' ? 'A' : 'B', Number(btn.dataset.id))));
    }

    function pick(side, id) {
        const p = RK.byId.get(id);
        if (!p) return;
        if (side === 'A') {
            if (RK.pickB === id) RK.pickB = null;
            RK.pickA = id;
        } else {
            if (RK.pickA === id) RK.pickA = null;
            RK.pickB = id;
        }
        paintPicked();
    }

    function paintPicked() {
        ['A', 'B'].forEach(side => {
            const id = side === 'A' ? RK.pickA : RK.pickB;
            const p = id ? RK.byId.get(id) : null;
            const box = $('picked' + side);
            box.className = 'rk-picked' + (p ? '' : ' empty');
            box.innerHTML = p
                ? `<img src="${escapeAttr(p.avatar)}" alt="" onerror="this.src='/images/default-avatar.svg'">
                   <div><div class="rk-pk-name">${escapeHtml(p.name)}</div>
                   <div class="rk-pk-realm">${escapeHtml(p.realmLabel + p.subText)} · ${escapeHtml(p.styleLabel)} · 战力 ${p.power.toLocaleString('zh-CN')}</div></div>`
                : '尚未选人';
            $('pick' + side).classList.toggle('ready', !!p);
            $('pick' + side).style.setProperty('--pc', p ? p.realmColor : '#6366f1');
        });
        $('rkFight').disabled = !(RK.pickA && RK.pickB);
        renderChips('rosterA', $('searchA').value);
        renderChips('rosterB', $('searchB').value);
        if (RK.pickA && RK.pickB) {
            const a = RK.byId.get(RK.pickA), b = RK.byId.get(RK.pickB);
            const gap = realmIdx(b.realm) - realmIdx(a.realm);
            $('rkMatchTip').textContent = gap === 0
                ? `同境界：${a.name} 与 ${b.name} 比的是小层、法宝、神识与谁先沉不住气。`
                : Math.abs(gap) >= 2
                    ? `差两个大境界：这一局按设定几乎没有悬念，看点是 ${(gap > 0 ? a : b).name} 能撑几合。`
                    : `差一整级：${(gap > 0 ? b : a).name} 占境界，${(gap > 0 ? a : b).name} 若有遁术倒有一线「全身而退」。`;
        }
    }

    const realmIdx = key => RK.realms.findIndex(r => r.realm === key);

    $('searchA').addEventListener('input', () => renderChips('rosterA', $('searchA').value));
    $('searchB').addEventListener('input', () => renderChips('rosterB', $('searchB').value));
    $('rkSpeed').addEventListener('click', e => {
        const btn = e.target.closest('button');
        if (!btn) return;
        RK.speed = Number(btn.dataset.speed);
        $('rkSpeed').querySelectorAll('button').forEach(b => b.classList.toggle('on', b === btn));
    });
    $('rkReset').addEventListener('click', () => { clearTimers(); RK.playing = false; resetStage(); });

    // ==================== 七、开打 ====================

    $('rkFight').addEventListener('click', async () => {
        if (RK.playing || !RK.pickA || !RK.pickB) return;
        RK.playing = true;
        $('rkFight').disabled = true;
        $('rkStageHint').textContent = '推演中...';
        try {
            const duel = await http.get(`/rank/duel?a=${RK.pickA}&b=${RK.pickB}`);
            resetStage();
            duel.a.avatar = duel.a.avatar || '/images/default-avatar.svg';
            duel.b.avatar = duel.b.avatar || '/images/default-avatar.svg';
            paintFighters(duel);
            $('rkSeedTip').textContent = `第 ${duel.outcome.rounds} 回合终 · 共 ${duel.events.length} 手 · 种子 ${duel.seed}`;
            play(duel);
        } catch (e) {
            $('rkStageHint').textContent = '推演失败：' + (e.message || '接口异常');
            RK.playing = false;
            $('rkFight').disabled = false;
        }
    });

    function paintFighters(duel) {
        ['A', 'B'].forEach((side, i) => {
            const f = i === 0 ? duel.a : duel.b;
            $('fg' + side).innerHTML = `
                <div class="rk-f-name">${escapeHtml(f.name)}<span class="rk-f-realm">${escapeHtml(f.realmLabel + f.subText)}</span></div>
                <div class="rk-bar hp" id="hpBar${side}"><i style="width:100%"></i><span id="hpTxt${side}">气血 ${f.hpMax}</span></div>
                <div class="rk-bar mp" id="mpBar${side}"><i style="width:100%"></i><span></span></div>
                <div class="rk-f-realm" style="margin-top:5px">${escapeHtml(f.styleLabel)} · ${escapeHtml(f.technique)}</div>`;
            $('fg' + side).style.setProperty('--pc', f.realmColor);
        });
    }

    function play(duel) {
        const { events, outcome } = duel;
        const step = () => Math.round(BASE_MS / RK.speed);
        events.forEach((ev, i) => later(() => runEvent(duel, ev, outcome, i === events.length - 1), step() * (i + 1)));
        const tail = step() * (events.length + 1);
        later(() => {
            showVerdict(outcome, duel);
            RK.playing = false;
            $('rkFight').disabled = false;
        }, tail);
    }

    function runEvent(duel, ev, outcome, isLast) {
        const actorSide = ev.actorId === duel.a.id ? 'A' : 'B';
        const foeSide = actorSide === 'A' ? 'B' : 'A';
        $('rkRoundTag').textContent = `第 ${ev.round} 合`;

        // 血条/灵力条：直接用服务端事件里的快照，前端不做任何数值推断
        setBar('hpBarA', duel.a.hpMax, ev.hpA, 'hpTxtA', '气血');
        setBar('hpBarB', duel.b.hpMax, ev.hpB, 'hpTxtB', '气血');
        setBar('mpBarA', duel.a.mpMax, ev.mpA, null, null);
        setBar('mpBarB', duel.b.mpMax, ev.mpB, null, null);

        const name = $('rkSkillName');
        name.textContent = ev.skill;
        name.classList.toggle('ult', !!ev.ult);
        name.classList.remove('play');
        void name.offsetWidth;          // 强制重排，让同一元素能连续重播动画
        name.classList.add('play');

        Fx.cast(ev, actorSide, foeSide);
        if (ev.damage > 0) floatText(foeSide, '-' + ev.damage, ev.crit ? 'crit' : '');
        if (ev.heal > 0) floatText(actorSide, '+' + ev.heal, 'heal');
        if (ev.crit) floatText(foeSide, '破绽!', 'crit');

        const log = $('rkLog');
        const item = document.createElement('div');
        item.className = 'rk-log-item' + (ev.ult ? ' ult' : '');
        item.innerHTML = `<span class="rk-lg-r">${ev.round}</span>
            <span class="rk-lg-n">${escapeHtml((ev.actorId === duel.a.id ? duel.a.name : duel.b.name))} · ${escapeHtml(ev.skill)}</span>
            <span class="rk-lg-t">${escapeHtml(ev.line)}${ev.damage ? ` 气血 -${ev.damage}` : ''}${ev.heal ? ` 气血 +${ev.heal}` : ''}${ev.crit ? '（命中破绽）' : ''}</span>`;
        log.prepend(item);
        log.scrollTop = 0;

        if (isLast) {
            setTimeout(() => {
                $('rkStageHint').textContent = outcome.headline;
            }, Math.round(BASE_MS / RK.speed));
        }
    }

    function setBar(barId, max, value, txtId, label) {
        const bar = $(barId);
        if (!bar) return;
        const pct = Math.max(0, Math.min(100, value / max * 100));
        bar.querySelector('i').style.width = pct + '%';
        bar.classList.toggle('low', pct < 35);
        if (txtId && label) $(txtId).textContent = label + ' ' + value;
    }

    function showVerdict(outcome, duel) {
        $('rkVerdictHead').textContent = outcome.headline;
        $('rkVerdictBody').textContent = outcome.verdict;
        $('rkVerdictSrc').textContent = `裁决方式：${outcome.method} · 原著依据：${outcome.basis} · 战力 ${Math.round(outcome.powerA).toLocaleString('zh-CN')} vs ${Math.round(outcome.powerB).toLocaleString('zh-CN')}`;
        $('rkVerdict').classList.add('show');
    }

    function resetStage() {
        clearTimers();
        $('rkLog').innerHTML = '';
        $('rkVerdict').classList.remove('show');
        $('rkRoundTag').textContent = '待 手';
        $('rkStageHint').textContent = '选好两位，点下方「开始斗法」。';
        ['A', 'B'].forEach(s => {
            ['hpBar', 'mpBar'].forEach(p => {
                const bar = $(p + s);
                if (bar) { bar.querySelector('i').style.width = '100%'; bar.classList.remove('low'); }
            });
        });
        Fx.reset();
    }

    // ==================== 八、特效层 ====================

    /* canvas 一层：背景灵雾常驻，出手时按 vfx 键加粒子与图形。
       全部用 requestAnimationFrame 驱动，页面切走时暂停，避免留一堆后台循环。 */
    const Fx = {
        canvas: null, ctx: null, parts: [], shapes: [], running: false,

        init() {
            this.canvas = $('rkFx');
            this.ctx = this.canvas.getContext('2d');
            this.resize();
            window.addEventListener('resize', () => this.resize());
            document.addEventListener('visibilitychange', () => {
                if (!document.hidden && !this.running) this.loop();
            });
            this.seedMist();
            this.loop();
        },

        resize() {
            const stage = $('rkStage');
            const dpr = window.devicePixelRatio || 1;
            this.w = stage.clientWidth;
            this.h = stage.clientHeight;
            this.canvas.width = Math.round(this.w * dpr);
            this.canvas.height = Math.round(this.h * dpr);
            this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        },

        seedMist() {
            for (let i = 0; i < 26; i++) {
                this.parts.push({
                    kind: 'mist', x: Math.random() * (this.w || 800), y: Math.random() * (this.h || 400),
                    vx: (Math.random() - 0.5) * 0.25, vy: -0.12 - Math.random() * 0.2,
                    life: 999999, max: 999999, r: 12 + Math.random() * 30, hue: 235 + Math.random() * 40
                });
            }
        },

        pos(side) {
            return { x: (side === 'A' ? ANCHOR.A : ANCHOR.B) * this.w, y: this.h * 0.58 };
        },

        reset() { this.parts = this.parts.filter(p => p.kind === 'mist'); this.shapes = []; },

        burst(p, n, color, speed, life) {
            for (let i = 0; i < n; i++) {
                const a = Math.random() * Math.PI * 2;
                const s = speed * (0.35 + Math.random() * 0.9);
                this.parts.push({
                    kind: 'dot', x: p.x, y: p.y, vx: Math.cos(a) * s, vy: Math.sin(a) * s - 0.4,
                    life: life * (0.6 + Math.random() * 0.7), max: life, r: 1.4 + Math.random() * 2.8, color
                });
            }
        },

        streak(from, to, color, count, jitter) {
            for (let i = 0; i < count; i++) {
                const dx = to.x - from.x, dy = to.y - from.y;
                this.parts.push({
                    kind: 'streak', x: from.x + (Math.random() - 0.5) * jitter, y: from.y + (Math.random() - 0.5) * jitter,
                    vx: dx / 26 + (Math.random() - 0.5) * 1.4, vy: dy / 26 + (Math.random() - 0.5) * 1.4,
                    life: 26, max: 26, color, len: 8 + Math.random() * 16
                });
            }
        },

        ring(p, color, radius, life, width) {
            this.shapes.push({ kind: 'ring', x: p.x, y: p.y, color, r: 10, maxR: radius, life, max: life, width: width || 3 });
        },

        cast(ev, actorSide, foeSide) {
            const from = this.pos(actorSide), to = this.pos(foeSide);
            const dir = actorSide === 'A' ? 1 : -1;
            const stage = $('rkStage');
            const ult = ev.ult;
            const shake = cls => {
                stage.classList.remove('shake', 'shake-hard');
                void stage.offsetWidth;
                stage.classList.add(cls);
            };

            if (ult) { shake('shake-hard'); stage.classList.add('flash'); later(() => stage.classList.remove('flash'), 520); }
            else if (ev.damage > 240) shake('shake');

            switch (ev.vfx) {
                case 'SLASH':
                    this.shapes.push({ kind: 'slash', x: to.x, y: to.y, color: '#e0f2fe', life: 22, max: 22, dir, r: 58 });
                    this.burst(to, ult ? 26 : 14, '#bae6fd', 4.2, 34);
                    break;
                case 'SWORD_RAIN':
                    for (let i = 0; i < (ult ? 34 : 18); i++) {
                        this.parts.push({
                            kind: 'sword', x: to.x + (Math.random() - 0.5) * 150, y: -20 - Math.random() * 120,
                            vx: (Math.random() - 0.5) * 0.6, vy: 6 + Math.random() * 5, life: 46, max: 46, color: '#c7d2fe'
                        });
                    }
                    this.burst(to, 16, '#a5b4fc', 3.6, 30);
                    break;
                case 'GOLD_BEAM':
                    this.shapes.push({ kind: 'beam', x1: from.x, y1: from.y, x2: to.x, y2: to.y, color: '#fcd34d', life: 30, max: 30, width: ult ? 26 : 14 });
                    this.burst(to, 30, '#fbbf24', 5, 42);
                    break;
                case 'ICE_FROST':
                    this.ring(to, '#a5f3fc', ult ? 170 : 110, 40, 2);
                    this.burst(to, 26, '#67e8f9', 3.2, 52);
                    break;
                case 'FIRE_ORB':
                    this.shapes.push({ kind: 'orb', x1: from.x, y1: from.y, x2: to.x, y2: to.y, color: '#fb923c', life: 26, max: 26, r: 16 });
                    this.burst(to, 22, '#f97316', 4.4, 36);
                    break;
                case 'SHIELD':
                    this.ring(from, '#7dd3fc', 92, 34, 3);
                    this.ring(from, '#dbeafe', 70, 28, 2);
                    break;
                case 'TURTLE_SHELL':
                    this.shapes.push({ kind: 'hex', x: from.x, y: from.y, color: '#a3a380', life: 44, max: 44, r: 74 });
                    this.burst(from, 10, '#d6d3d1', 2, 40);
                    break;
                case 'MIND_WAVE':
                    this.ring(from, '#c4b5fd', 260, 46, 2);
                    this.ring(from, '#a78bfa', 190, 38, 2);
                    break;
                case 'SOUND_RING':
                    this.ring(from, '#f0abfc', 240, 44, 3);
                    this.ring(from, '#e879f9', 160, 34, 2);
                    this.burst(to, 12, '#f5d0fe', 2.6, 34);
                    break;
                case 'BLOOD_SHADOW':
                    this.shapes.push({ kind: 'ghost', x1: from.x, y1: from.y, x2: to.x, y2: to.y, color: '#f87171', life: 26, max: 26 });
                    this.burst(from, 18, '#ef4444', 3.4, 30);
                    break;
                case 'MAGNET_BEAM':
                    this.shapes.push({ kind: 'pull', x: to.x, y: to.y, color: '#93c5fd', life: 38, max: 38 });
                    this.burst(from, 12, '#bfdbfe', 2.4, 34);
                    break;
                case 'ARRAY_PRESS':
                    this.shapes.push({ kind: 'grid', x: to.x, y: to.y, color: '#fcd34d', life: 40, max: 40, r: 150 });
                    this.burst(to, 20, '#fde68a', 2.8, 44);
                    break;
                case 'POISON_MIST':
                    for (let i = 0; i < 20; i++) {
                        this.parts.push({
                            kind: 'mist', x: to.x + (Math.random() - 0.5) * 90, y: to.y + (Math.random() - 0.5) * 70,
                            vx: (Math.random() - 0.5) * 0.7, vy: -0.4 - Math.random() * 0.5,
                            life: 60, max: 60, r: 16 + Math.random() * 22, hue: 110 + Math.random() * 30
                        });
                    }
                    break;
                case 'SUMMON':
                    for (let i = 0; i < (ult ? 26 : 14); i++) {
                        this.parts.push({
                            kind: 'rise', x: to.x + (Math.random() - 0.5) * 180, y: this.h,
                            vx: (Math.random() - 0.5) * 0.8, vy: -3 - Math.random() * 3,
                            life: 50, max: 50, r: 2 + Math.random() * 3, color: '#86efac'
                        });
                    }
                    break;
                case 'MOON_GLOW':
                    this.shapes.push({ kind: 'bloom', x: actorSide === 'A' ? to.x : from.x, y: this.h * 0.26, color: '#e0f2fe', life: 46, max: 46, r: 78 });
                    this.burst(from, 14, '#bae6fd', 2.2, 46);
                    break;
                case 'HEAL_GLOW':
                    this.burst(from, 16, '#4ade80', 2, 48);
                    this.ring(from, '#86efac', 78, 36, 2);
                    break;
                case 'QI_PALM':
                    this.shapes.push({ kind: 'palm', x: to.x, y: to.y, color: '#fdba74', life: 26, max: 26, dir, r: 62 });
                    this.burst(to, 14, '#fb923c', 3.6, 30);
                    break;
                case 'MORTAL_PUNCH':
                    this.burst(to, 8, '#e2e8f0', 2.2, 24);
                    break;
                case 'ULT_NOVA':
                    this.shapes.push({ kind: 'nova', x: to.x, y: to.y, color: '#fef3c7', life: 52, max: 52, r: Math.max(this.w, this.h) });
                    this.burst(to, 46, '#fde68a', 6.2, 60);
                    this.burst(to, 22, '#fff', 4, 46);
                    break;
                default:
                    this.burst(to, 10, '#cbd5e1', 3, 28);
            }
        },

        loop() {
            this.running = true;
            const ctx = this.ctx;
            const draw = () => {
                if (document.hidden) { this.running = false; return; }
                ctx.clearRect(0, 0, this.w, this.h);
                this.stepMist(ctx);
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
                if (p.y < -40) { p.y = this.h + 20; p.x = Math.random() * this.w; }
                if (p.x < -40) p.x = this.w + 20; else if (p.x > this.w + 40) p.x = -20;
                const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r);
                g.addColorStop(0, `hsla(${p.hue},80%,70%,0.14)`);
                g.addColorStop(1, 'hsla(230,60%,60%,0)');
                ctx.fillStyle = g;
                ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
            });
            ctx.restore();
        },

        stepParts(ctx) {
            this.parts = this.parts.filter(p => p.kind !== 'mist');
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.parts.forEach(p => {
                p.life--;
                p.x += p.vx; p.y += p.vy; p.vy += (p.kind === 'sword' ? 0.18 : 0.06);
                const k = Math.max(0, p.life / p.max);
                ctx.globalAlpha = k;
                ctx.fillStyle = p.color; ctx.strokeStyle = p.color;
                if (p.kind === 'streak') {
                    ctx.lineWidth = 2;
                    ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(p.x - p.vx * p.len / 4, p.y - p.vy * p.len / 4); ctx.stroke();
                } else if (p.kind === 'sword') {
                    ctx.lineWidth = 2.2;
                    ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(p.x - p.vx * 2.4, p.y - p.vy * 2.4); ctx.stroke();
                    ctx.beginPath(); ctx.arc(p.x, p.y, 1.8, 0, Math.PI * 2); ctx.fill();
                } else if (p.kind === 'rise') {
                    ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
                } else {
                    ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
                }
            });
            this.parts = this.parts.filter(p => p.life > 0);
            ctx.restore();
        },

        stepShapes(ctx) {
            ctx.save();
            ctx.globalCompositeOperation = 'lighter';
            this.shapes = this.shapes.filter(s => {
                s.life--;
                const k = Math.max(0, s.life / s.max);
                ctx.globalAlpha = k;
                ctx.strokeStyle = s.color; ctx.fillStyle = s.color;
                const t = 1 - k;
                switch (s.kind) {
                    case 'ring': {
                        ctx.lineWidth = s.width;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r + (s.maxR - s.r) * (1 - Math.pow(1 - (1 - k), 2)), 0, Math.PI * 2); ctx.stroke();
                        break;
                    }
                    case 'beam': {
                        const w = s.width * k;
                        const g = ctx.createLinearGradient(s.x1, s.y1, s.x2, s.y2);
                        g.addColorStop(0, 'rgba(255,255,255,0.1)'); g.addColorStop(0.5, s.color); g.addColorStop(1, 'rgba(255,255,255,0.9)');
                        ctx.strokeStyle = g; ctx.lineWidth = w;
                        ctx.beginPath(); ctx.moveTo(s.x1, s.y1); ctx.lineTo(s.x2, s.y2); ctx.stroke();
                        ctx.lineWidth = w * 0.3; ctx.strokeStyle = '#fff'; ctx.stroke();
                        break;
                    }
                    case 'orb': {
                        const x = s.x1 + (s.x2 - s.x1) * (1 - k), y = s.y1 + (s.y2 - s.y1) * (1 - k);
                        const g = ctx.createRadialGradient(x, y, 0, x, y, s.r * 2.2);
                        g.addColorStop(0, '#fff'); g.addColorStop(0.4, s.color); g.addColorStop(1, 'rgba(249,115,22,0)');
                        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, s.r * 2.2, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'slash': {
                        ctx.lineWidth = 4 * k + 1;
                        for (let i = 0; i < 3; i++) {
                            ctx.beginPath();
                            ctx.arc(s.x, s.y, s.r - i * 12, -0.9 + i * 0.2, 1.1 + i * 0.2);
                            ctx.stroke();
                        }
                        break;
                    }
                    case 'palm': {
                        ctx.lineWidth = 6 * k;
                        ctx.beginPath(); ctx.arc(s.x, s.y, s.r * (1 - t * 0.4), s.dir > 0 ? Math.PI * 0.6 : -Math.PI * 0.6, s.dir > 0 ? Math.PI * 1.4 : Math.PI * 0.4);
                        ctx.stroke();
                        break;
                    }
                    case 'hex': {
                        ctx.lineWidth = 3;
                        for (let i = 0; i < 6; i++) {
                            const a = i / 6 * Math.PI * 2 + t;
                            ctx.beginPath();
                            ctx.moveTo(s.x + Math.cos(a) * s.r * 0.6, s.y + Math.sin(a) * s.r * 0.6);
                            ctx.lineTo(s.x + Math.cos(a) * s.r, s.y + Math.sin(a) * s.r);
                            ctx.stroke();
                        }
                        break;
                    }
                    case 'grid': {
                        ctx.lineWidth = 1.4;
                        const n = 5, size = s.r;
                        for (let i = 0; i <= n; i++) {
                            const o = -size / 2 + i * size / n;
                            ctx.beginPath(); ctx.moveTo(s.x + o, s.y - size / 2); ctx.lineTo(s.x + o, s.y + size / 2); ctx.stroke();
                            ctx.beginPath(); ctx.moveTo(s.x - size / 2, s.y + o); ctx.lineTo(s.x + size / 2, s.y + o); ctx.stroke();
                        }
                        break;
                    }
                    case 'pull': {
                        ctx.lineWidth = 2;
                        for (let i = 0; i < 5; i++) {
                            const a = i / 5 * Math.PI * 2 + t * 3;
                            const rr = 130 * k;
                            ctx.beginPath(); ctx.moveTo(s.x + Math.cos(a) * rr, s.y + Math.sin(a) * rr * 0.7);
                            ctx.lineTo(s.x, s.y); ctx.stroke();
                        }
                        break;
                    }
                    case 'bloom': {
                        const g = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, s.r);
                        g.addColorStop(0, 'rgba(255,255,255,0.7)'); g.addColorStop(1, 'rgba(224,242,254,0)');
                        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2); ctx.fill();
                        break;
                    }
                    case 'ghost': {
                        ctx.globalAlpha = k * 0.55;
                        for (let i = 0; i < 4; i++) {
                            const p = (1 - k) + i * 0.12;
                            const x = s.x1 + (s.x2 - s.x1) * Math.min(1, p);
                            const y = s.y1 + (s.y2 - s.y1) * Math.min(1, p);
                            ctx.fillStyle = s.color;
                            ctx.fillRect(x - 12, y - 42, 24, 74);
                        }
                        break;
                    }
                    case 'nova': {
                        const rr = s.r * (1 - Math.pow(k, 2.2));
                        const g = ctx.createRadialGradient(s.x, s.y, Math.max(0, rr * 0.55), s.x, s.y, rr);
                        g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.72, s.color); g.addColorStop(1, 'rgba(254,243,199,0)');
                        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(s.x, s.y, rr, 0, Math.PI * 2); ctx.fill();
                        ctx.lineWidth = 3; ctx.strokeStyle = '#fff';
                        for (let i = 0; i < 12; i++) {
                            const a = i / 12 * Math.PI * 2;
                            ctx.beginPath(); ctx.moveTo(s.x + Math.cos(a) * rr * 0.4, s.y + Math.sin(a) * rr * 0.4);
                            ctx.lineTo(s.x + Math.cos(a) * rr, s.y + Math.sin(a) * rr); ctx.stroke();
                        }
                        break;
                    }
                }
                return s.life > 0;
            });
            ctx.restore();
        }
    };

    function floatText(side, text, cls) {
        const p = Fx.pos(side);
        const el = document.createElement('div');
        el.className = 'rk-dmg ' + (cls || '');
        el.textContent = text;
        el.style.left = (p.x + (Math.random() - 0.5) * 60) + 'px';
        el.style.top = (p.y - 54) + 'px';
        $('rkStage').appendChild(el);
        later(() => el.remove(), 1100);
    }

    Fx.init();
})();
