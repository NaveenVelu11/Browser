package com.naveen.browser.model;

public class HistoryItem {
    private long id;
    private String title;
    private String url;
    private long timestamp;

    public HistoryItem(long id, String title, String url, long timestamp) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.timestamp = timestamp;
    }

    public HistoryItem(String title, String url, long timestamp) {
        this(-1, title, url, timestamp);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
