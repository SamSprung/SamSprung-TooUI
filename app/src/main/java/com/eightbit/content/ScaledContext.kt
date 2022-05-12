/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed by AbandonedCart"
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
    companion object {
        fun getDisplayParams(context: Context): IntArray {
            val mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = mWindowManager.currentWindowMetrics.bounds
            return intArrayOf(metrics.width(), metrics.height())
        }

        fun internal(context: Context, density: Float): ScaledContext {
            val resources = context.resources
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
            return ScaledContext(context)
        }

        fun external(context: Context): ScaledContext {
            val resources = context.resources
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
            return ScaledContext(context)
        }

        fun restore(context: Context, display: Int): Context {
            context.resources.displayMetrics.setToDefaults()
            val displayManager = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val displayContext = context.createDisplayContext(displayManager.getDisplay(display))
            val wm = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
            val contextWrapper = object : ContextThemeWrapper(displayContext, context.theme) {
                override fun getSystemService(name: String): Any? {
                    return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
                }
            }
            contextWrapper.resources.displayMetrics.setToDefaults()
            return contextWrapper

        }

        fun cover(context: Context) : Context {
            val displayManager = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val displayContext = context.createDisplayContext(displayManager.getDisplay(1))
            val wm = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
            return object : ContextThemeWrapper(displayContext, context.theme) {
                override fun getSystemService(name: String): Any? {
                    return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
                }
            }
        }

        fun cover(context: Context, theme: Int) : Context {
            context.setTheme(theme)
            return cover(context)
        }
    }
}