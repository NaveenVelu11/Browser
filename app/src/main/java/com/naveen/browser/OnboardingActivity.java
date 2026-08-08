package com.naveen.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.naveen.browser.utils.PreferenceManager;

public class OnboardingActivity extends AppCompatActivity {

    static {
        androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private ViewPager viewPager;
    private LinearLayout layoutDots;
    private Button btnNext;
    private TextView btnSkip;

    private int[] layouts;
    private TextView[] dots;
    private PreferenceManager prefManager;

    private final String[] titles = {
            "Privacy Redefined",
            "Blazing Fast Speed",
            "Ad & Tracker Shield",
            "Modern Web Features"
    };

    private final String[] descriptions = {
            "Browse the web with peace of mind. We block cookies, trackers, and clear data to prevent profiling.",
            "Experience lightning fast page loads. DeerOne is built for high speed and efficient rendering.",
            "Block intrusive ads and hidden trackers. Reduce mobile data usage and load pages cleaner.",
            "Manage unlimited tabs, download files easily, customize themes, and access shortcuts instantly."
    };

    private final int[] images = {
            R.drawable.ic_onboarding_privacy,
            R.drawable.ic_onboarding_speed,
            R.drawable.ic_onboarding_adblock,
            R.drawable.ic_onboarding_features
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = new PreferenceManager(this);

        if (!prefManager.isFirstLaunch()) {
            launchHomeScreen();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.view_pager_onboarding);
        layoutDots = findViewById(R.id.layout_dots);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);

        addBottomDots(0);

        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnNext.setOnClickListener(v -> {
            int current = getItem(1);
            if (current < titles.length) {
                viewPager.setCurrentItem(current);
            } else {
                finishOnboarding();
            }
        });
    }

    private void addBottomDots(int currentPage) {
        layoutDots.removeAllViews();
        for (int i = 0; i < titles.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == currentPage ? dpToPx(24) : dpToPx(8),
                    dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == currentPage ? R.drawable.dot_active : R.drawable.dot_inactive);
            layoutDots.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void finishOnboarding() {
        prefManager.setFirstLaunch(false);
        launchHomeScreen();
    }

    private void launchHomeScreen() {
        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            if (position == titles.length - 1) {
                btnNext.setText("Get Started");
                btnSkip.setVisibility(View.GONE);
            } else {
                btnNext.setText("Next");
                btnSkip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    private class OnboardingPagerAdapter extends PagerAdapter {

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(OnboardingActivity.this)
                    .inflate(R.layout.item_onboarding_page, container, false);

            ImageView img = view.findViewById(R.id.onboarding_image);
            TextView title = view.findViewById(R.id.onboarding_title);
            TextView desc = view.findViewById(R.id.onboarding_desc);

            img.setImageResource(images[position]);
            title.setText(titles[position]);
            desc.setText(descriptions[position]);

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object obj) {
            return view == obj;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
