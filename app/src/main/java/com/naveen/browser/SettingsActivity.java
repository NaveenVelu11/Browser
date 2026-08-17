package com.naveen.browser;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.naveen.browser.utils.PreferenceManager;
import com.naveen.browser.utils.SitePermissionManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager pref;

    // General
    private EditText editHomepage;
    private Spinner spinnerSearch, spinnerUA, spinnerTheme, spinnerTextSize, spinnerCookies;

    // Privacy Switches
    private SwitchMaterial swAdBlock, swTrackers, swHttps, swDNT, swPopups, swSafeBrowsing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = new PreferenceManager(this);
        setContentView(R.layout.activity_settings);

        // Header Navigation Arrows (iOS / Chrome Sheet Style)
        ImageButton btnBack = findViewById(R.id.btn_back_settings);
        ImageButton btnForward = findViewById(R.id.btn_header_forward);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnForward != null) {
            btnForward.setEnabled(false);
            btnForward.setAlpha(0.35f);
        }

        // Stats card
        updateStatsCard();

        // --- GENERAL ---
        editHomepage = findViewById(R.id.edit_homepage);
        editHomepage.setText(pref.getHomepage());

        spinnerSearch = findViewById(R.id.spinner_search_engine);
        setSpinner(spinnerSearch, R.array.search_engine_names, pref.getSearchEngineIndex());

        spinnerUA = findViewById(R.id.spinner_user_agent);
        setSpinner(spinnerUA, R.array.user_agent_names, pref.getUserAgentIndex());

        spinnerTheme = findViewById(R.id.spinner_theme);
        setSpinner(spinnerTheme, R.array.theme_names, pref.getThemeMode());

        spinnerTextSize = findViewById(R.id.spinner_text_size);
        setSpinner(spinnerTextSize, R.array.text_size_names, pref.getTextSizeIndex());

        spinnerCookies = findViewById(R.id.spinner_cookies);
        setSpinner(spinnerCookies, R.array.cookie_policy_names, pref.getCookiePolicy());

        // --- PRIVACY ---
        swAdBlock      = sw(R.id.switch_ad_block,      pref.isAdBlockEnabled());
        swTrackers     = sw(R.id.switch_tracker_block, pref.isTrackerBlockEnabled());
        swHttps        = sw(R.id.switch_https_only,    pref.isHttpsOnlyEnabled());
        swDNT          = sw(R.id.switch_dnt,           pref.isDoNotTrack());
        swPopups       = sw(R.id.switch_block_popups,  pref.isBlockPopups());
        swSafeBrowsing = sw(R.id.switch_safe_browsing, pref.isSafeBrowsingEnabled());

        // --- CLEAR DATA ---
        View btnClearCache = findViewById(R.id.btn_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> confirmClear("Clear Cache?",
                    "Remove temporary files", () -> {
                        WebView tmp = new WebView(this); tmp.clearCache(true); tmp.destroy();
                        toast("Cache cleared");
                    }));
        }

        View btnClearCookies = findViewById(R.id.btn_clear_cookies);
        if (btnClearCookies != null) {
            btnClearCookies.setOnClickListener(v -> confirmClear("Clear Cookies?",
                    "You will be signed out of all sites", () -> {
                        CookieManager.getInstance().removeAllCookies(null);
                        CookieManager.getInstance().flush();
                        toast("Cookies cleared");
                    }));
        }

        View btnClearPermissions = findViewById(R.id.btn_clear_permissions);
        if (btnClearPermissions != null) {
            btnClearPermissions.setOnClickListener(v -> confirmClear("Clear Site Permissions?",
                    "Reset location, camera, and microphone permissions to Ask", () -> {
                        new SitePermissionManager(this).clearAllPermissions();
                        toast("Permissions cleared");
                    }));
        }

        View btnClearStorage = findViewById(R.id.btn_clear_storage);
        if (btnClearStorage != null) {
            btnClearStorage.setOnClickListener(v -> confirmClear("Clear All Data?",
                    "This removes cookies, storage and sessions", () -> {
                        CookieManager.getInstance().removeAllCookies(null);
                        WebStorage.getInstance().deleteAllData();
                        pref.clearAllData();
                        toast("All data cleared");
                    }));
        }

        // --- SAVE ---
        View btnSave = findViewById(R.id.btn_save_settings);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> save());
        }
    }

    private void updateStatsCard() {
        TextView tvBlocked = findViewById(R.id.tv_stat_blocked);
        TextView tvSaved   = findViewById(R.id.tv_stat_saved);
        TextView tvTabs    = findViewById(R.id.tv_stat_tabs);
        if (tvBlocked != null) tvBlocked.setText(String.valueOf(pref.getLifetimeBlockedAds()));
        if (tvSaved   != null) tvSaved.setText(pref.getLifetimeSavedMb() + " MB");
        if (tvTabs    != null) tvTabs.setText(String.valueOf(pref.getTotalTabsOpened()));
    }

    private SwitchMaterial sw(int id, boolean checked) {
        SwitchMaterial s = findViewById(id);
        if (s != null) s.setChecked(checked);
        return s;
    }

    private void setSpinner(Spinner s, int arrayRes, int selection) {
        if (s == null) return;
        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(this, arrayRes,
                android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setSelection(selection);
    }

    private void confirmClear(String title, String msg, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title).setMessage(msg)
                .setPositiveButton("Clear", (d, w) -> action.run())
                .setNegativeButton("Cancel", null).show();
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void save() {
        if (editHomepage != null) {
            String hp = editHomepage.getText().toString().trim();
            pref.setHomepage(hp.isEmpty() ? PreferenceManager.DEFAULT_HOMEPAGE : hp);
        }
        if (spinnerSearch != null) pref.setSearchEngineIndex(spinnerSearch.getSelectedItemPosition());
        if (spinnerUA != null) pref.setUserAgentIndex(spinnerUA.getSelectedItemPosition());
        if (spinnerTextSize != null) pref.setTextSizeIndex(spinnerTextSize.getSelectedItemPosition());
        if (spinnerCookies != null) pref.setCookiePolicy(spinnerCookies.getSelectedItemPosition());

        if (spinnerTheme != null) {
            int oldTheme = pref.getThemeMode();
            int newTheme = spinnerTheme.getSelectedItemPosition();
            pref.setThemeMode(newTheme);
            if (oldTheme != newTheme) pref.applyTheme();
        }

        if (swAdBlock != null) pref.setAdBlockEnabled(swAdBlock.isChecked());
        if (swTrackers != null) pref.setTrackerBlockEnabled(swTrackers.isChecked());
        if (swHttps != null) pref.setHttpsOnlyEnabled(swHttps.isChecked());
        if (swDNT != null) pref.setDoNotTrack(swDNT.isChecked());
        if (swPopups != null) pref.setBlockPopups(swPopups.isChecked());
        if (swSafeBrowsing != null) pref.setSafeBrowsingEnabled(swSafeBrowsing.isChecked());

        toast("Settings saved ✓");
        finish();
    }
}
