/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
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
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

package com.eightbit.material

import android.content.res.Configuration
import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.transition.TransitionManager
import com.eightbit.samsprung.R
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

class IconifiedSnackbar {
    private var mActivity: WeakReference<AppCompatActivity>
    private var layout: ViewGroup? = null

    constructor(activity: AppCompatActivity) {
        mActivity = WeakReference(activity)
    }

    constructor(activity: AppCompatActivity, layout: ViewGroup?) {
        mActivity = WeakReference(activity)
        this.layout = layout
    }

    fun buildSnackbar(msg: String?, drawable: Int, length: Int, anchor: View?): Snackbar {
        val snackbar = Snackbar.make(
            mActivity.get()!!.findViewById(R.id.coordinator), msg!!, length
        )
        val snackbarLayout = snackbar.view
        val textView = snackbarLayout.findViewById<TextView>(
            R.id.snackbar_text
        )
        when (mActivity.get()!!.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                snackbar.setBackgroundTint(
                    ContextCompat.getColor(mActivity.get()!!, R.color.snackbar_dark)
                )
                textView.setTextColor(
                    ContextCompat.getColor(mActivity.get()!!, R.color.primary_text_dark)
                )
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                snackbar.setBackgroundTint(
                    ContextCompat.getColor(mActivity.get()!!, android.R.color.darker_gray)
                )
                textView.setTextColor(
                    ContextCompat.getColor(mActivity.get()!!, R.color.primary_text_light)
                )
            }
        }
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            drawable, 0, 0, 0
        )
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.compoundDrawablePadding = textView.resources
            .getDimensionPixelOffset(R.dimen.snackbar_icon_padding)
        val params = snackbarLayout.layoutParams as CoordinatorLayout.LayoutParams
        params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
        snackbar.view.layoutParams = params
        snackbar.anchorView = anchor
        return snackbar
    }

    fun buildSnackbar(msgRes: Int, drawable: Int, length: Int): Snackbar {
        return buildSnackbar(mActivity.get()!!.getString(msgRes), drawable, length, null)
    }

    fun buildTickerBar(msg: String, drawable: Int, length: Int): Snackbar {
        val snackbar = buildSnackbar(msg, drawable, length, null)
            .addCallback(object : Snackbar.Callback() {
            val top = if (null != layout) layout!!.paddingTop else 0
            val bottom = if (null != layout) layout!!.paddingBottom else 0
            override fun onDismissed(snackbar: Snackbar, event: Int) {
                if (null == layout) {
                    super.onDismissed(snackbar, event)
                    return
                }
                TransitionManager.beginDelayedTransition(layout!!)
                layout!!.updatePadding(top = top, bottom = bottom)
                super.onDismissed(snackbar, event)
            }

            override fun onShown(snackbar: Snackbar) {
                if (null == layout) {
                    super.onShown(snackbar)
                    return
                }
                val adjusted = top + snackbar.view.measuredHeight
                layout!!.updatePadding(top = adjusted, bottom = bottom)
                super.onShown(snackbar)
            }
        })
        val params = snackbar.view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackbar.view.layoutParams = params
        return snackbar
    }

    fun buildTickerBar(msg: String, value: Int): Snackbar {
        return try {
            mActivity.get()!!.resources.getResourceTypeName(value)
            buildTickerBar(msg, value, Snackbar.LENGTH_LONG)
        } catch (exception: Exception) {
            buildTickerBar(msg, R.drawable.ic_baseline_samsprung_24dp, value)
        }
    }

    fun buildTickerBar(msg: String): Snackbar {
        return buildTickerBar(msg, R.drawable.ic_baseline_samsprung_24dp, Snackbar.LENGTH_LONG)
    }
}
