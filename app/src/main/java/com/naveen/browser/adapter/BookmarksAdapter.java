package com.naveen.browser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.R;
import com.naveen.browser.model.BookmarkItem;

import java.util.List;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.ViewHolder> {

    public interface OnBookmarkClickListener {
        void onBookmarkClick(BookmarkItem item);
        void onBookmarkDelete(BookmarkItem item);
    }

    private List<BookmarkItem> items;
    private final OnBookmarkClickListener listener;

    public BookmarksAdapter(List<BookmarkItem> items, OnBookmarkClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<BookmarkItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookmarkItem item = items.get(position);
        holder.txtTitle.setText(item.getTitle() != null && !item.getTitle().isEmpty() ? item.getTitle() : item.getUrl());
        holder.txtUrl.setText(item.getUrl());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookmarkClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onBookmarkDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtUrl;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_bookmark_title);
            txtUrl = itemView.findViewById(R.id.txt_bookmark_url);
            btnDelete = itemView.findViewById(R.id.btn_delete_bookmark);
        }
    }
}
