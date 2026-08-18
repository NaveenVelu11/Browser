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
        // === Ad Server, Popup & Network Domains ===
        BLOCKED_ADS.add("doubleclick.net");
        BLOCKED_ADS.add("googlesyndication.com");
        BLOCKED_ADS.add("googleadservices.com");
        BLOCKED_ADS.add("adservice.google.com");
        BLOCKED_ADS.add("pagead2.googlesyndication.com");
        BLOCKED_ADS.add("pagead.googlesyndication.com");
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
        BLOCKED_ADS.add("propellerads.com");
        BLOCKED_ADS.add("exoclick.com");
        BLOCKED_ADS.add("juicyads.com");
        BLOCKED_ADS.add("clickadu.com");
        BLOCKED_ADS.add("adcash.com");
        BLOCKED_ADS.add("adroll.com");
        BLOCKED_ADS.add("mgid.com");
        BLOCKED_ADS.add("revcontent.com");
        BLOCKED_ADS.add("applovin.com");
        BLOCKED_ADS.add("ironsrc.com");
        BLOCKED_ADS.add("chartboost.com");
        BLOCKED_ADS.add("fyber.com");
        BLOCKED_ADS.add("leadbolt.com");
        BLOCKED_ADS.add("adtop.net");
        BLOCKED_ADS.add("bidswitch.net");
        BLOCKED_ADS.add("casalemedia.com");
        BLOCKED_ADS.add("contextweb.com");
        BLOCKED_ADS.add("indexww.com");
        BLOCKED_ADS.add("liadm.com");
        BLOCKED_ADS.add("media.net");
        BLOCKED_ADS.add("smartadserver.com");
        BLOCKED_ADS.add("yieldmo.com");
        BLOCKED_ADS.add("zemanta.com");
        BLOCKED_ADS.add("ad-delivery.net");
        BLOCKED_ADS.add("adhigh.net");
        BLOCKED_ADS.add("adsystem.ru");
        BLOCKED_ADS.add("aniview.com");
        BLOCKED_ADS.add("beop.io");
        BLOCKED_ADS.add("brid.tv");
        BLOCKED_ADS.add("cootlogix.com");
        BLOCKED_ADS.add("media.admob.com");
        BLOCKED_ADS.add("googleads.g.doubleclick.net");
        BLOCKED_ADS.add("static.doubleclick.net");

        // === Tracker & Analytics Domains ===
        BLOCKED_TRACKERS.add("google-analytics.com");
        BLOCKED_TRACKERS.add("analytics.google.com");
        BLOCKED_TRACKERS.add("googletagmanager.com");
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
        BLOCKED_TRACKERS.add("omtrdc.net");
        BLOCKED_TRACKERS.add("demdex.net");
        BLOCKED_TRACKERS.add("everesttech.net");
        BLOCKED_TRACKERS.add("krxd.net");
        BLOCKED_TRACKERS.add("bluekai.com");
        BLOCKED_TRACKERS.add("mathtag.com");
        BLOCKED_TRACKERS.add("tns-counter.ru");
        BLOCKED_TRACKERS.add("statcounter.com");
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

        // Common ad path patterns & YouTube ad endpoints
        if (lowerUrl.contains("/pagead/") || lowerUrl.contains("/googleads")
                || lowerUrl.contains("ad_status") || lowerUrl.contains("video_ad_")
                || lowerUrl.contains("ad_banner") || lowerUrl.contains("/popunder")
                || lowerUrl.contains("/ad-provider/") || lowerUrl.contains("show_ad.js")
                || lowerUrl.contains("adservice") || lowerUrl.contains("adserver")
                || lowerUrl.contains("/api/stats/ads") || lowerUrl.contains("ptracking")
                || lowerUrl.contains("get_midroll_info") || lowerUrl.contains("gtag/js")
                || lowerUrl.contains("fbevents.js") || lowerUrl.contains("pixel.js")) {
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

    public static String getCosmeticAdBlockScript() {
        return "(function() {" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" +
                "ins.adsbygoogle, .ad-container, .ad-slot, .ad-wrapper, [id^=\"google_ads\"], " +
                ".native-ad, .sponsored-post, .sponsored-ad, .ad-banner, .banner-ad, " +
                ".ad_box, .ad_wrapper, .ad_container, .ad_slot, .ad-unit, .ad-block, " +
                "[class*=\"ad-\"], [class*=\"-ad\"], [id*=\"ad-\"], [id*=\"-ad\"], " +
                ".ytp-ad-module, .ytp-ad-overlay-container, #player-ads, " +
                "ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, " +
                "ytd-action-companion-ad-renderer, ytd-banner-promo-renderer, " +
                "ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer " +
                "{ display: none !important; visibility: hidden !important; opacity: 0 !important; height: 0 !important; width: 0 !important; pointer-events: none !important; }';" +
                "if (document.head) document.head.appendChild(style);" +
                "setInterval(function() {" +
                "  var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-slot');" +
                "  if (skipBtn) { skipBtn.click(); }" +
                "}, 400);" +
                "})();";
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
