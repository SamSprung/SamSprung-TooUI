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
package com.eightbit.samsprung.panels

import android.appwidget.AppWidgetHost
import android.content.*
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.eightbit.samsprung.SamSprungPanels
import com.eightbit.samsprung.panels.WidgetProvider
import com.eightbit.samsprung.panels.WidgetSettings.BaseLauncherColumns
import com.eightbit.samsprung.panels.WidgetSettings.Favorites
import java.util.*

class WidgetProvider : ContentProvider() {
    private var mOpenHelper: SQLiteOpenHelper? = null
    override fun onCreate(): Boolean {
        mOpenHelper = DatabaseHelper(context)
        return true
    }

    override fun getType(uri: Uri): String? {
        val args = SqlArguments(uri, null, null)
        return if (TextUtils.isEmpty(args.where)) {
            "vnd.android.cursor.dir/" + args.table
        } else {
            "vnd.android.cursor.item/" + args.table
        }
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val args = SqlArguments(uri, selection, selectionArgs)
        val qb = SQLiteQueryBuilder()
        qb.tables = args.table
        val db = mOpenHelper!!.writableDatabase
        val result = qb.query(db, projection, args.where, args.args, null, null, sortOrder)
        result.setNotificationUri(context!!.contentResolver, uri)
        return result
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        var uri = uri
        val args = SqlArguments(uri)
        val db = mOpenHelper!!.writableDatabase
        val rowId = db.insert(args.table, null, initialValues)
        if (rowId <= 0) return null
        uri = ContentUris.withAppendedId(uri, rowId)
        sendNotify(uri)
        return uri
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val args = SqlArguments(uri)
        val db = mOpenHelper!!.writableDatabase
        db.beginTransaction()
        try {
            for (value in values) {
                if (db.insert(args.table, null, value) < 0) return 0
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        sendNotify(uri)
        return values.size
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val args = SqlArguments(uri, selection, selectionArgs)
        val db = mOpenHelper!!.writableDatabase
        val count = db.delete(args.table, args.where, args.args)
        if (count > 0) sendNotify(uri)
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val args = SqlArguments(uri, selection, selectionArgs)
        val db = mOpenHelper!!.writableDatabase
        val count = db.update(args.table, values, args.where, args.args)
        if (count > 0) sendNotify(uri)
        return count
    }

    private fun sendNotify(uri: Uri) {
        val notify = uri.getQueryParameter(PARAMETER_NOTIFY)
        if (notify == null || "true" == notify) {
            context!!.contentResolver.notifyChange(uri, null)
        }
    }

    private class DatabaseHelper internal constructor(private val mContext: Context?) :
        SQLiteOpenHelper(
            mContext, DATABASE_NAME, null, DATABASE_VERSION
        ) {
        private val mAppWidgetHost: AppWidgetHost?

        /**
         * Send notification that we've deleted the [AppWidgetHost],
         * probably as part of the initial database creation. The receiver may
         * want to re-call [AppWidgetHost.startListening] to ensure
         * callbacks are correctly set.
         */
        private fun sendAppWidgetResetNotify() {
            val resolver = mContext!!.contentResolver
            resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null)
        }

        override fun onCreate(db: SQLiteDatabase) {
            if (LOGD) Log.d(LOG_TAG, "creating new launcher database")
            db.execSQL(
                "CREATE TABLE favorites (" +
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
                        ");"
            )

            // Database was just created, so wipe any previous widgets
            if (mAppWidgetHost != null) {
                mAppWidgetHost.deleteHost()
                sendAppWidgetResetNotify()
            }
            convertDatabase(db)
        }

        private fun convertDatabase(db: SQLiteDatabase) {
            if (LOGD) Log.d(LOG_TAG, "converting database from an older format, but not onUpgrade")
            var converted = false
            val uri = Uri.parse(
                "content://" + Settings.AUTHORITY +
                        "/old_favorites?notify=true"
            )
            val resolver = mContext!!.contentResolver
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(uri, null, null, null, null)
            } catch (e: Exception) {
                // Ignore
            }

            // We already have a favorites database in the old provider
            if (cursor != null && cursor.count > 0) {
                converted = try {
                    copyFromCursor(db, cursor) > 0
                } finally {
                    cursor.close()
                }
                if (converted) {
                    resolver.delete(uri, null, null)
                }
            }
            if (converted) {
                // Convert widgets from this import into widgets
                if (LOGD) Log.d(LOG_TAG, "converted and now triggering widget upgrade")
                convertWidgets(db)
            }
        }

        private fun copyFromCursor(db: SQLiteDatabase, c: Cursor): Int {
            val idIndex = c.getColumnIndexOrThrow(BaseColumns._ID)
            val intentIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.INTENT)
            val titleIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.TITLE)
            val iconTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ICON_TYPE)
            val iconIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ICON)
            val containerIndex = c.getColumnIndexOrThrow(Favorites.CONTAINER)
            val itemTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ITEM_TYPE)
            val uriIndex = c.getColumnIndexOrThrow(Favorites.URI)
            val rows = arrayOfNulls<ContentValues>(c.count)
            var i = 0
            while (c.moveToNext()) {
                val values = ContentValues(c.columnCount)
                values.put(BaseColumns._ID, c.getLong(idIndex))
                values.put(BaseLauncherColumns.Companion.INTENT, c.getString(intentIndex))
                values.put(BaseLauncherColumns.Companion.TITLE, c.getString(titleIndex))
                values.put(BaseLauncherColumns.Companion.ICON_TYPE, c.getInt(iconTypeIndex))
                values.put(BaseLauncherColumns.Companion.ICON, c.getBlob(iconIndex))
                values.put(Favorites.CONTAINER, c.getInt(containerIndex))
                values.put(BaseLauncherColumns.Companion.ITEM_TYPE, c.getInt(itemTypeIndex))
                values.put(Favorites.APPWIDGET_ID, -1)
                values.put(Favorites.URI, c.getString(uriIndex))
                rows[i++] = values
            }
            db.beginTransaction()
            var total = 0
            try {
                val numValues = rows.size
                i = 0
                while (i < numValues) {
                    if (db.insert(TABLE_FAVORITES, null, rows[i]) < 0) {
                        return 0
                    } else {
                        total++
                    }
                    i++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            return total
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (LOGD) Log.d(LOG_TAG, "onUpgrade triggered")
            var version = oldVersion
            if (version < 3) {
                // upgrade 1,2 -> 3 added appWidgetId column
                db.beginTransaction()
                try {
                    // Insert new column for holding appWidgetIds
                    db.execSQL(
                        "ALTER TABLE favorites " +
                                "ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1;"
                    )
                    db.setTransactionSuccessful()
                    version = 3
                } catch (ex: SQLException) {
                    // Old version remains, which means we wipe old data
                    Log.e(LOG_TAG, ex.message, ex)
                } finally {
                    db.endTransaction()
                }

                // Convert existing widgets only if table upgrade was successful
                if (version == 3) {
                    convertWidgets(db)
                }
            }
            if (version != DATABASE_VERSION) {
                Log.w(LOG_TAG, "Destroying all old data.")
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES)
                onCreate(db)
            }
        }

        /**
         * Upgrade existing clock and photo frame widgets into their new widget
         * equivalents. This method allocates appWidgetIds, and then hands off to
         * LauncherAppWidgetBinder to finish the actual binding.
         */
        private fun convertWidgets(db: SQLiteDatabase) {
            val bindSources = intArrayOf(
                Favorites.ITEM_TYPE_WIDGET_CLOCK,
                Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME
            )
            val bindTargets = ArrayList<ComponentName>()
            bindTargets.add(
                ComponentName(
                    "com.android.alarmclock",
                    "com.android.alarmclock.AnalogAppWidgetProvider"
                )
            )
            bindTargets.add(
                ComponentName(
                    "com.android.camera",
                    "com.android.camera.PhotoAppWidgetProvider"
                )
            )
            val selectWhere = buildOrWhereString(bindSources)
            var c: Cursor? = null
            var allocatedAppWidgets = false
            db.beginTransaction()
            try {
                // Select and iterate through each matching widget
                c = db.query(
                    TABLE_FAVORITES, arrayOf(BaseColumns._ID),
                    selectWhere, null, null, null, null
                )
                if (LOGD) Log.d(LOG_TAG, "found upgrade cursor count=" + c.count)
                val values = ContentValues()
                while (c != null && c.moveToNext()) {
                    val favoriteId = c.getLong(0)

                    // Allocate and update database with new appWidgetId
                    try {
                        val appWidgetId = mAppWidgetHost!!.allocateAppWidgetId()
                        if (LOGD) Log.d(
                            LOG_TAG,
                            "allocated appWidgetId=$appWidgetId for favoriteId=$favoriteId"
                        )
                        values.clear()
                        values.put(Favorites.APPWIDGET_ID, appWidgetId)

                        // Original widgets might not have valid spans when upgrading
                        values.put(Favorites.SPANX, 2)
                        values.put(Favorites.SPANY, 2)
                        val updateWhere = BaseColumns._ID + "=" + favoriteId
                        db.update(TABLE_FAVORITES, values, updateWhere, null)
                        allocatedAppWidgets = true
                    } catch (ex: RuntimeException) {
                        Log.e(LOG_TAG, "Problem allocating appWidgetId", ex)
                    }
                }
                db.setTransactionSuccessful()
            } catch (ex: SQLException) {
                Log.w(LOG_TAG, "Problem while allocating appWidgetIds for existing widgets", ex)
            } finally {
                db.endTransaction()
                c?.close()
            }

            // If any appWidgetIds allocated, then launch over to binder
            if (allocatedAppWidgets) {
                launchAppWidgetBinder(bindSources, bindTargets)
            }
        }

        /**
         * Launch the widget binder that walks through the Launcher database,
         * binding any matching widgets to the corresponding targets. We can't
         * bind ourselves because our parent process can't obtain the
         * BIND_APPWIDGET permission.
         */
        private fun launchAppWidgetBinder(
            bindSources: IntArray,
            bindTargets: ArrayList<ComponentName>
        ) {
            val intent = Intent()
            intent.component = ComponentName(
                "com.android.settings",
                "com.android.settings.LauncherAppWidgetBinder"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val extras = Bundle()
            extras.putIntArray(EXTRA_BIND_SOURCES, bindSources)
            extras.putParcelableArrayList(EXTRA_BIND_TARGETS, bindTargets)
            intent.putExtras(extras)
            mContext!!.startActivity(intent)
        }

        init {
            mAppWidgetHost = AppWidgetHost(mContext, SamSprungPanels.APPWIDGET_HOST_ID)
        }
    }

    internal class SqlArguments {
        var table: String? = null
        var where: String? = null
        var args: Array<String>?

        constructor(url: Uri, where: String?, args: Array<String>?) {
            if (url.pathSegments.size == 1) {
                table = url.pathSegments[0]
                this.where = where
                this.args = args
            } else require(url.pathSegments.size == 2) { "Invalid URI: $url" }
                if (!TextUtils.isEmpty(where)) {
                    throw java.lang.UnsupportedOperationException("WHERE clause not supported: $url")
                } else {
                    this.table = url.pathSegments[0]
                    this.where = "_id=" + ContentUris.parseId(url)
                    this.args = null
                }
        }

        constructor(url: Uri) {
            if (url.pathSegments.size == 1) {
                table = url.pathSegments[0]
                where = null
                args = null
            } else {
                throw IllegalArgumentException("Invalid URI: $url")
            }
        }
    }

    companion object {
        private val LOG_TAG = WidgetProvider::class.java.name
        private const val LOGD = true
        private const val DATABASE_NAME = "panels.db"
        private const val DATABASE_VERSION = 16
        const val AUTHORITY = "com.eightbit.samsprung.panels"
        const val EXTRA_BIND_SOURCES = AUTHORITY + ".bindsources"
        const val EXTRA_BIND_TARGETS = AUTHORITY + ".bindtargets"
        const val TABLE_FAVORITES = "favorites"
        const val PARAMETER_NOTIFY = "notify"

        /**
         * [Uri] triggered at any registered [android.database.ContentObserver] when
         * [AppWidgetHost.deleteHost] is called during database creation.
         * Use this to recall [AppWidgetHost.startListening] if needed.
         */
        val CONTENT_APPWIDGET_RESET_URI = Uri.parse("content://" + AUTHORITY + "/appWidgetReset")

        /**
         * Build a query string that will match any row where the column matches
         * anything in the values list.
         */
        fun buildOrWhereString(values: IntArray): String {
            val selectWhere = StringBuilder()
            for (i in values.indices.reversed()) {
                selectWhere.append(BaseLauncherColumns.Companion.ITEM_TYPE)
                    .append("=").append(values[i])
                if (i > 0) {
                    selectWhere.append(" OR ")
                }
            }
            return selectWhere.toString()
        }
    }
}