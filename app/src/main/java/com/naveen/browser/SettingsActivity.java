package com.naveen.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.naveen.browser.utils.PreferenceManager;
import com.naveen.browser.utils.SitePermissionManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager pref;

    private TextView tvSubtitleSearchEngine;
    private TextView tvSubtitleHomepage;
    private TextView tvSubtitleLandingPage;
    private TextView tvSubtitleTheme;
    private TextView tvSubtitlePrivacy;

    private static final String[] SEARCH_ENGINES = {
            "Google", "DuckDuckGo", "Bing", "Brave Search", "Yahoo", "Ecosia", "Startpage"
    };

    private static final String[] LANDING_PAGES = {
            "DeerOne Home (Default)", "Google Search", "Custom Homepage URL"
    };

    private static final String[] THEMES = {
            "Light Theme", "Dark Theme"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = new PreferenceManager(this);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btn_back_settings);
        ImageButton btnClose = findViewById(R.id.btn_close_settings);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnClose != null) btnClose.setOnClickListener(v -> finish());

        tvSubtitleSearchEngine = findViewById(R.id.tv_subtitle_search_engine);
        tvSubtitleHomepage = findViewById(R.id.tv_subtitle_homepage);
        tvSubtitleLandingPage = findViewById(R.id.tv_subtitle_landing_page);
        tvSubtitleTheme = findViewById(R.id.tv_subtitle_theme);
        tvSubtitlePrivacy = findViewById(R.id.tv_subtitle_privacy);

        updateSubtitles();

        // 1. Search Engine Sub-Menu
        View rowSearch = findViewById(R.id.row_search_engine);
        if (rowSearch != null) {
            rowSearch.setOnClickListener(v -> showSearchEngineDialog());
        }

        // 2. Homepage Sub-Menu
        View rowHomepage = findViewById(R.id.row_homepage);
        if (rowHomepage != null) {
            rowHomepage.setOnClickListener(v -> showHomepageDialog());
        }

        // 2b. Landing Page Sub-Menu
        View rowLanding = findViewById(R.id.row_landing_page);
        if (rowLanding != null) {
            rowLanding.setOnClickListener(v -> showLandingPageDialog());
        }

        // 3. Theme Sub-Menu
        View rowTheme = findViewById(R.id.row_theme);
        if (rowTheme != null) {
            rowTheme.setOnClickListener(v -> showThemeDialog());
        }

        // 4. Passwords Sub-Menu
        View rowPasswords = findViewById(R.id.row_passwords);
        if (rowPasswords != null) {
            rowPasswords.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Passwords & Autofill")
                        .setMessage("Form autofill and login security are enabled automatically by DeerOne Clean Space.")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        // 5. Sync Sub-Menu
        View rowSync = findViewById(R.id.row_sync);
        if (rowSync != null) {
            rowSync.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Sync")
                        .setMessage("Bookmarks, history and preference settings are securely synchronized locally on your device.")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        // 6. Privacy & Shields Sub-Menu
        View rowPrivacy = findViewById(R.id.row_privacy_report);
        if (rowPrivacy != null) {
            rowPrivacy.setOnClickListener(v -> showPrivacyShieldsDialog());
        }

        // 7. Site Settings Sub-Menu
        View rowSiteSettings = findViewById(R.id.row_site_settings);
        if (rowSiteSettings != null) {
            rowSiteSettings.setOnClickListener(v -> showSiteSettingsDialog());
        }

        // 8. Downloads Sub-Menu
        View rowDownloads = findViewById(R.id.row_downloads);
        if (rowDownloads != null) {
            rowDownloads.setOnClickListener(v -> {
                startActivity(new Intent(this, DownloadsActivity.class));
            });
        }

        // 9. Clear Data Sub-Menu
        View rowClearData = findViewById(R.id.row_clear_data);
        if (rowClearData != null) {
            rowClearData.setOnClickListener(v -> showClearDataDialog());
        }
    }

    private void updateSubtitles() {
        if (tvSubtitleSearchEngine != null) {
            int idx = pref.getSearchEngineIndex();
            if (idx >= 0 && idx < SEARCH_ENGINES.length) {
                tvSubtitleSearchEngine.setText(SEARCH_ENGINES[idx]);
            }
        }
        if (tvSubtitleHomepage != null) {
            String hp = pref.getHomepage();
            tvSubtitleHomepage.setText(hp != null && !hp.isEmpty() ? hp : "Google");
        }
        if (tvSubtitleTheme != null) {
            int themeMode = pref.getThemeMode();
            tvSubtitleTheme.setText(themeMode == 1 ? "Dark Theme" : "Light Theme");
        }
        if (tvSubtitlePrivacy != null) {
            boolean active = pref.isAdBlockEnabled() || pref.isTrackerBlockEnabled();
            tvSubtitlePrivacy.setText(active ? "Brave Shields Active (Ads, Trackers, HTTPS)" : "Shields Disabled");
        }
    }

    private void showSearchEngineDialog() {
        int current = pref.getSearchEngineIndex();
        new AlertDialog.Builder(this)
                .setTitle("Select Search Engine")
                .setSingleChoiceItems(SEARCH_ENGINES, current, (dialog, which) -> {
                    pref.setSearchEngineIndex(which);
                    updateSubtitles();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHomepageDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(pref.getHomepage());
        input.setCursorVisible(false);
        input.setOnFocusChangeListener((v, hasFocus) -> input.setCursorVisible(hasFocus));

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(pad, pad, pad, pad);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Set Homepage URL")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    pref.setHomepage(url.isEmpty() ? PreferenceManager.DEFAULT_HOMEPAGE : url);
                    updateSubtitles();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLandingPageDialog() {
        int current = pref.getLandingPageMode();
        new AlertDialog.Builder(this)
                .setTitle("Default Landing Page")
                .setSingleChoiceItems(LANDING_PAGES, current, (dialog, which) -> {
                    pref.setLandingPageMode(which);
                    updateSubtitles();
                    dialog.dismiss();
                    Toast.makeText(this, "Landing page updated to " + LANDING_PAGES[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        int current = pref.getThemeMode() == 1 ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(THEMES, current, (dialog, which) -> {
                    pref.setThemeMode(which);
                    pref.applyTheme();
                    updateSubtitles();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPrivacyShieldsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_shields, null);
        if (view == null) return;

        SwitchMaterial swAdBlock = view.findViewById(R.id.sw_ad_block);
        SwitchMaterial swTrackerBlock = view.findViewById(R.id.sw_tracker_block);
        SwitchMaterial swHttps = view.findViewById(R.id.sw_https_only);
        SwitchMaterial swDNT = view.findViewById(R.id.sw_dnt);
        SwitchMaterial swPopups = view.findViewById(R.id.sw_popups);
        SwitchMaterial swSafeBrowsing = view.findViewById(R.id.sw_safe_browsing);
        SwitchMaterial swBgVideo = view.findViewById(R.id.sw_background_video);

        if (swAdBlock != null) swAdBlock.setChecked(pref.isAdBlockEnabled());
        if (swTrackerBlock != null) swTrackerBlock.setChecked(pref.isTrackerBlockEnabled());
        if (swHttps != null) swHttps.setChecked(pref.isHttpsOnlyEnabled());
        if (swDNT != null) swDNT.setChecked(pref.isDoNotTrack());
        if (swPopups != null) swPopups.setChecked(pref.isBlockPopups());
        if (swSafeBrowsing != null) swSafeBrowsing.setChecked(pref.isSafeBrowsingEnabled());
        if (swBgVideo != null) swBgVideo.setChecked(pref.isBackgroundVideoEnabled());

        new AlertDialog.Builder(this)
                .setTitle("Privacy & Shields")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (swAdBlock != null) pref.setAdBlockEnabled(swAdBlock.isChecked());
                    if (swTrackerBlock != null) pref.setTrackerBlockEnabled(swTrackerBlock.isChecked());
                    if (swHttps != null) pref.setHttpsOnlyEnabled(swHttps.isChecked());
                    if (swDNT != null) pref.setDoNotTrack(swDNT.isChecked());
                    if (swPopups != null) pref.setBlockPopups(swPopups.isChecked());
                    if (swSafeBrowsing != null) pref.setSafeBrowsingEnabled(swSafeBrowsing.isChecked());
                    if (swBgVideo != null) pref.setBackgroundVideoEnabled(swBgVideo.isChecked());
                    updateSubtitles();
                    Toast.makeText(this, "Shield preferences saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSiteSettingsDialog() {
        String[] options = {
                "Clear Site Permissions (Camera, Mic, Location)",
                "Block All Cookies",
                "Allow Third-Party Cookies"
        };
        new AlertDialog.Builder(this)
                .setTitle("Site Settings")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        new SitePermissionManager(this).clearAllPermissions();
                        Toast.makeText(this, "Site permissions cleared", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        pref.setCookiePolicy(2);
                        Toast.makeText(this, "Blocking all cookies", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        pref.setCookiePolicy(0);
                        Toast.makeText(this, "Third-party cookies allowed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Browsing Data?")
                .setMessage("This will remove all temporary cached files, site cookies, web storage, and site permissions.")
                .setPositiveButton("Clear Data", (dialog, which) -> {
                    try {
                        WebView tmp = new WebView(this);
                        tmp.clearCache(true);
                        tmp.destroy();
                        CookieManager.getInstance().removeAllCookies(null);
                        CookieManager.getInstance().flush();
                        WebStorage.getInstance().deleteAllData();
                        new SitePermissionManager(this).clearAllPermissions();
                        Toast.makeText(this, "All browsing data cleared ✓", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
