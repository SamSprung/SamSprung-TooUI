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

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.BaseColumns
import com.eightbit.samsprung.panels.WidgetSettings.BaseLauncherColumns
import com.eightbit.samsprung.panels.WidgetSettings.Favorites
import java.lang.ref.WeakReference
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
class WidgetModel {
    private var mDesktopItemsLoaded = false
    private var mDesktopItems: ArrayList<WidgetInfo?>? = null
    private var mDesktopAppWidgets: ArrayList<CoverWidgetInfo>? = null
    private var mDesktopItemsLoader: DesktopItemsLoader? = null
    private var mDesktopLoaderThread: Thread? = null
    @Synchronized
    fun abortLoaders() {
        if (mDesktopItemsLoader != null && mDesktopItemsLoader!!.isRunning) {
            mDesktopItemsLoader!!.stop()
            mDesktopItemsLoaded = false
        }
    }

    val isDesktopLoaded: Boolean
        get() = mDesktopItems != null && mDesktopAppWidgets != null && mDesktopItemsLoaded

    /**
     * Loads all of the items on the desktop, in folders, or in the dock.
     * These can be apps, shortcuts or widgets
     */
    fun loadUserItems(isLaunching: Boolean, launcher: SamSprungPanels) {
        if (isLaunching && isDesktopLoaded) {
            // We have already loaded our data from the DB
            launcher.onDesktopItemsLoaded(mDesktopItems, mDesktopAppWidgets)
            return
        }
        if (mDesktopItemsLoader != null && mDesktopItemsLoader!!.isRunning) {
            mDesktopItemsLoader!!.stop()
            // Wait for the currently running thread to finish, this can take a little
            // time but it should be well below the timeout limit
            try {
                mDesktopLoaderThread!!.join(APPLICATION_NOT_RESPONDING_TIMEOUT)
            } catch (e: InterruptedException) {
                // Empty
            }
        }
        mDesktopItemsLoaded = false
        mDesktopItemsLoader = DesktopItemsLoader(launcher, isLaunching)
        mDesktopLoaderThread = Thread(mDesktopItemsLoader, "Desktop Items Loader")
        mDesktopLoaderThread!!.start()
    }

