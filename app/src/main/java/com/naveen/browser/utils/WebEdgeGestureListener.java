package com.naveen.browser.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * iOS HIG-inspired Edge Swipe Gesture Listener for WebView navigation.
 * - Swiping right from the left edge (< 30dp) triggers webView.goBack()
 * - Swiping left from the right edge (> width - 30dp) triggers webView.goForward()
 */
public class WebEdgeGestureListener implements View.OnTouchListener {

    private final WebView webView;
    private final float edgeThresholdPx;
    private final float swipeThresholdPx;
    private final float maxVerticalVariancePx;
    private final int screenWidthPx;

    private float downX;
    private float downY;
    private boolean isLeftEdgeStart;
    private boolean isRightEdgeStart;

    public WebEdgeGestureListener(Context context, WebView webView) {
        this.webView = webView;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.screenWidthPx = metrics.widthPixels;
        this.edgeThresholdPx = 30 * metrics.density; // 30dp edge zone
        this.swipeThresholdPx = 60 * metrics.density; // 60dp drag gesture distance
        this.maxVerticalVariancePx = 80 * metrics.density;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (webView == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                isLeftEdgeStart = downX <= edgeThresholdPx;
                isRightEdgeStart = downX >= (screenWidthPx - edgeThresholdPx);
                break;

            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - downX;
                float deltaY = event.getY() - downY;

                if (Math.abs(deltaY) < maxVerticalVariancePx) {
                    if (isLeftEdgeStart && deltaX > swipeThresholdPx) {
                        if (webView.canGoBack()) {
                            webView.goBack();
                            showGestureFeedback(v.getContext(), "← Back");
                            return true;
                        }
                    } else if (isRightEdgeStart && deltaX < -swipeThresholdPx) {
                        if (webView.canGoForward()) {
                            webView.goForward();
                            showGestureFeedback(v.getContext(), "Forward →");
                            return true;
                        }
                    }
                }
                break;
        }

        return false;
    }

    private void showGestureFeedback(Context context, String message) {
        if (context != null) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
