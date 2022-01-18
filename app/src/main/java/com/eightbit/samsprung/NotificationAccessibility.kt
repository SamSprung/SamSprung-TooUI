package com.eightbit.samsprung

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.view.accessibility.AccessibilityEvent

class NotificationAccessibility : AccessibilityService() {
    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val data = e.parcelableData
            if (data is Notification) {
                SamSprung.notifications.add(data)
            }
        }
    }

    override fun onInterrupt() {
        // TODO Auto-generated method stub
    }
}