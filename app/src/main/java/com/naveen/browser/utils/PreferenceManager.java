package com.naveen.browser.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "browser_preferences";

    private static final String KEY_HOMEPAGE = "homepage_url";
    private static final String KEY_SEARCH_ENGINE = "search_engine";
    private static final String KEY_USER_AGENT = "user_agent_index";
    private static final String KEY_CUSTOM_USER_AGENT = "custom_user_agent";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_DESKTOP_MODE = "desktop_mode";
    private static final String KEY_AD_BLOCK = "ad_block_enabled";
    private static final String KEY_TRACKER_BLOCK = "tracker_block_enabled";
    private static final String KEY_HTTPS_ONLY = "https_only_enabled";
    private static final String KEY_SESSION_RESTORE = "session_restore_enabled";
    private static final String KEY_AUTOFILL = "autofill_enabled";
    private static final String KEY_SAVED_SESSIONS = "saved_sessions_urls";

    public static final String DEFAULT_HOMEPAGE = "https://www.google.com";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getHomepage() {
        return prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE);
    }

    public void setHomepage(String homepage) {
        prefs.edit().putString(KEY_HOMEPAGE, homepage).apply();
    }

    public int getSearchEngineIndex() {
        return prefs.getInt(KEY_SEARCH_ENGINE, 0);
    }

    public void setSearchEngineIndex(int index) {
        prefs.edit().putInt(KEY_SEARCH_ENGINE, index).apply();
    }

    public int getUserAgentIndex() {
        return prefs.getInt(KEY_USER_AGENT, 0);
    }

    public void setUserAgentIndex(int index) {
        prefs.edit().putInt(KEY_USER_AGENT, index).apply();
    }

    public String getCustomUserAgent() {
        return prefs.getString(KEY_CUSTOM_USER_AGENT, "");
    }

    public void setCustomUserAgent(String userAgent) {
        prefs.edit().putString(KEY_CUSTOM_USER_AGENT, userAgent).apply();
    }

    public boolean isNightMode() {
        return prefs.getBoolean(KEY_NIGHT_MODE, false);
    }

    public void setNightMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply();
    }

    public boolean isDesktopMode() {
        return prefs.getBoolean(KEY_DESKTOP_MODE, false);
    }

    public void setDesktopMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply();
    }

    public boolean isAdBlockEnabled() {
        return prefs.getBoolean(KEY_AD_BLOCK, true);
    }

    public void setAdBlockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AD_BLOCK, enabled).apply();
    }

    public boolean isTrackerBlockEnabled() {
        return prefs.getBoolean(KEY_TRACKER_BLOCK, true);
    }

    public void setTrackerBlockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRACKER_BLOCK, enabled).apply();
    }

    public boolean isHttpsOnlyEnabled() {
        return prefs.getBoolean(KEY_HTTPS_ONLY, true);
    }

    public void setHttpsOnlyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HTTPS_ONLY, enabled).apply();
    }

    public boolean isSessionRestoreEnabled() {
        return prefs.getBoolean(KEY_SESSION_RESTORE, true);
    }

    public void setSessionRestoreEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SESSION_RESTORE, enabled).apply();
    }

    public boolean isAutofillEnabled() {
        return prefs.getBoolean(KEY_AUTOFILL, true);
    }

    public void setAutofillEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTOFILL, enabled).apply();
    }

    public String getSavedSessions() {
        return prefs.getString(KEY_SAVED_SESSIONS, "");
    }

    public void setSavedSessions(String sessionData) {
        prefs.edit().putString(KEY_SAVED_SESSIONS, sessionData).apply();
    }
}
