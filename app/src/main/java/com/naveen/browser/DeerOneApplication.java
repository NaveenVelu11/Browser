package com.naveen.browser;

import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.naveen.browser.utils.CrashHandler;
import com.naveen.browser.utils.PreferenceManager;

public class DeerOneApplication extends MultiDexApplication {

    private static final String TAG = "DeerOneApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize global crash report handler for catching uncaught errors
            CrashHandler.init(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize CrashHandler", e);
        }

        try {
            // Enable vector drawable support for all API levels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to enable vector drawable support", e);
        }

        // Apply user's saved theme preference (light / dark / follow system)
        try {
            new PreferenceManager(this).applyTheme();
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply theme", e);
            // Theme application failed but app should still launch
        }
    }
}
