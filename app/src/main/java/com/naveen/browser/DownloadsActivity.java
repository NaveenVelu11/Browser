package com.naveen.browser;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.adapter.DownloadsAdapter;
import com.naveen.browser.model.DownloadItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity {

    private DownloadManager downloadManager;
    private DownloadsAdapter adapter;
    private LinearLayout layoutEmpty;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadDownloads();
            handler.postDelayed(this, 1000); // refresh every second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        ImageButton btnBack = findViewById(R.id.btn_back_downloads);
        RecyclerView recyclerView = findViewById(R.id.recycler_downloads);
        layoutEmpty = findViewById(R.id.layout_empty_downloads);

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadsAdapter(new ArrayList<>(), new DownloadsAdapter.OnDownloadItemClickListener() {
            @Override
            public void onCancel(long downloadId) {
                downloadManager.remove(downloadId);
                loadDownloads();
                Toast.makeText(DownloadsActivity.this, "Download cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpen(DownloadItem item) {
                openDownloadedFile(item);
            }

            @Override
            public void onDelete(long downloadId) {
                downloadManager.remove(downloadId);
                loadDownloads();
                Toast.makeText(DownloadsActivity.this, "Download deleted", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }

    private void loadDownloads() {
        if (downloadManager == null) return;

        List<DownloadItem> list = new ArrayList<>();
        try {
            DownloadManager.Query query = new DownloadManager.Query();
            Cursor cursor = downloadManager.query(query);

            if (cursor != null) {
                int idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int titleCol = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int currentCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int localUriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                while (cursor.moveToNext()) {
                    long id = idCol >= 0 ? cursor.getLong(idCol) : 0;
                    String title = titleCol >= 0 ? cursor.getString(titleCol) : "Download";
                    long total = totalCol >= 0 ? cursor.getLong(totalCol) : 0;
                    long current = currentCol >= 0 ? cursor.getLong(currentCol) : 0;
                    int status = statusCol >= 0 ? cursor.getInt(statusCol) : 0;
                    String localUri = localUriCol >= 0 ? cursor.getString(localUriCol) : null;

                    int progress = 0;
                    if (total > 0) {
                        progress = (int) ((current * 100) / total);
                    }

                    list.add(new DownloadItem(id, title, progress, total, current, status, localUri));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.updateData(list);
        if (layoutEmpty != null) {
            if (list.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void openDownloadedFile(DownloadItem item) {
        if (item.getLocalUri() == null) {
            Toast.makeText(this, "Local path not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri fileUri = Uri.parse(item.getLocalUri());
            
            // Convert file:// Uri if needed, but DownloadManager usually gives file:/// or content:// scheme
            if ("file".equals(fileUri.getScheme())) {
                File file = new File(fileUri.getPath());
                fileUri = androidx.core.content.FileProvider.getUriForFile(this, "com.naveen.browser.fileprovider", file);
            }

            String mimeType = getContentResolver().getType(fileUri);
            if (mimeType == null) {
                String ext = MimeTypeMap.getFileExtensionFromUrl(item.getLocalUri());
                if (ext != null) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                }
            }
            if (mimeType == null) {
                mimeType = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file type.", Toast.LENGTH_SHORT).show();
        }
    }
}
