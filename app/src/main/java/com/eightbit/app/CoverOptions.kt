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