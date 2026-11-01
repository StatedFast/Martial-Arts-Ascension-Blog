/**
 * Spring Blog - 前端 JavaScript
 * API 封装 & 通用工具
 */

const API_BASE = '/api';

// ==================== HTTP 请求封装 ====================
const http = {
    async request(url, options = {}) {
        const token = localStorage.getItem('token');
        const headers = {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            ...options.headers
        };
        // 文件上传（FormData）：不能手动设 Content-Type，交给浏览器生成 multipart boundary
        if (options.body instanceof FormData) {
            delete headers['Content-Type'];
        }

        const res = await fetch(API_BASE + url, { ...options, headers });
        const data = await res.json();

        if (data.code === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
            throw new Error('未登录');
        }
        if (data.code !== 200) {
            throw new Error(data.message || '请求失败');
        }
        return data.data;
    },

    get(url, params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.request(url + (query ? '?' + query : ''));
    },

    post(url, body) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    put(url, body) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    delete(url) {
        return this.request(url, { method: 'DELETE' });
    },

    /**
     * 文件上传：单个文件以 form-data 提交，返回后端 data
     */
    upload(url, file, field = 'file') {
        const fd = new FormData();
        fd.append(field, file);
        return this.request(url, { method: 'POST', body: fd });
    }
};

// ==================== Toast 提示 ====================
const Toast = {
    show(message, type = 'success', duration = 3000) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), duration);
    }
};

// ==================== 用户状态管理 ====================
const Auth = {
    isLoggedIn() {
        return !!localStorage.getItem('token');
    },

    getUser() {
        try {
            return JSON.parse(localStorage.getItem('user'));
        } catch {
            return null;
        }
    },

    saveAuth(token, user) {
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
    },

    getUserId() {
        const user = this.getUser();
        return user ? user.id : null;
    }
};

// ==================== 导航栏渲染 ====================
/** 是否管理员（与后端 role=ADMIN 对齐），前端仅用于控制入口显隐，真正的权限拦截在后端 */
function isAdmin(user) {
    return !!user && typeof user.role === 'string' && user.role.trim().toUpperCase() === 'ADMIN';
}

function renderNavbar() {
    const navLinks = document.getElementById('navLinks');
    if (!navLinks) return;

    const user = Auth.getUser();
    if (Auth.isLoggedIn() && user) {
        navLinks.innerHTML = `
            <a href="/">首页</a>
            <a href="/write" class="btn-write">写文章</a>
            <a href="/notifications" class="nav-bell" title="通知中心">
                🔔<span class="nav-bell-badge" id="navBellBadge" hidden>0</span>
            </a>
            <div class="user-menu" onclick="toggleUserMenu()">
                <img src="${user.avatar || '/images/default-avatar.svg'}" alt="头像" class="user-avatar"
                     onerror="this.onerror=null;this.src='/images/default-avatar.svg'">
                <span>${escapeHtml(user.nickname || user.username)}</span>
                <div class="user-dropdown" id="userDropdown">
                    <a href="/profile">个人中心</a>
                    <a href="/notifications">通知中心</a>
                    ${isAdmin(user) ? '<a href="/config">配置中心</a>' : ''}
                    <a href="#" class="logout" onclick="Auth.logout();return false;">退出登录</a>
                </div>
            </div>
        `;
        NotificationCenter.start();
    } else {
        navLinks.innerHTML = `
            <a href="/">首页</a>
            <a href="/login">登录</a>
            <a href="/register" class="btn btn-primary btn-sm">注册</a>
        `;
    }
}

function toggleUserMenu() {
    const dropdown = document.getElementById('userDropdown');
    if (dropdown) {
        dropdown.classList.toggle('show');
    }
}

// 点击外部关闭下拉菜单
document.addEventListener('click', function(e) {
    const menu = document.querySelector('.user-menu');
    if (menu && !menu.contains(e.target)) {
        const dropdown = document.getElementById('userDropdown');
        if (dropdown) dropdown.classList.remove('show');
    }
});

