# ProGuard rules for Browser application
-keep class com.naveen.browser.** { *; }

# Preserve WebView Javascript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(...);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(...);
}

# AndroidX Keep Rules
-keep class androidx.webkit.** { *; }
