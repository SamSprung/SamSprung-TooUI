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

import android.content.ContentValues
import android.graphics.Bitmap
import android.util.Log
import com.eightbit.samsprung.panels.WidgetSettings.BaseLauncherColumns
import com.eightbit.samsprung.panels.WidgetSettings.Favorites
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Represents an item in the launcher.
 */
open class WidgetInfo {
    /**
     * The id in the settings database for this item
     */
    var id = NO_ID.toLong()

    /**
     * One of [WidgetSettings.Favorites.ITEM_TYPE_APPLICATION],
     * [WidgetSettings.Favorites.ITEM_TYPE_SHORTCUT], or
     * [WidgetSettings.Favorites.ITEM_TYPE_APPWIDGET].
     */
    var itemType = 0

    /**
     * The id of the container that holds this item. For the desktop, this will be
     * [WidgetSettings.Favorites.CONTAINER_DESKTOP]. For the all applications folder it
     * will be [.NO_ID] (since it is not stored in the settings DB). For user folders
     * it will be the id of the folder.
     */
    var container = NO_ID.toLong()

    /**
     * Indicates the X cell span.
     */
    var spanX = 1

    /**
     * Indicates the Y cell span.
     */
    var spanY = 1

    /**
     * Indicates whether the item is a gesture.
     */
    var isGesture = false

    internal constructor() {}
    constructor(info: WidgetInfo) {
        id = info.id
        spanX = info.spanX
        spanY = info.spanY
        itemType = info.itemType
        container = info.container
    }

    /**
     * Write the fields of this item to the DB
     *
     * @param values
     */
    open fun onAddToDatabase(values: ContentValues) {
        values.put(BaseLauncherColumns.Companion.ITEM_TYPE, itemType)
        if (!isGesture) {
            values.put(Favorites.CONTAINER, container)
            values.put(Favorites.SPANX, spanX)
            values.put(Favorites.SPANY, spanY)
        }
    }

    companion object {
        const val NO_ID = -1
        fun writeBitmap(values: ContentValues, bitmap: Bitmap?) {
            if (bitmap != null) {
                // Try go guesstimate how much space the icon will take when serialized
                // to avoid unnecessary allocations/copies during the write.
                val size = bitmap.width * bitmap.height * 4
                val out = ByteArrayOutputStream(size)
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    out.close()
                    values.put(BaseLauncherColumns.Companion.ICON, out.toByteArray())
                } catch (e: IOException) {
                    Log.w("Favorite", "Could not write icon")
                }
            }
        }
    }
}