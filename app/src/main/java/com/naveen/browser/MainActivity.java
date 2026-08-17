package com.naveen.browser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naveen.browser.adapter.HistoryAdapter;
import com.naveen.browser.adapter.ShortcutsAdapter;
import com.naveen.browser.adapter.TabsAdapter;
import com.naveen.browser.db.DatabaseHelper;
import com.naveen.browser.model.BookmarkItem;
import com.naveen.browser.model.HistoryItem;
import com.naveen.browser.model.MediaItem;
import com.naveen.browser.model.ShortcutItem;
import com.naveen.browser.model.WebTab;
import com.naveen.browser.dialog.BottomSheetContextMenuDialog;
import com.naveen.browser.dialog.FormatPickerBottomSheetDialog;
import com.naveen.browser.utils.AdBlocker;
import com.naveen.browser.utils.MediaSnifferEngine;
import com.naveen.browser.utils.PreferenceManager;
import com.naveen.browser.utils.WebEdgeGestureListener;
import com.naveen.browser.utils.WebUtils;
import com.naveen.browser.utils.YtDlpExtractor;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private static final int REQUEST_PERMISSIONS = 101;
    private static final int REQUEST_FILE_CHOOSER = 102;
    private static final int REQUEST_BOOKMARKS = 103;
    private static final int REQUEST_HISTORY = 104;

    private FrameLayout webViewContainer;
    private FrameLayout customViewContainer;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private EditText editUrl;
    private ImageView btnClearUrl;
    private ImageView btnSslLock;
    private TextView txtTabCount;
    private View homeScreenLayout;
    private View topBarContainer;
    private View centerContainer;
    private View bottomNavigationBar;
    private View btnFloatingHeaderTrigger;
    private View btnFloatingDownloadBadge;
    private View btnCollapseHeader;
    private final Handler headerAutoHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideHeaderRunnable = this::hideTopHeaderBar;
    private final MediaSnifferEngine mediaSnifferEngine = new MediaSnifferEngine();
    private boolean isBarsHidden = false;
    private boolean isLayoutTransitioning = false;

    // Media Sniffer
    private TextView txtMediaDetected;
    private final List<MediaItem> detectedMediaList = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Shortcuts & Recent Visited
    private RecyclerView recyclerCustomShortcuts;
    private RecyclerView recyclerRecentVisited;
    private View layoutHomeEmptyState;
    private ShortcutsAdapter shortcutsAdapter;
    private HistoryAdapter recentVisitedAdapter;
    private final List<ShortcutItem> shortcutsList = new ArrayList<>();
    private final List<HistoryItem> recentVisitedList = new ArrayList<>();

    private PreferenceManager prefManager;
    private DatabaseHelper dbHelper;

    private final List<WebTab> tabList = new ArrayList<>();
    private int currentTabPosition = -1;

    private ValueCallback<Uri[]> filePathCallback;
    private String cameraPhotoPath;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        prefManager = new PreferenceManager(this);
        setContentView(R.layout.activity_main);
        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        requestPermissionsIfNecessary();

        Intent intent = getIntent();
        String initialUrl = prefManager.getHomepage();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            initialUrl = intent.getData().toString();
        } else {
            if (initialUrl == null || initialUrl.equals("https://www.google.com") || initialUrl.equals("about:blank")) {
                initialUrl = "about:blank";
            }
        }

        // Restore Session or Create Initial Tab
        if (savedInstanceState != null) {
            int tabCount = savedInstanceState.getInt("tab_count", 0);
            int activePos = savedInstanceState.getInt("current_tab_position", -1);
            for (int i = 0; i < tabCount; i++) {
                Bundle tabState = savedInstanceState.getBundle("tab_state_" + i);
                if (tabState != null) {
                    String id = tabState.getString("tab_id");
                    String title = tabState.getString("tab_title");
                    boolean incognito = tabState.getBoolean("tab_incognito");
                    Bundle webState = tabState.getBundle("web_state");
                    
                    WebView webView = createConfiguredWebView(incognito, false);
                    if (webState != null) {
                        webView.restoreState(webState);
                    }
                    WebTab tab = new WebTab(id, title, webView.getUrl(), webView, incognito);
                    tabList.add(tab);
                }
            }
            if (!tabList.isEmpty()) {
                switchToTab(activePos >= 0 ? activePos : 0);
            } else {
                createNewTab(initialUrl, false);
            }
        } else {
            if (prefManager.isSessionRestoreEnabled() && !prefManager.getSavedSessions().isEmpty()) {
                restoreSession();
            } else {
                createNewTab(initialUrl, false);
            }
        }
    }

    private void initViews() {
        webViewContainer = findViewById(R.id.webview_container);
        customViewContainer = findViewById(R.id.custom_view_container);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        editUrl = findViewById(R.id.edit_url);
        btnClearUrl = findViewById(R.id.btn_clear_url);
        btnSslLock = findViewById(R.id.btn_ssl_lock);
        if (btnSslLock != null) {
            btnSslLock.setOnClickListener(v -> showPrivacyDashboard());
        }
        txtTabCount = findViewById(R.id.txt_tab_count);
        homeScreenLayout = findViewById(R.id.home_screen_layout);
        topBarContainer = findViewById(R.id.top_bar_container);
        centerContainer = findViewById(R.id.center_container);
        bottomNavigationBar = null;

        btnFloatingHeaderTrigger = findViewById(R.id.btn_floating_header_trigger);
        btnFloatingDownloadBadge = null;
        btnCollapseHeader = findViewById(R.id.btn_collapse_header);
        txtMediaDetected = null;

        if (btnFloatingHeaderTrigger != null) {
            btnFloatingHeaderTrigger.setOnClickListener(v -> showTopHeaderBar());
        }
        if (btnCollapseHeader != null) {
            btnCollapseHeader.setOnClickListener(v -> hideTopHeaderBar());
        }
        if (btnFloatingDownloadBadge != null) {
            btnFloatingDownloadBadge.setOnClickListener(v -> handleDownloadBadgeClick());
        }

        mediaSnifferEngine.setOnMediaDetectedListener(item -> runOnUiThread(() -> {
            if (btnFloatingDownloadBadge != null) {
                btnFloatingDownloadBadge.setVisibility(View.VISIBLE);
            }
        }));

        View btnGo = findViewById(R.id.btn_go);
        View btnOverflow = findViewById(R.id.btn_overflow);
        View btnBack = findViewById(R.id.btn_back);
        View btnForward = findViewById(R.id.btn_forward);
        View btnRefresh = findViewById(R.id.btn_refresh);
        View btnHome = findViewById(R.id.btn_home);
        FrameLayout btnTabsLayout = findViewById(R.id.btn_tabs_layout);

        // Native Home Screen Integration
        setupHomeScreenContent();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                WebView currentWeb = getCurrentWebView();
                if (currentWeb != null && currentWeb.getUrl() != null && !currentWeb.getUrl().equals("about:blank")) {
                    currentWeb.reload();
                } else {
                    swipeRefresh.setRefreshing(false);
                }
            });
            swipeRefresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
                WebView currentWeb = getCurrentWebView();
                if (currentWeb == null) return false;
                if (currentWeb.getUrl() == null || currentWeb.getUrl().equals("about:blank")) {
                    return true;
                }
                return currentWeb.getScrollY() > 0 || currentWeb.canScrollVertically(-1);
            });
        }

        if (editUrl != null) {
            editUrl.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    loadEnteredUrl();
                    return true;
                }
                return false;
            });
        }

        if (btnClearUrl != null && editUrl != null) {
            editUrl.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    btnClearUrl.setVisibility(View.VISIBLE);
                } else {
                    btnClearUrl.setVisibility(View.GONE);
                }
            });
            btnClearUrl.setOnClickListener(v -> editUrl.setText(""));
        }

        if (btnGo != null) btnGo.setOnClickListener(v -> loadEnteredUrl());
        if (btnOverflow != null) btnOverflow.setOnClickListener(this::showOverflowMenu);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                WebView web = getCurrentWebView();
                if (web != null && web.canGoBack()) web.goBack();
            });
        }

        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                WebView web = getCurrentWebView();
                if (web != null && web.canGoForward()) web.goForward();
            });
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                WebView web = getCurrentWebView();
                if (web != null) web.reload();
            });
        }

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                WebView web = getCurrentWebView();
                if (web != null) {
                    String home = prefManager.getHomepage();
                    if (home == null || home.equals("https://www.google.com") || home.equals("about:blank")) {
                        web.loadUrl("about:blank");
                    } else {
                        web.loadUrl(home);
                    }
                }
            });
        }

        if (btnTabsLayout != null) {
            btnTabsLayout.setOnClickListener(v -> showTabsManagerDialog());
        }
    }

    private void setupHomeScreenContent() {
        if (homeScreenLayout == null) return;
        View cardHomeSearch = homeScreenLayout.findViewById(R.id.card_home_search);
        if (cardHomeSearch != null) cardHomeSearch.setOnClickListener(v -> showSearchInput());

        ImageView btnHomeVoice = homeScreenLayout.findViewById(R.id.btn_home_voice);
        if (btnHomeVoice != null) {
            btnHomeVoice.setOnClickListener(v -> {
                try {
                    Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search DeerOne...");
                    startActivityForResult(voiceIntent, 110);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Voice search not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View shortcutBookmarks = homeScreenLayout.findViewById(R.id.shortcut_bookmarks);
        View shortcutHistory = homeScreenLayout.findViewById(R.id.shortcut_history);
        View shortcutDownloads = homeScreenLayout.findViewById(R.id.shortcut_downloads);
        View shortcutSettings = homeScreenLayout.findViewById(R.id.shortcut_settings);

        if (shortcutBookmarks != null) shortcutBookmarks.setOnClickListener(v -> startActivityForResult(new Intent(MainActivity.this, BookmarksActivity.class), REQUEST_BOOKMARKS));
        if (shortcutHistory != null) shortcutHistory.setOnClickListener(v -> startActivityForResult(new Intent(MainActivity.this, HistoryActivity.class), REQUEST_HISTORY));
        if (shortcutDownloads != null) shortcutDownloads.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DownloadsActivity.class)));
        if (shortcutSettings != null) shortcutSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        TextView btnAddShortcut = homeScreenLayout.findViewById(R.id.btn_add_custom_shortcut);
        if (btnAddShortcut != null) btnAddShortcut.setOnClickListener(v -> showAddShortcutDialog());

        recyclerCustomShortcuts = homeScreenLayout.findViewById(R.id.recycler_custom_shortcuts);
        layoutHomeEmptyState = homeScreenLayout.findViewById(R.id.layout_home_empty_state);

        if (recyclerCustomShortcuts != null) {
            recyclerCustomShortcuts.setLayoutManager(new GridLayoutManager(this, 4));
            shortcutsAdapter = new ShortcutsAdapter(shortcutsList, new ShortcutsAdapter.OnShortcutClickListener() {
                @Override
                public void onShortcutClick(ShortcutItem item) {
                    loadUrl(item.getUrl());
                }

                @Override
                public void onShortcutLongClick(ShortcutItem item) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Remove Shortcut?")
                            .setMessage("Delete " + item.getTitle() + " from homepage shortcuts?")
                            .setPositiveButton("Delete", (d, w) -> {
                                dbHelper.deleteShortcut(item.getId());
                                loadShortcutsData();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
            recyclerCustomShortcuts.setAdapter(shortcutsAdapter);
        }

        if (recyclerRecentVisited != null) {
            recyclerRecentVisited.setLayoutManager(new LinearLayoutManager(this));
            recentVisitedAdapter = new HistoryAdapter(recentVisitedList, new HistoryAdapter.OnHistoryClickListener() {
                @Override
                public void onHistoryClick(HistoryItem item) {
                    loadUrl(item.getUrl());
                }

                @Override
                public void onHistoryDelete(HistoryItem item) {
                    dbHelper.deleteHistory(item.getId());
                    loadRecentVisitedData();
                }
            });
            recyclerRecentVisited.setAdapter(recentVisitedAdapter);
        }

        loadShortcutsData();
        loadRecentVisitedData();
    }

    private void loadShortcutsData() {
        shortcutsList.clear();
        shortcutsList.addAll(dbHelper.getAllShortcuts());
        if (shortcutsAdapter != null) shortcutsAdapter.notifyDataSetChanged();
    }

    private void loadRecentVisitedData() {
        recentVisitedList.clear();
        List<HistoryItem> allHistory = dbHelper.getAllHistory("");
        int limit = Math.min(5, allHistory.size());
        for (int i = 0; i < limit; i++) {
            recentVisitedList.add(allHistory.get(i));
        }

        if (recentVisitedAdapter != null) recentVisitedAdapter.notifyDataSetChanged();

        if (recentVisitedList.isEmpty() && shortcutsList.isEmpty()) {
            if (layoutHomeEmptyState != null) layoutHomeEmptyState.setVisibility(View.VISIBLE);
        } else {
            if (layoutHomeEmptyState != null) layoutHomeEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddShortcutDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText editTitle = new EditText(this);
        editTitle.setHint("Shortcut Name (e.g. GitHub)");
        layout.addView(editTitle);

        final EditText editUrlInput = new EditText(this);
        editUrlInput.setHint("Website URL (e.g. github.com)");
        layout.addView(editUrlInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Quick Shortcut")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String title = editTitle.getText().toString().trim();
                    String url = editUrlInput.getText().toString().trim();
                    if (!title.isEmpty() && !url.isEmpty()) {
                        String fullUrl = WebUtils.processUrlOrQuery(url, prefManager.getSearchEngineIndex());
                        dbHelper.addShortcut(new ShortcutItem(title, fullUrl));
                        loadShortcutsData();
                        Toast.makeText(MainActivity.this, "Shortcut added ✓", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestPermissionsIfNecessary() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void createNewTab(String url, boolean isIncognito) {
        prefManager.incrementTabsOpened();
        String finalUrl = prefManager.isHttpsOnlyEnabled() ? WebUtils.upgradeToHttps(url) : url;
        WebView webView = createConfiguredWebView(isIncognito, false);
        WebTab tab = new WebTab(UUID.randomUUID().toString(), "New Tab", finalUrl, webView, isIncognito);
        tabList.add(tab);
        switchToTab(tabList.size() - 1);
        String target = WebUtils.processUrlOrQuery(finalUrl, prefManager.getSearchEngineIndex());
        if (prefManager.isDoNotTrack()) {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("DNT", "1");
            webView.loadUrl(target, headers);
        } else {
            webView.loadUrl(target);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private WebView createConfiguredWebView(boolean isIncognito, boolean isDesktop) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Use default LAYER_TYPE_NONE so Chromium hardware renderer draws directly to window surface without offscreen GPU buffer overhead
        webView.setLayerType(android.view.View.LAYER_TYPE_NONE, null);


        WebSettings settings = webView.getSettings();

        // Speed & Performance Optimizations for Android 5.0/6.0+
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setEnableSmoothTransition(true);

        // Javascript & DOM Storage
        boolean enableJs = prefManager.isJavaScriptEnabled();
        settings.setJavaScriptEnabled(enableJs);
        settings.setDomStorageEnabled(!isIncognito && enableJs);
        settings.setDatabaseEnabled(!isIncognito && enableJs);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Text Zoom / Scaling
        settings.setTextZoom(prefManager.getTextZoomPercent());

        // Image Loading & Data Saver
        boolean loadImages = prefManager.isShowImages() && (!prefManager.isDataSaverMode());
        settings.setLoadsImagesAutomatically(loadImages);
        settings.setBlockNetworkImage(!loadImages);

        // Mixed Content Mode — API 21+, always true with minSdk 21
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(prefManager.isSafeBrowsingEnabled());
        }

        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setGeolocationEnabled(!isIncognito && prefManager.isLocationEnabled());
        settings.setJavaScriptCanOpenWindowsAutomatically(!prefManager.isBlockPopups());
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(!prefManager.isMediaAutoplay());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            settings.setOffscreenPreRaster(prefManager.isPreloadPages());
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }

        if (!isIncognito) {
            if (prefManager.isDataSaverMode()) {
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            } else {
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            }
        }

        applyUserAgent(settings, isDesktop);

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onLongPress(final String type, final String extra) {
                runOnUiThread(() -> {
                    int hitType = WebView.HitTestResult.UNKNOWN_TYPE;
                    if ("image".equals(type)) {
                        hitType = WebView.HitTestResult.IMAGE_TYPE;
                    } else if ("video".equals(type)) {
                        hitType = 99; // Custom code for video!
                    } else if ("link".equals(type)) {
                        hitType = WebView.HitTestResult.SRC_ANCHOR_TYPE;
                    } else if ("image_link".equals(type)) {
                        hitType = WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
                    }
                    showContextMenuBottomSheet(hitType, extra);
                });
            }
        }, "AndroidLongPress");

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            startDownload(url, userAgent, contentDisposition, mimeType);
        });

        // Cookie Policy
        CookieManager cookieManager = CookieManager.getInstance();
        if (isIncognito) {
            settings.setSaveFormData(false);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setDomStorageEnabled(false);
            settings.setDatabaseEnabled(false);
            cookieManager.setAcceptCookie(false);
        } else {
            int cookiePolicy = prefManager.getCookiePolicy();
            if (cookiePolicy == 2) {
                cookieManager.setAcceptCookie(false);
            } else {
                cookieManager.setAcceptCookie(true);
                // setAcceptThirdPartyCookies is API 21+ — always available with minSdk 21
                cookieManager.setAcceptThirdPartyCookies(webView, cookiePolicy == 0);
            }
        }

        // Scroll listener for auto-hiding top toolbar with anti-jitter threshold
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (isLayoutTransitioning) return;
                if (scrollY <= 5) {
                    showTopHeaderBar();
                } else if (scrollY > oldScrollY + 24) {
                    hideTopHeaderBar();
                } else if (scrollY < oldScrollY - 24) {
                    showTopHeaderBar();
                }
            });
        }

        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                showTopHeaderBar();
                return false;
            }
        });

        final WebEdgeGestureListener edgeGestureListener = new com.naveen.browser.utils.WebEdgeGestureListener(this, webView);
        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (editUrl != null && editUrl.hasFocus()) {
                    editUrl.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = 
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(editUrl.getWindowToken(), 0);
                    }
                }
            }
            boolean handled = edgeGestureListener.onTouch(v, event);
            if (!handled) {
                gestureDetector.onTouchEvent(event);
            }
            return handled;
        });

        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            if (result != null && result.getType() != WebView.HitTestResult.UNKNOWN_TYPE) {
                String extra = result.getExtra();
                if (extra != null && !extra.trim().isEmpty()) {
                    showContextMenuBottomSheet(result.getType(), extra);
                    return true;
                }
            }
            return false;
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                if (prefManager.isBlockPopups()) return false;
                try {
                    WebView newWebView = createConfiguredWebView(isIncognito, false);
                    WebTab tab = new WebTab(UUID.randomUUID().toString(), "Popup", "about:blank", newWebView, isIncognito);
                    tabList.add(tab);
                    switchToTab(tabList.size() - 1);

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    if (transport != null) {
                        transport.setWebView(newWebView);
                        resultMsg.sendToTarget();
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == getCurrentWebView()) {
                    if (progressBar != null) {
                        if (progressBar.getVisibility() != View.VISIBLE && newProgress < 100) {
                            progressBar.setAlpha(1f);
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                            android.animation.ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), newProgress)
                                    .setDuration(250)
                                    .start();
                        } else {
                            progressBar.setProgress(newProgress);
                        }

                        if (newProgress == 100) {
                            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                            progressBar.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                                progressBar.setVisibility(View.GONE);
                                progressBar.setProgress(0);
                            }).start();
                        }
                    } else {
                        if (newProgress == 100 && swipeRefresh != null) {
                            swipeRefresh.setRefreshing(false);
                        }
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (view == getCurrentWebView()) {
                    WebTab currentTab = getCurrentTab();
                    if (currentTab != null) currentTab.setTitle(title);
                    if (editUrl != null && !editUrl.hasFocus()) {
                        editUrl.setText(view.getUrl());
                    }
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, !isIncognito, false);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (request != null) request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                try {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File photoFile = null;
                    try {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        if (storageDir != null) {
                            photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
                            cameraPhotoPath = photoFile.getAbsolutePath();
                        }
                    } catch (IOException ex) {
                        cameraPhotoPath = null;
                    }

                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.naveen.browser.fileprovider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    }

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");

                    Intent[] intentArray = photoFile != null ? new Intent[]{takePictureIntent} : new Intent[0];
                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File or Camera");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                    startActivityForResult(chooserIntent, REQUEST_FILE_CHOOSER);
                    return true;
                } catch (Exception e) {
                    if (MainActivity.this.filePathCallback != null) {
                        MainActivity.this.filePathCallback.onReceiveValue(null);
                        MainActivity.this.filePathCallback = null;
                    }
                    Toast.makeText(MainActivity.this, "Cannot open file picker", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                if (customViewContainer != null) {
                    customViewContainer.addView(view);
                    customViewContainer.setVisibility(View.VISIBLE);
                }
                if (webViewContainer != null) webViewContainer.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                if (customViewContainer != null) {
                    customViewContainer.removeView(customView);
                    customViewContainer.setVisibility(View.GONE);
                }
                if (webViewContainer != null) webViewContainer.setVisibility(View.VISIBLE);
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customView = null;
                customViewCallback = null;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    if (request != null && request.getUrl() != null) {
                        String url = request.getUrl().toString();
                        if (prefManager.isAdBlockEnabled() || prefManager.isTrackerBlockEnabled()) {
                            if (AdBlocker.isAdOrTracker(url)) {
                                WebTab currentTab = getCurrentTab();
                                if (currentTab != null) currentTab.incrementBlockedCount();
                                return AdBlocker.createEmptyResource(url);
                            }
                        }
                        checkMediaResource(url, null);
                    }
                } catch (Throwable ignored) {}
                return super.shouldInterceptRequest(view, request);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                try {
                    if (url != null) {
                        if (prefManager.isAdBlockEnabled() || prefManager.isTrackerBlockEnabled()) {
                            if (AdBlocker.isAdOrTracker(url)) {
                                WebTab currentTab = getCurrentTab();
                                if (currentTab != null) currentTab.incrementBlockedCount();
                                return AdBlocker.createEmptyResource(url);
                            }
                        }
                        checkMediaResource(url, null);
                    }
                } catch (Throwable ignored) {}
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                String url = request.getUrl().toString();
                if (prefManager.isHttpsOnlyEnabled()) {
                    url = WebUtils.upgradeToHttps(url);
                }
                return WebUtils.handleSpecialIntents(MainActivity.this, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (prefManager.isHttpsOnlyEnabled()) {
                    url = WebUtils.upgradeToHttps(url);
                }
                return WebUtils.handleSpecialIntents(MainActivity.this, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view == getCurrentWebView()) {
                    detectedMediaList.clear();
                    mediaSnifferEngine.clearDetectedMedia();
                    if (btnFloatingDownloadBadge != null) btnFloatingDownloadBadge.setVisibility(View.GONE);
                    showTopHeaderBar();
                    checkHomeScreenVisibility(url);
                    if (editUrl != null) {
                        editUrl.clearFocus();
                        if (!editUrl.hasFocus()) {
                            editUrl.setText(url != null && url.equals("about:blank") ? "" : url);
                        }
                        android.view.inputmethod.InputMethodManager imm = 
                                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(editUrl.getWindowToken(), 0);
                        }
                    }
                    updateSslIcon(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == getCurrentWebView()) {
                    updateSslIcon(url);
                    if (url != null && !url.equalsIgnoreCase("about:blank")) {
                        scheduleHeaderAutoHide();
                        mediaSnifferEngine.inspectUrl(url);
                    }
                    WebTab currentTab = getCurrentTab();
                    if (currentTab != null && !currentTab.isIncognito() && url != null && !url.equals("about:blank")) {
                        final String pageTitle = view.getTitle();
                        final String pageUrl = url;
                        new Thread(() -> {
                            dbHelper.addHistory(new HistoryItem(pageTitle, pageUrl, System.currentTimeMillis()));
                            runOnUiThread(MainActivity.this::loadRecentVisitedData);
                        }).start();
                    }

                    if (prefManager.isNightMode() && url != null && !url.equals("about:blank")) {
                        view.evaluateJavascript(WebUtils.getNightModeScript(), null);
                    }
                    if (url != null && !url.equals("about:blank")) {
                        view.evaluateJavascript(WebUtils.getLongPressScript(), null);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request != null && request.isForMainFrame()) {
                    String failingUrl = request.getUrl() != null ? request.getUrl().toString() : "";
                    String description = error != null ? error.getDescription().toString() : "Unknown Error";
                    String errorTitle = "Page Load Error";
                    String errorMsg = "DeerOne Browser could not load this webpage (" + description + ").";
                    view.loadDataWithBaseURL(failingUrl, WebUtils.getErrorHtml(errorTitle, errorMsg, failingUrl), "text/html", "UTF-8", failingUrl);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String errorTitle = "Page Load Error";
                String errorMsg = "DeerOne Browser could not load this webpage (" + description + "). Please verify your network connection.";
                if (errorCode == ERROR_CONNECT || errorCode == ERROR_HOST_LOOKUP) {
                    errorTitle = "No Internet Connection";
                    errorMsg = "You appear to be offline or the web server could not be reached.";
                }
                view.loadDataWithBaseURL(failingUrl, WebUtils.getErrorHtml(errorTitle, errorMsg, failingUrl), "text/html", "UTF-8", failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (isFinishing() || isDestroyed()) {
                    if (handler != null) handler.cancel();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.ssl_error_title)
                        .setMessage(R.string.ssl_error_msg)
                        .setPositiveButton(R.string.proceed, (dialog, which) -> {
                            if (handler != null) handler.proceed();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            if (handler != null) handler.cancel();
                        })
                        .setOnCancelListener(dialog -> {
                            if (handler != null) handler.cancel();
                        })
                        .show();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            triggerDownload(url, userAgent, contentDisposition, mimetype);
        });

        return webView;
    }

    private void checkMediaResource(String url, String pageTitle) {
        if (url == null) return;
        String lower = url.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".m3u8") || lower.endsWith(".webm") ||
                lower.endsWith(".mp3") || lower.endsWith(".pdf") || lower.endsWith(".apk") || lower.endsWith(".zip")) {
            for (MediaItem item : detectedMediaList) {
                if (item.getUrl().equals(url)) return;
            }
            String title = pageTitle != null ? pageTitle : URLUtil.guessFileName(url, null, null);
            detectedMediaList.add(new MediaItem(url, title, getMimeFromUrl(lower)));
            runOnUiThread(() -> {
                if (btnFloatingDownloadBadge != null) {
                    if (txtMediaDetected != null) {
                        txtMediaDetected.setText("Download Media (" + detectedMediaList.size() + ")");
                    }
                    btnFloatingDownloadBadge.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private String getMimeFromUrl(String url) {
        if (url.endsWith(".mp4") || url.endsWith(".webm")) return "video/mp4";
        if (url.endsWith(".mp3")) return "audio/mpeg";
        if (url.endsWith(".pdf")) return "application/pdf";
        if (url.endsWith(".apk")) return "application/vnd.android.package-archive";
        return "*/*";
    }

    private void showMediaDownloadChooser() {
        if (isFinishing() || isDestroyed()) return;
        if (detectedMediaList.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundDark));
        layout.setPadding(24, 24, 24, 32);

        TextView header = new TextView(this);
        header.setText("Detected Media (" + detectedMediaList.size() + ")");
        header.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(16, 8, 16, 16);
        layout.addView(header);

        for (final MediaItem item : detectedMediaList) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 20, 16, 20);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_download);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            row.addView(icon, new LinearLayout.LayoutParams(48, 48));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(20, 0, 10, 0);

            TextView txtTitle = new TextView(this);
            txtTitle.setText(item.getTitle());
            txtTitle.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            txtTitle.setTextSize(14);
            txtTitle.setSingleLine(true);
            txtTitle.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(txtTitle);

            TextView txtUrl = new TextView(this);
            txtUrl.setText(item.getUrl());
            txtUrl.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            txtUrl.setTextSize(11);
            txtUrl.setSingleLine(true);
            txtUrl.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(txtUrl);

            row.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(v -> {
                triggerDownload(item.getUrl(), WebUtils.MOBILE_USER_AGENT, null, item.getMimeType());
                dialog.dismiss();
            });

            layout.addView(row);
        }

        dialog.setContentView(layout);
        dialog.show();
    }

    private void triggerDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype != null ? mimetype : "*/*");
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file via DeerOne...");
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            request.setTitle(fileName);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading " + fileName + "...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyUserAgent(WebSettings settings, boolean isDesktopForTab) {
        if (isDesktopForTab) {
            settings.setUserAgentString(WebUtils.DESKTOP_USER_AGENT);
            return;
        }
        int uaIndex = prefManager.getUserAgentIndex();
        switch (uaIndex) {
            case 1:
                settings.setUserAgentString(WebUtils.DESKTOP_USER_AGENT);
                break;
            case 2:
                settings.setUserAgentString(WebUtils.TABLET_USER_AGENT);
                break;
            case 3:
                String customUa = prefManager.getCustomUserAgent();
                if (!customUa.isEmpty()) settings.setUserAgentString(customUa);
                break;
            case 0:
            default:
                settings.setUserAgentString(WebUtils.MOBILE_USER_AGENT);
                break;
        }
    }

    private void saveSession() {
        StringBuilder sb = new StringBuilder();
        for (WebTab tab : tabList) {
            if (!tab.isIncognito() && tab.getUrl() != null) {
                sb.append(tab.getUrl()).append(";;;");
            }
        }
        prefManager.setSavedSessions(sb.toString());
    }

    private void restoreSession() {
        String saved = prefManager.getSavedSessions();
        String[] urls = saved.split(";;;");
        boolean loadedAny = false;
        for (String u : urls) {
            if (!u.trim().isEmpty()) {
                createNewTab(u.trim(), false);
                loadedAny = true;
            }
        }
        if (!loadedAny) {
            createNewTab(prefManager.getHomepage(), false);
        }
    }

    private void scheduleHeaderAutoHide() {
        headerAutoHideHandler.removeCallbacks(hideHeaderRunnable);
    }

    private void handleDownloadBadgeClick() {
        WebView currentWeb = getCurrentWebView();
        String currentUrl = currentWeb != null ? currentWeb.getUrl() : "";
        if (YtDlpExtractor.isYouTubeUrl(currentUrl)) {
            String pageTitle = currentWeb != null ? currentWeb.getTitle() : "YouTube Video";
            YtDlpExtractor.VideoDetails details = YtDlpExtractor.extractDetails(currentUrl, pageTitle);
            FormatPickerBottomSheetDialog picker = FormatPickerBottomSheetDialog.newInstance(details);
            picker.show(getSupportFragmentManager(), "FormatPicker");
        } else {
            List<MediaItem> mediaList = mediaSnifferEngine.getDetectedMediaList();
            if (!mediaList.isEmpty()) {
                MediaItem first = mediaList.get(0);
                enqueueDirectDownload(first.getUrl());
            } else if (currentUrl != null && !currentUrl.isEmpty()) {
                enqueueDirectDownload(currentUrl);
            }
        }
    }

    private void enqueueDirectDownload(String url) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle("DeerOne Download");
                request.setDescription(url);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                String fileName = "DeerOne_Media_" + System.currentTimeMillis() + ".mp4";
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "DeerOne/" + fileName);

                downloadManager.enqueue(request);
                Toast.makeText(this, "Download started ✓", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void switchToTab(int position) {
        if (position < 0 || position >= tabList.size()) return;

        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            WebView oldWeb = tabList.get(currentTabPosition).getWebView();
            if (webViewContainer != null && oldWeb != null) {
                webViewContainer.removeView(oldWeb);
            }
        }

        currentTabPosition = position;
        WebTab currentTab = tabList.get(position);
        WebView currentWeb = currentTab.getWebView();

        if (webViewContainer != null && currentWeb != null) {
            if (currentWeb.getParent() == null) {
                webViewContainer.addView(currentWeb);
            }
        }
        String currentUrl = currentWeb != null ? currentWeb.getUrl() : "";
        if (editUrl != null) {
            editUrl.setText(currentUrl == null || currentUrl.equals("about:blank") ? "" : currentUrl);
        }
        updateTabCount();
        updateSslIcon(currentUrl);
        checkHomeScreenVisibility(currentUrl);
    }

    private void closeTab(int position) {
        if (position < 0 || position >= tabList.size()) return;

        WebTab tab = tabList.get(position);
        WebView webView = tab.getWebView();
        if (webViewContainer != null && webView != null) {
            webViewContainer.removeView(webView);
            webView.destroy();
        }

        tabList.remove(position);

        if (tabList.isEmpty()) {
            createNewTab(prefManager.getHomepage(), false);
        } else {
            int nextPos = Math.max(0, position - 1);
            switchToTab(nextPos);
        }
    }

    private void updateTabCount() {
        if (txtTabCount != null) {
            txtTabCount.setText(String.valueOf(tabList.size()));
        }
    }

    private void updateSslIcon(String url) {
        // btnSslLock is hidden (visibility=gone, size=0x0) for the minimal design.
        // We keep this method to avoid NPE on callers but do nothing.
    }

    private synchronized WebView getCurrentWebView() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            return tabList.get(currentTabPosition).getWebView();
        }
        return null;
    }

    private synchronized WebTab getCurrentTab() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            return tabList.get(currentTabPosition);
        }
        return null;
    }

    private void loadEnteredUrl() {
        if (editUrl == null) return;
        String input = editUrl.getText().toString().trim();
        loadUrl(input);
    }

    private void loadUrl(String url) {
        String targetUrl = WebUtils.processUrlOrQuery(url, prefManager.getSearchEngineIndex());
        if (prefManager.isHttpsOnlyEnabled()) {
            targetUrl = WebUtils.upgradeToHttps(targetUrl);
        }

        if (homeScreenLayout != null) homeScreenLayout.setVisibility(View.GONE);
        if (topBarContainer != null) {
            topBarContainer.setVisibility(View.VISIBLE);
            topBarContainer.setTranslationY(0);
        }
        if (centerContainer != null) {
            centerContainer.setTranslationY(0);
        }
        if (bottomNavigationBar != null) {
            bottomNavigationBar.setVisibility(View.VISIBLE);
            bottomNavigationBar.setTranslationY(0);
        }
        isBarsHidden = false;
        isLayoutTransitioning = false;
        if (swipeRefresh != null) swipeRefresh.setVisibility(View.VISIBLE);

        if (editUrl != null) {
            editUrl.setText(targetUrl.equals("about:blank") ? "" : targetUrl);
        }

        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null) {
            if (prefManager.isDoNotTrack()) {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("DNT", "1");
                currentWeb.loadUrl(targetUrl, headers);
            } else {
                currentWeb.loadUrl(targetUrl);
            }
        }

        if (editUrl != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editUrl.getWindowToken(), 0);
            }
        }
    }

    private void showSearchInput() {
        if (homeScreenLayout != null) homeScreenLayout.setVisibility(View.GONE);
        if (topBarContainer != null) {
            topBarContainer.setVisibility(View.VISIBLE);
            topBarContainer.setTranslationY(0);
        }
        if (bottomNavigationBar != null) {
            bottomNavigationBar.setVisibility(View.VISIBLE);
            bottomNavigationBar.setTranslationY(0);
        }
        isBarsHidden = false;
        isLayoutTransitioning = false;
        if (swipeRefresh != null) swipeRefresh.setVisibility(View.VISIBLE);
        if (editUrl != null) {
            editUrl.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editUrl, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void checkHomeScreenVisibility(String url) {
        if (topBarContainer != null) {
            topBarContainer.setVisibility(View.VISIBLE);
            topBarContainer.setTranslationY(0);
        }
        if (centerContainer != null) {
            centerContainer.setTranslationY(0);
        }
        if (bottomNavigationBar != null) {
            bottomNavigationBar.setVisibility(View.VISIBLE);
            bottomNavigationBar.setTranslationY(0);
        }
        isBarsHidden = false;
        isLayoutTransitioning = false;
        if (url == null || url.trim().isEmpty() || url.equals("about:blank")) {
            if (homeScreenLayout != null) homeScreenLayout.setVisibility(View.VISIBLE);
            if (swipeRefresh != null) swipeRefresh.setVisibility(View.GONE);
            loadShortcutsData();
            loadRecentVisitedData();
        } else {
            if (homeScreenLayout != null) homeScreenLayout.setVisibility(View.GONE);
            if (swipeRefresh != null) swipeRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void showOverflowMenu(View v) {
        if (isFinishing() || isDestroyed()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_menu_overflow, null);
        bottomSheet.setContentView(dialogView);

        TextView txtMenuTitle = dialogView.findViewById(R.id.txt_menu_title);
        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null && currentWeb.getUrl() != null && !currentWeb.getUrl().equals("about:blank")) {
            if (txtMenuTitle != null) {
                txtMenuTitle.setText(currentWeb.getTitle() != null ? currentWeb.getTitle() : currentWeb.getUrl());
            }
        }

        // Setup overflow menu navigation controls
        View menuBack = dialogView.findViewById(R.id.menu_nav_back);
        if (menuBack != null) {
            final WebView web = currentWeb;
            menuBack.setEnabled(web != null && web.canGoBack());
            menuBack.setAlpha((web != null && web.canGoBack()) ? 1.0f : 0.4f);
            menuBack.setOnClickListener(view -> {
                bottomSheet.dismiss();
                if (web != null && web.canGoBack()) web.goBack();
            });
        }

        View menuForward = dialogView.findViewById(R.id.menu_nav_forward);
        if (menuForward != null) {
            final WebView web = currentWeb;
            menuForward.setEnabled(web != null && web.canGoForward());
            menuForward.setAlpha((web != null && web.canGoForward()) ? 1.0f : 0.4f);
            menuForward.setOnClickListener(view -> {
                bottomSheet.dismiss();
                if (web != null && web.canGoForward()) web.goForward();
            });
        }

        View menuHome = dialogView.findViewById(R.id.menu_nav_home);
        if (menuHome != null) {
            menuHome.setOnClickListener(view -> {
                bottomSheet.dismiss();
                WebView web = getCurrentWebView();
                if (web != null) {
                    String home = prefManager.getHomepage();
                    if (home == null || home.equals("https://www.google.com") || home.equals("about:blank")) {
                        web.loadUrl("about:blank");
                    } else {
                        web.loadUrl(home);
                    }
                }
            });
        }

        View menuRefresh = dialogView.findViewById(R.id.menu_nav_refresh);
        if (menuRefresh != null) {
            menuRefresh.setOnClickListener(view -> {
                bottomSheet.dismiss();
                WebView web = getCurrentWebView();
                if (web != null) web.reload();
            });
        }

        androidx.appcompat.widget.SwitchCompat switchDesktop = dialogView.findViewById(R.id.switch_desktop_site);
        WebTab currentTab = getCurrentTab();
        if (switchDesktop != null && currentTab != null) {
            switchDesktop.setChecked(currentTab.isDesktopMode());
        }

        androidx.appcompat.widget.SwitchCompat switchNight = dialogView.findViewById(R.id.switch_night_mode);
        if (switchNight != null) {
            switchNight.setChecked(prefManager.isNightMode());
        }

        View btnNewTab = dialogView.findViewById(R.id.menu_action_new_tab);
        if (btnNewTab != null) {
            btnNewTab.setOnClickListener(view -> {
                bottomSheet.dismiss();
                createNewTab(prefManager.getHomepage(), false);
            });
        }

        View btnIncognito = dialogView.findViewById(R.id.menu_action_incognito);
        if (btnIncognito != null) {
            btnIncognito.setOnClickListener(view -> {
                bottomSheet.dismiss();
                createNewTab(prefManager.getHomepage(), true);
                Toast.makeText(this, "Incognito Tab Opened", Toast.LENGTH_SHORT).show();
            });
        }

        View btnAddBookmark = dialogView.findViewById(R.id.menu_action_add_bookmark);
        if (btnAddBookmark != null) {
            btnAddBookmark.setOnClickListener(view -> {
                bottomSheet.dismiss();
                if (currentWeb != null && currentWeb.getUrl() != null) {
                    dbHelper.addBookmark(new BookmarkItem(currentWeb.getTitle(), currentWeb.getUrl(), System.currentTimeMillis()));
                    Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No web page to bookmark", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnShare = dialogView.findViewById(R.id.menu_action_share);
        if (btnShare != null) {
            btnShare.setOnClickListener(view -> {
                bottomSheet.dismiss();
                if (currentWeb != null && currentWeb.getUrl() != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, currentWeb.getUrl());
                    startActivity(Intent.createChooser(shareIntent, "Share Page"));
                }
            });
        }

        View btnBookmarks = dialogView.findViewById(R.id.menu_action_bookmarks);
        if (btnBookmarks != null) {
            btnBookmarks.setOnClickListener(view -> {
                bottomSheet.dismiss();
                startActivityForResult(new Intent(this, BookmarksActivity.class), REQUEST_BOOKMARKS);
            });
        }

        View btnHistory = dialogView.findViewById(R.id.menu_action_history);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(view -> {
                bottomSheet.dismiss();
                startActivityForResult(new Intent(this, HistoryActivity.class), REQUEST_HISTORY);
            });
        }

        View btnDownloads = dialogView.findViewById(R.id.menu_action_downloads);
        if (btnDownloads != null) {
            btnDownloads.setOnClickListener(view -> {
                bottomSheet.dismiss();
                startActivity(new Intent(this, DownloadsActivity.class));
            });
        }

        View btnDesktopSite = dialogView.findViewById(R.id.menu_action_desktop_site);
        if (btnDesktopSite != null) {
            btnDesktopSite.setOnClickListener(view -> {
                bottomSheet.dismiss();
                if (currentTab != null) {
                    boolean targetMode = !currentTab.isDesktopMode();
                    currentTab.setDesktopMode(targetMode);
                    if (currentTab.getWebView() != null) {
                        WebUtils.setDesktopMode(currentTab.getWebView(), targetMode);
                        currentTab.getWebView().reload();
                    }
                }
            });
        }

        View btnNightMode = dialogView.findViewById(R.id.menu_action_night_mode);
        if (btnNightMode != null) {
            btnNightMode.setOnClickListener(view -> {
                boolean targetNight = !prefManager.isNightMode();
                prefManager.setNightMode(targetNight);
                prefManager.applyTheme();
                bottomSheet.setOnDismissListener(d -> {
                    if (!isFinishing() && !isDestroyed()) recreate();
                });
                bottomSheet.dismiss();
            });
        }

        View btnSettings = dialogView.findViewById(R.id.menu_action_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(view -> {
                bottomSheet.dismiss();
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }

        bottomSheet.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        WebView currentWeb = getCurrentWebView();

        if (id == R.id.menu_new_tab) {
            createNewTab(prefManager.getHomepage(), false);
            return true;
        } else if (id == R.id.menu_incognito) {
            createNewTab(prefManager.getHomepage(), true);
            Toast.makeText(this, "Incognito Tab Opened (Zero Traces)", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_bookmarks) {
            startActivityForResult(new Intent(this, BookmarksActivity.class), REQUEST_BOOKMARKS);
            return true;
        } else if (id == R.id.menu_history) {
            startActivityForResult(new Intent(this, HistoryActivity.class), REQUEST_HISTORY);
            return true;
        } else if (id == R.id.menu_add_bookmark) {
            if (currentWeb != null) {
                String title = currentWeb.getTitle();
                String url = currentWeb.getUrl();
                if (url != null) {
                    dbHelper.addBookmark(new BookmarkItem(title, url, System.currentTimeMillis()));
                    Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else if (id == R.id.menu_qr_scanner) {
            showQrScannerDialog();
            return true;
        } else if (id == R.id.menu_translate) {
            if (currentWeb != null && currentWeb.getUrl() != null) {
                currentWeb.loadUrl(WebUtils.getTranslateUrl(currentWeb.getUrl()));
            }
            return true;
        } else if (id == R.id.menu_add_to_home) {
            if (currentWeb != null && currentWeb.getUrl() != null) {
                WebUtils.createPwaShortcut(this, currentWeb.getTitle(), currentWeb.getUrl(), currentWeb.getFavicon());
                Toast.makeText(this, "Shortcut / PWA added to home screen", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.menu_ad_blocker) {
            boolean nextState = !prefManager.isAdBlockEnabled();
            prefManager.setAdBlockEnabled(nextState);
            prefManager.setTrackerBlockEnabled(nextState);
            if (currentWeb != null) currentWeb.reload();
            Toast.makeText(this, nextState ? "Ad & Tracker Blocker Enabled" : "Ad Blocker Disabled", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_desktop_site) {
            WebTab currentTab = getCurrentTab();
            if (currentTab != null) {
                boolean nextState = !currentTab.isDesktopMode();
                currentTab.setDesktopMode(nextState);
                if (currentWeb != null) {
                    applyUserAgent(currentWeb.getSettings(), nextState);
                    currentWeb.reload();
                }
            }
            return true;
        } else if (id == R.id.menu_night_mode) {
            boolean nextState = !prefManager.isNightMode();
            prefManager.setNightMode(nextState);
            if (currentWeb != null) {
                currentWeb.reload();
            }
            return true;
        } else if (id == R.id.menu_find_in_page) {
            showFindInPageDialog();
            return true;
        } else if (id == R.id.menu_reader_mode) {
            if (currentWeb != null) {
                currentWeb.evaluateJavascript(WebUtils.getReaderModeScript(prefManager.isNightMode()), null);
            }
            return true;
        } else if (id == R.id.menu_share) {
            if (currentWeb != null && currentWeb.getUrl() != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, currentWeb.getUrl());
                startActivity(Intent.createChooser(shareIntent, "Share URL"));
            }
            return true;
        } else if (id == R.id.menu_copy_url) {
            if (currentWeb != null && currentWeb.getUrl() != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", currentWeb.getUrl());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else if (id == R.id.menu_open_external) {
            if (currentWeb != null && currentWeb.getUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentWeb.getUrl()));
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return false;
    }

    private void showQrScannerDialog() {
        if (isFinishing() || isDestroyed()) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_scanner, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        EditText editQrResult = dialogView.findViewById(R.id.edit_qr_result);
        Button btnCancel = dialogView.findViewById(R.id.btn_qr_cancel);
        Button btnOpen = dialogView.findViewById(R.id.btn_qr_open);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOpen.setOnClickListener(v -> {
            String target = editQrResult.getText().toString().trim();
            if (!target.isEmpty()) {
                WebView currentWeb = getCurrentWebView();
                if (currentWeb != null) {
                    currentWeb.loadUrl(WebUtils.processUrlOrQuery(target, prefManager.getSearchEngineIndex()));
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showFindInPageDialog() {
        if (isFinishing() || isDestroyed()) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_find_in_page, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        EditText editQuery = dialogView.findViewById(R.id.edit_find_query);
        TextView txtCount = dialogView.findViewById(R.id.txt_find_count);
        ImageButton btnPrev = dialogView.findViewById(R.id.btn_find_prev);
        ImageButton btnNext = dialogView.findViewById(R.id.btn_find_next);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_find_close);

        WebView currentWeb = getCurrentWebView();

        if (currentWeb != null) {
            currentWeb.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                if (numberOfMatches > 0) {
                    txtCount.setVisibility(View.VISIBLE);
                    txtCount.setText((activeMatchOrdinal + 1) + "/" + numberOfMatches);
                } else {
                    txtCount.setVisibility(View.GONE);
                }
            });
        }

        editQuery.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentWeb != null) currentWeb.findAllAsync(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        editQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (currentWeb != null) currentWeb.findAllAsync(editQuery.getText().toString());
            return true;
        });

        btnPrev.setOnClickListener(v -> { if (currentWeb != null) currentWeb.findNext(false); });
        btnNext.setOnClickListener(v -> { if (currentWeb != null) currentWeb.findNext(true); });
        btnClose.setOnClickListener(v -> {
            if (currentWeb != null) {
                currentWeb.clearMatches();
                currentWeb.setFindListener(null);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showTabsManagerDialog() {
        if (isFinishing() || isDestroyed()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_tabs, null);
        dialog.setContentView(view);

        RecyclerView recyclerTabs = view.findViewById(R.id.recycler_tabs);
        ImageButton btnAdd = view.findViewById(R.id.btn_dialog_add_tab);
        ImageButton btnClose = view.findViewById(R.id.btn_dialog_close_tabs);
        View btnCloseAll = view.findViewById(R.id.btn_close_all_tabs);
        View btnIncognito = view.findViewById(R.id.btn_new_incognito_tab);

        recyclerTabs.setLayoutManager(new LinearLayoutManager(this));
        TabsAdapter tabsAdapter = new TabsAdapter(tabList, new TabsAdapter.OnTabClickListener() {
            @Override
            public void onTabSelect(int position) {
                switchToTab(position);
                dialog.dismiss();
            }

            @Override
            public void onTabClose(int position) {
                closeTab(position);
                if (tabList.isEmpty()) {
                    dialog.dismiss();
                } else {
                    dialog.dismiss();
                    showTabsManagerDialog();
                }
            }
        });
        recyclerTabs.setAdapter(tabsAdapter);

        btnAdd.setOnClickListener(v -> {
            createNewTab(prefManager.getHomepage(), false);
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (btnCloseAll != null) {
            btnCloseAll.setOnClickListener(v -> {
                for (WebTab tab : new ArrayList<>(tabList)) {
                    if (tab.getWebView() != null) tab.getWebView().destroy();
                }
                tabList.clear();
                createNewTab(prefManager.getHomepage(), false);
                dialog.dismiss();
            });
        }

        if (btnIncognito != null) {
            btnIncognito.setOnClickListener(v -> {
                createNewTab(prefManager.getHomepage(), true);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null) {
            currentWeb.onResume();
            currentWeb.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null) {
            currentWeb.onPause();
            currentWeb.pauseTimers();
        }
        if (prefManager.isSessionRestoreEnabled()) {
            saveSession();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab_count", tabList.size());
        outState.putInt("current_tab_position", currentTabPosition);
        for (int i = 0; i < tabList.size(); i++) {
            WebTab tab = tabList.get(i);
            Bundle tabState = new Bundle();
            tabState.putString("tab_id", tab.getId());
            tabState.putString("tab_title", tab.getTitle());
            tabState.putBoolean("tab_incognito", tab.isIncognito());
            if (tab.getWebView() != null) {
                Bundle webState = new Bundle();
                tab.getWebView().saveState(webState);
                tabState.putBundle("web_state", webState);
            }
            outState.putBundle("tab_state_" + i, tabState);
        }
    }

    @Override
    protected void onDestroy() {
        if (headerAutoHideHandler != null && hideHeaderRunnable != null) {
            headerAutoHideHandler.removeCallbacks(hideHeaderRunnable);
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webViewContainer != null) {
            webViewContainer.removeAllViews();
        }
        for (WebTab tab : tabList) {
            if (tab != null && tab.getWebView() != null) {
                try {
                    WebView web = tab.getWebView();
                    web.setWebChromeClient(null);
                    web.setWebViewClient(null);
                    web.stopLoading();
                    web.loadUrl("about:blank");
                    web.destroy();
                } catch (Exception ignored) {}
            }
        }
        tabList.clear();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_CHOOSER) {
            if (filePathCallback == null) return;
            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                if (data == null || data.getData() == null) {
                    if (cameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(cameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else if ((requestCode == REQUEST_BOOKMARKS || requestCode == REQUEST_HISTORY) && resultCode == RESULT_OK && data != null) {
            String urlsString = data.getStringExtra("urls");
            if (urlsString != null && !urlsString.isEmpty()) {
                String[] list = urlsString.split(";;;");
                for (String u : list) {
                    if (!u.trim().isEmpty()) {
                        createNewTab(u.trim(), false);
                    }
                }
            } else {
                String selectedUrl = data.getStringExtra("url");
                if (selectedUrl != null && getCurrentWebView() != null) {
                    loadUrl(selectedUrl);
                }
            }
        } else if (requestCode == 110 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                loadUrl(matches.get(0));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            if (customViewContainer != null && customView != null) {
                customViewContainer.removeView(customView);
                customViewContainer.setVisibility(View.GONE);
            }
            if (webViewContainer != null) {
                webViewContainer.setVisibility(View.VISIBLE);
            }
            customView = null;
            customViewCallback = null;
            return;
        }

        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null && currentWeb.canGoBack()) {
            currentWeb.goBack();
        } else if (tabList.size() > 1) {
            closeTab(currentTabPosition);
        } else {
            super.onBackPressed();
        }
    }

    private void showPrivacyDashboard() {
        if (isFinishing() || isDestroyed()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_privacy_dashboard, null);
        dialog.setContentView(view);

        TextView txtHostname = view.findViewById(R.id.txt_dashboard_hostname);
        ImageView imgSecurityStatus = view.findViewById(R.id.img_security_status);
        TextView txtSecurityTitle = view.findViewById(R.id.txt_security_title);
        TextView txtSecurityDesc = view.findViewById(R.id.txt_security_desc);
        TextView txtBlockedCurrent = view.findViewById(R.id.txt_blocked_current);
        TextView txtBlockedLifetime = view.findViewById(R.id.txt_blocked_lifetime);
        LinearLayout layoutCertInfo = view.findViewById(R.id.layout_certificate_info);
        TextView txtCertSubject = view.findViewById(R.id.txt_cert_subject);
        TextView txtCertIssuer = view.findViewById(R.id.txt_cert_issuer);
        TextView txtCertValidity = view.findViewById(R.id.txt_cert_validity);
        ImageView btnClose = view.findViewById(R.id.btn_close_dashboard);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        WebView currentWeb = getCurrentWebView();
        WebTab currentTab = getCurrentTab();
        String url = currentWeb != null ? currentWeb.getUrl() : "";

        String hostname = "DeerOne Home";
        if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
            try {
                Uri uri = Uri.parse(url);
                hostname = uri.getHost();
                if (hostname == null) hostname = url;
            } catch (Exception e) {
                hostname = url;
            }
        }
        if (txtHostname != null) txtHostname.setText(hostname);

        int currentBlocked = currentTab != null ? currentTab.getBlockedCount() : 0;
        int lifetimeBlocked = prefManager.getLifetimeBlockedAds();
        if (txtBlockedCurrent != null) txtBlockedCurrent.setText(String.valueOf(currentBlocked));
        if (txtBlockedLifetime != null) txtBlockedLifetime.setText(String.valueOf(lifetimeBlocked));

        if (url != null && url.startsWith("https://")) {
            if (imgSecurityStatus != null) {
                imgSecurityStatus.setImageResource(R.drawable.ic_lock);
                imgSecurityStatus.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            }
            if (txtSecurityTitle != null) txtSecurityTitle.setText("Connection is Secure");
            if (txtSecurityDesc != null) txtSecurityDesc.setText("Your traffic and credentials are fully encrypted.");

            if (layoutCertInfo != null) layoutCertInfo.setVisibility(View.VISIBLE);
            if (currentWeb != null && currentWeb.getCertificate() != null) {
                android.net.http.SslCertificate cert = currentWeb.getCertificate();
                if (txtCertSubject != null) txtCertSubject.setText("Issued To: " + cert.getIssuedTo().getDName());
                if (txtCertIssuer != null) txtCertIssuer.setText("Issued By: " + cert.getIssuedBy().getDName());
                try {
                    java.util.Date expiryDate = cert.getValidNotAfterDate();
                    if (expiryDate != null && txtCertValidity != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        txtCertValidity.setText("Valid Until: " + sdf.format(expiryDate));
                    }
                } catch (Exception e) {
                    if (txtCertValidity != null) txtCertValidity.setText("Valid Until: N/A");
                }
            }
        } else {
            if (imgSecurityStatus != null) {
                imgSecurityStatus.setImageResource(R.drawable.ic_lock);
                imgSecurityStatus.setColorFilter(ContextCompat.getColor(this, R.color.textSecondary));
            }
            if (txtSecurityTitle != null) txtSecurityTitle.setText("DeerOne Clean Space");
            if (txtSecurityDesc != null) txtSecurityDesc.setText("You are viewing a local browser page.");
            if (layoutCertInfo != null) layoutCertInfo.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void showContextMenuBottomSheet(int type, final String extra) {
        if (extra == null || extra.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundDark));
        layout.setPadding(16, 16, 16, 24);

        TextView previewHeader = new TextView(this);
        previewHeader.setPadding(32, 16, 32, 16);
        previewHeader.setText(extra);
        previewHeader.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        previewHeader.setTextSize(13);
        previewHeader.setEllipsize(TextUtils.TruncateAt.END);
        previewHeader.setSingleLine(true);
        layout.addView(previewHeader);

        boolean isImage = (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
        boolean isVideoOrMedia = (type == 99 || extra.contains(".mp4") || extra.contains(".webm") || extra.contains(".mkv")
                || extra.contains(".mov") || extra.contains(".avi") || extra.contains(".mp3") || extra.contains(".m4a")
                || extra.contains("video") || extra.contains("/video"));

        if (isImage) {
            addContextOption(layout, "Download Image", v -> {
                dialog.dismiss();
                startDownload(extra, null, null, "image/*");
            });
            addContextOption(layout, "Open Image in New Tab", v -> {
                dialog.dismiss();
                createNewTab(extra, false);
            });
            addContextOption(layout, "Copy Image Address", v -> {
                dialog.dismiss();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", extra);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
            });
            addContextOption(layout, "Share Image", v -> {
                dialog.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, extra);
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            });
        } else if (isVideoOrMedia) {
            addContextOption(layout, "Download Video / Media", v -> {
                dialog.dismiss();
                startDownload(extra, null, null, "video/*");
            });
            addContextOption(layout, "Open Media in New Tab", v -> {
                dialog.dismiss();
                createNewTab(extra, false);
            });
            addContextOption(layout, "Copy Media Link", v -> {
                dialog.dismiss();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Media URL", extra);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
            });
            addContextOption(layout, "Share Media Link", v -> {
                dialog.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, extra);
                startActivity(Intent.createChooser(shareIntent, "Share Media"));
            });
        } else {
            addContextOption(layout, "Open in New Tab", v -> {
                dialog.dismiss();
                createNewTab(extra, false);
            });
            addContextOption(layout, "Download Link Target", v -> {
                dialog.dismiss();
                startDownload(extra, null, null, null);
            });
            addContextOption(layout, "Copy Link Address", v -> {
                dialog.dismiss();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", extra);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
            });
            addContextOption(layout, "Share Link", v -> {
                dialog.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, extra);
                startActivity(Intent.createChooser(shareIntent, "Share Link"));
            });
        }

        dialog.setContentView(layout);
        dialog.show();
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            if (url == null || url.trim().isEmpty()) return;
            String filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));

            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);

            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename);
            request.setTitle(filename);
            request.setDescription("Downloading file...");

            android.app.DownloadManager dm = (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Downloading " + filename, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addContextOption(LinearLayout parent, String title, View.OnClickListener listener) {
        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        textView.setTextSize(16);
        textView.setPadding(40, 36, 40, 36);
        textView.setBackgroundResource(android.R.drawable.list_selector_background);
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setOnClickListener(listener);
        parent.addView(textView);
    }

    private void hideTopHeaderBar() {
        WebView currentWeb = getCurrentWebView();
        if (currentWeb == null || currentWeb.getUrl() == null || currentWeb.getUrl().equals("about:blank")) {
            return;
        }
        if (topBarContainer != null && !isBarsHidden) {
            isBarsHidden = true;
            isLayoutTransitioning = true;
            int height = topBarContainer.getHeight() > 0 ? topBarContainer.getHeight() : (int) (56 * getResources().getDisplayMetrics().density);
            topBarContainer.animate().translationY(-height).setDuration(220)
                    .withEndAction(() -> {
                        topBarContainer.postDelayed(() -> isLayoutTransitioning = false, 150);
                    }).start();
            if (centerContainer != null) {
                centerContainer.animate().translationY(-height).setDuration(220).start();
            }
        }
    }

    private void showTopHeaderBar() {
        if (topBarContainer != null && isBarsHidden) {
            isBarsHidden = false;
            isLayoutTransitioning = true;
            topBarContainer.animate().translationY(0).setDuration(220)
                    .withEndAction(() -> {
                        topBarContainer.postDelayed(() -> isLayoutTransitioning = false, 150);
                    }).start();
            if (centerContainer != null) {
                centerContainer.animate().translationY(0).setDuration(220).start();
            }
        }
    }

    private void hideSystemBars() {
        hideTopHeaderBar();
    }

    private void showSystemBars() {
        showTopHeaderBar();
    }
}
