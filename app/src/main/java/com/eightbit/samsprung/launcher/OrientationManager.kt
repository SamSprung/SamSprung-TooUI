package com.eightbit.samsprung.launcher

import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout

class OrientationManager(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val orientationManager = LinearLayout(context)

    fun addOrientationManager(orientation: Int) {
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = orientation
        windowManager.addView(orientationManager, orientationLayout)
        orientationManager.visibility = View.VISIBLE
    }

    fun removeOrientationManager() {
        windowManager.removeViewImmediate(orientationManager)
    }
}