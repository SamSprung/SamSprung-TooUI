package com.eightbit.samsprung.drawer.panels

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.MessageQueue
import com.eightbit.samsprung.SamSprungOverlay
import java.lang.ref.SoftReference
import java.util.LinkedList

class DesktopBinder(
    launcher: SamSprungOverlay, shortcuts: ArrayList<WidgetInfo?>?,
    appWidgets: ArrayList<PanelWidgetInfo>?
) : Handler(Looper.getMainLooper()), MessageQueue.IdleHandler {
    private val mShortcuts: ArrayList<WidgetInfo?>? = shortcuts
    private val mAppWidgets: LinkedList<PanelWidgetInfo>
    private val mLauncher: SoftReference<SamSprungOverlay> = SoftReference(launcher)
    var mTerminate = false
    fun startBindingItems() {
        obtainMessage(MESSAGE_BIND_ITEMS, 0, mShortcuts!!.size).sendToTarget()
    }

    fun startBindingAppWidgetsWhenIdle() {
        // Ask for notification when message queue becomes idle
        val messageQueue = Looper.myQueue()
        messageQueue.addIdleHandler(this)
    }

    override fun queueIdle(): Boolean {
        // Queue is idle, so start binding items
        startBindingAppWidgets()
        return false
    }

    private fun startBindingAppWidgets() {
        obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget()
    }

    override fun handleMessage(msg: Message) {
        val launcher = mLauncher.get()
        if (launcher == null || mTerminate) {
            return
        }
        when (msg.what) {
            MESSAGE_BIND_ITEMS -> {
                launcher.bindItems(this, mShortcuts, msg.arg1, msg.arg2)
            }
            MESSAGE_BIND_APPWIDGETS -> {
                launcher.bindAppWidgets(this, mAppWidgets)
            }
        }
    }

    companion object {
        const val MESSAGE_BIND_ITEMS = 0x1
        const val MESSAGE_BIND_APPWIDGETS = 0x2

        // Number of items to bind in every pass
        const val ITEMS_COUNT = 6
    }

    init {
        // Sort widgets so active workspace is bound first
        val size = appWidgets!!.size
        mAppWidgets = LinkedList()
        for (i in 0 until size) {
            val appWidgetInfo = appWidgets[i]
            // if (appWidgetInfo.screen == 0) {
            //     mAppWidgets.addFirst(appWidgetInfo)
            // } else {
            mAppWidgets.addLast(appWidgetInfo)
            // }
        }
    }
}