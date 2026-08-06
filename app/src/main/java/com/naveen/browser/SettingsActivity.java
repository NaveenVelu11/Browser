package com.naveen.browser;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.naveen.browser.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager pref;

    // General
    private EditText editHomepage;
    private Spinner spinnerSearch, spinnerUA, spinnerTheme, spinnerTextSize, spinnerCookies;

    // Privacy
    private SwitchMaterial swAdBlock, swTrackers, swHttps, swDNT, swPopups, swSafeBrowsing;

    // Browsing
    private SwitchMaterial swJavascript, swImages, swAutofill, swSession, swLocation,
            swAutoplay, swNewTab, swPullRefresh;

    // Performance
    private SwitchMaterial swDataSaver, swPreload, swHardware;

    // Downloads
    private SwitchMaterial swAskDownload, swDlNotify;

    // Appearance
    private SwitchMaterial swShowTabCount, swShowScheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = new PreferenceManager(this);
        pref.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back
        findViewById(R.id.btn_back_settings).setOnClickListener(v -> finish());

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

        // --- BROWSING ---
        swJavascript = sw(R.id.switch_javascript,   pref.isJavaScriptEnabled());
        swImages     = sw(R.id.switch_images,       pref.isShowImages());
        swAutofill   = sw(R.id.switch_autofill,     pref.isAutofillEnabled());
        swSession    = sw(R.id.switch_session_restore, pref.isSessionRestoreEnabled());
        swLocation   = sw(R.id.switch_location,     pref.isLocationEnabled());
        swAutoplay   = sw(R.id.switch_media_autoplay, pref.isMediaAutoplay());
        swNewTab     = sw(R.id.switch_open_new_tab,  pref.isOpenLinksNewTab());
        swPullRefresh= sw(R.id.switch_pull_refresh,  pref.isPullToRefresh());

        // --- PERFORMANCE ---
        swDataSaver = sw(R.id.switch_data_saver, pref.isDataSaverMode());
        swPreload   = sw(R.id.switch_preload,    pref.isPreloadPages());
        swHardware  = sw(R.id.switch_hardware,   pref.isHardwareAccelEnabled());

        // --- DOWNLOADS ---
        swAskDownload = sw(R.id.switch_ask_download, pref.isAskBeforeDownload());
        swDlNotify    = sw(R.id.switch_dl_notify,    pref.isDownloadNotifications());

        // --- APPEARANCE ---
        swShowTabCount = sw(R.id.switch_show_tab_count, pref.isShowTabCount());
        swShowScheme   = sw(R.id.switch_show_scheme,    pref.isShowUrlScheme());

        // --- CLEAR DATA ---
        findViewById(R.id.btn_clear_cache).setOnClickListener(v -> confirmClear("Clear Cache?",
                "Remove temporary files", () -> {
                    WebView tmp = new WebView(this); tmp.clearCache(true); tmp.destroy();
                    toast("Cache cleared");
                }));
        findViewById(R.id.btn_clear_cookies).setOnClickListener(v -> confirmClear("Clear Cookies?",
                "You will be signed out of all sites", () -> {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    toast("Cookies cleared");
                }));
        findViewById(R.id.btn_clear_storage).setOnClickListener(v -> confirmClear("Clear All Data?",
                "This removes cookies, storage and sessions", () -> {
                    CookieManager.getInstance().removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    pref.clearAllData();
                    toast("All data cleared");
                }));

        // --- SAVE ---
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> save());
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
        String hp = editHomepage.getText().toString().trim();
        pref.setHomepage(hp.isEmpty() ? PreferenceManager.DEFAULT_HOMEPAGE : hp);
        pref.setSearchEngineIndex(spinnerSearch.getSelectedItemPosition());
        pref.setUserAgentIndex(spinnerUA.getSelectedItemPosition());
        pref.setTextSizeIndex(spinnerTextSize.getSelectedItemPosition());
        pref.setCookiePolicy(spinnerCookies.getSelectedItemPosition());

        int oldTheme = pref.getThemeMode();
        int newTheme = spinnerTheme.getSelectedItemPosition();
        pref.setThemeMode(newTheme);
        if (oldTheme != newTheme) pref.applyTheme();

        pref.setAdBlockEnabled(swAdBlock.isChecked());
        pref.setTrackerBlockEnabled(swTrackers.isChecked());
        pref.setHttpsOnlyEnabled(swHttps.isChecked());
        pref.setDoNotTrack(swDNT.isChecked());
        pref.setBlockPopups(swPopups.isChecked());
        pref.setSafeBrowsingEnabled(swSafeBrowsing.isChecked());
        pref.setJavaScriptEnabled(swJavascript.isChecked());
        pref.setShowImages(swImages.isChecked());
        pref.setAutofillEnabled(swAutofill.isChecked());
        pref.setSessionRestoreEnabled(swSession.isChecked());
        pref.setLocationEnabled(swLocation.isChecked());
        pref.setMediaAutoplay(swAutoplay.isChecked());
        pref.setOpenLinksNewTab(swNewTab.isChecked());
        pref.setPullToRefresh(swPullRefresh.isChecked());
        pref.setDataSaverMode(swDataSaver.isChecked());
        pref.setPreloadPages(swPreload.isChecked());
        pref.setHardwareAccelEnabled(swHardware.isChecked());
        pref.setAskBeforeDownload(swAskDownload.isChecked());
        pref.setDownloadNotifications(swDlNotify.isChecked());
        pref.setShowTabCount(swShowTabCount.isChecked());
        pref.setShowUrlScheme(swShowScheme.isChecked());

        toast("Settings saved ✓");
        finish();
    }
}
