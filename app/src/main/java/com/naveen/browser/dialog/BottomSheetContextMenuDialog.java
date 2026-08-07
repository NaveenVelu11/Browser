package com.naveen.browser.dialog;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.naveen.browser.R;

public class BottomSheetContextMenuDialog extends BottomSheetDialogFragment {

    public interface OnContextMenuActionListener {
        void onOpenInNewTab(String url);
        void onCopyLink(String url);
        void onDownloadMedia(String url);
        void onShareMedia(String url);
    }

    private static final String ARG_TARGET_URL = "target_url";
    private static final String ARG_TITLE = "title";

    private String targetUrl;
    private String title;
    private OnContextMenuActionListener listener;

    public static BottomSheetContextMenuDialog newInstance(String title, String targetUrl) {
        BottomSheetContextMenuDialog fragment = new BottomSheetContextMenuDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TARGET_URL, targetUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnContextMenuActionListener(OnContextMenuActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "Media Link");
            targetUrl = getArguments().getString(ARG_TARGET_URL, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_context_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Haptic Feedback on Open
        triggerHapticFeedback(view);

        TextView txtTitle = view.findViewById(R.id.txt_context_title);
        TextView txtUrl = view.findViewById(R.id.txt_context_url);

        if (txtTitle != null) txtTitle.setText(title);
        if (txtUrl != null) txtUrl.setText(targetUrl);

        view.findViewById(R.id.btn_action_open_new_tab).setOnClickListener(v -> {
            if (listener != null) listener.onOpenInNewTab(targetUrl);
            dismiss();
        });

        view.findViewById(R.id.btn_action_copy_link).setOnClickListener(v -> {
            if (listener != null) listener.onCopyLink(targetUrl);
            dismiss();
        });

        view.findViewById(R.id.btn_action_download).setOnClickListener(v -> {
            if (listener != null) listener.onDownloadMedia(targetUrl);
            dismiss();
        });

        view.findViewById(R.id.btn_action_share).setOnClickListener(v -> {
            if (listener != null) listener.onShareMedia(targetUrl);
            dismiss();
        });
    }

    private void triggerHapticFeedback(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }
}
