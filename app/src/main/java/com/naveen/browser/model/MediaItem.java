package com.naveen.browser.model;

public class MediaItem {
    private String url;
    private String title;
    private String mimeType;

    public MediaItem(String url, String title, String mimeType) {
        this.url = url;
        this.title = title;
        this.mimeType = mimeType;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getMimeType() { return mimeType; }
}
