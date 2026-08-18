package com.naveen.browser.utils;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {

    private static final Set<String> BLOCKED_DOMAINS = new HashSet<>();

    static {
        // Popular Ad & Tracker Domains
        BLOCKED_DOMAINS.add("doubleclick.net");
        BLOCKED_DOMAINS.add("google-analytics.com");
        BLOCKED_DOMAINS.add("googlesyndication.com");
        BLOCKED_DOMAINS.add("googleadservices.com");
        BLOCKED_DOMAINS.add("adservice.google.com");
        BLOCKED_DOMAINS.add("pagead2.googlesyndication.com");
        BLOCKED_DOMAINS.add("amazon-adsystem.com");
        BLOCKED_DOMAINS.add("adnxs.com");
        BLOCKED_DOMAINS.add("criteo.com");
        BLOCKED_DOMAINS.add("outbrain.com");
        BLOCKED_DOMAINS.add("taboola.com");
        BLOCKED_DOMAINS.add("scorecardresearch.com");
        BLOCKED_DOMAINS.add("facebook.net/connect/xbp");
        BLOCKED_DOMAINS.add("connect.facebook.net");
        BLOCKED_DOMAINS.add("analytics.tiktok.com");
        BLOCKED_DOMAINS.add("ads.twitter.com");
        BLOCKED_DOMAINS.add("quantserve.com");
        BLOCKED_DOMAINS.add("mixpanel.com");
        BLOCKED_DOMAINS.add("segment.io");
        BLOCKED_DOMAINS.add("hotjar.com");
        BLOCKED_DOMAINS.add("bugsnag.com");
        BLOCKED_DOMAINS.add("popads.net");
        BLOCKED_DOMAINS.add("popcash.net");
        BLOCKED_DOMAINS.add("adform.net");
        BLOCKED_DOMAINS.add("rubiconproject.com");
        BLOCKED_DOMAINS.add("pubmatic.com");
        BLOCKED_DOMAINS.add("moatads.com");
        BLOCKED_DOMAINS.add("openx.net");
        BLOCKED_DOMAINS.add("exponential.com");
        BLOCKED_DOMAINS.add("adcolony.com");
        BLOCKED_DOMAINS.add("vungle.com");
        BLOCKED_DOMAINS.add("clarity.ms");
        BLOCKED_DOMAINS.add("newrelic.com");
    }

    public static boolean isAdOrTracker(String url) {
        if (url == null || url.isEmpty()) return false;
        String lowerUrl = url.toLowerCase();
        for (String domain : BLOCKED_DOMAINS) {
            if (lowerUrl.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public static WebResourceResponse createEmptyResource() {
        return createEmptyResource(null);
    }

    public static WebResourceResponse createEmptyResource(String url) {
        String mime = "text/plain";
        if (url != null) {
            String lower = url.toLowerCase();
            if (lower.endsWith(".js") || lower.contains(".js?")) {
                mime = "application/javascript";
            } else if (lower.endsWith(".css") || lower.contains(".css?")) {
                mime = "text/css";
            } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".gif") || lower.endsWith(".ico") || lower.endsWith(".webp")) {
                mime = "image/png";
            }
        }
        return new WebResourceResponse(mime, "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}
