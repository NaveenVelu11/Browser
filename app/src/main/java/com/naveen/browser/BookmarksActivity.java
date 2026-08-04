package com.naveen.browser;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.adapter.BookmarksAdapter;
import com.naveen.browser.db.DatabaseHelper;
import com.naveen.browser.model.BookmarkItem;

import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarksAdapter.OnBookmarkClickListener {

    private DatabaseHelper dbHelper;
    private BookmarksAdapter adapter;
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        dbHelper = new DatabaseHelper(this);

        ImageButton btnBack = findViewById(R.id.btn_back_bookmarks);
        EditText editSearch = findViewById(R.id.edit_search_bookmarks);
        RecyclerView recyclerView = findViewById(R.id.recycler_bookmarks);
        txtEmpty = findViewById(R.id.txt_empty_bookmarks);

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookmarksAdapter(null, this);
        recyclerView.setAdapter(adapter);

        loadBookmarks(null);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadBookmarks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBookmarks(String query) {
        List<BookmarkItem> list = dbHelper.getAllBookmarks(query);
        adapter.updateData(list);
        if (list.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBookmarkClick(BookmarkItem item) {
        Intent data = new Intent();
        data.putExtra("url", item.getUrl());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onBookmarkDelete(BookmarkItem item) {
        dbHelper.deleteBookmark(item.getId());
        Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        loadBookmarks(null);
    }
}
