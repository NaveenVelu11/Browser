package com.naveen.browser.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class SitePermissionManager {
    private static final String PREF_NAME = "site_permissions_pref";
    private final SharedPreferences prefs;

    public SitePermissionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getHost(String url) {
        if (url == null) return "";
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : url.toLowerCase();
        } catch (Exception e) {
            return url.toLowerCase();
        }
    }

    public int getPermission(String url, String permissionType) {
        String host = getHost(url);
        if (host.isEmpty()) return 0; // 0 = Ask
        return prefs.getInt("permission_" + host + "_" + permissionType, 0);
    }

    public void setPermission(String url, String permissionType, int value) {
        String host = getHost(url);
        if (host.isEmpty()) return;
        prefs.edit().putInt("permission_" + host + "_" + permissionType, value).apply();
    }

    public void clearAllPermissions() {
        prefs.edit().clear().apply();
    }
}
