package com.naveen.browser.utils;

import android.webkit.WebResourceRequest;
import com.naveen.browser.model.MediaItem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Automatic Social Media and Media Extraction Engine for DeerOne Browser.
 * Intercepts network requests and detects downloadable images, videos, and CDN audio streams.
 */
public class MediaSnifferEngine {

    public interface OnMediaDetectedListener {
        void onMediaDetected(MediaItem mediaItem);
    }

    private final List<MediaItem> detectedMediaList = new CopyOnWriteArrayList<>();
    private OnMediaDetectedListener listener;

    public MediaSnifferEngine() {}

    public void setOnMediaDetectedListener(OnMediaDetectedListener listener) {
        this.listener = listener;
    }

    public void clearDetectedMedia() {
        detectedMediaList.clear();
    }

    public List<MediaItem> getDetectedMediaList() {
        return detectedMediaList;
    }

    public void inspectRequest(WebResourceRequest request) {
        if (request == null || request.getUrl() == null) return;
        String url = request.getUrl().toString();
        inspectUrl(url);
    }

    public void inspectUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;

        String lowerUrl = url.toLowerCase();
        String mediaType = null;
        String extension = "";

        if (lowerUrl.contains(".mp4") || lowerUrl.contains("video/mp4")) {
            mediaType = "Video (MP4)";
            extension = "mp4";
        } else if (lowerUrl.contains(".m3u8") || lowerUrl.contains("application/x-mpegurl") || lowerUrl.contains("vnd.apple.mpegurl")) {
            mediaType = "HLS Video Stream";
            extension = "m3u8";
        } else if (lowerUrl.contains(".webm") || lowerUrl.contains("video/webm")) {
            mediaType = "Video (WebM)";
            extension = "webm";
        } else if (lowerUrl.contains(".mp3") || lowerUrl.contains("audio/mpeg") || lowerUrl.contains("audio/mp3")) {
            mediaType = "Audio (MP3)";
            extension = "mp3";
        } else if (lowerUrl.contains(".m4a") || lowerUrl.contains("audio/mp4") || lowerUrl.contains("audio/aac")) {
            mediaType = "Audio (M4A)";
            extension = "m4a";
        } else if (lowerUrl.contains(".pdf") || lowerUrl.contains("application/pdf")) {
            mediaType = "Document (PDF)";
            extension = "pdf";
        } else if (lowerUrl.contains(".apk") || lowerUrl.contains("application/vnd.android.package-archive")) {
            mediaType = "Android Package (APK)";
            extension = "apk";
        } else if (lowerUrl.contains(".zip") || lowerUrl.contains("application/zip") || lowerUrl.contains(".rar")) {
            mediaType = "Archive (ZIP)";
            extension = "zip";
        } else if ((lowerUrl.contains("twimg.com/video") || lowerUrl.contains("instagram.com/p/") || lowerUrl.contains("tiktok.com")) && (lowerUrl.contains(".mp4") || lowerUrl.contains("video"))) {
            mediaType = "Social Media Video";
            extension = "mp4";
        }

        if (mediaType != null) {
            // Avoid duplicate URLs
            for (MediaItem item : detectedMediaList) {
                if (item.getUrl().equalsIgnoreCase(url)) return;
            }

            String title = "Media (" + extension.toUpperCase() + ")";
            MediaItem item = new MediaItem(url, title, mediaType);
            detectedMediaList.add(item);

            if (listener != null) {
                listener.onMediaDetected(item);
            }
        }
    }
}
