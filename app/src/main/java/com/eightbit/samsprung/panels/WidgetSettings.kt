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

import android.net.Uri
import android.provider.BaseColumns
import com.eightbit.samsprung.panels.WidgetProvider

/**
 * Settings related utilities.
 */
class WidgetSettings {
    interface BaseLauncherColumns : BaseColumns {
        companion object {
            /**
             * Descriptive name of the gesture that can be displayed to the user.
             * <P>Type: TEXT</P>
             */
            const val TITLE = "title"

            /**
             * The Intent URL of the gesture, describing what it points to. This
             * value is given to [android.content.Intent.parseUri] to create
             * an Intent that can be launched.
             * <P>Type: TEXT</P>
             */
            const val INTENT = "intent"

            /**
             * The type of the gesture
             *
             * <P>Type: INTEGER</P>
             */
            const val ITEM_TYPE = "itemType"

            /**
             * The gesture is an application
             */
            const val ITEM_TYPE_APPLICATION = 0

            /**
             * The gesture is an application created shortcut
             */
            const val ITEM_TYPE_SHORTCUT = 1

            /**
             * The icon type.
             * <P>Type: INTEGER</P>
             */
            const val ICON_TYPE = "iconType"

            /**
             * The icon is a resource identified by a package name and an integer id.
             */
            const val ICON_TYPE_RESOURCE = 0

            /**
             * The icon is a bitmap.
             */
            const val ICON_TYPE_BITMAP = 1

            /**
             * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
             * <P>Type: BLOB</P>
             */
            const val ICON = "icon"
        }
    }

    /**
     * Favorites. When changing these values, be sure to update
     * LauncherAppWidgetBinder as needed.
     */
    object Favorites : BaseLauncherColumns {
        /**
         * The content:// style URL for this table
         */
        val CONTENT_URI = Uri.parse(
            "content://" +
                    WidgetProvider.Companion.AUTHORITY + "/" + WidgetProvider.Companion.TABLE_FAVORITES +
                    "?" + WidgetProvider.Companion.PARAMETER_NOTIFY + "=true"
        )

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
        val CONTENT_URI_NO_NOTIFICATION = Uri.parse(
            "content://" +
                    WidgetProvider.Companion.AUTHORITY + "/" + WidgetProvider.Companion.TABLE_FAVORITES +
                    "?" + WidgetProvider.Companion.PARAMETER_NOTIFY + "=false"
        )

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        fun getContentUri(id: Long, notify: Boolean): Uri {
            return Uri.parse(
                "content://" + WidgetProvider.Companion.AUTHORITY +
                        "/" + WidgetProvider.Companion.TABLE_FAVORITES + "/" + id + "?" +
                        WidgetProvider.Companion.PARAMETER_NOTIFY + "=" + notify
            )
        }

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        const val CONTAINER = "container"

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        const val CONTAINER_DESKTOP = -100

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        const val SPANX = "spanX"

        /**
         * The Y span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        const val SPANY = "spanY"

        /**
         * The favorite is a widget
         */
        const val ITEM_TYPE_APPWIDGET = 4

        /**
         * The favorite is a clock
         */
        const val ITEM_TYPE_WIDGET_CLOCK = 1000

        /**
         * The favorite is a photo frame
         */
        const val ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002

        /**
         * The appWidgetId of the widget
         *
         * <P>Type: INTEGER</P>
         */
        const val APPWIDGET_ID = "appWidgetId"

        /**
         * The URI associated with the favorite. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        const val URI = "uri"
    }
}