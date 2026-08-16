package com.naveen.browser.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.naveen.browser.CrashReportActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final String PREF_NAME = "deerone_crash_logs";
    private static final String KEY_LAST_CRASH = "last_crash_report";

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context context) {
        CrashHandler handler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            Log.e(TAG, "Uncaught Exception caught in thread: " + thread.getName(), throwable);
            String report = buildCrashReport(thread, throwable);

            // Store in SharedPreferences for easy retrieval
            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            pref.edit().putString(KEY_LAST_CRASH, report).apply();

            // Launch CrashReportActivity
            Intent intent = new Intent(context, CrashReportActivity.class);
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, report);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            // Terminate current crashing process cleanly
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            Log.e(TAG, "Error in CrashHandler while processing uncaught exception", e);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        }
    }

    private String buildCrashReport(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        sb.append("===== CRASH REPORT =====\n");
        sb.append("Timestamp: ").append(timeStamp).append("\n");
        sb.append("Thread: ").append(thread.getName()).append(" (ID: ").append(thread.getId()).append(")\n\n");

        sb.append("--- DEVICE DETAILS ---\n");
        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Model: ").append(Build.MODEL).append("\n");
        sb.append("Device: ").append(Build.DEVICE).append("\n");
        sb.append("Android Version: ").append(Build.VERSION.RELEASE)
          .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Board: ").append(Build.BOARD).append("\n\n");

        sb.append("--- EXCEPTION DETAILS ---\n");
        sb.append("Exception: ").append(throwable.getClass().getName()).append("\n");
        sb.append("Message: ").append(throwable.getMessage()).append("\n\n");

        sb.append("--- STACK TRACE ---\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        sb.append(sw.toString());

        return sb.toString();
    }

    public static String getLastCrashReport(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_LAST_CRASH, null);
    }

    public static void clearLastCrashReport(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().remove(KEY_LAST_CRASH).apply();
    }
}
