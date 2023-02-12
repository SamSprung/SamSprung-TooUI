/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
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

package com.eightbit.view

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

open class OnSwipeTouchListener(ctx: Context) : OnTouchListener {

    private val gestureDetector: GestureDetector

    companion object {
        private const val SWIPE_VERTICAL_THRESHOLD = 125
        private const val SWIPE_HORIZONTAL_THRESHOLD = 175
        private const val SWIPE_VELOCITY_THRESHOLD = 250
    }

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_MOVE) {
            gestureDetector.onTouchEvent(event)
        } else {
            v.performClick()
            false
        }
    }

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_HORIZONTAL_THRESHOLD
                        && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        return if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else if (abs(diffY) > SWIPE_VERTICAL_THRESHOLD
                    && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    return if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight() : Boolean {
        return false
    }

    open fun onSwipeLeft() : Boolean {
        return false
    }

    open fun onSwipeTop() : Boolean {
        return false
    }

    open fun onSwipeBottom() : Boolean {
        return false
    }
}
