/*
 * ====================================================================
 * Copyright (c) 2012-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.content

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.view.ContextThemeWrapper
import android.view.WindowManager

class ScaledContext(base: Context) : ContextWrapper(base) {
    fun getDisplayParams(): IntArray {
        val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = mWindowManager.currentWindowMetrics.bounds
        return intArrayOf(metrics.width(), metrics.height())
    }

    fun internal(density: Float): ScaledContext {
        val resources = resources
        val metrics = resources.displayMetrics
        val orientation = resources.configuration.orientation
        metrics.density = density // 2
        metrics.densityDpi = 360 // 360
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = 2640 // 2640
            metrics.widthPixels = 1080 // 1080
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 1080 // 1080
            metrics.widthPixels = 2640 // 2640
        }
        metrics.scaledDensity = density // 2
        metrics.xdpi = 425f // 425
        metrics.ydpi = 425f // 425
        metrics.setTo(metrics)
        return ScaledContext(this)
    }

    private fun screen() : Context {
        createDisplayContext(
            (getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(0)
        ).run {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            return object : ContextThemeWrapper(this, theme) {
                override fun getSystemService(name: String): Any? {
                    return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
                }
            }
        }
    }

    private fun screen(density: Float): Context {
        val displayContext = screen()
        val resources = displayContext.resources
        val metrics = resources.displayMetrics
        val orientation = resources.configuration.orientation
        metrics.density = density // 2
        metrics.densityDpi = 360 // 360
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = 2640 // 2640
            metrics.widthPixels = 1080 // 1080
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 1080 // 1080
            metrics.widthPixels = 2640 // 2640
        }
        metrics.scaledDensity = density // 2
        metrics.xdpi = 425f // 425
        metrics.ydpi = 425f // 425
        metrics.setTo(metrics)
        return ScaledContext(displayContext)
    }

    fun screen(density: Float, theme: Int): Context {
        setTheme(theme)
        return screen(density)
    }

    fun external(): ScaledContext {
        val resources = resources
        val metrics = resources.displayMetrics
        val orientation = resources.configuration.orientation
        metrics.density = 1f
        metrics.densityDpi = 160 // 160
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = 512 // 512
            metrics.widthPixels = 260 // 260
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 260 // 260
            metrics.widthPixels = 512 // 512
        }
        metrics.scaledDensity = 1f
        metrics.xdpi = 302f // 302
        metrics.ydpi = 302f // 302
        metrics.setTo(metrics)
        return ScaledContext(this)
    }

    fun restore(display: Int): Context {
        resources.displayMetrics.setToDefaults()
        val contextWrapper = createDisplayContext(
            (getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(display)
        ).run {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            object : ContextThemeWrapper(this, theme) {
                override fun getSystemService(name: String): Any? {
                    return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
                }
            }
        }
        contextWrapper.resources.displayMetrics.setToDefaults()
        return contextWrapper

    }

    fun cover() : Context {
        createDisplayContext(
            (getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(1)
        ).run {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            return object : ContextThemeWrapper(this, theme) {
                override fun getSystemService(name: String): Any? {
                    return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
                }
            }
        }
    }

    fun cover(theme: Int) : Context {
        setTheme(theme)
        return cover()
    }
}
