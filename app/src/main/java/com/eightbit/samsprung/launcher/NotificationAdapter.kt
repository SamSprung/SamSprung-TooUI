package com.eightbit.samsprung.launcher

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
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.NotificationReceiver
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprungOverlay
import java.util.*
import java.util.concurrent.Executors

class NotificationAdapter(
    private var activity: Activity,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>(),
    NotificationReceiver.NotificationsListener {

    private var sbNotifications: ArrayList<StatusBarNotification> = arrayListOf()

    override fun getItemCount(): Int {
        return sbNotifications.size
    }

    override fun getItemId(i: Int): Long {
        return UUID.nameUUIDFromBytes(sbNotifications[i].key.toByteArray()).mostSignificantBits
    }

    private fun getItem(i: Int): StatusBarNotification {
        return sbNotifications[i]
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        return SimpleViewHolder(parent, listener, activity)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.itemView,
                    holder.bindingAdapterPosition, holder.notice)
        }
        holder.itemView.setOnLongClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeLongClicked(holder.itemView,
                    holder.bindingAdapterPosition, holder.notice)
            else
                false
        }
        holder.bind(getItem(holder.bindingAdapterPosition))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?, val activity: Activity
    ) : RecyclerView.ViewHolder(itemView) {
        lateinit var notice: StatusBarNotification
        fun bind(notice: StatusBarNotification) {
            this.notice = notice
            val notification = notice.notification
            Executors.newSingleThreadExecutor().execute {
                val iconView = itemView.findViewById<AppCompatImageView>(R.id.icon)
                when {
                    null != notification.getLargeIcon() -> {
                        val icon = notification.getLargeIcon().loadDrawable(activity)
                        activity.runOnUiThread {
                            iconView.setImageDrawable(icon)
                        }
                    }
                    null != notification.smallIcon -> {
                        val icon = notification.smallIcon.loadDrawable(activity)
                        activity.runOnUiThread {
                            iconView.setImageDrawable(icon)
                        }
                    }
                }
                val linesText = itemView.findViewById<TextView>(R.id.lines)
                if (null != notification.extras) {
                    val imageView = itemView.findViewById<AppCompatImageView>(R.id.image)
                    if (notification.extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG)) {
                        val image = notification.extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG)
                        var bitmap: Bitmap? = null
                        if (image is Bitmap)
                            bitmap = image
                        else if (image is Icon)
                            bitmap = image.loadDrawable(activity).toBitmap()
                        activity.runOnUiThread {
                            imageView.setImageBitmap(getScaledBitmap(activity, bitmap))
                        }
                    }
                    if (notification.extras.containsKey(NotificationCompat.EXTRA_PICTURE)) {
                        val image = notification.extras.get(NotificationCompat.EXTRA_PICTURE)
                        var bitmap: Bitmap? = null
                        if (image is Bitmap)
                            bitmap = image
                        else if (image is Icon)
                            bitmap = image.loadDrawable(activity).toBitmap()
                        activity.runOnUiThread {
                            imageView.setImageBitmap(getScaledBitmap(activity, bitmap))
                        }
                    }
                    if (notification.extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) {
                        val textBig = notification.extras.get(NotificationCompat.EXTRA_BIG_TEXT)
                        activity.runOnUiThread {
                            if (null != textBig) linesText.text = textBig.toString()
                        }
                    }
                    if (linesText.text.isEmpty() && notification.extras.containsKey(
                            NotificationCompat.EXTRA_TEXT_LINES
                        )
                    ) {
                        val textLines = notification.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES
                        )
                        activity.runOnUiThread {
                            if (null != textLines) linesText.text = Arrays.toString(textLines)
                        }
                    }
                    if (linesText.text.isEmpty() && notification.extras.containsKey(
                            NotificationCompat.EXTRA_TEXT
                        )
                    ) {
                        val textExtra = notification.extras
                            .getCharSequence(NotificationCompat.EXTRA_TEXT)
                        activity.runOnUiThread {
                            if (null != textExtra) linesText.text = textExtra.toString()
                        }
                    }
                }
                if (linesText.text.isEmpty() && null != notification.tickerText) {
                    activity.runOnUiThread {
                        linesText.text = notification.tickerText.toString()
                    }
                }
                if (linesText.text.isEmpty()) linesText.text = ""
                activity.runOnUiThread {
                    itemView.findViewById<AppCompatImageView>(R.id.launch).setOnClickListener {
                        (activity as SamSprungOverlay)
                            .processIntentSender(notification.contentIntent.intentSender)
                    }
                }
            }
        }

        private fun getScaledBitmap(activity: Activity, bitmap: Bitmap?): Bitmap? {
            if (null == bitmap) return null

//            val width: Int = bitmap.width
//            val height: Int = bitmap.height
//            val scaleWidth = activity.window.decorView.width.toFloat() / width
//            val scaleHeight = activity.window.decorView.height.toFloat() / height
//            val matrix = Matrix()
//            matrix.postScale(scaleWidth, scaleHeight)
//            return Bitmap.createBitmap(
//                bitmap, 0, 0, width, height, matrix, false
//            )
            return bitmap
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnNoticeClickListener?,
        activity: Activity
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
            activity.runOnUiThread { this.notifyItemChanged(current) }
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