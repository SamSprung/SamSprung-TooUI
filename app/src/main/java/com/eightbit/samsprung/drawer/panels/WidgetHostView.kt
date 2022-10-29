/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.eightbit.samsprung.drawer.panels

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.eightbit.samsprung.R

/**
 * {@inheritDoc}
 */
class WidgetHostView(context: Context) : AppWidgetHostView(context) {
    private val mInflater: LayoutInflater = context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    ) as LayoutInflater
    override fun getErrorView(): View {
        return mInflater.inflate(R.layout.appwidget_error, this, false)
    }
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> { }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { }
        }
        return false
    }
}