/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eightbit.samsprung.drawer.panels;

import android.appwidget.AppWidgetHost;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.eightbit.samsprung.SamSprungOverlay;
import com.eightbit.samsprung.drawer.panels.WidgetSettings.Favorites;

import java.util.ArrayList;

public class WidgetProvider extends ContentProvider {
    private static final String DATABASE_NAME = "samsprung.db";
    
    private static final int DATABASE_VERSION = 6;

    static final String AUTHORITY = "com.eightbit.samsprung.drawer.panels";
    static final String EXTRA_BIND_SOURCES = AUTHORITY + ".bindsources";
    static final String EXTRA_BIND_TARGETS = AUTHORITY + ".bindtargets";
    
    static final String TABLE_FAVORITES = "favorites";
    static final String PARAMETER_NOTIFY = "notify";

    /**
     * {@link Uri} triggered at any registered {@link android.database.ContentObserver} when
     * {@link AppWidgetHost#deleteHost()} is called during database creation.
     * Use this to recall {@link AppWidgetHost#startListening()} if needed.
     */
    static final Uri CONTENT_APPWIDGET_RESET_URI =
            Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
    
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                if (db.insert(args.table, null, value) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private final Context mContext;
        private final AppWidgetHost mAppWidgetHost;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
            mAppWidgetHost = new AppWidgetHost(context, SamSprungOverlay.APPWIDGET_HOST_ID);
        }

        /**
         * Send notification that we've deleted the {@link AppWidgetHost},
         * probably as part of the initial database creation. The receiver may
         * want to re-call {@link AppWidgetHost#startListening()} to ensure
         * callbacks are correctly set.
         */
        private void sendAppWidgetResetNotify() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE favorites (" +
                    "_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "intent TEXT," +
                    "container INTEGER," +
                    "spanX INTEGER," +
                    "spanY INTEGER," +
                    "itemType INTEGER," +
                    "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                    "isShortcut INTEGER," +
                    "iconType INTEGER," +
                    "iconPackage TEXT," +
                    "iconResource TEXT," +
                    "icon BLOB," +
                    "uri TEXT," +
                    "displayMode INTEGER" +
                    ");");

            // Database was just created, so wipe any previous widgets
            if (mAppWidgetHost != null) {
                mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }

