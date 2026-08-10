package com.naveen.browser;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;

import com.naveen.browser.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load splash layout", e);
            // Fallback: Navigate directly
            navigateAfterSplash();
            return;
        }

        final PreferenceManager pm = new PreferenceManager(this);

        try {
            View splashContent = findViewById(R.id.splash_content);
            if (splashContent != null) {
                splashContent.setAlpha(0f);
                splashContent.setScaleX(0.88f);
                splashContent.setScaleY(0.88f);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    splashContent.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(480)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                } else {
                    // Fallback for older Android versions
                    splashContent.setAlpha(1f);
                    splashContent.setScaleX(1f);
                    splashContent.setScaleY(1f);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error animating splash content", e);
        }

        // Navigate after 700ms
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateAfterSplash, 700);
    }

    private void navigateAfterSplash() {
        try {
            Intent intent;
            PreferenceManager pm = new PreferenceManager(this);
            
            if (pm.isFirstLaunch()) {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
            
            startActivity(intent);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating after splash", e);
            // Emergency fallback
            try {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            } catch (Exception ex) {
                Log.e(TAG, "Critical error in navigation", ex);
            }
        }
    }
}
