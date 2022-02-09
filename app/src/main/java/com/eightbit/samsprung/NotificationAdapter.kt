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
import android.graphics.Bitmap




class NotificationAdapter(
    private var activity: AppCompatActivity,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>(),
    NotificationReceiver.NotificationsListener {

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
                holder.listener.onNoticeClicked(holder.itemView, position, holder.notice)
        }
        holder.itemView.setOnLongClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeLongClicked(holder.itemView, position, holder.notice)
            else
                false
        }
        holder.bind(getItem(position))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?, val activity: AppCompatActivity
    ) : RecyclerView.ViewHolder(itemView) {
        private val iconView: AppCompatImageView = itemView.findViewById(R.id.icon)
        private val imageView: AppCompatImageView = itemView.findViewById(R.id.image)
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
            if (null != notification.extras) {
                if (notification.extras.containsKey(NotificationCompat.EXTRA_PICTURE)) {
                    imageView.visibility = View.VISIBLE
                    imageView.setImageBitmap(notification.extras.get(
                        NotificationCompat.EXTRA_PICTURE) as Bitmap)
                }
                if (notification.extras.containsKey(NotificationCompat.EXTRA_TEXT_LINES)) {
                    linesText.text = (Arrays.toString(notification.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES)
                    ))
                } else if (notification.extras.containsKey(NotificationCompat.EXTRA_TEXT)) {
                    linesText.text = (notification.extras.getCharSequence(
                        NotificationCompat.EXTRA_TEXT
                    ).toString())
                }
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onActiveNotifications(activeNotifications: ArrayList<StatusBarNotification>) {
        for (sbn: StatusBarNotification in activeNotifications) {
            var isRecent = false
            for (current in sbNotifications) {
                if (current.key == sbn.key) {
                    isRecent = true
                    break
                }
            }
            if (!isRecent) {
                sbNotifications.add(sbn)
                activity.runOnUiThread { this.notifyItemInserted(sbNotifications.size - 1) }
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (null == sbn) return
        var current = -1
        for (index in 0 until sbNotifications.size) {
            if (sbNotifications[index].key == sbn.key) {
                current = index
                break
            }
        }
        if (-1 == current) {
            sbNotifications.add(0, sbn)
            activity.runOnUiThread { this.notifyItemInserted(0) }
        } else {
            sbNotifications[current] = sbn
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (null == sbn) return
        for (index in 0 until sbNotifications.size) {
            if (sbNotifications[index].key == sbn.key) {
                sbNotifications.remove(sbNotifications[index])
                activity.runOnUiThread { this.notifyItemRemoved(index) }
                break
            }
        }
    }

    interface OnNoticeClickListener {
        fun onNoticeClicked(itemView: View, position: Int, notice: StatusBarNotification)
        fun onNoticeLongClicked(itemView: View, position: Int, notice: StatusBarNotification) : Boolean
    }
}