package com.naveen.browser.model;

public class DownloadItem {
    private final long id;
    private final String name;
    private final int progress;
    private final long totalSize;
    private final long downloadedSize;
    private final int status;
    private final String localUri;

    public DownloadItem(long id, String name, int progress, long totalSize, long downloadedSize, int status, String localUri) {
        this.id = id;
        this.name = name;
        this.progress = progress;
        this.totalSize = totalSize;
        this.downloadedSize = downloadedSize;
        this.status = status;
        this.localUri = localUri;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getProgress() { return progress; }
    public long getTotalSize() { return totalSize; }
    public long getDownloadedSize() { return downloadedSize; }
    public int getStatus() { return status; }
    public String getLocalUri() { return localUri; }
}
