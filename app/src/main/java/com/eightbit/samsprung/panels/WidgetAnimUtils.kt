/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.view.View
import android.view.ViewTreeObserver.OnDrawListener
import java.util.*

object WidgetAnimUtils {
    var sAnimators = HashSet<Animator>()
    var sEndAnimListener: Animator.AnimatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            sAnimators.remove(animation)
        }

        override fun onAnimationCancel(animation: Animator) {
            sAnimators.remove(animation)
        }
    }

    fun cancelOnDestroyActivity(a: Animator) {
        sAnimators.add(a)
        a.addListener(sEndAnimListener)
    }

    // Helper method. Assumes a draw is pending, and that if the animation's duration is 0
    // it should be cancelled
    fun startAnimationAfterNextDraw(animator: Animator, view: View) {
        view.viewTreeObserver.addOnDrawListener(object : OnDrawListener {
            private var mStarted = false
            override fun onDraw() {
                if (mStarted) return
                mStarted = true
                // Use this as a signal that the animation was cancelled
                if (animator.duration == 0L) {
                    return
                }
                animator.start()
                val listener: OnDrawListener = this
                view.post { view.viewTreeObserver.removeOnDrawListener(listener) }
            }
        })
    }

    fun onDestroyActivity() {
        val animators = HashSet(sAnimators)
        for (a in animators) {
            if (a.isRunning) {
                a.cancel()
            } else {
                sAnimators.remove(a)
            }
        }
    }

    fun createAnimatorSet(): AnimatorSet {
        val anim = AnimatorSet()
        cancelOnDestroyActivity(anim)
        return anim
    }

    fun ofFloat(target: View?, vararg values: Float): ValueAnimator {
        val anim = ValueAnimator()
        anim.setFloatValues(*values)
        cancelOnDestroyActivity(anim)
        return anim
    }

    fun ofFloat(target: View, propertyName: String?, vararg values: Float): ObjectAnimator {
        val anim = ObjectAnimator()
        anim.target = target
        anim.setPropertyName(propertyName!!)
        anim.setFloatValues(*values)
        cancelOnDestroyActivity(anim)
        FirstFrameAnimatorHelper(anim, target)
        return anim
    }

    fun ofPropertyValuesHolder(
        target: View,
        vararg values: PropertyValuesHolder?
    ): ObjectAnimator {
        val anim = ObjectAnimator()
        anim.target = target
        anim.setValues(*values)
        cancelOnDestroyActivity(anim)
        FirstFrameAnimatorHelper(anim, target)
        return anim
    }

    fun ofPropertyValuesHolder(
        target: Any?,
        view: View, vararg values: PropertyValuesHolder?
    ): ObjectAnimator {
        val anim = ObjectAnimator()
        anim.target = target
        anim.setValues(*values)
        cancelOnDestroyActivity(anim)
        FirstFrameAnimatorHelper(anim, view)
        return anim
    }
}