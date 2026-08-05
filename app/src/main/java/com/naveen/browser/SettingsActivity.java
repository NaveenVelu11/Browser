package com.naveen.browser;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.naveen.browser.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private EditText editHomepage;
    private Spinner spinnerSearchEngine;
    private Spinner spinnerUserAgent;
    private Spinner spinnerTheme;

    private SwitchMaterial switchAdBlock;
    private SwitchMaterial switchHttpsOnly;
    private SwitchMaterial switchSessionRestore;
    private SwitchMaterial switchAutofill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefManager = new PreferenceManager(this);
        prefManager.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btn_back_settings);
        editHomepage = findViewById(R.id.edit_homepage);
        spinnerSearchEngine = findViewById(R.id.spinner_search_engine);
        spinnerUserAgent = findViewById(R.id.spinner_user_agent);
        spinnerTheme = findViewById(R.id.spinner_theme);

        switchAdBlock = findViewById(R.id.switch_ad_block);
        switchHttpsOnly = findViewById(R.id.switch_https_only);
        switchSessionRestore = findViewById(R.id.switch_session_restore);
        switchAutofill = findViewById(R.id.switch_autofill);

        Button btnClearCache = findViewById(R.id.btn_clear_cache);
        Button btnClearCookies = findViewById(R.id.btn_clear_cookies);
        Button btnClearStorage = findViewById(R.id.btn_clear_storage);
        Button btnSave = findViewById(R.id.btn_save_settings);

        btnBack.setOnClickListener(v -> finish());

        ArrayAdapter<CharSequence> engineAdapter = ArrayAdapter.createFromResource(
                this, R.array.search_engine_names, android.R.layout.simple_spinner_item);
        engineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchEngine.setAdapter(engineAdapter);

        ArrayAdapter<CharSequence> uaAdapter = ArrayAdapter.createFromResource(
                this, R.array.user_agent_names, android.R.layout.simple_spinner_item);
        uaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserAgent.setAdapter(uaAdapter);

        editHomepage.setText(prefManager.getHomepage());
        spinnerSearchEngine.setSelection(prefManager.getSearchEngineIndex());
        spinnerUserAgent.setSelection(prefManager.getUserAgentIndex());
        spinnerTheme.setSelection(prefManager.getThemeMode());

        switchAdBlock.setChecked(prefManager.isAdBlockEnabled());
        switchHttpsOnly.setChecked(prefManager.isHttpsOnlyEnabled());
        switchSessionRestore.setChecked(prefManager.isSessionRestoreEnabled());
        switchAutofill.setChecked(prefManager.isAutofillEnabled());

        btnSave.setOnClickListener(v -> saveSettings());

        btnClearCache.setOnClickListener(v -> {
            WebView tempWeb = new WebView(this);
            tempWeb.clearCache(true);
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });

        btnClearCookies.setOnClickListener(v -> {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            Toast.makeText(this, R.string.cookies_cleared, Toast.LENGTH_SHORT).show();
        });

        btnClearStorage.setOnClickListener(v -> {
            WebStorage.getInstance().deleteAllData();
            Toast.makeText(this, R.string.storage_cleared, Toast.LENGTH_SHORT).show();
        });
    }

    private void saveSettings() {
        String hp = editHomepage.getText().toString().trim();
        if (hp.isEmpty()) {
            hp = PreferenceManager.DEFAULT_HOMEPAGE;
        }
        prefManager.setHomepage(hp);
        prefManager.setSearchEngineIndex(spinnerSearchEngine.getSelectedItemPosition());
        prefManager.setUserAgentIndex(spinnerUserAgent.getSelectedItemPosition());
        
        int oldTheme = prefManager.getThemeMode();
        int newTheme = spinnerTheme.getSelectedItemPosition();
        prefManager.setThemeMode(newTheme);
        if (oldTheme != newTheme) {
            prefManager.applyTheme();
        }

        prefManager.setAdBlockEnabled(switchAdBlock.isChecked());
        prefManager.setHttpsOnlyEnabled(switchHttpsOnly.isChecked());
        prefManager.setSessionRestoreEnabled(switchSessionRestore.isChecked());
        prefManager.setAutofillEnabled(switchAutofill.isChecked());

        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
