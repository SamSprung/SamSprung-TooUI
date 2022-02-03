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

import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NotificationAdapter(
    private var activity: AppCompatActivity,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>(),
    NotificationObserver.NotificationsChangedListener {

    private var sbNotifications: ArrayList<StatusBarNotification> = arrayListOf()

    override fun getItemCount(): Int {
        return sbNotifications.size
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private fun getItem(i: Int): StatusBarNotification {
        return sbNotifications[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        return SimpleViewHolder(parent, listener, activity)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notice, position)
        }
        holder.itemView.setOnLongClickListener {
            if (null != holder.listener)
                holder.listener
                    .onNoticeLongClicked(holder.notice, position)
            else
                false
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notice, position)
        }
        holder.iconView.setOnLongClickListener {
            if (null != holder.listener)
                holder.listener
                    .onNoticeLongClicked(holder.notice, position)
            else
                false
        }
        holder.bind(getItem(position))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?, val activity: AppCompatActivity
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: AppCompatImageView = itemView.findViewById(R.id.icon)
        private val linesText: TextView = itemView.findViewById(R.id.lines)
        lateinit var notice: StatusBarNotification
        fun bind(notice: StatusBarNotification) {
            this.notice = notice
            val notification = notice.notification
            when {
                null != notification.getLargeIcon() -> iconView.setImageDrawable(
                    notification.getLargeIcon().loadDrawable(activity)
                )
                null != notification.smallIcon -> iconView.setImageDrawable(
                    notification.smallIcon.loadDrawable(activity)
                )
            }
            if (null != notification.extras
                && null != notification.extras.getCharSequenceArray(
                    NotificationCompat.EXTRA_TEXT_LINES)) {
                linesText.text = (Arrays.toString(
                    notification.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES)
                ))
            } else if (null != notification.extras && null != notification.extras
                    .getCharSequence(NotificationCompat.EXTRA_TEXT)) {
                linesText.text = (notification.extras.getCharSequence(
                    NotificationCompat.EXTRA_TEXT).toString())
            } else if (null != notification.tickerText) {
                linesText.text = (notification.tickerText.toString())
            }
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnNoticeClickListener?,
        activity: AppCompatActivity
    ) : NoticeViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.notification_card,
            parent, false
        ), listener, activity
    )

    interface OnNoticeClickListener {
        fun onNoticeClicked(notice: StatusBarNotification, position: Int)
        fun onNoticeLongClicked(notice: StatusBarNotification, position: Int) : Boolean
    }

    override fun onActiveNotifications(activeNotifications: ArrayList<StatusBarNotification>) {
        for (sbn: StatusBarNotification in activeNotifications) {
            if (!sbNotifications.contains(sbn)) {
                sbNotifications.add(sbn)
                activity.runOnUiThread { this.notifyDataSetChanged() }
            }
        }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (null == sbn) return
        sbNotifications.add(sbn)
        activity.runOnUiThread { this.notifyDataSetChanged() }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (null == sbn) return
        for (sbNotice: StatusBarNotification in sbNotifications) {
            if (sbNotice == sbn) {
                sbNotifications.remove(sbNotice)
                activity.runOnUiThread { this.notifyDataSetChanged() }
                break
            }
        }
    }
}