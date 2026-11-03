/* 混沌万灵榜：法宝分页榜（数据来自 /api/treasures，页面只做展示与筛选） */
(function () {
    'use strict';

    const SIZE = 8;                       // 每页 8 件，24 件正好三页
    const TIER_COLOR = {
        '玄天仙品': ['#fef3c7', '#b45309'],
        '通天灵宝': ['#ede9fe', '#6d28d9'],
        '灵宝': ['#dbeafe', '#1d4ed8'],
        '法宝': ['#dcfce7', '#15803d'],
        '灵器': ['#e2e8f0', '#475569'],
        '法器': ['#f1f5f9', '#64748b'],
    };

    const state = { page: 1, tier: '全部', keyword: '', total: 0, pages: 1 };

    const el = id => document.getElementById(id);
    const esc = s => String(s == null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

    function tierStyle(tier) {
        const c = TIER_COLOR[tier] || ['#eef2f7', '#475569'];
        return `background:${c[0]};color:${c[1]}`;
    }

    function rankClass(rank) {
        if (rank === 1) return 'tl-rank top1';
        if (rank === 2) return 'tl-rank top2';
        if (rank === 3) return 'tl-rank top3';
        return 'tl-rank';
    }

    function renderTierBar(tiers) {
        const box = el('tlTierBar');
        if (!box) return;
        const list = ['全部'].concat(tiers || []);
        box.innerHTML = list.map(t => `
            <button class="tl-chip ${state.tier === t ? 'on' : ''}" data-tier="${esc(t)}">${esc(t)}</button>
        `).join('') + `
            <input class="tl-search" id="tlSearch" placeholder="搜法宝 / 持有者 / 类型"
                   value="${esc(state.keyword)}" autocomplete="off">
        `;
        box.querySelectorAll('.tl-chip').forEach(btn => {
            btn.addEventListener('click', () => {
                state.tier = btn.dataset.tier;
                state.page = 1;
                load();
            });
        });
        const input = el('tlSearch');
        let timer = 0;
        input.addEventListener('input', e => {
            clearTimeout(timer);
            const v = e.target.value;
            timer = setTimeout(() => {
                state.keyword = v.trim();
                state.page = 1;
                load();
            }, 240);
        });
    }

    function renderList(data) {
        const grid = el('tlGrid');
        const records = data.records || [];
        if (!records.length) {
            grid.innerHTML = '<div class="tl-empty">没有匹配的法宝，换个品阶或关键词试试。</div>';
        } else {
            grid.innerHTML = records.map(t => `
                <article class="tl-item">
                    <div class="tl-item-top">
                        <span class="${rankClass(t.rank)}">${t.rank}</span>
                        <span class="tl-name">${esc(t.name)}</span>
                        <span class="tl-tier" style="${tierStyle(t.tier)}">${esc(t.tier)}</span>
                    </div>
                    <div class="tl-meta">
                        <span>类型 <b>${esc(t.type)}</b></span>
                        <span>持有 <b>${esc(t.owner)}</b></span>
                        <span>${esc(t.origin)}</span>
                    </div>
                    <p class="tl-effect">${esc(t.effect)}</p>
                </article>
            `).join('');
        }
        el('tlCount').textContent = `共 ${data.total} 件 · 第 ${data.page}/${data.pages} 页`;
    }

    function renderPager(data) {
        const box = el('tlPager');
        const { page, pages } = data;
        const nums = [];
        for (let i = 1; i <= pages; i++) {
            if (pages <= 7 || i === 1 || i === pages || Math.abs(i - page) <= 1) nums.push(i);
            else if (nums[nums.length - 1] !== '...') nums.push('...');
        }
        box.innerHTML = `
            <button data-go="prev" ${page <= 1 ? 'disabled' : ''}>上一页</button>
            ${nums.map(n => n === '...'
                ? '<span class="tl-page-info">…</span>'
                : `<button data-go="${n}" class="${n === page ? 'on' : ''}">${n}</button>`).join('')}
            <button data-go="next" ${page >= pages ? 'disabled' : ''}>下一页</button>
            <span class="tl-page-info">每页 ${data.size} 件</span>
        `;
        box.querySelectorAll('button[data-go]').forEach(btn => {
            btn.addEventListener('click', () => {
                const go = btn.dataset.go;
                if (go === 'prev') state.page = Math.max(1, state.page - 1);
                else if (go === 'next') state.page = Math.min(state.pages, state.page + 1);
                else state.page = Number(go);
                load();
            });
        });
    }

    async function load() {
        const qs = new URLSearchParams({
            page: String(state.page), size: String(SIZE), tier: state.tier
        });
        if (state.keyword) qs.set('keyword', state.keyword);
        try {
            const res = await fetch('/api/treasures?' + qs.toString());
            const json = await res.json();
            if (!json || json.code !== 200) throw new Error((json && json.message) || '加载失败');
            const data = json.data;
            state.total = data.total;
            state.pages = data.pages;
            state.page = data.page;
            renderTierBar(data.tiers);
            renderList(data);
            renderPager(data);
        } catch (e) {
            el('tlGrid').innerHTML = `<div class="tl-empty">加载失败：${esc(e.message)}</div>`;
        }
    }

    document.addEventListener('DOMContentLoaded', load);
})();
