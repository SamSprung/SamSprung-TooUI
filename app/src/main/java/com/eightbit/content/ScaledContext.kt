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
import com.eightbit.io.Debug

@Suppress("unused")
class ScaledContext(base: Context) : ContextWrapper(base) {
    private val ppiDisplay0 = if (Debug.isOppoDevice) 403f else 425f
    private val ppiDisplay1 = if (Debug.isOppoDevice) 250f else 302f
    private val portDisplay0 =  if (Debug.isOppoDevice) 2520 else 2640 // 2636

    fun getDisplayParams(): IntArray {
        val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = mWindowManager.maximumWindowMetrics.bounds
        return intArrayOf(metrics.width(), metrics.height())
    }

    fun internal(density: Float): ScaledContext {
        val resources = resources
        val metrics = resources.displayMetrics
        val orientation = resources.configuration.orientation
        metrics.density = density // 2
        metrics.densityDpi = 360
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = portDisplay0
            metrics.widthPixels = 1080
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 1080
            metrics.widthPixels = portDisplay0
        }
        metrics.scaledDensity = density // 2
        metrics.xdpi = ppiDisplay0
        metrics.ydpi = ppiDisplay0
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
        metrics.densityDpi = 360
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = portDisplay0
            metrics.widthPixels = 1080
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 1080
            metrics.widthPixels = portDisplay0
        }
        metrics.scaledDensity = density // 2
        metrics.xdpi = ppiDisplay0
        metrics.ydpi = ppiDisplay0
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
        metrics.densityDpi = 160
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (Debug.isOppoDevice) {
                metrics.heightPixels = 720
                metrics.widthPixels = 382
            } else {
                metrics.heightPixels = 512
                metrics.widthPixels = 260
            }
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Debug.isOppoDevice) {
                metrics.heightPixels = 382
                metrics.widthPixels = 720
            } else {
                metrics.heightPixels = 260
                metrics.widthPixels = 512
            }
        }
        metrics.scaledDensity = 1f
        metrics.xdpi = ppiDisplay1
        metrics.ydpi = ppiDisplay1
        metrics.setTo(metrics)
        return ScaledContext(this)
    }

    fun restore(displayNumber: Int): Context {
        resources.displayMetrics.setToDefaults()
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val display = try {
            displayManager.getDisplay(displayNumber)
        } catch (iae: IllegalArgumentException) {
            displayManager.getDisplay(0)
        }
        val contextWrapper = createDisplayContext(display).run {
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
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val diplay = try {
            displayManager.getDisplay(1)
        } catch (iae: IllegalArgumentException) {
            displayManager.getDisplay(0)
        }
        createDisplayContext(diplay).run {
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
