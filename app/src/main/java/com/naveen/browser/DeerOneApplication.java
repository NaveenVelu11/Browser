package com.naveen.browser;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.naveen.browser.utils.PreferenceManager;

public class DeerOneApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable vector drawable support on API 21+ (must be before any UI inflation)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        // Apply user's saved theme preference (light/dark/system)
        try {
            new PreferenceManager(this).applyTheme();
        } catch (Exception ignored) {
        }
    }
}
