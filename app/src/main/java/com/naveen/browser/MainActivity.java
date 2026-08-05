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
import android.text.TextUtils;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naveen.browser.adapter.TabsAdapter;
import com.naveen.browser.db.DatabaseHelper;
import com.naveen.browser.model.BookmarkItem;
import com.naveen.browser.model.HistoryItem;
import com.naveen.browser.model.WebTab;
import com.naveen.browser.utils.AdBlocker;
import com.naveen.browser.utils.PreferenceManager;
import com.naveen.browser.utils.WebUtils;

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
    private View bottomBarContainer;
    private boolean isBarsHidden = false;

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
        prefManager = new PreferenceManager(this);
        prefManager.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);

        initViews();
        requestPermissionsIfNecessary();

        Intent intent = getIntent();
        String initialUrl = prefManager.getHomepage();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            initialUrl = intent.getData().toString();
        }

        // Restore Session or Create Initial Tab
        if (prefManager.isSessionRestoreEnabled() && !prefManager.getSavedSessions().isEmpty()) {
            restoreSession();
        } else {
            createNewTab(initialUrl, false);
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
        btnSslLock.setOnClickListener(v -> showPrivacyDashboard());
        txtTabCount = findViewById(R.id.txt_tab_count);
        homeScreenLayout = findViewById(R.id.home_screen_layout);
        topBarContainer = findViewById(R.id.top_bar_container);
        bottomBarContainer = findViewById(R.id.bottom_bar_container);

        ImageButton btnGo = findViewById(R.id.btn_go);
        ImageButton btnOverflow = findViewById(R.id.btn_overflow);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);
        ImageButton btnHome = findViewById(R.id.btn_home);
        FrameLayout btnTabsLayout = findViewById(R.id.btn_tabs_layout);

        // Hook Native Home Screen Actions
        View cardHomeSearch = homeScreenLayout.findViewById(R.id.card_home_search);
        cardHomeSearch.setOnClickListener(v -> showSearchInput());

        ImageView btnHomeVoice = homeScreenLayout.findViewById(R.id.btn_home_voice);
        btnHomeVoice.setOnClickListener(v -> {
            try {
                Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search the web...");
                startActivityForResult(voiceIntent, 110);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Voice search not available", Toast.LENGTH_SHORT).show();
            }
        });

        View shortcutBookmarks = homeScreenLayout.findViewById(R.id.shortcut_bookmarks);
        View shortcutHistory = homeScreenLayout.findViewById(R.id.shortcut_history);
        View shortcutDownloads = homeScreenLayout.findViewById(R.id.shortcut_downloads);
        View shortcutSettings = homeScreenLayout.findViewById(R.id.shortcut_settings);

        shortcutBookmarks.setOnClickListener(v -> startActivityForResult(new Intent(MainActivity.this, BookmarksActivity.class), REQUEST_BOOKMARKS));
        shortcutHistory.setOnClickListener(v -> startActivityForResult(new Intent(MainActivity.this, HistoryActivity.class), REQUEST_HISTORY));
        shortcutDownloads.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DownloadsActivity.class)));
        shortcutSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        View linkGoogle = homeScreenLayout.findViewById(R.id.link_google);
        View linkYoutube = homeScreenLayout.findViewById(R.id.link_youtube);
        View linkFacebook = homeScreenLayout.findViewById(R.id.link_facebook);
        View linkWikipedia = homeScreenLayout.findViewById(R.id.link_wikipedia);

        linkGoogle.setOnClickListener(v -> loadUrl("https://www.google.com"));
        linkYoutube.setOnClickListener(v -> loadUrl("https://www.youtube.com"));
        linkFacebook.setOnClickListener(v -> loadUrl("https://www.facebook.com"));
        linkWikipedia.setOnClickListener(v -> loadUrl("https://www.wikipedia.org"));

        swipeRefresh.setOnRefreshListener(() -> {
            WebView currentWeb = getCurrentWebView();
            if (currentWeb != null) {
                currentWeb.reload();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        editUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadEnteredUrl();
                return true;
            }
            return false;
        });

        editUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnClearUrl.setVisibility(View.VISIBLE);
            } else {
                btnClearUrl.setVisibility(View.GONE);
            }
        });

        btnClearUrl.setOnClickListener(v -> editUrl.setText(""));

        btnGo.setOnClickListener(v -> loadEnteredUrl());

        btnOverflow.setOnClickListener(this::showOverflowMenu);

        btnBack.setOnClickListener(v -> {
            WebView web = getCurrentWebView();
            if (web != null && web.canGoBack()) {
                web.goBack();
            }
        });

        btnForward.setOnClickListener(v -> {
            WebView web = getCurrentWebView();
            if (web != null && web.canGoForward()) {
                web.goForward();
            }
        });

        btnRefresh.setOnClickListener(v -> {
            WebView web = getCurrentWebView();
            if (web != null) {
                web.reload();
            }
        });

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

        btnTabsLayout.setOnClickListener(v -> showTabsManagerDialog());
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
        String finalUrl = prefManager.isHttpsOnlyEnabled() ? WebUtils.upgradeToHttps(url) : url;
        WebView webView = createConfiguredWebView(isIncognito, false);
        WebTab tab = new WebTab(UUID.randomUUID().toString(), "New Tab", finalUrl, webView, isIncognito);
        tabList.add(tab);
        switchToTab(tabList.size() - 1);
        webView.loadUrl(WebUtils.processUrlOrQuery(finalUrl, prefManager.getSearchEngineIndex()));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createConfiguredWebView(boolean isIncognito, boolean isDesktop) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(!isIncognito);
        settings.setDatabaseEnabled(!isIncognito);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setGeolocationEnabled(!isIncognito);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        if (prefManager.isAutofillEnabled()) {
            settings.setSavePassword(true);
        }

        applyUserAgent(settings, isDesktop);

        CookieManager cookieManager = CookieManager.getInstance();
        if (isIncognito) {
            settings.setSaveFormData(false);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            cookieManager.setAcceptCookie(false);
        } else {
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollY > oldScrollY + 10) {
                        hideSystemBars();
                    } else if (scrollY < oldScrollY - 10) {
                        showSystemBars();
                    }
                }
            });
        }

        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                if (isBarsHidden) {
                    showSystemBars();
                } else {
                    hideSystemBars();
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(android.view.MotionEvent e) {
                if (isBarsHidden) {
                    showSystemBars();
                } else {
                    hideSystemBars();
                }
                return false;
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = webView.getHitTestResult();
                int type = result.getType();
                if (type != WebView.HitTestResult.UNKNOWN_TYPE) {
                    showContextMenuBottomSheet(result);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == getCurrentWebView()) {
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (view == getCurrentWebView()) {
                    WebTab currentTab = getCurrentTab();
                    if (currentTab != null) currentTab.setTitle(title);
                    if (!editUrl.hasFocus()) {
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
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
                    cameraPhotoPath = photoFile.getAbsolutePath();
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

                Intent[] intentArray = takePictureIntent != null ? new Intent[]{takePictureIntent} : new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File or Camera");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, REQUEST_FILE_CHOOSER);
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                customViewContainer.addView(view);
                customViewContainer.setVisibility(View.VISIBLE);
                webViewContainer.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                customViewContainer.removeView(customView);
                customViewContainer.setVisibility(View.GONE);
                webViewContainer.setVisibility(View.VISIBLE);
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customView = null;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (prefManager.isAdBlockEnabled() || prefManager.isTrackerBlockEnabled()) {
                    String url = request.getUrl().toString();
                    if (AdBlocker.isAdOrTracker(url)) {
                        WebTab currentTab = getCurrentTab();
                        if (currentTab != null) {
                            currentTab.incrementBlockedCount();
                        }
                        prefManager.incrementLifetimeBlockedAds(1);
                        return AdBlocker.createEmptyResource();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (prefManager.isHttpsOnlyEnabled()) {
                    url = WebUtils.upgradeToHttps(url);
                }
                if (WebUtils.handleSpecialIntents(MainActivity.this, url)) {
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view == getCurrentWebView()) {
                    checkHomeScreenVisibility(url);
                    if (!editUrl.hasFocus()) {
                        if (url != null && url.equals("about:blank")) {
                            editUrl.setText("");
                        } else {
                            editUrl.setText(url);
                        }
                    }
                    updateSslIcon(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == getCurrentWebView()) {
                    updateSslIcon(url);
                    WebTab currentTab = getCurrentTab();
                    if (currentTab != null && !currentTab.isIncognito() && url != null && !url.equals("about:blank")) {
                        dbHelper.addHistory(new HistoryItem(view.getTitle(), url, System.currentTimeMillis()));
                    }

                    if (prefManager.isNightMode() && url != null && !url.equals("about:blank")) {
                        view.evaluateJavascript(WebUtils.getNightModeScript(), null);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.ssl_error_title)
                        .setMessage(R.string.ssl_error_msg)
                        .setPositiveButton(R.string.proceed, (dialog, which) -> handler.proceed())
                        .setNegativeButton(R.string.cancel, (dialog, which) -> handler.cancel())
                        .show();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), R.string.download_started, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return webView;
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
                if (!customUa.isEmpty()) {
                    settings.setUserAgentString(customUa);
                }
                break;
            case 0:
            default:
                settings.setUserAgentString(null);
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

    private void switchToTab(int position) {
        if (position < 0 || position >= tabList.size()) return;

        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            WebView oldWeb = tabList.get(currentTabPosition).getWebView();
            webViewContainer.removeView(oldWeb);
        }

        currentTabPosition = position;
        WebTab currentTab = tabList.get(position);
        WebView currentWeb = currentTab.getWebView();

        webViewContainer.addView(currentWeb);
        String currentUrl = currentWeb.getUrl();
        if (currentUrl == null || currentUrl.equals("about:blank")) {
            editUrl.setText("");
        } else {
            editUrl.setText(currentUrl);
        }
        updateTabCount();
        updateSslIcon(currentUrl);
        checkHomeScreenVisibility(currentUrl);
    }

    private void closeTab(int position) {
        if (position < 0 || position >= tabList.size()) return;

        WebTab tab = tabList.get(position);
        WebView webView = tab.getWebView();
        webViewContainer.removeView(webView);
        webView.destroy();

        tabList.remove(position);

        if (tabList.isEmpty()) {
            createNewTab(prefManager.getHomepage(), false);
        } else {
            int nextPos = Math.max(0, position - 1);
            switchToTab(nextPos);
        }
    }

    private void updateTabCount() {
        txtTabCount.setText(String.valueOf(tabList.size()));
    }

    private void updateSslIcon(String url) {
        if (url != null && url.startsWith("https://")) {
            btnSslLock.setImageResource(R.drawable.ic_lock);
        } else {
            btnSslLock.setImageResource(R.drawable.ic_warning);
        }
    }

    private WebView getCurrentWebView() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            return tabList.get(currentTabPosition).getWebView();
        }
        return null;
    }

    private WebTab getCurrentTab() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            return tabList.get(currentTabPosition);
        }
        return null;
    }

    private void loadEnteredUrl() {
        String input = editUrl.getText().toString().trim();
        loadUrl(input);
    }

    private void loadUrl(String url) {
        String targetUrl = WebUtils.processUrlOrQuery(url, prefManager.getSearchEngineIndex());
        if (prefManager.isHttpsOnlyEnabled()) {
            targetUrl = WebUtils.upgradeToHttps(targetUrl);
        }

        homeScreenLayout.setVisibility(View.GONE);
        topBarContainer.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.VISIBLE);
        
        // Don't show about:blank in the editUrl text box
        if (targetUrl.equals("about:blank")) {
            editUrl.setText("");
        } else {
            editUrl.setText(targetUrl);
        }

        WebView currentWeb = getCurrentWebView();
        if (currentWeb != null) {
            currentWeb.loadUrl(targetUrl);
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editUrl.getWindowToken(), 0);
        }
    }

    private void showSearchInput() {
        homeScreenLayout.setVisibility(View.GONE);
        topBarContainer.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.VISIBLE);
        editUrl.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editUrl, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void checkHomeScreenVisibility(String url) {
        String homepage = prefManager.getHomepage();
        boolean isDefaultHome = homepage == null || homepage.equals("https://www.google.com") || homepage.equals("about:blank");
        
        if (url == null || url.trim().isEmpty() || url.equals("about:blank") || (isDefaultHome && url.equals(homepage))) {
            homeScreenLayout.setVisibility(View.VISIBLE);
            topBarContainer.setVisibility(View.GONE);
            swipeRefresh.setVisibility(View.GONE);
        } else {
            homeScreenLayout.setVisibility(View.GONE);
            topBarContainer.setVisibility(View.VISIBLE);
            swipeRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void showOverflowMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.main_overflow_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);

        MenuItem desktopItem = popup.getMenu().findItem(R.id.menu_desktop_site);
        if (desktopItem != null) {
            WebTab currentTab = getCurrentTab();
            desktopItem.setChecked(currentTab != null && currentTab.isDesktopMode());
        }

        MenuItem nightItem = popup.getMenu().findItem(R.id.menu_night_mode);
        if (nightItem != null) {
            nightItem.setChecked(prefManager.isNightMode());
        }

        MenuItem adBlockItem = popup.getMenu().findItem(R.id.menu_ad_blocker);
        if (adBlockItem != null) {
            adBlockItem.setChecked(prefManager.isAdBlockEnabled());
        }

        popup.show();
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
            currentWeb.setFindListener(new WebView.FindListener() {
                @Override
                public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                    if (numberOfMatches > 0) {
                        txtCount.setVisibility(View.VISIBLE);
                        txtCount.setText((activeMatchOrdinal + 1) + "/" + numberOfMatches);
                    } else {
                        txtCount.setVisibility(View.GONE);
                    }
                }
            });
        }

        editQuery.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentWeb != null) {
                    currentWeb.findAllAsync(s.toString());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        editQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (currentWeb != null) {
                currentWeb.findAllAsync(editQuery.getText().toString());
            }
            return true;
        });

        btnPrev.setOnClickListener(v -> {
            if (currentWeb != null) currentWeb.findNext(false);
        });

        btnNext.setOnClickListener(v -> {
            if (currentWeb != null) currentWeb.findNext(true);
        });

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
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_tabs, null);
        dialog.setContentView(view);

        RecyclerView recyclerTabs = view.findViewById(R.id.recycler_tabs);
        ImageButton btnAdd = view.findViewById(R.id.btn_dialog_add_tab);
        ImageButton btnClose = view.findViewById(R.id.btn_dialog_close_tabs);

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

        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (prefManager.isSessionRestoreEnabled()) {
            saveSession();
        }
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
                String query = matches.get(0);
                loadUrl(query);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            WebChromeClient chromeClient = new WebChromeClient();
            chromeClient.onHideCustomView();
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

        btnClose.setOnClickListener(v -> dialog.dismiss());

        WebView currentWeb = getCurrentWebView();
        WebTab currentTab = getCurrentTab();
        String url = currentWeb != null ? currentWeb.getUrl() : "";

        String hostname = "Home Screen";
        if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
            try {
                Uri uri = Uri.parse(url);
                hostname = uri.getHost();
                if (hostname == null) hostname = url;
            } catch (Exception e) {
                hostname = url;
            }
        }
        txtHostname.setText(hostname);

        int currentBlocked = currentTab != null ? currentTab.getBlockedCount() : 0;
        int lifetimeBlocked = prefManager.getLifetimeBlockedAds();
        txtBlockedCurrent.setText(String.valueOf(currentBlocked));
        txtBlockedLifetime.setText(String.valueOf(lifetimeBlocked));

        if (url != null && url.startsWith("https://")) {
            imgSecurityStatus.setImageResource(R.drawable.ic_lock);
            imgSecurityStatus.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            txtSecurityTitle.setText("Connection is Secure");
            txtSecurityDesc.setText("Your traffic and credentials are fully encrypted.");

            layoutCertInfo.setVisibility(View.VISIBLE);
            if (currentWeb != null && currentWeb.getCertificate() != null) {
                android.net.http.SslCertificate cert = currentWeb.getCertificate();
                txtCertSubject.setText("Issued To: " + cert.getIssuedTo().getDName());
                txtCertIssuer.setText("Issued By: " + cert.getIssuedBy().getDName());
                try {
                    java.util.Date expiryDate = cert.getValidNotAfterDate();
                    if (expiryDate != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        txtCertValidity.setText("Valid Until: " + sdf.format(expiryDate));
                    } else {
                        txtCertValidity.setText("Valid Until: N/A");
                    }
                } catch (Exception e) {
                    txtCertValidity.setText("Valid Until: N/A");
                }
            } else {
                txtCertSubject.setText("Issued To: " + hostname);
                txtCertIssuer.setText("Issued By: Secure CA");
                txtCertValidity.setText("Valid Until: N/A");
            }
        } else if (url != null && (url.startsWith("http://") || url.startsWith("content://") || url.startsWith("file://"))) {
            imgSecurityStatus.setImageResource(R.drawable.ic_warning);
            imgSecurityStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            txtSecurityTitle.setText("Connection is Unsecure");
            txtSecurityDesc.setText("Warning: This connection is unencrypted. Do not enter sensitive information.");
            layoutCertInfo.setVisibility(View.GONE);
        } else {
            imgSecurityStatus.setImageResource(R.drawable.ic_lock);
            imgSecurityStatus.setColorFilter(ContextCompat.getColor(this, R.color.textSecondary));
            txtSecurityTitle.setText("Velocity Native Page");
            txtSecurityDesc.setText("You are viewing a local browser page.");
            layoutCertInfo.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void showContextMenuBottomSheet(final WebView.HitTestResult result) {
        int type = result.getType();
        final String extra = result.getExtra();
        if (extra == null || extra.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundDark));
        layout.setPadding(16, 16, 16, 24);

        if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            TextView previewHeader = new TextView(this);
            previewHeader.setPadding(32, 16, 32, 16);
            previewHeader.setText("Link: " + extra);
            previewHeader.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            previewHeader.setTextSize(13);
            previewHeader.setEllipsize(TextUtils.TruncateAt.END);
            previewHeader.setSingleLine(true);
            layout.addView(previewHeader);

            addContextOption(layout, "Open in New Tab", v -> {
                createNewTab(extra, false);
                dialog.dismiss();
            });
            addContextOption(layout, "Open in Background Tab", v -> {
                WebView backgroundWeb = createConfiguredWebView(false, false);
                WebTab tab = new WebTab(UUID.randomUUID().toString(), "New Tab", extra, backgroundWeb, false);
                tabList.add(tab);
                updateTabCount();
                backgroundWeb.loadUrl(WebUtils.processUrlOrQuery(extra, prefManager.getSearchEngineIndex()));
                Toast.makeText(MainActivity.this, "Opened in background tab", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            addContextOption(layout, "Copy Link Address", v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", extra);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            addContextOption(layout, "Share Link", v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, extra);
                startActivity(Intent.createChooser(shareIntent, "Share Link"));
                dialog.dismiss();
            });
            addContextOption(layout, "Bookmark Link", v -> {
                dbHelper.addBookmark(new com.naveen.browser.model.BookmarkItem("Bookmarked Link", extra, System.currentTimeMillis()));
                Toast.makeText(MainActivity.this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            addContextOption(layout, "Open in External Browser", v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(extra));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Cannot open external browser", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
        } else if (type == WebView.HitTestResult.IMAGE_TYPE) {
            TextView previewHeader = new TextView(this);
            previewHeader.setPadding(32, 16, 32, 16);
            previewHeader.setText("Image: " + extra);
            previewHeader.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            previewHeader.setTextSize(13);
            previewHeader.setEllipsize(TextUtils.TruncateAt.END);
            previewHeader.setSingleLine(true);
            layout.addView(previewHeader);

            addContextOption(layout, "Save Image", v -> {
                try {
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(extra));
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    String fileName = URLUtil.guessFileName(extra, null, "image/*");
                    request.setTitle(fileName);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "Downloading image...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
            addContextOption(layout, "Open Image in New Tab", v -> {
                createNewTab(extra, false);
                dialog.dismiss();
            });
            addContextOption(layout, "Copy Image URL", v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", extra);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            addContextOption(layout, "Share Image Link", v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, extra);
                startActivity(Intent.createChooser(shareIntent, "Share Image URL"));
                dialog.dismiss();
            });
        }

        dialog.setContentView(layout);
        dialog.show();
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
    private void hideSystemBars() {
        if (isBarsHidden) return;
        isBarsHidden = true;

        if (topBarContainer != null) {
            topBarContainer.animate()
                    .translationY(-topBarContainer.getHeight() - 20)
                    .setDuration(250)
                    .start();
        }

        if (bottomBarContainer != null) {
            bottomBarContainer.animate()
                    .translationY(bottomBarContainer.getHeight() + 20)
                    .setDuration(250)
                    .start();
        }
    }

    private void showSystemBars() {
        if (!isBarsHidden) return;
        isBarsHidden = false;

        if (topBarContainer != null) {
            topBarContainer.animate()
                    .translationY(0)
                    .setDuration(250)
                    .start();
        }

        if (bottomBarContainer != null) {
            bottomBarContainer.animate()
                    .translationY(0)
                    .setDuration(250)
                    .start();
        }
    }
}
