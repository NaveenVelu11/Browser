package com.naveen.browser.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Central preference store for DeerOne Browser.
 * All feature flags, settings, and persistent state live here.
 * Android 5+ compatible
 */
public class PreferenceManager {
    private static final String PREF_NAME = "browser_preferences";
    private static final String TAG = "PreferenceManager";

    // --- General ---
    private static final String KEY_HOMEPAGE          = "homepage_url";
    private static final String KEY_SEARCH_ENGINE     = "search_engine";
    private static final String KEY_USER_AGENT        = "user_agent_index";
    private static final String KEY_CUSTOM_USER_AGENT = "custom_user_agent";
    private static final String KEY_THEME_MODE        = "theme_mode";
    private static final String KEY_TEXT_SIZE         = "text_size_index";
    private static final String KEY_SHOW_IMAGES       = "show_images";
    private static final String KEY_OPEN_LINKS_NEW_TAB= "open_links_new_tab";
    private static final String KEY_PULL_REFRESH      = "pull_to_refresh";

    // --- Privacy & Security ---
    private static final String KEY_AD_BLOCK          = "ad_block_enabled";
    private static final String KEY_TRACKER_BLOCK     = "tracker_block_enabled";
    private static final String KEY_HTTPS_ONLY        = "https_only_enabled";
    private static final String KEY_DO_NOT_TRACK      = "do_not_track";
    private static final String KEY_SAFE_BROWSING     = "safe_browsing";
    private static final String KEY_BLOCK_POPUPS      = "block_popups";
    private static final String KEY_BLOCK_SCRIPTS     = "block_third_party_scripts";
    private static final String KEY_SEND_REFERRER     = "send_referrer";
    private static final String KEY_COOKIE_POLICY     = "cookie_policy";

    // --- Browsing ---
    private static final String KEY_NIGHT_MODE        = "night_mode";
    private static final String KEY_DESKTOP_MODE      = "desktop_mode";
    private static final String KEY_SESSION_RESTORE   = "session_restore_enabled";
    private static final String KEY_AUTOFILL          = "autofill_enabled";
    private static final String KEY_JAVASCRIPT        = "javascript_enabled";
    private static final String KEY_LOCATION          = "location_enabled";
    private static final String KEY_MEDIA_AUTOPLAY    = "media_autoplay";
    private static final String KEY_READER_FONT_SIZE  = "reader_font_size";

    // --- Performance ---
    private static final String KEY_DATA_SAVER        = "data_saver_mode";
    private static final String KEY_PRELOAD_PAGES     = "preload_pages";
    private static final String KEY_HARDWARE_ACCEL    = "hardware_accel";
    private static final String KEY_MAX_TABS          = "max_tabs_allowed";

    // --- Downloads ---
    private static final String KEY_DOWNLOAD_PATH     = "download_path";
    private static final String KEY_DOWNLOAD_NOTIFY   = "download_notifications";
    private static final String KEY_ASK_BEFORE_DL     = "ask_before_download";

    // --- Appearance ---
    private static final String KEY_SHOW_TAB_COUNT    = "show_tab_count";
    private static final String KEY_SHOW_URL_SCHEME   = "show_url_scheme";
    private static final String KEY_TOOLBAR_POSITION  = "toolbar_position";

    // --- Session & State ---
    private static final String KEY_SAVED_SESSIONS    = "saved_sessions_urls";
    private static final String KEY_FIRST_LAUNCH      = "first_launch";
    private static final String KEY_LIFETIME_BLOCKED  = "lifetime_blocked_ads";
    private static final String KEY_LIFETIME_SAVED_MB = "lifetime_saved_mb";
    private static final String KEY_TOTAL_TABS_OPENED = "total_tabs_opened";

