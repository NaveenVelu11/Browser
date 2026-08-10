package com.naveen.browser.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.naveen.browser.model.BookmarkItem;
import com.naveen.browser.model.HistoryItem;
import com.naveen.browser.model.ShortcutItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "deerone_browser_db.db";
    private static final int DATABASE_VERSION = 2;

    // Bookmarks Table
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COLUMN_BM_ID = "id";
    private static final String COLUMN_BM_TITLE = "title";
    private static final String COLUMN_BM_URL = "url";
    private static final String COLUMN_BM_TIMESTAMP = "timestamp";

    // History Table
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_HIST_ID = "id";
    private static final String COLUMN_HIST_TITLE = "title";
    private static final String COLUMN_HIST_URL = "url";
    private static final String COLUMN_HIST_TIMESTAMP = "timestamp";

    // Shortcuts Table
    private static final String TABLE_SHORTCUTS = "shortcuts";
    private static final String COLUMN_SC_ID = "id";
    private static final String COLUMN_SC_TITLE = "title";
    private static final String COLUMN_SC_URL = "url";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createBookmarksTable = "CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                COLUMN_BM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BM_TITLE + " TEXT, " +
                COLUMN_BM_URL + " TEXT UNIQUE, " +
                COLUMN_BM_TIMESTAMP + " INTEGER)";

        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HIST_TITLE + " TEXT, " +
                COLUMN_HIST_URL + " TEXT, " +
                COLUMN_HIST_TIMESTAMP + " INTEGER)";

        String createShortcutsTable = "CREATE TABLE " + TABLE_SHORTCUTS + " (" +
                COLUMN_SC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SC_TITLE + " TEXT, " +
                COLUMN_SC_URL + " TEXT UNIQUE)";

        db.execSQL(createBookmarksTable);
        db.execSQL(createHistoryTable);
        db.execSQL(createShortcutsTable);

        // Insert Clean Initial Shortcuts
        insertInitialShortcuts(db);
    }

    private void insertInitialShortcuts(SQLiteDatabase db) {
        String[][] initial = {
                {"Google", "https://www.google.com"},
                {"YouTube", "https://www.youtube.com"},
                {"X / Twitter", "https://x.com"},
                {"GitHub", "https://github.com"}
        };
        for (String[] sc : initial) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_SC_TITLE, sc[0]);
            cv.put(COLUMN_SC_URL, sc[1]);
            db.insert(TABLE_SHORTCUTS, null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHORTCUTS);
        onCreate(db);
    }

    // --- Bookmarks Methods ---

    public boolean addBookmark(BookmarkItem bookmark) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_BM_TITLE, bookmark.getTitle());
            values.put(COLUMN_BM_URL, bookmark.getUrl());
            values.put(COLUMN_BM_TIMESTAMP, bookmark.getTimestamp());
            long result = db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<BookmarkItem> getAllBookmarks(String query) {
        List<BookmarkItem> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            String selection = null;
            String[] selectionArgs = null;
            if (query != null && !query.trim().isEmpty()) {
                selection = COLUMN_BM_TITLE + " LIKE ? OR " + COLUMN_BM_URL + " LIKE ?";
                selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
            }

            try (Cursor cursor = db.query(TABLE_BOOKMARKS, null, selection, selectionArgs, null, null, COLUMN_BM_TIMESTAMP + " DESC")) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BM_ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BM_TITLE));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BM_URL));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BM_TIMESTAMP));
                        list.add(new BookmarkItem(id, title, url, timestamp));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isBookmarked(String url) {
        if (url == null) return false;
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.query(TABLE_BOOKMARKS, new String[]{COLUMN_BM_ID}, COLUMN_BM_URL + "=?", new String[]{url}, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteBookmark(long id) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            int rows = db.delete(TABLE_BOOKMARKS, COLUMN_BM_ID + "=?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteBookmarkByUrl(String url) {
        if (url == null) return false;
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            int rows = db.delete(TABLE_BOOKMARKS, COLUMN_BM_URL + "=?", new String[]{url});
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // --- History Methods ---

    public void addHistory(HistoryItem history) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_HIST_TITLE, history.getTitle());
            values.put(COLUMN_HIST_URL, history.getUrl());
            values.put(COLUMN_HIST_TIMESTAMP, history.getTimestamp());
            db.insert(TABLE_HISTORY, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<HistoryItem> getAllHistory(String query) {
        List<HistoryItem> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            String selection = null;
            String[] selectionArgs = null;
            if (query != null && !query.trim().isEmpty()) {
                selection = COLUMN_HIST_TITLE + " LIKE ? OR " + COLUMN_HIST_URL + " LIKE ?";
                selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
            }

            try (Cursor cursor = db.query(TABLE_HISTORY, null, selection, selectionArgs, null, null, COLUMN_HIST_TIMESTAMP + " DESC")) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HIST_ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_TITLE));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HIST_URL));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HIST_TIMESTAMP));

                        list.add(new HistoryItem(id, title, url, timestamp));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteHistory(long id) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            int rows = db.delete(TABLE_HISTORY, COLUMN_HIST_ID + "=?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void clearAllHistory() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            db.delete(TABLE_HISTORY, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Shortcuts Methods ---

    public boolean addShortcut(ShortcutItem shortcut) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SC_TITLE, shortcut.getTitle());
            values.put(COLUMN_SC_URL, shortcut.getUrl());
            long result = db.insertWithOnConflict(TABLE_SHORTCUTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public List<ShortcutItem> getAllShortcuts() {
        List<ShortcutItem> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.query(TABLE_SHORTCUTS, null, null, null, null, null, COLUMN_SC_ID + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SC_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SC_TITLE));
                    String url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SC_URL));
                    list.add(new ShortcutItem(id, title, url));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteShortcut(long id) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            int rows = db.delete(TABLE_SHORTCUTS, COLUMN_SC_ID + "=?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
