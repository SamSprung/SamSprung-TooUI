package com.eightbit.samsprung

import androidx.core.util.Consumer
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

class LayoutStateChangeCallback : Consumer<WindowLayoutInfo> {

    interface FoldingFeatureListener {
        fun onHalfOpened()
        fun onSeparating()
        fun onFlat()
        fun onNormal()
    }

    private var listener: FoldingFeatureListener? = null

    fun setListener(listener: FoldingFeatureListener) {
        this.listener = listener
    }

    override fun accept(newLayoutInfo: WindowLayoutInfo) {
        for (feature in newLayoutInfo.displayFeatures) {
            if (feature is FoldingFeature) {
                when (feature.state) {
                    FoldingFeature.State.HALF_OPENED -> {
                        listener?.onHalfOpened()
                    }
                    FoldingFeature.State.FLAT -> {
                        if (feature.isSeparating) {
                            listener?.onSeparating()
                        } else {
                            listener?.onFlat()
                        }
                    }
                    else -> {
                        listener?.onNormal()
                    }
                }
            }
        }
    }
}