    private inner class DesktopItemsLoader internal constructor(
        launcher: SamSprungPanels,
        isLaunching: Boolean
    ) : Runnable {
        @Volatile
        private var mStopped = false

        @Volatile
        var isRunning = false
            private set
        private val mLauncher: WeakReference<SamSprungPanels>
        private val mId: Int
        fun stop() {
            mStopped = true
        }

        override fun run() {
            isRunning = true
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            val launcher = mLauncher.get()
            val contentResolver = launcher!!.contentResolver
            mDesktopItems = ArrayList()
            mDesktopAppWidgets = ArrayList()
            val desktopItems = mDesktopItems!!
            val desktopAppWidgets = mDesktopAppWidgets!!
            contentResolver.query(
                Favorites.CONTENT_URI,
                null, null, null, null
            ).use { c ->
                val idIndex = c!!.getColumnIndexOrThrow(BaseColumns._ID)
                val intentIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.INTENT)
                val titleIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.TITLE)
                val iconTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ICON_TYPE)
                val iconIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ICON)
                val containerIndex = c.getColumnIndexOrThrow(Favorites.CONTAINER)
                val itemTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ITEM_TYPE)
                val appWidgetIdIndex = c.getColumnIndexOrThrow(Favorites.APPWIDGET_ID)
                val spanXIndex = c.getColumnIndexOrThrow(Favorites.SPANX)
                val spanYIndex = c.getColumnIndexOrThrow(Favorites.SPANY)
                val uriIndex = c.getColumnIndexOrThrow(Favorites.URI)
                var appWidgetInfo: CoverWidgetInfo
                var container: Int
                while (!mStopped && c.moveToNext()) {
                    try {
                        val itemType = c.getInt(itemTypeIndex)
                        if (itemType == Favorites.ITEM_TYPE_APPWIDGET) { // Read all Launcher-specific widget details
                            val appWidgetId = c.getInt(appWidgetIdIndex)
                            appWidgetInfo = CoverWidgetInfo(appWidgetId)
                            appWidgetInfo.id = c.getLong(idIndex)
                            appWidgetInfo.spanX = c.getInt(spanXIndex)
                            appWidgetInfo.spanY = c.getInt(spanYIndex)
                            container = c.getInt(containerIndex)
                            if (container != Favorites.CONTAINER_DESKTOP) {
                                continue
                            }
                            appWidgetInfo.container = c.getInt(containerIndex).toLong()
                            desktopAppWidgets.add(appWidgetInfo)
                        }
                    } catch (ignored: Exception) {
                    }
                }
            }
            if (!mStopped) {
                // Create a copy of the lists in case the workspace loader is restarted
                // and the list are cleared before the UI can go through them
                val uiDesktopItems = ArrayList(desktopItems)
                val uiDesktopWidgets = ArrayList(desktopAppWidgets)
                if (!mStopped) {
                    launcher.runOnUiThread {
                        launcher.onDesktopItemsLoaded(
                            uiDesktopItems,
                            uiDesktopWidgets
                        )
                    }
                }
                mDesktopItemsLoaded = true
            }
            isRunning = false
        }

        init {
            mLauncher = WeakReference(launcher)
            mId = sWorkspaceLoaderCount.getAndIncrement()
        }
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    fun unbind() {
        // Interrupt the applications loader before setting the adapter to null
        unbindAppWidgetHostViews(mDesktopAppWidgets)
    }

    /**
     * Remove any [CoverWidgetHostView] references in our widgets.
     */
    private fun unbindAppWidgetHostViews(appWidgets: ArrayList<CoverWidgetInfo>?) {
        if (appWidgets != null) {
            val count = appWidgets.size
            for (i in 0 until count) {
                val launcherInfo = appWidgets[i]
                launcherInfo.hostView = null
            }
        }
    }

    /**
     * Add a widget to the desktop
     */
    fun addDesktopAppWidget(info: CoverWidgetInfo) {
        mDesktopAppWidgets!!.add(info)
    }

    /**
     * Remove a widget from the desktop
     */
    fun removeDesktopAppWidget(info: CoverWidgetInfo) {
        mDesktopAppWidgets!!.remove(info)
    }

    companion object {
        private const val APPLICATION_NOT_RESPONDING_TIMEOUT: Long = 5000
        private val sWorkspaceLoaderCount = AtomicInteger(1)
        private fun updateShortcutLabels(resolver: ContentResolver, manager: PackageManager) {
            resolver.query(
                Favorites.CONTENT_URI, arrayOf(
                    BaseColumns._ID, BaseLauncherColumns.Companion.TITLE,
                    BaseLauncherColumns.Companion.INTENT, BaseLauncherColumns.Companion.ITEM_TYPE
                ),
                null, null, null
            ).use { c ->
                val idIndex = c!!.getColumnIndexOrThrow(BaseColumns._ID)
                val intentIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.INTENT)
                val itemTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.ITEM_TYPE)
                val titleIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.Companion.TITLE)
                while (c.moveToNext()) {
                    try {
                        if (c.getInt(itemTypeIndex) !=
                            BaseLauncherColumns.Companion.ITEM_TYPE_APPLICATION
                        ) {
                            continue
                        }
                        val intentUri = c.getString(intentIndex)
                        if (intentUri != null) {
                            val shortcut = Intent.parseUri(intentUri, 0)
                            if (Intent.ACTION_MAIN == shortcut.action) {
                                val name = shortcut.component
                                if (name != null) {
                                    val activityInfo = manager.getActivityInfo(name, 0)
                                    val title = c.getString(titleIndex)
                                    val label = getLabel(manager, activityInfo)
                                    if (title == null || title != label) {
                                        val values = ContentValues()
                                        values.put(BaseLauncherColumns.Companion.TITLE, label)
                                        resolver.update(
                                            Favorites.CONTENT_URI_NO_NOTIFICATION,
                                            values, "_id=?", arrayOf(c.getLong(idIndex).toString())
                                        )
                                    }
                                }
                            }
                        }
                    } catch (ignored: URISyntaxException) {
                    } catch (ignored: PackageManager.NameNotFoundException) {
                    }
                }
            }
        }

        private fun getLabel(manager: PackageManager, activityInfo: ActivityInfo): String {
            return activityInfo.loadLabel(manager).toString()
        }

        /**
         * Move an item in the DB to a new <container></container>, screen, cellX, cellY>
         */
        fun moveItemInDatabase(
            context: Context, item: WidgetInfo, container: Long, screen: Int,
            spanX: Int, spanY: Int
        ) {
            item.container = container
            item.spanX = spanX
            item.spanY = spanY
            val values = ContentValues()
            val cr = context.contentResolver
            values.put(Favorites.CONTAINER, item.container)
            values.put(Favorites.SPANX, item.spanX)
            values.put(Favorites.SPANY, item.spanY)
            cr.update(Favorites.getContentUri(item.id, false), values, null, null)
        }

        /**
         * Add an item to the database in a specified container. Sets the container, screen, cellX and
         * cellY fields of the item. Also assigns an ID to the item.
         */
        fun addItemToDatabase(
            context: Context, item: WidgetInfo, container: Int,
            spanX: Int, spanY: Int, notify: Boolean
        ) {
            item.container = container.toLong()
            item.spanX = spanX
            item.spanY = spanY
            val values = ContentValues()
            val cr = context.contentResolver
            item.onAddToDatabase(values)
            val result = cr.insert(
                if (notify) Favorites.CONTENT_URI else Favorites.CONTENT_URI_NO_NOTIFICATION,
                values
            )
            if (result != null) {
                item.id = result.pathSegments[1].toInt().toLong()
            }
        }

        /**
         * Removes the specified item from the database
         * @param context
         * @param item
         */
        fun deleteItemFromDatabase(context: Context, item: WidgetInfo) {
            val cr = context.contentResolver
            cr.delete(Favorites.getContentUri(item.id, false), null, null)
        }
    }
}