// ==================== HTML 转义（统一工具） ====================
/**
 * 所有插入 innerHTML 的用户内容都必须先过这里。
 * 注意：escapeHtml 只处理文本节点风险，不能用于属性值里的单引号拼接（旧版回复按钮的 XSS 就是这样来的）。
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
    return div.innerHTML;
}

/** 属性值安全化：在模板里用 data-* 传参，避免把昵称直接拼进 onclick */
function escapeAttr(text) {
    return escapeHtml(text).replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ==================== 站内通知（铃铛 + 未读红点 + SSE） ====================
/**
 * 实时通知：
 * - 登录后拉一次未读数，并订阅 /api/notifications/stream
 * - EventSource 不支持自定义请求头，所以用 fetch + ReadableStream 手动解析 SSE
 * - 断线 5 秒后自动重连，同时保留 60 秒轮询兜底
 */
const NotificationCenter = {
    started: false,

    start() {
        if (this.started || !Auth.isLoggedIn()) return;
        this.started = true;
        this.refresh();
        this.connect();
        setInterval(() => this.refresh(), 60000);
    },

    async refresh() {
        if (!Auth.isLoggedIn()) return;
        try {
            const data = await http.get('/notifications/unread-count');
            this.render(data ? data.count : 0);
        } catch (e) {
            // 静默失败：红点不是关键路径，网络抖动时下一轮轮询会补上
        }
    },

    render(count) {
        const badge = document.getElementById('navBellBadge');
        if (!badge) return;
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.hidden = false;
        } else {
            badge.hidden = true;
        }
    },

    async connect() {
        while (Auth.isLoggedIn()) {
            try {
                const token = localStorage.getItem('token');
                const res = await fetch(API_BASE + '/notifications/stream', {
                    headers: token ? { 'Authorization': `Bearer ${token}` } : {}
                });
                if (!res.ok || !res.body) throw new Error('stream closed');

                const reader = res.body.getReader();
                const decoder = new TextDecoder('utf-8');
                let buffer = '';
                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;
                    buffer += decoder.decode(value, { stream: true });
                    let idx;
                    while ((idx = buffer.indexOf('\n\n')) >= 0) {
                        const event = buffer.slice(0, idx);
                        buffer = buffer.slice(idx + 2);
                        this.handleEvent(event);
                    }
                }
            } catch (e) {
                // 连接异常：走下方重连
            }
            await new Promise(resolve => setTimeout(resolve, 5000));
        }
    },

    handleEvent(raw) {
        // 心跳是注释行（: ping），没有 data 前缀，直接忽略
        const dataLine = raw.split('\n').find(line => line.startsWith('data:'));
        if (!dataLine) return;
        try {
            const payload = JSON.parse(dataLine.slice(5).trim());
            if (typeof payload.count === 'number') {
                this.render(payload.count);
            }
            if (payload.type) {
                Toast.show(payload.content || '你有一条新通知');
            }
        } catch (e) {
            // 非法帧忽略
        }
    }
};

// ==================== 时间格式化 ====================
function formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;

    if (diff < minute) return '刚刚';
    if (diff < hour) return Math.floor(diff / minute) + '分钟前';
    if (diff < day) return Math.floor(diff / hour) + '小时前';
    if (diff < 7 * day) return Math.floor(diff / day) + '天前';

    return date.getFullYear() + '-' +
           String(date.getMonth() + 1).padStart(2, '0') + '-' +
           String(date.getDate()).padStart(2, '0');
}

// ==================== 页面初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
});

// ==================== AI 助手（Agent 前端） ====================
/**
 * AI 助手对话框（右下角悬浮）
 * - UI 动态注入：页面只需调用 AiAssistant.init()
 * - 对接 /api/ai/chat/stream（SSE 流式，打字机效果）
 * - EventSource 不支持 POST + Authorization 头，故用 fetch + ReadableStream 手动解析 SSE
 * - 回复按轻量 Markdown 渲染（先整体转义再生成标签，天然防 XSS）
 */
