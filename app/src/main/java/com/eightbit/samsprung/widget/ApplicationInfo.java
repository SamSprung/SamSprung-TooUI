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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.eightbit.samsprung.WidgetSettings;

/**
 * Represents a launchable application. An application is made of a name (or title),
 * an intent and an icon.
 */
class ApplicationInfo extends WidgetInfo {

    /**
     * The application name.
     */
    CharSequence title;

    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * The application icon.
     */
    Drawable icon;

    /**
     * When set to true, indicates that the icon has been resized.
     */
    boolean filtered;

    /**
     * Indicates whether the icon comes from an application's resource (if false)
     * or from a custom Bitmap (if true.)
     */
    boolean customIcon;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    Intent.ShortcutIconResource iconResource;

    ApplicationInfo() {
        itemType = WidgetSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    @Override
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);

        String titleStr = title != null ? title.toString() : null;
        values.put(WidgetSettings.BaseLauncherColumns.TITLE, titleStr);

        String uri = intent != null ? intent.toUri(0) : null;
        values.put(WidgetSettings.BaseLauncherColumns.INTENT, uri);

        if (customIcon) {
            values.put(WidgetSettings.BaseLauncherColumns.ICON_TYPE,
                    WidgetSettings.BaseLauncherColumns.ICON_TYPE_BITMAP);
            Bitmap bitmap = ((FastBitmapDrawable) icon).getBitmap();
            writeBitmap(values, bitmap);
        } else {
            values.put(WidgetSettings.BaseLauncherColumns.ICON_TYPE,
                    WidgetSettings.BaseLauncherColumns.ICON_TYPE_RESOURCE);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return title.toString();
    }
}
