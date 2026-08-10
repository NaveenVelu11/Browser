package com.naveen.browser;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.naveen.browser.utils.PreferenceManager;

public class DeerOneApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable vector drawable support (API 21+)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        // Apply user's saved theme preference (light / dark / follow system)
        try {
            new PreferenceManager(this).applyTheme();
        } catch (Exception ignored) {
        }
    }
}
