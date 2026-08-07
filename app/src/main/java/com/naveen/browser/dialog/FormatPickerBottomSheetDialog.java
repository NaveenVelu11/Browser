package com.naveen.browser.dialog;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.naveen.browser.R;
import com.naveen.browser.utils.YtDlpExtractor;
import java.util.ArrayList;
import java.util.List;

public class FormatPickerBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnFormatSelectedListener {
        void onFormatSelected(YtDlpExtractor.FormatOption selectedOption);
    }

    private YtDlpExtractor.VideoDetails videoDetails;
    private OnFormatSelectedListener listener;
    private int selectedPosition = 0;
    private boolean isAudioTabSelected = false;

    private RecyclerView recyclerView;
    private FormatAdapter adapter;
    private TextView tabVideoFormats;
    private TextView tabAudioOnly;

    public static FormatPickerBottomSheetDialog newInstance(YtDlpExtractor.VideoDetails details) {
        FormatPickerBottomSheetDialog dialog = new FormatPickerBottomSheetDialog();
        dialog.videoDetails = details;
        return dialog;
    }

    public void setOnFormatSelectedListener(OnFormatSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_media_format_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (videoDetails == null) {
            dismiss();
            return;
        }

        TextView txtTitle = view.findViewById(R.id.txt_picker_title);
        TextView txtUploader = view.findViewById(R.id.txt_picker_uploader);
        TextView txtDuration = view.findViewById(R.id.txt_picker_duration);
        tabVideoFormats = view.findViewById(R.id.tab_video_formats);
        tabAudioOnly = view.findViewById(R.id.tab_audio_only);
        recyclerView = view.findViewById(R.id.recycler_format_options);

        txtTitle.setText(videoDetails.getTitle());
        txtUploader.setText(videoDetails.getUploader());
        txtDuration.setText(videoDetails.getDuration());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        updateOptionsList();

        tabVideoFormats.setOnClickListener(v -> {
            isAudioTabSelected = false;
            updateTabStyles();
            updateOptionsList();
        });

        tabAudioOnly.setOnClickListener(v -> {
            isAudioTabSelected = true;
            updateTabStyles();
            updateOptionsList();
        });

        view.findViewById(R.id.btn_start_download).setOnClickListener(v -> {
            List<YtDlpExtractor.FormatOption> filtered = getFilteredOptions();
            if (!filtered.isEmpty() && selectedPosition < filtered.size()) {
                YtDlpExtractor.FormatOption option = filtered.get(selectedPosition);
                if (listener != null) {
                    listener.onFormatSelected(option);
                } else {
                    enqueueDownload(option);
                }
            }
            dismiss();
        });
    }

    private List<YtDlpExtractor.FormatOption> getFilteredOptions() {
        List<YtDlpExtractor.FormatOption> filtered = new ArrayList<>();
        if (videoDetails != null && videoDetails.getFormatOptions() != null) {
            for (YtDlpExtractor.FormatOption opt : videoDetails.getFormatOptions()) {
                if (opt.isAudioOnly() == isAudioTabSelected) {
                    filtered.add(opt);
                }
            }
        }
        return filtered;
    }

    private void updateTabStyles() {
        if (getContext() == null) return;
        int primaryColor = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ios_text_primary_light);
        int secondaryColor = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ios_text_secondary_light);
        if (isAudioTabSelected) {
            tabAudioOnly.setTextColor(primaryColor);
            tabVideoFormats.setTextColor(secondaryColor);
        } else {
            tabVideoFormats.setTextColor(primaryColor);
            tabAudioOnly.setTextColor(secondaryColor);
        }
    }

    private void updateOptionsList() {
        selectedPosition = 0;
        List<YtDlpExtractor.FormatOption> filtered = getFilteredOptions();
        adapter = new FormatAdapter(filtered);
        recyclerView.setAdapter(adapter);
    }

    private void enqueueDownload(YtDlpExtractor.FormatOption option) {
        if (getContext() == null) return;
        try {
            DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(option.getDirectUrl()));
                request.setTitle(videoDetails.getTitle());
                request.setDescription("DeerOne Media Download • " + option.getLabel());
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                String dir = option.isAudioOnly() ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES;
                String fileName = "DeerOne_" + System.currentTimeMillis() + "." + option.getExt();
                request.setDestinationInExternalPublicDir(dir, "DeerOne/" + fileName);

                downloadManager.enqueue(request);
                Toast.makeText(getContext(), "Download started ✓", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class FormatAdapter extends RecyclerView.Adapter<FormatAdapter.ViewHolder> {

        private final List<YtDlpExtractor.FormatOption> items;

        public FormatAdapter(List<YtDlpExtractor.FormatOption> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_format_option, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YtDlpExtractor.FormatOption item = items.get(position);
            holder.txtLabel.setText(item.getLabel() + " (" + item.getExt().toUpperCase() + ")");
            holder.txtSize.setText(item.getEstimatedSize());
            holder.radioButton.setChecked(position == selectedPosition);

            holder.container.setOnClickListener(v -> {
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View container;
            RadioButton radioButton;
            TextView txtLabel;
            TextView txtSize;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                container = itemView.findViewById(R.id.container_format_item);
                radioButton = itemView.findViewById(R.id.radio_format_selected);
                txtLabel = itemView.findViewById(R.id.txt_format_label);
                txtSize = itemView.findViewById(R.id.txt_format_size);
            }
        }
    }
}
