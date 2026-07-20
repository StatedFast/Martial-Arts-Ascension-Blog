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
function renderNavbar() {
    const navLinks = document.getElementById('navLinks');
    if (!navLinks) return;

    const user = Auth.getUser();
    if (Auth.isLoggedIn() && user) {
        navLinks.innerHTML = `
            <a href="/">首页</a>
            <a href="/write" class="btn-write">写文章</a>
            <div class="user-menu" onclick="toggleUserMenu()">
                <img src="${user.avatar || '/images/default-avatar.svg'}" alt="头像" class="user-avatar"
                     onerror="this.onerror=null;this.src='/images/default-avatar.svg'">
                <span>${user.nickname || user.username}</span>
                <div class="user-dropdown" id="userDropdown">
                    <a href="/profile">个人中心</a>
                    <a href="#" class="logout" onclick="Auth.logout();return false;">退出登录</a>
                </div>
            </div>
        `;
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
