package com.eightbit.material

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
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

    fun buildTickerBar(msg: String?, drawable: Int, length: Int): Snackbar {
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
                layout!!.setPadding(0, top, 0, bottom)
                super.onDismissed(snackbar, event)
            }

            override fun onShown(snackbar: Snackbar) {
                if (null == layout) {
                    super.onShown(snackbar)
                    return
                }
                val adjusted = top + snackbar.view.measuredHeight
                layout!!.setPadding(0, adjusted, 0, bottom)
                super.onShown(snackbar)
            }
        })
        val params = snackbar.view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackbar.view.layoutParams = params
        return snackbar
    }

    fun buildTickerBar(msg: String?): Snackbar {
        return buildTickerBar(msg, R.drawable.ic_baseline_samsprung_24dp, Snackbar.LENGTH_LONG)
    }
}