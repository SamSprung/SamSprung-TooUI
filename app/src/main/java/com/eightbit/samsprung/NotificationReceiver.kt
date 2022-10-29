/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

package com.eightbit.samsprung

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

@SuppressLint("NotifyDataSetChanged")
class NotificationReceiver : NotificationListenerService() {

    companion object {
        private lateinit var receiverInstance: NotificationReceiver
        private var isConnected: Boolean = false
        fun getReceiver() : NotificationReceiver? {
            return if (isConnected) receiverInstance else null
        }
    }

    init {
        receiverInstance = this
    }

    private var mNotificationsListener: NotificationsListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        try {
            requestRebind(ComponentName(applicationContext, NotificationReceiver::class.java))
        } catch (ignored: Exception) { }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        getReceiver()?.mNotificationsListener?.onNotificationPosted(sbn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        getReceiver()?.mNotificationsListener?.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        getReceiver()?.mNotificationsListener?.onNotificationRemoved(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationRemoved(sbn, rankingMap)
        getReceiver()?.mNotificationsListener?.onNotificationRemoved(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        getReceiver()?.mNotificationsListener?.onNotificationRemoved(sbn)
    }

    fun setNotificationsListener(listener: NotificationsListener?) {
        getReceiver()?.mNotificationsListener = listener
        val notifications: ArrayList<StatusBarNotification> = arrayListOf()
        if (notifications.addAll(activeNotifications))
            getReceiver()?.mNotificationsListener?.onActiveNotifications(notifications)
    }

    interface NotificationsListener {
        fun onActiveNotifications(activeNotifications: ArrayList<StatusBarNotification>)
        fun onNotificationPosted(sbn: StatusBarNotification)
        fun onNotificationRemoved(sbn: StatusBarNotification)
    }
}