            convertDatabase(db);
        }

        private void convertDatabase(SQLiteDatabase db) {
            boolean converted = false;

            final Uri uri = Uri.parse("content://" + Settings.AUTHORITY +
                    "/old_favorites?notify=true");
            final ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = null;

            try {
                cursor = resolver.query(uri, null, null, null, null);
            } catch (Exception e) {
	            // Ignore
            }

            // We already have a favorites database in the old provider
            if (cursor != null && cursor.getCount() > 0) {
                try {
                    converted = copyFromCursor(db, cursor) > 0;
                } finally {
                    cursor.close();
                }

                if (converted) {
                    resolver.delete(uri, null, null);
                }
            }
            
            if (converted) {
                // Convert widgets from this import into widgets
                convertWidgets(db);
            }

        }

        private int copyFromCursor(SQLiteDatabase db, Cursor c) {
            final int idIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites._ID);
            final int intentIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.INTENT);
            final int titleIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.TITLE);
            final int iconTypeIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ICON_TYPE);
            final int iconIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ICON);
            final int containerIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.CONTAINER);
            final int itemTypeIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ITEM_TYPE);
            final int uriIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.URI);

            ContentValues[] rows = new ContentValues[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                ContentValues values = new ContentValues(c.getColumnCount());
                values.put(WidgetSettings.Favorites._ID, c.getLong(idIndex));
                values.put(WidgetSettings.Favorites.INTENT, c.getString(intentIndex));
                values.put(WidgetSettings.Favorites.TITLE, c.getString(titleIndex));
                values.put(WidgetSettings.Favorites.ICON_TYPE, c.getInt(iconTypeIndex));
                values.put(WidgetSettings.Favorites.ICON, c.getBlob(iconIndex));
                values.put(WidgetSettings.Favorites.CONTAINER, c.getInt(containerIndex));
                values.put(WidgetSettings.Favorites.ITEM_TYPE, c.getInt(itemTypeIndex));
                values.put(WidgetSettings.Favorites.APPWIDGET_ID, -1);
                values.put(WidgetSettings.Favorites.URI, c.getString(uriIndex));
                rows[i++] = values;
            }

            db.beginTransaction();
            int total = 0;
            try {
                int numValues = rows.length;
                for (i = 0; i < numValues; i++) {
                    if (db.insert(TABLE_FAVORITES, null, rows[i]) < 0) {
                        return 0;
                    } else {
                        total++;
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            return total;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            int version = oldVersion;
            if (version < 3) {
                // upgrade 1,2 -> 3 added appWidgetId column
                db.beginTransaction();
                try {
                    // Insert new column for holding appWidgetIds
                    db.execSQL("ALTER TABLE favorites " +
                        "ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1;");
                    db.setTransactionSuccessful();
                    version = 3;
                } catch (SQLException ex) {
                    // Old version remains, which means we wipe old data
                    Log.e(WidgetProvider.class.getName(), ex.getMessage(), ex);
                } finally {
                    db.endTransaction();
                }
                
                // Convert existing widgets only if table upgrade was successful
                if (version == 3) {
                    convertWidgets(db);
                }
            }
            
            if (version != DATABASE_VERSION) {
                Log.w(WidgetProvider.class.getName(), "Destroying all old data.");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
                onCreate(db);
            }
        }
        
        /**
         * Upgrade existing clock and photo frame widgets into their new widget
         * equivalents. This method allocates appWidgetIds, and then hands off to
         * LauncherAppWidgetBinder to finish the actual binding.
         */
        private void convertWidgets(SQLiteDatabase db) {
            final int[] bindSources = new int[] {
                    Favorites.ITEM_TYPE_WIDGET_CLOCK,
                    Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME,
            };
            
            final ArrayList<ComponentName> bindTargets = new ArrayList<>();
            bindTargets.add(new ComponentName("com.android.alarmclock",
                    "com.android.alarmclock.AnalogAppWidgetProvider"));
            bindTargets.add(new ComponentName("com.android.camera",
                    "com.android.camera.PhotoAppWidgetProvider"));
            
            final String selectWhere = buildOrWhereString(bindSources);
            
            Cursor c = null;
            boolean allocatedAppWidgets = false;
            
            db.beginTransaction();
            try {
                // Select and iterate through each matching widget
                c = db.query(TABLE_FAVORITES, new String[] { Favorites._ID },
                        selectWhere, null, null, null, null);
                
                final ContentValues values = new ContentValues();
                while (c != null && c.moveToNext()) {
                    long favoriteId = c.getLong(0);
                    
                    // Allocate and update database with new appWidgetId
                    try {
                        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                        values.clear();
                        values.put(WidgetSettings.Favorites.APPWIDGET_ID, appWidgetId);
                        
                        // Original widgets might not have valid spans when upgrading
                        values.put(WidgetSettings.Favorites.SPANX, 2);
                        values.put(WidgetSettings.Favorites.SPANY, 2);

                        String updateWhere = Favorites._ID + "=" + favoriteId;
                        db.update(TABLE_FAVORITES, values, updateWhere, null);
                        
                        allocatedAppWidgets = true;
                    } catch (RuntimeException ex) {
                        Log.e(WidgetProvider.class.getName(),
                                "Problem allocating appWidgetId", ex);
                    }
                }
                
                db.setTransactionSuccessful();
            } catch (SQLException ex) {
                Log.w(WidgetProvider.class.getName(),
                        "Problem while allocating appWidgetIds for existing widgets", ex);
            } finally {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
            }
            
            // If any appWidgetIds allocated, then launch over to binder
            if (allocatedAppWidgets) {
                launchAppWidgetBinder(bindSources, bindTargets);
            }
        }

        /**
         * Launch the widget binder that walks through the Launcher database,
         * binding any matching widgets to the corresponding targets. We can't
         * bind ourselves because our parent process can't obtain the
         * BIND_APPWIDGET permission.
         */
        private void launchAppWidgetBinder(int[] bindSources, ArrayList<ComponentName> bindTargets) {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                    "com.android.settings.LauncherAppWidgetBinder"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            final Bundle extras = new Bundle();
            extras.putIntArray(EXTRA_BIND_SOURCES, bindSources);
            extras.putParcelableArrayList(EXTRA_BIND_TARGETS, bindTargets);
            intent.putExtras(extras);
            
            mContext.startActivity(intent);
        }

    }

    /**
     * Build a query string that will match any row where the column matches
     * anything in the values list.
     */
    static String buildOrWhereString(int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(WidgetSettings.BaseLauncherColumns.ITEM_TYPE)
                    .append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);                
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
