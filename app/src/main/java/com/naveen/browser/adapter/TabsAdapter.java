package com.naveen.browser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.R;
import com.naveen.browser.model.WebTab;

import java.util.List;

public class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.ViewHolder> {

    public interface OnTabClickListener {
        void onTabSelect(int position);
        void onTabClose(int position);
    }

    private final List<WebTab> tabs;
    private final OnTabClickListener listener;

    public TabsAdapter(List<WebTab> tabs, OnTabClickListener listener) {
        this.tabs = tabs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WebTab tab = tabs.get(position);
        holder.txtTitle.setText(tab.getTitle());
        holder.txtUrl.setText(tab.isIncognito() ? "Incognito Tab" : tab.getUrl());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTabSelect(holder.getAdapterPosition());
        });

        holder.btnClose.setOnClickListener(v -> {
            if (listener != null) listener.onTabClose(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return tabs != null ? tabs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtUrl;
        ImageButton btnClose;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_tab_title);
            txtUrl = itemView.findViewById(R.id.txt_tab_url);
            btnClose = itemView.findViewById(R.id.btn_close_item_tab);
        }
    }
}
