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

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;

import com.eightbit.samsprung.WidgetSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents an item in the launcher.
 */
public class WidgetInfo {
    
    static final int NO_ID = -1;
    
    /**
     * The id in the settings database for this item
     */
    long id = NO_ID;
    
    /**
     * One of {@link WidgetSettings.Favorites#ITEM_TYPE_APPLICATION},
     * {@link WidgetSettings.Favorites#ITEM_TYPE_SHORTCUT}, or
     * {@link WidgetSettings.Favorites#ITEM_TYPE_APPWIDGET}.
     */
    public int itemType;
    
    /**
     * The id of the container that holds this item. For the desktop, this will be 
     * {@link WidgetSettings.Favorites#CONTAINER_DESKTOP}. For the all applications folder it
     * will be {@link #NO_ID} (since it is not stored in the settings DB). For user folders
     * it will be the id of the folder.
     */
    public long container = NO_ID;
    
    /**
     * Iindicates the screen in which the shortcut appears.
     */
    public int screen = -1;
    
    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    public int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    public int spanY = 1;

    /**
     * Indicates whether the item is a gesture.
     */
    public boolean isGesture = false;

    WidgetInfo() { }

    WidgetInfo(WidgetInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        screen = info.screen;
        itemType = info.itemType;
        container = info.container;
    }

    /**
     * Write the fields of this item to the DB
     * 
     * @param values
     */
    public void onAddToDatabase(ContentValues values) {
        values.put(WidgetSettings.BaseLauncherColumns.ITEM_TYPE, itemType);
        if (!isGesture) {
            values.put(WidgetSettings.Favorites.CONTAINER, container);
            values.put(WidgetSettings.Favorites.SCREEN, screen);
            values.put(WidgetSettings.Favorites.CELLX, cellX);
            values.put(WidgetSettings.Favorites.CELLY, cellY);
            values.put(WidgetSettings.Favorites.SPANX, spanX);
            values.put(WidgetSettings.Favorites.SPANY, spanY);
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            // Try go guesstimate how much space the icon will take when serialized
            // to avoid unnecessary allocations/copies during the write.
            int size = bitmap.getWidth() * bitmap.getHeight() * 4;
            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                values.put(WidgetSettings.Favorites.ICON, out.toByteArray());
            } catch (IOException e) {
                Log.w("Favorite", "Could not write icon");
            }
        }
    }
    
}
