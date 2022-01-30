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

package com.eightbit.samsprung.widget;

import static android.util.Log.d;
import static android.util.Log.e;
import static android.util.Log.w;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;

import com.eightbit.samsprung.SamSprungWidget;
import com.eightbit.samsprung.WidgetSettings;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class WidgetModel {
    public static final boolean DEBUG_LOADERS = true;
    public static final String LOG_TAG = WidgetModel.class.getName();

    private static final long APPLICATION_NOT_RESPONDING_TIMEOUT = 5000;
    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static final Collator sCollator = Collator.getInstance();

    private boolean mDesktopItemsLoaded;

    private ArrayList<WidgetInfo> mDesktopItems;
    private ArrayList<CoverWidgetInfo> mDesktopAppWidgets;

    private ApplicationsLoader mApplicationsLoader;
    private DesktopItemsLoader mDesktopItemsLoader;
    private Thread mApplicationsLoaderThread;
    private Thread mDesktopLoaderThread;

    private final HashMap<ComponentName, ApplicationInfo> mAppInfoCache =
            new HashMap<>(INITIAL_ICON_CACHE_CAPACITY);

    public WidgetModel() {
    }

    public synchronized void abortLoaders() {
        if (DEBUG_LOADERS) d(LOG_TAG, "aborting loaders");

        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> aborting applications loader");
            mApplicationsLoader.stop();
        }

        if (mDesktopItemsLoader != null && mDesktopItemsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> aborting workspace loader");
            mDesktopItemsLoader.stop();
            mDesktopItemsLoaded = false;
        }
    }

    /**
     * Drop our cache of components to their lables & icons.  We do
     * this from Launcher when applications are added/removed.  It's a
     * bit overkill, but it's a rare operation anyway.
     */
    synchronized void dropApplicationCache() {
        mAppInfoCache.clear();
    }

    /**
     * Loads the list of installed applications in mApplications.
     *
     * @return true if the applications loader must be started
     *         (see startApplicationsLoader()), false otherwise.
     */
    public synchronized boolean loadApplications(boolean isLaunching, SamSprungWidget launcher,
            boolean localeChanged) {

        if (DEBUG_LOADERS) d(LOG_TAG, "load applications");

        stopAndWaitForApplicationsLoader();

        if (localeChanged) {
            dropApplicationCache();
        }

        if (!isLaunching) {
            startApplicationsLoaderLocked(launcher, false);
            return false;
        }

        return true;
    }

    private synchronized void stopAndWaitForApplicationsLoader() {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            if (DEBUG_LOADERS) {
                d(LOG_TAG, "  --> wait for applications loader (" + mApplicationsLoader.mId + ")");
            }

            mApplicationsLoader.stop();
            // Wait for the currently running thread to finish, this can take a little
            // time but it should be well below the timeout limit
            try {
                mApplicationsLoaderThread.join(APPLICATION_NOT_RESPONDING_TIMEOUT);
            } catch (InterruptedException e) {
                // Empty
            }
        }
    }

    private synchronized void startApplicationsLoader(SamSprungWidget launcher, boolean isLaunching) {
        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting applications loader unlocked");
        startApplicationsLoaderLocked(launcher, isLaunching);
    }

    private void startApplicationsLoaderLocked(SamSprungWidget launcher, boolean isLaunching) {
        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting applications loader");

        stopAndWaitForApplicationsLoader();

        mApplicationsLoader = new ApplicationsLoader(launcher, isLaunching);
        mApplicationsLoaderThread = new Thread(mApplicationsLoader, "Applications Loader");
        mApplicationsLoaderThread.start();
    }

    private static final AtomicInteger sAppsLoaderCount = new AtomicInteger(1);
    private static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);

    private static class ApplicationsLoader implements Runnable {
        private final WeakReference<SamSprungWidget> mLauncher;

        private volatile boolean mStopped;
        private volatile boolean mRunning;
        private final boolean mIsLaunching;
        private final int mId;

        ApplicationsLoader(SamSprungWidget launcher, boolean isLaunching) {
            mIsLaunching = isLaunching;
            mLauncher = new WeakReference<>(launcher);
            mRunning = true;
            mId = sAppsLoaderCount.getAndIncrement();
        }

        void stop() {
            mStopped = true;
        }

        boolean isRunning() {
            return mRunning;
        }

        public void run() {
            if (DEBUG_LOADERS) d(LOG_TAG, "  ----> running applications loader (" + mId + ")");

            // Elevate priority when Home launches for the first time to avoid
            // starving at boot time. Staring at a blank home is not cool.
            android.os.Process.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT :
                    Process.THREAD_PRIORITY_BACKGROUND);

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final SamSprungWidget launcher = mLauncher.get();

            if (mStopped) {
                if (DEBUG_LOADERS) d(LOG_TAG, "  ----> applications loader stopped (" + mId + ")");
            }
            mRunning = false;
        }
    }

    public boolean isDesktopLoaded() {
        return mDesktopItems != null && mDesktopAppWidgets != null && mDesktopItemsLoaded;
    }

    /**
     * Loads all of the items on the desktop, in folders, or in the dock.
     * These can be apps, shortcuts or widgets
     */
    public void loadUserItems(boolean isLaunching, SamSprungWidget launcher, boolean localeChanged,
                       boolean loadApplications) {
        if (DEBUG_LOADERS) d(LOG_TAG, "loading user items");

        if (isLaunching && isDesktopLoaded()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> items loaded, return");
            if (loadApplications) startApplicationsLoader(launcher, true);
            // We have already loaded our data from the DB
            launcher.onDesktopItemsLoaded(mDesktopItems, mDesktopAppWidgets);
            return;
        }

        if (mDesktopItemsLoader != null && mDesktopItemsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> stopping workspace loader");
            mDesktopItemsLoader.stop();
            // Wait for the currently running thread to finish, this can take a little
            // time but it should be well below the timeout limit
            try {
                mDesktopLoaderThread.join(APPLICATION_NOT_RESPONDING_TIMEOUT);
            } catch (InterruptedException e) {
                // Empty
            }

            // If the thread we are interrupting was tasked to load the list of
            // applications make sure we keep that information in the new loader
            // spawned below
            // note: we don't apply this to localeChanged because the thread can
            // only be stopped *after* the localeChanged handling has occured
            loadApplications = mDesktopItemsLoader.mLoadApplications;
        }

        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting workspace loader");
        mDesktopItemsLoaded = false;
        mDesktopItemsLoader = new DesktopItemsLoader(launcher, localeChanged, loadApplications,
                isLaunching);
        mDesktopLoaderThread = new Thread(mDesktopItemsLoader, "Desktop Items Loader");
        mDesktopLoaderThread.start();
    }

    private static void updateShortcutLabels(ContentResolver resolver, PackageManager manager) {
        try (Cursor c = resolver.query(WidgetSettings.Favorites.CONTENT_URI,
                new String[] { WidgetSettings.Favorites._ID, WidgetSettings.Favorites.TITLE,
                        WidgetSettings.Favorites.INTENT, WidgetSettings.Favorites.ITEM_TYPE },
                null, null, null)) {
            final int idIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites._ID);
            final int intentIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.INTENT);
            final int itemTypeIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ITEM_TYPE);
            final int titleIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.TITLE);
            while (c.moveToNext()) {
                try {
                    if (c.getInt(itemTypeIndex) !=
                            WidgetSettings.Favorites.ITEM_TYPE_APPLICATION) {
                        continue;
                    }

                    final String intentUri = c.getString(intentIndex);
                    if (intentUri != null) {
                        final Intent shortcut = Intent.parseUri(intentUri, 0);
                        if (Intent.ACTION_MAIN.equals(shortcut.getAction())) {
                            final ComponentName name = shortcut.getComponent();
                            if (name != null) {
                                final ActivityInfo activityInfo = manager.getActivityInfo(name, 0);
                                final String title = c.getString(titleIndex);
                                String label = getLabel(manager, activityInfo);

                                if (title == null || !title.equals(label)) {
                                    final ContentValues values = new ContentValues();
                                    values.put(WidgetSettings.Favorites.TITLE, label);

                                    resolver.update(
                                            WidgetSettings.Favorites.CONTENT_URI_NO_NOTIFICATION,
                                            values, "_id=?",
                                            new String[]{String.valueOf(c.getLong(idIndex))});
                                }
                            }
                        }
                    }
                } catch (URISyntaxException | PackageManager.NameNotFoundException ignored) {
                }
            }
        }
    }

    private static String getLabel(PackageManager manager, ActivityInfo activityInfo) {
        return activityInfo.loadLabel(manager).toString();
    }

    private class DesktopItemsLoader implements Runnable {
        private volatile boolean mStopped;
        private volatile boolean mRunning;

        private final WeakReference<SamSprungWidget> mLauncher;
        private final boolean mLocaleChanged;
        private final boolean mLoadApplications;
        private final boolean mIsLaunching;
        private final int mId;        

        DesktopItemsLoader(SamSprungWidget launcher, boolean localeChanged, boolean loadApplications,
                           boolean isLaunching) {
            mLoadApplications = loadApplications;
            mIsLaunching = isLaunching;
            mLauncher = new WeakReference<>(launcher);
            mLocaleChanged = localeChanged;
            mId = sWorkspaceLoaderCount.getAndIncrement();
        }

        void stop() {
            mStopped = true;
        }

        boolean isRunning() {
            return mRunning;
        }

        public void run() {
            if (DEBUG_LOADERS) d(LOG_TAG, "  ----> running workspace loader (" + mId + ")");

            mRunning = true;

            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            final SamSprungWidget launcher = mLauncher.get();
            final ContentResolver contentResolver = launcher.getContentResolver();
            final PackageManager manager = launcher.getPackageManager();

            if (mLocaleChanged) {
                updateShortcutLabels(contentResolver, manager);
            }

            mDesktopItems = new ArrayList<>();
            mDesktopAppWidgets = new ArrayList<>();

            final ArrayList<WidgetInfo> desktopItems = mDesktopItems;
            final ArrayList<CoverWidgetInfo> desktopAppWidgets = mDesktopAppWidgets;

            try (Cursor c = contentResolver.query(WidgetSettings.Favorites.CONTENT_URI,
                    null, null, null, null)) {
                final int idIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites._ID);
                final int intentIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.INTENT);
                final int titleIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.TITLE);
                final int iconTypeIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ICON_TYPE);
                final int iconIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ICON);
                final int containerIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.CONTAINER);
                final int itemTypeIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.ITEM_TYPE);
                final int appWidgetIdIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.APPWIDGET_ID);
                final int screenIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.CELLY);
                final int spanXIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.SPANX);
                final int spanYIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.SPANY);
                final int uriIndex = c.getColumnIndexOrThrow(WidgetSettings.Favorites.URI);

                CoverWidgetInfo appWidgetInfo;
                int container;

                while (!mStopped && c.moveToNext()) {
                    try {
                        int itemType = c.getInt(itemTypeIndex);

                        if (itemType == WidgetSettings.Favorites.ITEM_TYPE_APPWIDGET) {// Read all Launcher-specific widget details
                            int appWidgetId = c.getInt(appWidgetIdIndex);
                            appWidgetInfo = new CoverWidgetInfo(appWidgetId);
                            appWidgetInfo.id = c.getLong(idIndex);
                            appWidgetInfo.screen = c.getInt(screenIndex);
                            appWidgetInfo.cellX = c.getInt(cellXIndex);
                            appWidgetInfo.cellY = c.getInt(cellYIndex);
                            appWidgetInfo.spanX = c.getInt(spanXIndex);
                            appWidgetInfo.spanY = c.getInt(spanYIndex);

                            container = c.getInt(containerIndex);
                            if (container != WidgetSettings.Favorites.CONTAINER_DESKTOP) {
                                e(SamSprungWidget.Companion.getLogTag(), "Widget found where container "
                                        + "!= CONTAINER_DESKTOP -- ignoring!");
                                continue;
                            }
                            appWidgetInfo.container = c.getInt(containerIndex);

                            desktopAppWidgets.add(appWidgetInfo);
                        }
                    } catch (Exception e) {
                        w(SamSprungWidget.Companion.getLogTag(), "Desktop items loading interrupted:", e);
                    }
                }
            }

            if (!mStopped) {
                if (DEBUG_LOADERS)  {
                    d(LOG_TAG, "  --> done loading workspace");
                    d(LOG_TAG, "  ----> worskpace items=" + desktopItems.size());                
                    d(LOG_TAG, "  ----> worskpace widgets=" + desktopAppWidgets.size());
                }

                // Create a copy of the lists in case the workspace loader is restarted
                // and the list are cleared before the UI can go through them
                final ArrayList<WidgetInfo> uiDesktopItems =
                        new ArrayList<>(desktopItems);
                final ArrayList<CoverWidgetInfo> uiDesktopWidgets =
                        new ArrayList<>(desktopAppWidgets);

                if (!mStopped) {
                    d(LOG_TAG, "  ----> items cloned, ready to refresh UI");                
                    launcher.runOnUiThread(() -> {
                        if (DEBUG_LOADERS) d(LOG_TAG, "  ----> onDesktopItemsLoaded()");
                        launcher.onDesktopItemsLoaded(uiDesktopItems, uiDesktopWidgets);
                    });
                }

                if (mLoadApplications) {
                    if (DEBUG_LOADERS) {
                        d(LOG_TAG, "  ----> loading applications from workspace loader");
                    }
                    startApplicationsLoader(launcher, mIsLaunching);
                }

                mDesktopItemsLoaded = true;
            } else {
                if (DEBUG_LOADERS) d(LOG_TAG, "  ----> worskpace loader was stopped");
            }
            mRunning = false;
        }
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    public void unbind() {
        // Interrupt the applications loader before setting the adapter to null
        stopAndWaitForApplicationsLoader();
        unbindDrawables(mDesktopItems);
        unbindAppWidgetHostViews(mDesktopAppWidgets);
        unbindCachedIconDrawables();
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    private void unbindDrawables(ArrayList<WidgetInfo> desktopItems) {
        if (desktopItems != null) {
            final int count = desktopItems.size();
            for (int i = 0; i < count; i++) {
                WidgetInfo item = desktopItems.get(i);
                switch (item.itemType) {
                case WidgetSettings.Favorites.ITEM_TYPE_APPLICATION:
                case WidgetSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ((ApplicationInfo)item).icon.setCallback(null);
                    break;
                }
            }
        }
    }

    /**
     * Remove any {@link CoverWidgetHostView} references in our widgets.
     */
    private void unbindAppWidgetHostViews(ArrayList<CoverWidgetInfo> appWidgets) {
        if (appWidgets != null) {
            final int count = appWidgets.size();
            for (int i = 0; i < count; i++) {
                CoverWidgetInfo launcherInfo = appWidgets.get(i);
                launcherInfo.hostView = null;
            }
        }
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    private void unbindCachedIconDrawables() {
        for (ApplicationInfo appInfo : mAppInfoCache.values()) {
            appInfo.icon.setCallback(null);
        }
    }

    /**
     * Remove an item from the desktop
     * @param info
     */
    public void removeDesktopItem(WidgetInfo info) {
        // TODO: write to DB; figure out if we should remove folder from folders list
        mDesktopItems.remove(info);
    }

    /**
     * Add a widget to the desktop
     */
    public void addDesktopAppWidget(CoverWidgetInfo info) {
        mDesktopAppWidgets.add(info);
    }

    /**
     * Remove a widget from the desktop
     */
    public void removeDesktopAppWidget(CoverWidgetInfo info) {
        mDesktopAppWidgets.remove(info);
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    static void moveItemInDatabase(Context context, WidgetInfo item, long container, int screen,
                                   int cellX, int cellY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(WidgetSettings.Favorites.CONTAINER, item.container);
        values.put(WidgetSettings.Favorites.CELLX, item.cellX);
        values.put(WidgetSettings.Favorites.CELLY, item.cellY);
        values.put(WidgetSettings.Favorites.SCREEN, item.screen);

        cr.update(WidgetSettings.Favorites.getContentUri(item.id, false), values, null, null);
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public static void addItemToDatabase(Context context, WidgetInfo item, int container,
                                         int screen, int cellX, int cellY, boolean notify) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        item.onAddToDatabase(values);

        Uri result = cr.insert(notify ? WidgetSettings.Favorites.CONTENT_URI :
                WidgetSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);

        if (result != null) {
            item.id = Integer.parseInt(result.getPathSegments().get(1));
        }
    }

    /**
     * Removes the specified item from the database
     * @param context
     * @param item
     */
    public static void deleteItemFromDatabase(Context context, WidgetInfo item) {
        final ContentResolver cr = context.getContentResolver();

        cr.delete(WidgetSettings.Favorites.getContentUri(item.id, false), null, null);
    }
}
