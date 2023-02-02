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

package com.eightbit.samsprung

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AccessibilityObserver : AccessibilityService() {

    companion object {
        private lateinit var observerInstance: AccessibilityObserver
        private var isConnected: Boolean = false
        fun getInstance() : AccessibilityObserver? {
            return if (isConnected) observerInstance else null
        }
    }

    init {
        observerInstance = this
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
        info.notificationTimeout = 100
        serviceInfo = info
        isConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
//        if (AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED == event.eventType) {
//            val notification = event.parcelableData
//            if (notification is Notification) { }
//        }
    }

    override fun onInterrupt() {
        if (null == getInstance()) observerInstance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        return super.onUnbind(intent)
    }
}
