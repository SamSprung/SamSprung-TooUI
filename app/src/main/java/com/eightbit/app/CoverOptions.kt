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

package com.eightbit.app

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Rect
import android.view.View

class CoverOptions(private var bounds: Rect?) {

    fun getActivityOptions(display: Int): ActivityOptions {
        return ActivityOptions.makeBasic().setLaunchDisplayId(display).setLaunchBounds(bounds)
    }

    fun getAnimatedOptions(display: Int, anchor: View?, intent: Intent?): ActivityOptions {
        return if (null != intent?.sourceBounds) {
            ActivityOptions.makeScaleUpAnimation(
                anchor,
                intent.sourceBounds!!.left,
                intent.sourceBounds!!.top,
                intent.sourceBounds!!.width(),
                intent.sourceBounds!!.height()
            ).setLaunchDisplayId(display).setLaunchBounds(bounds)
        } else {
            getActivityOptions(display)
        }
    }
}
