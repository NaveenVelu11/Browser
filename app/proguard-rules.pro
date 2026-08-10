# Keep all Browser classes
-keep class com.naveen.browser.** { *; }

# Keep AndroidX classes
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Keep WebView
-keep class android.webkit.** { *; }
-keepclassmembers class android.webkit.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep custom application classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver

# Reduce build size
-dontwarn javax.**
-dontwarn sun.misc.**
-dontwarn org.apache.**