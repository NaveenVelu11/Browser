package com.naveen.browser.adapter;

import android.app.DownloadManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.R;
import com.naveen.browser.model.DownloadItem;

import java.util.List;
import java.util.Locale;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    public interface OnDownloadItemClickListener {
        void onCancel(long downloadId);
        void onOpen(DownloadItem item);
        void onDelete(long downloadId);
    }

    private List<DownloadItem> items;
    private final OnDownloadItemClickListener listener;

    public DownloadsAdapter(List<DownloadItem> items, OnDownloadItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<DownloadItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = items.get(position);

        holder.txtName.setText(item.getName() != null ? item.getName() : "Unknown File");
        holder.progressDownload.setProgress(item.getProgress());
        holder.txtPercent.setText(String.format(Locale.getDefault(), "%d%%", item.getProgress()));

        String statusStr;
        switch (item.getStatus()) {
            case DownloadManager.STATUS_RUNNING:
                statusStr = "Downloading... " + formatSize(item.getDownloadedSize()) + " / " + formatSize(item.getTotalSize());
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnOpen.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                statusStr = "Completed • " + formatSize(item.getTotalSize());
                holder.progressDownload.setProgress(100);
                holder.txtPercent.setText("100%");
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnOpen.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
                break;
            case DownloadManager.STATUS_PAUSED:
                statusStr = "Paused";
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnOpen.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
                break;
            case DownloadManager.STATUS_FAILED:
                statusStr = "Failed";
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnOpen.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.VISIBLE);
                break;
            default:
                statusStr = "Queued";
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnOpen.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
                break;
        }
        holder.txtStatus.setText(statusStr);

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(item.getId());
        });

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item.getId());
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtStatus, txtPercent;
        ProgressBar progressDownload;
        Button btnCancel, btnOpen, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_download_name);
            txtStatus = itemView.findViewById(R.id.txt_download_status);
            txtPercent = itemView.findViewById(R.id.txt_download_percent);
            progressDownload = itemView.findViewById(R.id.progress_download);
            btnCancel = itemView.findViewById(R.id.btn_download_cancel);
            btnOpen = itemView.findViewById(R.id.btn_download_open);
            btnDelete = itemView.findViewById(R.id.btn_download_delete);
        }
    }
}
