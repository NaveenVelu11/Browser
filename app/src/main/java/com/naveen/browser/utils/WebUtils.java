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

    public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    public static final String TABLET_USER_AGENT = "Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1";

    private static final Pattern WEB_URL_PATTERN = Pattern.compile(
            "^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(/.*)?$"
    );

    public static String processUrlOrQuery(String input, int searchEngineIndex) {
        if (input == null || input.trim().isEmpty()) {
            return PreferenceManager.DEFAULT_HOMEPAGE;
        }

        String trimmed = input.trim();

        if (URLUtil.isValidUrl(trimmed) || WEB_URL_PATTERN.matcher(trimmed).matches()) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("file://")) {
                return "https://" + trimmed;
            }
            return trimmed;
        }

        try {
            String encodedQuery = URLEncoder.encode(trimmed, "UTF-8");
            switch (searchEngineIndex) {
                case 1:
                    return "https://duckduckgo.com/?q=" + encodedQuery;
                case 2:
                    return "https://www.bing.com/search?q=" + encodedQuery;
                case 3:
                    return "https://search.brave.com/search?q=" + encodedQuery;
                case 4:
                    return "https://search.yahoo.com/search?p=" + encodedQuery;
                case 0:
                default:
                    return "https://www.google.com/search?q=" + encodedQuery;
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
            return "https://translate.google.com/translate?sl=auto&tl=en&u=" + URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return "https://translate.google.com/translate?u=" + url;
        }
    }

    public static boolean handleSpecialIntents(Context context, String url) {
        if (url == null) return false;

        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("geo:") ||
            url.startsWith("whatsapp:") || url.startsWith("intent:") || url.contains("youtube.com/watch") || url.startsWith("youtu.be/")) {
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
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));

                Icon icon = favicon != null ? Icon.createWithBitmap(favicon) : Icon.createWithResource(context, R.drawable.ic_browser);

                ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, "pwa_" + url.hashCode())
                        .setShortLabel(title)
                        .setIcon(icon)
                        .setIntent(intent)
                        .build();

                shortcutManager.requestPinShortcut(pinShortcutInfo, null);
            }
        }
    }

    public static String getNightModeScript() {
        return "javascript:(function() {" +
                "var css = 'html { filter: invert(90%) hue-rotate(180deg) !important; } " +
                "img, video, iframe, canvas { filter: invert(100%) hue-rotate(180deg) !important; }';" +
                "var head = document.getElementsByTagName('head')[0];" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.appendChild(document.createTextNode(css));" +
                "head.appendChild(style);" +
                "})();";
    }

    public static String getReaderModeScript() {
        return "javascript:(function() {" +
                "var article = document.querySelector('article') || document.querySelector('main') || document.body;" +
                "document.body.innerHTML = '<div style=\"max-width:700px;margin:auto;padding:20px;font-family:sans-serif;line-height:1.6;color:#e5e7eb;background:#121216;\">' + article.innerHTML + '</div>';" +
                "})();";
    }
}
