package com.naveen.browser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;

import com.naveen.browser.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    static {
        androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager pm = new PreferenceManager(this);
        pm.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashContent = findViewById(R.id.splash_content);
        if (splashContent != null) {
            splashContent.setAlpha(0f);
            splashContent.setScaleX(0.85f);
            splashContent.setScaleY(0.85f);
            splashContent.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(450)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // Sub-second startup target (650ms max before navigation)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (pm.isFirstLaunch()) {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 650);
    }
}
