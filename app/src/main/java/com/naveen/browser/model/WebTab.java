package com.naveen.browser.model;

import android.webkit.WebView;

public class WebTab {
    private String id;
    private String title;
    private String url;
    private WebView webView;
    private boolean isIncognito;

    public WebTab(String id, String title, String url, WebView webView, boolean isIncognito) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.webView = webView;
        this.isIncognito = isIncognito;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return webView != null && webView.getTitle() != null && !webView.getTitle().isEmpty() ? webView.getTitle() : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return webView != null && webView.getUrl() != null ? webView.getUrl() : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public boolean isIncognito() {
        return isIncognito;
    }

    public void setIncognito(boolean incognito) {
        isIncognito = incognito;
    }
}