const AiAssistant = {
    streaming: false,
    initialized: false,
    welcomed: false,

    /** 注入 UI 并绑定事件（页面只需调用一次） */
    init() {
        if (this.initialized) return;
        this.initialized = true;

        document.body.insertAdjacentHTML('beforeend', `
            <button class="ai-fab" id="aiFab" title="AI 助手">🤖</button>
            <div class="ai-panel" id="aiPanel">
                <div class="ai-header">
                    <div class="ai-header-info">
                        <div class="ai-header-avatar">🤖<span class="ai-status-dot"></span></div>
                        <div>
                            <div class="ai-header-title">银月 · AI 助手</div>
                            <div class="ai-header-sub">在线 · 找文章 / 看排行 / 总结内容</div>
                        </div>
                    </div>
                    <button class="ai-close" id="aiClose" title="关闭">✕</button>
                </div>
                <div class="ai-messages" id="aiMessages"></div>
                <div class="ai-login-tip" id="aiLoginTip" style="display:none">
                    <a href="/login">登录</a> 后即可与 AI 助手对话
                </div>
                <div class="ai-input-area" id="aiInputArea">
                    <textarea class="ai-input" id="aiInput" rows="1"
                              placeholder="试试：推荐几篇文章 / 热门排行 / 我发布了多少篇文章"></textarea>
                    <button class="ai-send" id="aiSend" title="发送">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round">
                            <path d="M22 2 11 13"></path>
                            <path d="M22 2 15 22 11 13 2 9z"></path>
                        </svg>
                        <span class="ai-spinner"></span>
                    </button>
                </div>
            </div>
        `);

        document.getElementById('aiFab').addEventListener('click', () => this.toggle());
        document.getElementById('aiClose').addEventListener('click', () => this.toggle());
        document.getElementById('aiSend').addEventListener('click', () => this.send());

        const input = document.getElementById('aiInput');
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.send();
            }
        });
        // 输入框自适应高度
        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 96) + 'px';
        });

        // 快捷问题：事件委托，点击直接发送
        document.getElementById('aiMessages').addEventListener('click', (e) => {
            const chip = e.target.closest('.ai-chip');
            if (chip) this.send(chip.dataset.q);
        });
    },

    toggle() {
        const panel = document.getElementById('aiPanel');
        const show = !panel.classList.contains('show');
        panel.classList.toggle('show', show);
        if (!show) return;

        this.refreshLoginState();
        if (!this.welcomed) {
            this.welcomed = true;
            this.appendWelcome();
        }
        document.getElementById('aiInput').focus();
    },

    /** 欢迎卡片：头像 + 简介 + 快捷问题（点击直接发送） */
    appendWelcome() {
        const chips = [
            ['🔍', '推荐几篇和 JWT 有关的文章'],
            ['🔥', '看看热门文章排行'],
            ['📊', '我发布了多少篇文章']
        ];
        const el = document.createElement('div');
        el.className = 'ai-welcome';
        el.innerHTML = `
            <div class="ai-welcome-avatar">🤖</div>
            <div class="ai-welcome-title">你好，我是银月</div>
            <div class="ai-welcome-sub">你的博客 AI 助手<br>找文章 · 看排行 · 总结内容 · 答疑</div>
            <div class="ai-chips">
                ${chips.map(([icon, q]) => `
                    <button class="ai-chip" data-q="${q}">
                        <span class="ai-chip-icon">${icon}</span>${q}
                    </button>`).join('')}
            </div>
        `;
        document.getElementById('aiMessages').appendChild(el);
        this.scrollToBottom(true);
    },

    /** 根据登录态切换输入区 / 登录提示 */
    refreshLoginState() {
        const logged = Auth.isLoggedIn();
        document.getElementById('aiLoginTip').style.display = logged ? 'none' : 'block';
        document.getElementById('aiInputArea').style.display = logged ? 'flex' : 'none';
    },

    /** 追加一条消息（头像 + 气泡），返回气泡元素 */
    appendMessage(role, text) {
        const msg = document.createElement('div');
        msg.className = 'ai-msg ' + role;

        const avatar = document.createElement('div');
        avatar.className = 'ai-avatar';
        if (role === 'user') {
            const user = Auth.getUser() || {};
            if (user.avatar) {
                avatar.innerHTML = `<img src="${this.escapeHtml(user.avatar)}" alt=""
                    onerror="this.onerror=null;this.src='/images/default-avatar.svg'">`;
            } else {
                avatar.textContent = (user.nickname || user.username || '我').slice(0, 1);
            }
        } else {
            avatar.textContent = '🤖';
        }

        const bubble = document.createElement('div');
        bubble.className = 'ai-bubble';
        if (role === 'assistant') {
            // 助手回复按 Markdown 渲染（renderMarkdown 内部已转义，防 XSS）
            bubble.innerHTML = this.renderMarkdown(text || '');
        } else {
            bubble.textContent = text || '';
        }

        msg.appendChild(avatar);
        msg.appendChild(bubble);
        document.getElementById('aiMessages').appendChild(msg);
        this.scrollToBottom(true);
        return bubble;
    },

    /** 智能滚动：仅当接近底部时自动跟随，用户上翻查看历史时不打扰 */
    scrollToBottom(force) {
        const list = document.getElementById('aiMessages');
        const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 80;
        if (force || nearBottom) list.scrollTop = list.scrollHeight;
    },

    /** HTML 属性值转义（用于头像 URL 等拼接场景） */
    escapeHtml(str) {
        return String(str).replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    },

    /** 发送消息并流式接收回复（text 传入时优先，否则读输入框） */
    async send(text) {
        if (this.streaming) return;
        if (!Auth.isLoggedIn()) {
            Toast.show('请先登录后再使用 AI 助手', 'error');
            return;
        }
        const input = document.getElementById('aiInput');
        const message = (text != null ? String(text) : input.value).trim();
        if (!message) return;

        input.value = '';
        input.style.height = 'auto';
        this.appendMessage('user', message);

        // 占位气泡：等待期显示三点动画
        const bubble = this.appendMessage('assistant', '');
        bubble.innerHTML = '<span class="ai-dots"><i></i><i></i><i></i></span>';

        const btn = document.getElementById('aiSend');
        this.streaming = true;
        btn.disabled = true;
        btn.classList.add('loading');

        let paintTimer = null;
        try {
            const token = localStorage.getItem('token');
            const res = await fetch('/api/ai/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { 'Authorization': 'Bearer ' + token } : {})
                },
                body: JSON.stringify({ message })
            });

            const contentType = (res.headers.get('Content-Type') || '').toLowerCase();

            // 非流式响应 = 统一异常（未登录 / 限流 / AI 服务异常），按 Result JSON 解析
            if (contentType.includes('application/json')) {
                const data = await res.json().catch(() => null);
                if (data && data.code === 401) {
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    this.refreshLoginState();
                }
                throw new Error((data && data.message) || ('请求失败（HTTP ' + res.status + '）'));
            }
            if (!res.ok || !res.body) {
                throw new Error('请求失败（HTTP ' + res.status + '）');
            }

            // 手动解析 SSE：事件以空行（两个换行）分隔，取 data: 行拼接
            const reader = res.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let started = false;
            let raw = '';
            let lastPaint = 0;

            // Markdown 渲染有开销，流式期间节流刷新（≥70ms 一次），结束后再全量渲染一次
            const paint = (streaming) => {
                bubble.innerHTML = this.renderMarkdown(raw)
                    + (streaming ? '<span class="ai-cursor"></span>' : '');
                this.scrollToBottom(false);
            };

            const consume = (chunk) => {
                if (chunk === null) return;
                if (!started) { started = true; bubble.innerHTML = ''; }
                raw += chunk;
                const now = Date.now();
                if (now - lastPaint >= 70) {
                    lastPaint = now;
                    paint(true);
                } else {
                    clearTimeout(paintTimer);
                    paintTimer = setTimeout(() => paint(true), 70);
                }
            };

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                let idx;
                while ((idx = buffer.indexOf('\n\n')) !== -1) {
                    consume(this.parseSseEvent(buffer.slice(0, idx)));
                    buffer = buffer.slice(idx + 2);
                }
            }
            consume(this.parseSseEvent(buffer)); // 尾部残余

            clearTimeout(paintTimer);
            if (started) {
                paint(false); // 结束后全量渲染，去掉打字光标
            } else {
                bubble.textContent = '（没有收到回复，请稍后再试）';
            }
        } catch (e) {
            clearTimeout(paintTimer);
            bubble.classList.add('ai-error');
            bubble.textContent = '😥 ' + e.message;
        } finally {
            this.streaming = false;
            btn.disabled = false;
            btn.classList.remove('loading');
            this.scrollToBottom(false);
        }
    },

    /** 解析单个 SSE 事件块，返回 data 文本（无数据返回 null） */
    parseSseEvent(raw) {
        if (!raw) return null;
        const dataLines = [];
        for (const line of raw.split('\n')) {
            if (line.startsWith('data:')) {
                let d = line.slice(5);
                if (d.startsWith(' ')) d = d.slice(1); // SSE 规范：忽略一个前导空格
                dataLines.push(d);
            }
        }
        return dataLines.length ? dataLines.join('\n') : null;
    },

    /**
     * 轻量 Markdown 渲染
     * 支持：代码块 / 行内代码 / 标题 / 加粗 / 斜体 / 链接 / 无序有序列表 / 引用 / 分隔线
     * 安全策略：先整体 HTML 转义，再生成固定的安全标签；链接仅放行 http(s)
     */
    renderMarkdown(text) {
        const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');

        // 行内格式（作用于已转义文本）
        const inline = (s) => s
            .replace(/`([^`\n]+)`/g, '<code>$1</code>')
            .replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
            .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
            .replace(/\[([^\]\n]+)\]\((https?:\/\/[^)\s]+)\)/g,
                '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

        const lines = esc(text || '').split('\n');
        let html = '';
        let code = null;   // 代码块行缓冲（null = 不在代码块中）
        let para = [];     // 段落行缓冲
        let list = null;   // 当前列表类型：ul / ol

        const flushPara = () => {
            if (para.length) {
                html += '<p>' + para.map(inline).join('<br>') + '</p>';
                para = [];
            }
        };
        const flushList = () => {
            if (list) { html += list === 'ul' ? '</ul>' : '</ol>'; list = null; }
        };
        const flushAll = () => { flushPara(); flushList(); };

        for (const line of lines) {
            const trimmed = line.trim();

            // 代码块围栏
            if (trimmed.startsWith('```')) {
                if (code === null) { flushAll(); code = []; }
                else { html += '<pre><code>' + code.join('\n') + '</code></pre>'; code = null; }
                continue;
            }
            if (code !== null) { code.push(line); continue; }

            // 空行 = 段落 / 列表收尾
            if (!trimmed) { flushAll(); continue; }

            // 标题
            const h = trimmed.match(/^(#{1,4})\s+(.*)$/);
            if (h) {
                flushAll();
                const lv = h[1].length;
                html += `<h${lv}>${inline(h[2])}</h${lv}>`;
                continue;
            }

            // 分隔线
            if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
                flushAll();
                html += '<hr>';
                continue;
            }

            // 无序列表
            const ul = trimmed.match(/^[-*•]\s+(.*)$/);
            if (ul) {
                flushPara();
                if (list !== 'ul') { flushList(); html += '<ul>'; list = 'ul'; }
                html += '<li>' + inline(ul[1]) + '</li>';
                continue;
            }

            // 有序列表
            const ol = trimmed.match(/^\d+[.、)]\s*(.*)$/);
            if (ol) {
                flushPara();
                if (list !== 'ol') { flushList(); html += '<ol>'; list = 'ol'; }
                html += '<li>' + inline(ol[1]) + '</li>';
                continue;
            }

            // 引用（> 已被转义为 &gt;，两种前缀都要匹配）
            const bq = trimmed.match(/^(?:>|&gt;)\s?(.*)$/);
            if (bq) {
                flushAll();
                html += '<blockquote>' + inline(bq[1]) + '</blockquote>';
                continue;
            }

            // 普通文本行：并入当前段落（段内换行转 <br>）
            flushList();
            para.push(line);
        }

        // 收尾：流式过程中可能存在未闭合的代码块 / 段落
        if (code !== null && code.length) {
            html += '<pre><code>' + code.join('\n') + '</code></pre>';
        }
        flushAll();
        return html;
    }
};
