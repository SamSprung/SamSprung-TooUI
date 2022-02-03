package com.eightbit.samsprung

/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class AccessibilityObserver : AccessibilityService() {

    companion object {
        private lateinit var observerInstance: AccessibilityObserver
        private var isConnected: Boolean = false
        fun getObserver() : AccessibilityObserver? {
            return if (isConnected) observerInstance else null
        }
        fun executeButtonBack() {
            getObserver()?.performGlobalAction(GLOBAL_ACTION_BACK)
        }
        private var cachedInputMethod: String? = null
        private fun getInputMethod(context: Context): String {
            return Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
        }
        @JvmStatic
        fun enableKeyboard(context: Context) {
            if (null == getObserver()) return
            if (!getInputMethod(context).startsWith(BuildConfig.APPLICATION_ID, true))
                cachedInputMethod = getInputMethod(context)
            val mInputMethodProperties = (context.getSystemService(INPUT_METHOD_SERVICE)
                    as InputMethodManager).enabledInputMethodList
            for (i in 0 until mInputMethodProperties.size) {
                val imi = mInputMethodProperties[i]
                if (imi.id.startsWith(BuildConfig.APPLICATION_ID, true)) {
                    getObserver()!!.softKeyboardController.switchToInputMethod(imi.id)
                }
            }
        }
        fun disableKeyboard(context: Context) {
            if (null == getObserver() || null == cachedInputMethod) return
            if (getInputMethod(context).startsWith(BuildConfig.APPLICATION_ID, true)) {
                val parameters = cachedInputMethod!!.split('/')
                val defaultKeyboard = ComponentName(parameters[0], parameters[1])
                val mInputMethodProperties = (context.getSystemService(INPUT_METHOD_SERVICE)
                        as InputMethodManager).enabledInputMethodList
                for (i in 0 until mInputMethodProperties.size) {
                    val imi = mInputMethodProperties[i]
                    if (imi.id.startsWith(defaultKeyboard.packageName, true)) {
                        getObserver()!!.softKeyboardController.switchToInputMethod(imi.id)
                    }
                }
            }
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
        isConnected = false
    }
}