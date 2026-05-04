window.ThemeManager = {
    STORAGE_KEY: 'sp-theme',

    init: function () {
        var saved = localStorage.getItem(this.STORAGE_KEY) || 'system';
        this._apply(saved);
        // system preference 변경 감지
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
            var current = localStorage.getItem(ThemeManager.STORAGE_KEY) || 'system';
            if (current === 'system') {
                ThemeManager._apply('system');
            }
        });
    },

    set: function (theme) {
        localStorage.setItem(this.STORAGE_KEY, theme);
        this._apply(theme);
    },

    get: function () {
        return localStorage.getItem(this.STORAGE_KEY) || 'system';
    },

    toggle: function () {
        var resolved = this._resolvedTheme();
        this.set(resolved === 'light' ? 'dark' : 'light');
        this._updateToggleIcon();
    },

    _apply: function (theme) {
        var resolved = theme;
        if (theme === 'system') {
            resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }
        document.documentElement.setAttribute('data-bs-theme', resolved);
        this._updateToggleIcon();
    },

    _resolvedTheme: function () {
        return document.documentElement.getAttribute('data-bs-theme') || 'light';
    },

    _updateToggleIcon: function () {
        var btn = document.getElementById('themeToggleIcon');
        if (!btn) return;
        var resolved = this._resolvedTheme();
        btn.className = resolved === 'dark' ? 'bi bi-sun' : 'bi bi-moon';
    }
};

// 즉시 초기화 (FOUC 방지)
ThemeManager.init();
