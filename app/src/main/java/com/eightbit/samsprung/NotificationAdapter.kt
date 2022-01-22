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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class NotificationAdapter(
    private var activity: Activity,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>(),
    NotificationListener.NotificationsChangedListener,
    AccessibilityHandler.EventsChangedListener {

    private var notices: ArrayList<SamSprungNotice> = arrayListOf()

    private fun setNotices(notices: ArrayList<SamSprungNotice>) {
        this.notices = notices
    }

    override fun getItemCount(): Int {
        return notices.size
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private fun getItem(i: Int): SamSprungNotice {
        return notices[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        return SimpleViewHolder(parent, listener)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notice!!, position)
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notice!!, position)
        }
        holder.bind(getItem(position))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.icon)
        private val linesText: TextView = itemView.findViewById(R.id.lines)
        var notice: SamSprungNotice? = null
        fun bind(notice: SamSprungNotice) {
            this.notice = notice
            iconView.setImageDrawable(notice.getDrawable())
            linesText.text = notice.getString().trim()
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnNoticeClickListener?
    ) : NoticeViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.notification_card,
            parent, false
        ), listener
    )

    interface OnNoticeClickListener {
        fun onNoticeClicked(notice: SamSprungNotice, position: Int)
    }

    private var notifications: ArrayList<Notification> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshNotifications() {
        Executors.newSingleThreadExecutor().execute {
            val groups: HashMap<String, SamSprungNotice> = hashMapOf()
            for (notification: Notification in notifications) {
                if (groups.containsKey(notification.group)
                    && null != groups[notification.group]) {
                    val notice : SamSprungNotice = groups[notification.group]!!
                    if (null != notification.extras
                        && null != notification.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES)) {
                        notice.setString(
                            Arrays.toString(
                                notification.extras.getCharSequenceArray(
                                    NotificationCompat.EXTRA_TEXT_LINES)
                            ))
                    } else if (null != notification.tickerText) {
                        notice.setString(notification.tickerText.toString())
                    }
                    groups.replace(notification.group, notice)
                } else {
                    val notice = SamSprungNotice()
                    when {
                        null != notification.getLargeIcon() -> notice.setDrawable(
                            notification.getLargeIcon().loadDrawable(activity)
                        )
                        null != notification.smallIcon -> notice.setDrawable(
                            notification.smallIcon.loadDrawable(activity)
                        )
                    }
                    if (null != notification.extras
                        && null != notification.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES)) {
                        notice.setString(
                            Arrays.toString(
                                notification.extras.getCharSequenceArray(
                                    NotificationCompat.EXTRA_TEXT_LINES)
                            ))
                    } else if (null != notification.tickerText) {
                        notice.setString(notification.tickerText.toString())
                    }
                    if (null != notification.contentIntent)
                        notice.setIntentSender(notification.contentIntent.intentSender)
                    if (null != notification.group) {
                        groups[notification.group] = notice
                    } else {
                        groups[activity.packageName] = notice
                    }

                }
            }
            val notices: ArrayList<SamSprungNotice> = ArrayList(groups.values)

            this.setNotices(notices)
            activity.runOnUiThread { this.notifyDataSetChanged() }
        }
    }

    override fun onActiveNotifications(activeNotifications: ArrayList<StatusBarNotification>) {
        var notification: Notification
        for (sbn: StatusBarNotification in activeNotifications) {
            notification = sbn.notification
            if (!notifications.contains(notification)) {
                notifications.add(sbn.notification)
                refreshNotifications()
            }
        }
    }
    override fun onSnoozedNotifications(snoozedNotifications: ArrayList<StatusBarNotification>) {
        var notification: Notification
        for (sbn: StatusBarNotification in snoozedNotifications) {
            notification = sbn.notification
            if (notifications.contains(notification)) {
                notifications.remove(sbn.notification)
                refreshNotifications()
            }
        }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (null != sbn) {
            notifications.add(sbn.notification)
            refreshNotifications()
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (null == sbn) return
        for (notice: Notification in notifications) {
            if (notice == sbn.notification) {
                notifications.remove(sbn.notification)
                refreshNotifications()
                break
            }
        }
    }

    override fun onEventPosted(notification: Notification) {
        if (!notifications.contains(notification)) {
            notifications.add(notification)
            refreshNotifications()
        }
    }
}