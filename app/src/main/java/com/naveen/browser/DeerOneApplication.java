package com.naveen.browser;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.naveen.browser.utils.PreferenceManager;

public class DeerOneApplication extends Application {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        try {
            new PreferenceManager(this).applyTheme();
        } catch (Exception ignored) {
        }
    }
}
