package com.naveen.browser;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class DeerOneApplication extends Application {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
