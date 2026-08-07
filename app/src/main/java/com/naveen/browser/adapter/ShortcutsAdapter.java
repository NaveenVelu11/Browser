package com.naveen.browser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.R;
import com.naveen.browser.model.ShortcutItem;

import java.util.List;

public class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {

    public interface OnShortcutClickListener {
        void onShortcutClick(ShortcutItem item);
        void onShortcutLongClick(ShortcutItem item);
    }

    private final List<ShortcutItem> list;
    private final OnShortcutClickListener listener;

    public ShortcutsAdapter(List<ShortcutItem> list, OnShortcutClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shortcut_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutItem item = list.get(position);
        holder.txtTitle.setText(item.getTitle());
        String initial = item.getTitle() != null && !item.getTitle().isEmpty()
                ? item.getTitle().substring(0, 1).toUpperCase()
                : "W";
        holder.txtInitial.setText(initial);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onShortcutClick(item);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onShortcutLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtInitial;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_shortcut_title);
            txtInitial = itemView.findViewById(R.id.txt_shortcut_initial);
        }
    }
}
