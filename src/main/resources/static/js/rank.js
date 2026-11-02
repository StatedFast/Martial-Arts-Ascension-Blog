/* ==================== 修仙排行榜（人界篇）· 榜单页 ====================
 * 职责边界：
 *  - 一次 /api/rank/roster 铺满境界塔、各境界人物、战力榜；
 *  - 不做演出。选两人打起来是「斗法台」（/duel，/js/duel.js）那一页的事，
 *    那页需要的人物、动作、报招与特效都在那里，榜单页只回答「谁在哪一格」。
 * 全部样式走 rk- 前缀，写在 rank.html 的 <style> 里，不往全局 CSS 塞东西。
 */
(function () {
    'use strict';

    const RK = {
        realms: [],
        people: [],
        byId: new Map()
    };

    const $ = id => document.getElementById(id);

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
            renderTierBoard();
            renderPowerBoard();
            growBars();
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

    /* 战力条CSS里是 width:0 + transition，宽度值写在 data-w 上。
       必须等这一帧画完再赋值，否则浏览器只看到「一开始就那么宽」，动画不会跑。 */
    function growBars() {
        requestAnimationFrame(() => requestAnimationFrame(() => {
            document.querySelectorAll('[data-w]').forEach(el => {
                el.style.width = (parseFloat(el.dataset.w) || 0) + '%';
            });
        }));
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
            <div class="rk-power-track"><i class="rk-power-fill" data-w="${p.powerBar}" style="background:${p.realmColor}"></i></div>
            <div class="rk-quote">${escapeHtml(p.quote)}</div>
            <div class="rk-skills">${chips}</div>
            <div class="rk-person-foot">
                <a class="rk-duel-link" href="/duel" title="到斗法台把 ${escapeAttr(p.name)} 摆上桌">⚔ 斗法台</a>
                <span class="rk-voice-hint" title="${escapeAttr((p.voice || {}).timbre || '')}">
                    报招音色 · ${escapeHtml((p.voice || {}).timbre || '未标注')}</span>
            </div>
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
})();
