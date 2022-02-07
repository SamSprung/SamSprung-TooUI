package com.eightbit.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java.util.*
import kotlin.math.abs

// Rewritten from https://stackoverflow.com/a/49527237/461982
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
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
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

class SnapHorizontalScrollView : HorizontalScrollView {
    private val mItems = ArrayList<LinearLayout>()
    private var mGestureDetector: GestureDetector? = null
    private var mActiveFeature = 0

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    fun removeFeatureItem(featureLayout: LinearLayout) {
        mItems.remove(featureLayout)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun addFeatureItem(featureLayout: LinearLayout) {
        mItems.add(featureLayout)
        setOnTouchListener { v: View, event: MotionEvent ->
            //If the user swipes
            if (mGestureDetector!!.onTouchEvent(event)) {
                return@setOnTouchListener true
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                val scrollX = scrollX
                val featureWidth = v.measuredWidth
                mActiveFeature = (scrollX + featureWidth / 2) / featureWidth
                val scrollTo = mActiveFeature * featureWidth
                smoothScrollTo(scrollTo, 0)
                return@setOnTouchListener true
            } else {
                return@setOnTouchListener false
            }
        }
        mGestureDetector = GestureDetector(featureLayout.context, SnapGestureDetector())
    }

    internal inner class SnapGestureDetector : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                // right to left
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureWidth = measuredWidth
                    mActiveFeature =
                        if (mActiveFeature < mItems.size - 1) mActiveFeature + 1 else mItems.size - 1
                    smoothScrollTo(mActiveFeature * featureWidth, 0)
                    return true
                } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureWidth = measuredWidth
                    mActiveFeature = if (mActiveFeature > 0) mActiveFeature - 1 else 0
                    smoothScrollTo(mActiveFeature * featureWidth, 0)
                    return true
                }
            } catch (ignored : Exception) { }
            return false
        }
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE = 10
        private const val SWIPE_THRESHOLD_VELOCITY = 250
    }
}