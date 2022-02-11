/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.animation.*
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnDrawListener

/*
 *  This is a helper class that listens to updates from the corresponding animation.
 *  For the first two frames, it adjusts the current play time of the animation to
 *  prevent jank at the beginning of the animation
 */
class FirstFrameAnimatorHelper : AnimatorListenerAdapter, AnimatorUpdateListener {
    private val mTarget: View
    private var mStartFrame: Long = 0
    private var mStartTime: Long = -1
    private var mHandlingOnAnimationUpdate = false
    private var mAdjustedSecondFrameTime = false

    constructor(animator: ValueAnimator, target: View) {
        mTarget = target
        animator.addUpdateListener(this)
    }

    constructor(vpa: ViewPropertyAnimator, target: View) {
        mTarget = target
        vpa.setListener(this)
    }

    // only used for ViewPropertyAnimators
    override fun onAnimationStart(animation: Animator) {
        val va = animation as ValueAnimator
        va.addUpdateListener(this@FirstFrameAnimatorHelper)
        onAnimationUpdate(va)
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val currentTime = System.currentTimeMillis()
        if (mStartTime == -1L) {
            mStartFrame = sGlobalFrameCounter
            mStartTime = currentTime
        }
        val isFinalFrame = java.lang.Float.compare(1f, animation.animatedFraction) == 0
        if (!mHandlingOnAnimationUpdate &&
            sVisible && // If the current play time exceeds the duration, or the animated fraction is 1,
            // the animation will get finished, even if we call setCurrentPlayTime -- therefore
            // don't adjust the animation in that case
            animation.currentPlayTime < animation.duration && !isFinalFrame
        ) {
            mHandlingOnAnimationUpdate = true
            val frameNum = sGlobalFrameCounter - mStartFrame
            // If we haven't drawn our first frame, reset the time to t = 0
            // (give up after MAX_DELAY ms of waiting though - might happen, for example, if we
            // are no longer in the foreground and no frames are being rendered ever)
            if (frameNum == 0L && currentTime < mStartTime + MAX_DELAY) {
                // The first frame on animations doesn't always trigger an invalidate...
                // force an invalidate here to make sure the animation continues to advance
                mTarget.rootView.invalidate()
                animation.currentPlayTime = 0
                // For the second frame, if the first frame took more than 16ms,
                // adjust the start time and pretend it took only 16ms anyway. This
                // prevents a large jump in the animation due to an expensive first frame
            } else if (frameNum == 1L && currentTime < mStartTime + MAX_DELAY &&
                !mAdjustedSecondFrameTime && currentTime > mStartTime + IDEAL_FRAME_DURATION
            ) {
                animation.currentPlayTime = IDEAL_FRAME_DURATION.toLong()
                mAdjustedSecondFrameTime = true
            } else {
                if (frameNum > 1) {
                    mTarget.post { animation.removeUpdateListener(this@FirstFrameAnimatorHelper) }
                }
                if (DEBUG) print(animation)
            }
            mHandlingOnAnimationUpdate = false
        } else {
            if (DEBUG) print(animation)
        }
    }

    fun print(animation: ValueAnimator) {
        val flatFraction = animation.currentPlayTime / animation.duration
            .toFloat()
        Log.d(
            "FirstFrameAnimatorHelper", sGlobalFrameCounter.toString() +
                    "(" + (sGlobalFrameCounter - mStartFrame) + ") " + mTarget + " dirty? " +
                    mTarget.isDirty + " " + flatFraction + " " + this + " " + animation
        )
    }

    companion object {
        private const val DEBUG = false
        private const val MAX_DELAY = 1000
        private const val IDEAL_FRAME_DURATION = 16
        private var sGlobalDrawListener: OnDrawListener? = null
        private var sGlobalFrameCounter: Long = 0
        private var sVisible = false
        fun setIsVisible(visible: Boolean) {
            sVisible = visible
        }

        fun initializeDrawListener(view: View) {
            if (sGlobalDrawListener != null) {
                view.viewTreeObserver.removeOnDrawListener(sGlobalDrawListener)
            }
            sGlobalDrawListener = object : OnDrawListener {
                private var mTime = System.currentTimeMillis()
                override fun onDraw() {
                    sGlobalFrameCounter++
                    if (DEBUG) {
                        val newTime = System.currentTimeMillis()
                        Log.d("FirstFrameAnimatorHelper", "TICK " + (newTime - mTime))
                        mTime = newTime
                    }
                }
            }
            view.viewTreeObserver.addOnDrawListener(sGlobalDrawListener)
            sVisible = true
        }
    }
}