package com.naveen.browser.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class NestedWebView extends WebView {
    public interface OnScrollDeltaListener {
        void onScrollDelta(int dx, int dy, MotionEvent event);
    }

    private OnScrollDeltaListener scrollDeltaListener;
    private float lastY;
    private boolean isDragging = false;

    public NestedWebView(Context context) {
        super(context);
    }

    public NestedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NestedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnScrollDeltaListener(OnScrollDeltaListener listener) {
        this.scrollDeltaListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = event.getRawY();
                isDragging = true;
                if (scrollDeltaListener != null) {
                    scrollDeltaListener.onScrollDelta(0, 0, event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float currentY = event.getRawY();
                float deltaY = currentY - lastY;
                lastY = currentY;
                if (isDragging && scrollDeltaListener != null) {
                    scrollDeltaListener.onScrollDelta(0, (int) deltaY, event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                if (scrollDeltaListener != null) {
                    scrollDeltaListener.onScrollDelta(0, 0, event);
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
