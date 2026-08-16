package com.naveen.browser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.naveen.browser.utils.CrashHandler;

public class CrashReportActivity extends AppCompatActivity {

    public static final String EXTRA_CRASH_REPORT = "extra_crash_report";
    private String crashReportText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);

        TextView txtCrashDetails = findViewById(R.id.txt_crash_details);
        Button btnCopy = findViewById(R.id.btn_copy_report);
        Button btnShare = findViewById(R.id.btn_share_report);
        Button btnRestart = findViewById(R.id.btn_restart_app);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_CRASH_REPORT)) {
            crashReportText = getIntent().getStringExtra(EXTRA_CRASH_REPORT);
        }

        if (crashReportText == null || crashReportText.isEmpty()) {
            crashReportText = CrashHandler.getLastCrashReport(this);
        }

        if (crashReportText == null || crashReportText.isEmpty()) {
            crashReportText = "No crash log recorded.";
        }

        txtCrashDetails.setText(crashReportText);

        btnCopy.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("DeerOne Crash Report", crashReportText);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(CrashReportActivity.this, "Crash report copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(CrashReportActivity.this, "Failed to copy report", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DeerOne Browser Crash Report");
                shareIntent.putExtra(Intent.EXTRA_TEXT, crashReportText);
                startActivity(Intent.createChooser(shareIntent, "Share Crash Report via"));
            } catch (Exception e) {
                Toast.makeText(CrashReportActivity.this, "Failed to share crash log", Toast.LENGTH_SHORT).show();
            }
        });

        btnRestart.setOnClickListener(v -> {
            CrashHandler.clearLastCrashReport(this);
            Intent restartIntent = new Intent(CrashReportActivity.this, SplashActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(restartIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back press from trapping user in crash state; restart app instead
        CrashHandler.clearLastCrashReport(this);
        Intent restartIntent = new Intent(CrashReportActivity.this, SplashActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restartIntent);
        finish();
    }
}