    public static final String DEFAULT_HOMEPAGE = "about:blank";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== GENERAL ====================
    public String getHomepage() { 
        return prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE); 
    }
    public void setHomepage(String h) { 
        prefs.edit().putString(KEY_HOMEPAGE, h).apply(); 
    }

    public int getSearchEngineIndex() { 
        return prefs.getInt(KEY_SEARCH_ENGINE, 0); 
    }
    public void setSearchEngineIndex(int i) { 
        prefs.edit().putInt(KEY_SEARCH_ENGINE, i).apply(); 
    }

    public int getUserAgentIndex() { 
        return prefs.getInt(KEY_USER_AGENT, 0); 
    }
    public void setUserAgentIndex(int i) { 
        prefs.edit().putInt(KEY_USER_AGENT, i).apply(); 
    }

    public String getCustomUserAgent() { 
        return prefs.getString(KEY_CUSTOM_USER_AGENT, ""); 
    }
    public void setCustomUserAgent(String ua) { 
        prefs.edit().putString(KEY_CUSTOM_USER_AGENT, ua).apply(); 
    }

    public int getTextSizeIndex() { 
        return prefs.getInt(KEY_TEXT_SIZE, 1); 
    }
    public void setTextSizeIndex(int i) { 
        prefs.edit().putInt(KEY_TEXT_SIZE, i).apply(); 
    }

    public int getTextZoomPercent() {
        switch (getTextSizeIndex()) {
            case 0: return 75;
            case 2: return 125;
            case 3: return 150;
            default: return 100;
        }
    }

    public boolean isShowImages() { 
        return prefs.getBoolean(KEY_SHOW_IMAGES, true); 
    }
    public void setShowImages(boolean b) { 
        prefs.edit().putBoolean(KEY_SHOW_IMAGES, b).apply(); 
    }

    public boolean isOpenLinksNewTab() { 
        return prefs.getBoolean(KEY_OPEN_LINKS_NEW_TAB, false); 
    }
    public void setOpenLinksNewTab(boolean b) { 
        prefs.edit().putBoolean(KEY_OPEN_LINKS_NEW_TAB, b).apply(); 
    }

    public boolean isPullToRefresh() { 
        return prefs.getBoolean(KEY_PULL_REFRESH, true); 
    }
    public void setPullToRefresh(boolean b) { 
        prefs.edit().putBoolean(KEY_PULL_REFRESH, b).apply(); 
    }

    // ==================== THEME ====================
    public int getThemeMode() { 
        return prefs.getInt(KEY_THEME_MODE, 0); 
    }
    public void setThemeMode(int m) { 
        prefs.edit().putInt(KEY_THEME_MODE, m).apply(); 
    }
    
    public void applyTheme() {
        try {
            int themeMode = getThemeMode();
            int targetNightMode;
            switch (themeMode) {
                case 1:
                    targetNightMode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case 2:
                    targetNightMode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                default:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        targetNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        targetNightMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                    } else {
                        targetNightMode = AppCompatDelegate.MODE_NIGHT_NO;
                    }
                    break;
            }
            if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
                AppCompatDelegate.setDefaultNightMode(targetNightMode);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error applying theme", e);
        }
    }

    // ==================== PRIVACY & SECURITY ====================
    public boolean isAdBlockEnabled() { 
        return prefs.getBoolean(KEY_AD_BLOCK, true); 
    }
    public void setAdBlockEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_AD_BLOCK, b).apply(); 
    }

    public boolean isTrackerBlockEnabled() { 
        return prefs.getBoolean(KEY_TRACKER_BLOCK, true); 
    }
    public void setTrackerBlockEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_TRACKER_BLOCK, b).apply(); 
    }

    public boolean isHttpsOnlyEnabled() { 
        return prefs.getBoolean(KEY_HTTPS_ONLY, true); 
    }
    public void setHttpsOnlyEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_HTTPS_ONLY, b).apply(); 
    }

    public boolean isDoNotTrack() { 
        return prefs.getBoolean(KEY_DO_NOT_TRACK, true); 
    }
    public void setDoNotTrack(boolean b) { 
        prefs.edit().putBoolean(KEY_DO_NOT_TRACK, b).apply(); 
    }

    public boolean isSafeBrowsingEnabled() { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return prefs.getBoolean(KEY_SAFE_BROWSING, false);
        }
        return false;
    }
    public void setSafeBrowsingEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_SAFE_BROWSING, b).apply(); 
    }

    public boolean isBlockPopups() { 
        return prefs.getBoolean(KEY_BLOCK_POPUPS, true); 
    }
    public void setBlockPopups(boolean b) { 
        prefs.edit().putBoolean(KEY_BLOCK_POPUPS, b).apply(); 
    }

    public int getCookiePolicy() { 
        return prefs.getInt(KEY_COOKIE_POLICY, 0); 
    }
    public void setCookiePolicy(int p) { 
        prefs.edit().putInt(KEY_COOKIE_POLICY, p).apply(); 
    }

    // ==================== BROWSING ====================
    public boolean isNightMode() { 
        return prefs.getBoolean(KEY_NIGHT_MODE, false); 
    }
    public void setNightMode(boolean b) { 
        prefs.edit().putBoolean(KEY_NIGHT_MODE, b).apply(); 
    }

    public boolean isDesktopMode() { 
        return prefs.getBoolean(KEY_DESKTOP_MODE, false); 
    }
    public void setDesktopMode(boolean b) { 
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, b).apply(); 
    }

    public boolean isSessionRestoreEnabled() { 
        return prefs.getBoolean(KEY_SESSION_RESTORE, true); 
    }
    public void setSessionRestoreEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_SESSION_RESTORE, b).apply(); 
    }

    public boolean isAutofillEnabled() { 
        return prefs.getBoolean(KEY_AUTOFILL, true); 
    }
    public void setAutofillEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_AUTOFILL, b).apply(); 
    }

    public boolean isJavaScriptEnabled() { 
        return prefs.getBoolean(KEY_JAVASCRIPT, true); 
    }
    public void setJavaScriptEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_JAVASCRIPT, b).apply(); 
    }

    public boolean isLocationEnabled() { 
        return prefs.getBoolean(KEY_LOCATION, true); 
    }
    public void setLocationEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_LOCATION, b).apply(); 
    }

    public boolean isMediaAutoplay() { 
        return prefs.getBoolean(KEY_MEDIA_AUTOPLAY, false); 
    }
    public void setMediaAutoplay(boolean b) { 
        prefs.edit().putBoolean(KEY_MEDIA_AUTOPLAY, b).apply(); 
    }

    // ==================== PERFORMANCE ====================
    public boolean isDataSaverMode() { 
        return prefs.getBoolean(KEY_DATA_SAVER, false); 
    }
    public void setDataSaverMode(boolean b) { 
        prefs.edit().putBoolean(KEY_DATA_SAVER, b).apply(); 
    }

    public boolean isPreloadPages() { 
        return prefs.getBoolean(KEY_PRELOAD_PAGES, false); 
    }
    public void setPreloadPages(boolean b) { 
        prefs.edit().putBoolean(KEY_PRELOAD_PAGES, b).apply(); 
    }

    public boolean isHardwareAccelEnabled() { 
        return prefs.getBoolean(KEY_HARDWARE_ACCEL, true); 
    }
    public void setHardwareAccelEnabled(boolean b) { 
        prefs.edit().putBoolean(KEY_HARDWARE_ACCEL, b).apply(); 
    }

    // ==================== DOWNLOADS ====================
    public boolean isAskBeforeDownload() { 
        return prefs.getBoolean(KEY_ASK_BEFORE_DL, true); 
    }
    public void setAskBeforeDownload(boolean b) { 
        prefs.edit().putBoolean(KEY_ASK_BEFORE_DL, b).apply(); 
    }

    public boolean isDownloadNotifications() { 
        return prefs.getBoolean(KEY_DOWNLOAD_NOTIFY, true); 
    }
    public void setDownloadNotifications(boolean b) { 
        prefs.edit().putBoolean(KEY_DOWNLOAD_NOTIFY, b).apply(); 
    }

    // ==================== APPEARANCE ====================
    public boolean isShowTabCount() { 
        return prefs.getBoolean(KEY_SHOW_TAB_COUNT, true); 
    }
    public void setShowTabCount(boolean b) { 
        prefs.edit().putBoolean(KEY_SHOW_TAB_COUNT, b).apply(); 
    }

    public boolean isShowUrlScheme() { 
        return prefs.getBoolean(KEY_SHOW_URL_SCHEME, false); 
    }
    public void setShowUrlScheme(boolean b) { 
        prefs.edit().putBoolean(KEY_SHOW_URL_SCHEME, b).apply(); 
    }

    // ==================== SESSION ====================
    public String getSavedSessions() { 
        return prefs.getString(KEY_SAVED_SESSIONS, ""); 
    }
    public void setSavedSessions(String s) { 
        prefs.edit().putString(KEY_SAVED_SESSIONS, s).apply(); 
    }

    public boolean isFirstLaunch() { 
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true); 
    }
    public void setFirstLaunch(boolean b) { 
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, b).apply(); 
    }

    // ==================== STATS ====================
    public int getLifetimeBlockedAds() { 
        return prefs.getInt(KEY_LIFETIME_BLOCKED, 0); 
    }
    public void incrementLifetimeBlockedAds(int n) {
        prefs.edit().putInt(KEY_LIFETIME_BLOCKED, getLifetimeBlockedAds() + n).apply();
    }

    public int getLifetimeSavedMb() { 
        return prefs.getInt(KEY_LIFETIME_SAVED_MB, 0); 
    }
    public void addLifetimeSavedMb(int mb) {
        prefs.edit().putInt(KEY_LIFETIME_SAVED_MB, getLifetimeSavedMb() + mb).apply();
    }

    public int getTotalTabsOpened() { 
        return prefs.getInt(KEY_TOTAL_TABS_OPENED, 0); 
    }
    public void incrementTabsOpened() {
        prefs.edit().putInt(KEY_TOTAL_TABS_OPENED, getTotalTabsOpened() + 1).apply();
    }

    public void clearAllData() {
        prefs.edit()
            .remove(KEY_SAVED_SESSIONS)
            .apply();
    }
}
