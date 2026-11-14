package com.znarfm.siphon.ui.browser

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

private const val DARK_MODE_INJECT_JS = """
(function() {
    var parseLuma = function(bg) {
        if (!bg || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)') return null;
        var m = bg.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)/);
        if (!m) return null;
        if (m[4] !== undefined && parseFloat(m[4]) < 0.15) return null;
        return (0.299 * parseInt(m[1], 10) + 0.587 * parseInt(m[2], 10) + 0.114 * parseInt(m[3], 10));
    };
    var isPageNativelyDark = function() {
        var docEl = document.documentElement;
        var body = document.body;
        if (!docEl) return false;
        var meta = document.querySelector('meta[name="color-scheme"], meta[name="supported-color-schemes"]');
        if (meta && /dark/i.test(meta.content)) return true;
        if ([docEl, body].some(function(el) {
            return el && (el.hasAttribute('dark') || /dark|night|black/i.test([el.className, el.getAttribute('data-theme'), el.getAttribute('data-color-mode'), el.getAttribute('data-bs-theme')].join(' ')));
        })) return true;
        var existingStyle = document.getElementById('__siphon_dark_mode');
        if (existingStyle) existingStyle.disabled = true;
        var darkBg = false;
        try {
            var ds = window.getComputedStyle(docEl);
            if (ds && (ds.colorScheme || '').toLowerCase().includes('dark')) darkBg = true;
            if (!darkBg) {
                var luma = parseLuma(body ? window.getComputedStyle(body).backgroundColor : null);
                if (luma === null) luma = parseLuma(ds ? ds.backgroundColor : null);
                if (luma !== null && luma < 115) darkBg = true;
            }
        } catch (e) {}
        if (existingStyle) existingStyle.disabled = false;
        return darkBg;
    };
    var applyDark = function() {
        var root = document.head || document.documentElement;
        if (!root) return;
        var existing = document.getElementById('__siphon_dark_mode');
        if (isPageNativelyDark()) {
            if (existing) existing.remove();
            return;
        }
        if (!existing) {
            var s = document.createElement('style');
            s.id = '__siphon_dark_mode';
            s.textContent = ':root { color-scheme: dark !important; } html { filter: invert(90%) hue-rotate(180deg) !important; background-color: #121212 !important; } img, video, iframe, canvas, svg, picture, [style*="background-image"] { filter: invert(100%) hue-rotate(180deg) !important; }';
            root.appendChild(s);
        }
    };
    applyDark();
    if (!window.__siphon_dark_observer && document.documentElement) {
        var timer = null;
        window.__siphon_dark_observer = new MutationObserver(function() {
            if (timer) return;
            timer = setTimeout(function() { timer = null; applyDark(); }, 120);
        });
        window.__siphon_dark_observer.observe(document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'style', 'data-theme', 'data-color-mode', 'data-color-scheme', 'data-bs-theme', 'dark']
        });
    }
})();
"""

private const val DARK_MODE_REMOVE_JS = """
(function() {
    if (window.__siphon_dark_observer) {
        window.__siphon_dark_observer.disconnect();
        window.__siphon_dark_observer = null;
    }
    var s = document.getElementById('__siphon_dark_mode');
    if (s) s.remove();
})();
"""

internal fun applyWebDarkModeToWebView(webView: WebView, enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        webView.settings.forceDark = if (enabled) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        webView.settings.isAlgorithmicDarkeningAllowed = enabled
    }
    webView.evaluateJavascript(if (enabled) DARK_MODE_INJECT_JS else DARK_MODE_REMOVE_JS, null)
}
