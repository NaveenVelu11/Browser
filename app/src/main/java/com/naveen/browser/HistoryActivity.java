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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naveen.browser.adapter.HistoryAdapter;
import com.naveen.browser.db.DatabaseHelper;
import com.naveen.browser.model.HistoryItem;

import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryClickListener {

    private DatabaseHelper dbHelper;
    private HistoryAdapter adapter;
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.naveen.browser.utils.PreferenceManager pm = new com.naveen.browser.utils.PreferenceManager(this);
        pm.applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        ImageButton btnBack = findViewById(R.id.btn_back_history);
        ImageButton btnClear = findViewById(R.id.btn_clear_history);
        EditText editSearch = findViewById(R.id.edit_search_history);
        RecyclerView recyclerView = findViewById(R.id.recycler_history);
        txtEmpty = findViewById(R.id.txt_empty_history);

        btnBack.setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> confirmClearHistory());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(null, this);
        recyclerView.setAdapter(adapter);

        loadHistory(null);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadHistory(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadHistory(String query) {
        List<HistoryItem> list = dbHelper.getAllHistory(query);
        adapter.updateData(list);
        if (list.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.confirm_clear_history)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.clearAllHistory();
                    loadHistory(null);
                    Toast.makeText(HistoryActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onHistoryClick(HistoryItem item) {
        Intent data = new Intent();
        data.putExtra("url", item.getUrl());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onHistoryDelete(HistoryItem item) {
        dbHelper.deleteHistory(item.getId());
        loadHistory(null);
    }
}
