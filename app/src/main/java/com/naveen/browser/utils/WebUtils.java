package com.naveen.browser.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.webkit.URLUtil;

import com.naveen.browser.MainActivity;
import com.naveen.browser.R;

import java.net.URLEncoder;
import java.util.regex.Pattern;

public class WebUtils {

    /**
     * Chrome-compatible mobile UA — required for Twitter/X, Gmail, and modern SPAs.
     * Matches current Chrome release and identifies as Android Chrome correctly.
     */
    public static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36";

    /**
     * Desktop UA — exact match of Chrome on Windows for desktop-mode requests.
     */
    public static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    public static final String TABLET_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Tablet) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    public static void setDesktopMode(android.webkit.WebView webView, boolean enable) {
        if (webView == null) return;
        android.webkit.WebSettings settings = webView.getSettings();
        if (enable) {
            settings.setUserAgentString(DESKTOP_USER_AGENT);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        } else {
            settings.setUserAgentString(MOBILE_USER_AGENT);
            settings.setUseWideViewPort(false);
            settings.setLoadWithOverviewMode(false);
        }
    }

    private static final Pattern WEB_URL_PATTERN = Pattern.compile(
            "^(https?://)?(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}|localhost|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(:\\d+)?(/.*)?$"
    );

    public static String processUrlOrQuery(String input, int searchEngineIndex) {
        if (input == null || input.trim().isEmpty()) {
            return PreferenceManager.DEFAULT_HOMEPAGE;
        }
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("about:blank")) return "about:blank";

        if (URLUtil.isValidUrl(trimmed) || WEB_URL_PATTERN.matcher(trimmed).matches()) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")
                    && !trimmed.startsWith("file://")) {
                return "https://" + trimmed;
            }
            return trimmed;
        }

        try {
            String q = URLEncoder.encode(trimmed, "UTF-8");
            switch (searchEngineIndex) {
                case 1: return "https://duckduckgo.com/?q=" + q;
                case 2: return "https://www.bing.com/search?q=" + q;
                case 3: return "https://search.brave.com/search?q=" + q;
                case 4: return "https://search.yahoo.com/search?p=" + q;
                case 5: return "https://www.ecosia.org/search?q=" + q;
                case 6: return "https://www.startpage.com/sp/search?query=" + q;
                default: return "https://www.google.com/search?q=" + q;
            }
        } catch (Exception e) {
            return "https://www.google.com/search?q=" + trimmed;
        }
    }

    public static String upgradeToHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            return url.replaceFirst("http://", "https://");
        }
        return url;
    }

    public static String getTranslateUrl(String url) {
        try {
            return "https://translate.google.com/translate?sl=auto&tl=en&u="
                    + URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return "https://translate.google.com/translate?u=" + url;
        }
    }

    public static boolean handleSpecialIntents(Context context, String url) {
        if (url == null) return false;
        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("geo:")
                || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                try {
                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(fallback);
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public static void createPwaShortcut(Context context, String title, String url, Bitmap favicon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager sm = context.getSystemService(ShortcutManager.class);
            if (sm != null && sm.isRequestPinShortcutSupported()) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Icon icon = favicon != null
                        ? Icon.createWithBitmap(favicon)
                        : Icon.createWithResource(context, R.drawable.ic_browser);
                ShortcutInfo info = new ShortcutInfo.Builder(context, "pwa_" + url.hashCode())
                        .setShortLabel(title != null ? title : "Shortcut")
                        .setIcon(icon)
                        .setIntent(intent)
                        .build();
                sm.requestPinShortcut(info, null);
            }
        }
    }

    public static String getNightModeScript() {
        return "javascript:(function() {" +
                "var bg = '#121212';" +
                "var fg = '#E0E0E0';" +
                "document.documentElement.style.backgroundColor = bg;" +
                "document.body.style.backgroundColor = bg;" +
                "var s = document.createElement('style');" +
                "s.type = 'text/css';" +
                "s.appendChild(document.createTextNode(" +
                "'html, body { background-color: ' + bg + ' !important; color: ' + fg + ' !important; } " +
                "a { color: #64D2FF !important; } " +
                "p, span, div, h1, h2, h3, h4, h5, h6 { color: ' + fg + ' !important; }'" +
                "));" +
                "(document.head || document.documentElement).appendChild(s);" +
                "})();";
    }

    public static String getReaderModeScript(boolean isNightMode) {
        String bg = isNightMode ? "#121216" : "#ffffff";
        String fg = isNightMode ? "#e5e7eb" : "#1f2937";
        return "javascript:(function() {" +
                "var title = document.title;" +
                "var article = document.querySelector('article');" +
                "if (!article) {" +
                "    var candidates = document.querySelectorAll('div,section,main');" +
                "    var best = null; var max = 0;" +
                "    for(var i=0;i<candidates.length;i++){" +
                "        var p=candidates[i].querySelectorAll('p').length;" +
                "        if(p>max){max=p;best=candidates[i];}" +
                "    }" +
                "    article = best || document.body;" +
                "}" +
                "var clone = article.cloneNode(true);" +
                "var junk = clone.querySelectorAll('script,style,iframe,.ads,header,footer,nav,noscript');" +
                "for(var j=0;j<junk.length;j++) junk[j].parentNode.removeChild(junk[j]);" +
                "document.body.innerHTML='<div style=\"max-width:700px;margin:auto;padding:24px;" +
                "font-family:-apple-system,BlinkMacSystemFont,sans-serif;line-height:1.7;" +
                "font-size:17px;color:" + fg + ";background:" + bg + "\">" +
                "<h1>' + title + '</h1>' + clone.innerHTML + '</div>';" +
                "document.body.style.backgroundColor='" + bg + "';" +
                "})();";
    }

    public static String getErrorHtml(String title, String message, String failedUrl) {
        return "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "background-color: #0F172A; color: #F8FAFC; display: flex; flex-direction: column; " +
                "align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 24px; box-sizing: border-box; text-align: center; }" +
                ".card { background: rgba(30, 41, 59, 0.7); border: 1px solid rgba(255, 255, 255, 0.1); " +
                "border-radius: 24px; padding: 32px 24px; max-width: 400px; width: 100%; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }" +
                "h2 { color: #3B82F6; margin-top: 16px; margin-bottom: 8px; font-size: 22px; }" +
                "p { color: #94A3B8; font-size: 14px; line-height: 1.5; margin-bottom: 24px; }" +
                ".btn { background: #2563EB; color: white; border: none; padding: 12px 28px; border-radius: 12px; " +
                "font-size: 15px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-block; transition: background 0.2s; }" +
                ".btn:active { background: #1D4ED8; }" +
                ".url { font-size: 11px; color: #64748B; word-break: break-all; margin-top: 16px; }" +
                "</style></head><body>" +
                "<div class=\"card\">" +
                "<svg width=\"64\" height=\"64\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#3B82F6\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"10\"></circle><line x1=\"12\" y1=\"8\" x2=\"12\" y2=\"12\"></line><line x1=\"12\" y1=\"16\" x2=\"12.01\" y2=\"16\"></line></svg>" +
                "<h2>" + title + "</h2>" +
                "<p>" + message + "</p>" +
                "<a class=\"btn\" onclick=\"location.reload()\">Retry Loading</a>" +
                "<div class=\"url\">" + (failedUrl != null ? failedUrl : "") + "</div>" +
                "</div>" +
                "</body></html>";
    }
}
