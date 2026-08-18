package com.naveen.browser.utils;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdBlocker {

    private static final Set<String> BLOCKED_ADS = new HashSet<>();
    private static final Set<String> BLOCKED_TRACKERS = new HashSet<>();

    private static final Map<String, AtomicInteger> SITE_ADS_BLOCKED = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> SITE_TRACKERS_BLOCKED = new ConcurrentHashMap<>();

    static {
        // === Ad Server & Popup Domains ===
        BLOCKED_ADS.add("doubleclick.net");
        BLOCKED_ADS.add("googlesyndication.com");
        BLOCKED_ADS.add("googleadservices.com");
        BLOCKED_ADS.add("adservice.google.com");
        BLOCKED_ADS.add("pagead2.googlesyndication.com");
        BLOCKED_ADS.add("amazon-adsystem.com");
        BLOCKED_ADS.add("adnxs.com");
        BLOCKED_ADS.add("criteo.com");
        BLOCKED_ADS.add("outbrain.com");
        BLOCKED_ADS.add("taboola.com");
        BLOCKED_ADS.add("popads.net");
        BLOCKED_ADS.add("popcash.net");
        BLOCKED_ADS.add("adform.net");
        BLOCKED_ADS.add("rubiconproject.com");
        BLOCKED_ADS.add("pubmatic.com");
        BLOCKED_ADS.add("moatads.com");
        BLOCKED_ADS.add("openx.net");
        BLOCKED_ADS.add("exponential.com");
        BLOCKED_ADS.add("adcolony.com");
        BLOCKED_ADS.add("vungle.com");
        BLOCKED_ADS.add("inmobi.com");
        BLOCKED_ADS.add("unityads.unity3d.com");
        BLOCKED_ADS.add("adkey.biz");
        BLOCKED_ADS.add("adsterra.com");

        // === Tracker & Analytics Domains ===
        BLOCKED_TRACKERS.add("google-analytics.com");
        BLOCKED_TRACKERS.add("scorecardresearch.com");
        BLOCKED_TRACKERS.add("facebook.net/connect/xbp");
        BLOCKED_TRACKERS.add("connect.facebook.net");
        BLOCKED_TRACKERS.add("analytics.tiktok.com");
        BLOCKED_TRACKERS.add("ads.twitter.com");
        BLOCKED_TRACKERS.add("quantserve.com");
        BLOCKED_TRACKERS.add("mixpanel.com");
        BLOCKED_TRACKERS.add("segment.io");
        BLOCKED_TRACKERS.add("hotjar.com");
        BLOCKED_TRACKERS.add("bugsnag.com");
        BLOCKED_TRACKERS.add("clarity.ms");
        BLOCKED_TRACKERS.add("newrelic.com");
        BLOCKED_TRACKERS.add("sentry.io");
        BLOCKED_TRACKERS.add("amplitude.com");
        BLOCKED_TRACKERS.add("appsflyer.com");
        BLOCKED_TRACKERS.add("adjust.com");
        BLOCKED_TRACKERS.add("branch.io");
    }

    public static boolean isAdOrTracker(String url) {
        return isAdOrTracker(url, null, null);
    }

    public static boolean isAdOrTracker(String url, String pageHost, PreferenceManager prefManager) {
        if (url == null || url.isEmpty()) return false;
        if (pageHost != null && prefManager != null && prefManager.isSiteShieldsDisabled(pageHost)) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // Check Tracker list first
        for (String domain : BLOCKED_TRACKERS) {
            if (lowerUrl.contains(domain)) {
                if (pageHost != null) {
                    recordBlockedTracker(pageHost, prefManager);
                }
                return true;
            }
        }

        // Check Ad list
        for (String domain : BLOCKED_ADS) {
            if (lowerUrl.contains(domain)) {
                if (pageHost != null) {
                    recordBlockedAd(pageHost, prefManager);
                }
                return true;
            }
        }

        // Common ad path patterns
        if (lowerUrl.contains("/pagead/") || lowerUrl.contains("/googleads.")
                || lowerUrl.contains("ad_status") || lowerUrl.contains("video_ad_")
                || lowerUrl.contains("ad_banner") || lowerUrl.contains("/popunder")
                || lowerUrl.contains("/ad-provider/") || lowerUrl.contains("show_ad.js")) {
            if (pageHost != null) {
                recordBlockedAd(pageHost, prefManager);
            }
            return true;
        }

        return false;
    }

    private static void recordBlockedAd(String host, PreferenceManager prefManager) {
        String cleanHost = host.toLowerCase().replace("www.", "");
        SITE_ADS_BLOCKED.computeIfAbsent(cleanHost, k -> new AtomicInteger(0)).incrementAndGet();
        if (prefManager != null) {
            prefManager.incrementLifetimeBlockedAds(1);
        }
    }

    private static void recordBlockedTracker(String host, PreferenceManager prefManager) {
        String cleanHost = host.toLowerCase().replace("www.", "");
        SITE_TRACKERS_BLOCKED.computeIfAbsent(cleanHost, k -> new AtomicInteger(0)).incrementAndGet();
        if (prefManager != null) {
            prefManager.incrementLifetimeBlockedTrackers(1);
        }
    }

    public static int getBlockedAdsCount(String host) {
        if (host == null) return 0;
        String cleanHost = host.toLowerCase().replace("www.", "");
        AtomicInteger count = SITE_ADS_BLOCKED.get(cleanHost);
        return count != null ? count.get() : 0;
    }

    public static int getBlockedTrackersCount(String host) {
        if (host == null) return 0;
        String cleanHost = host.toLowerCase().replace("www.", "");
        AtomicInteger count = SITE_TRACKERS_BLOCKED.get(cleanHost);
        return count != null ? count.get() : 0;
    }

    public static void resetSiteStats(String host) {
        if (host == null) return;
        String cleanHost = host.toLowerCase().replace("www.", "");
        SITE_ADS_BLOCKED.remove(cleanHost);
        SITE_TRACKERS_BLOCKED.remove(cleanHost);
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
