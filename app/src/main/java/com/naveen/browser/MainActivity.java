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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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
        setContentView(R.layout.activity_main);

        prefManager = new PreferenceManager(this);
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
        txtTabCount = findViewById(R.id.txt_tab_count);

        ImageButton btnGo = findViewById(R.id.btn_go);
        ImageButton btnOverflow = findViewById(R.id.btn_overflow);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);
        ImageButton btnHome = findViewById(R.id.btn_home);
        FrameLayout btnTabsLayout = findViewById(R.id.btn_tabs_layout);

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
                web.loadUrl(prefManager.getHomepage());
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
        WebView webView = createConfiguredWebView(isIncognito);
        WebTab tab = new WebTab(UUID.randomUUID().toString(), "New Tab", finalUrl, webView, isIncognito);
        tabList.add(tab);
        switchToTab(tabList.size() - 1);
        webView.loadUrl(WebUtils.processUrlOrQuery(finalUrl, prefManager.getSearchEngineIndex()));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createConfiguredWebView(boolean isIncognito) {
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

        applyUserAgent(settings);

        CookieManager cookieManager = CookieManager.getInstance();
        if (isIncognito) {
            settings.setSaveFormData(false);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            cookieManager.setAcceptCookie(false);
        } else {
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

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
                    if (!editUrl.hasFocus()) {
                        editUrl.setText(url);
                    }
                    updateSslIcon(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == getCurrentWebView()) {
                    updateSslIcon(url);
                    WebTab currentTab = getCurrentTab();
                    if (currentTab != null && !currentTab.isIncognito()) {
                        dbHelper.addHistory(new HistoryItem(view.getTitle(), url, System.currentTimeMillis()));
                    }

                    if (prefManager.isNightMode()) {
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

    private void applyUserAgent(WebSettings settings) {
        if (prefManager.isDesktopMode()) {
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
        editUrl.setText(currentWeb.getUrl());
        updateTabCount();
        updateSslIcon(currentWeb.getUrl());
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
        String targetUrl = WebUtils.processUrlOrQuery(input, prefManager.getSearchEngineIndex());
        if (prefManager.isHttpsOnlyEnabled()) {
            targetUrl = WebUtils.upgradeToHttps(targetUrl);
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

    private void showOverflowMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.main_overflow_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);

        MenuItem desktopItem = popup.getMenu().findItem(R.id.menu_desktop_site);
        if (desktopItem != null) {
            desktopItem.setChecked(prefManager.isDesktopMode());
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
            boolean nextState = !prefManager.isDesktopMode();
            prefManager.setDesktopMode(nextState);
            if (currentWeb != null) {
                applyUserAgent(currentWeb.getSettings());
                currentWeb.reload();
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
                currentWeb.evaluateJavascript(WebUtils.getReaderModeScript(), null);
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
        ImageButton btnPrev = dialogView.findViewById(R.id.btn_find_prev);
        ImageButton btnNext = dialogView.findViewById(R.id.btn_find_next);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_find_close);

        WebView currentWeb = getCurrentWebView();

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
            if (currentWeb != null) currentWeb.clearMatches();
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
            String selectedUrl = data.getStringExtra("url");
            if (selectedUrl != null && getCurrentWebView() != null) {
                getCurrentWebView().loadUrl(selectedUrl);
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
}
