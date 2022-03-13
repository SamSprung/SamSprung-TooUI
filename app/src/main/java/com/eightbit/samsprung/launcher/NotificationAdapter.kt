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
import android.app.Notification
import android.app.PendingIntent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.NotificationReceiver
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
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
        private val prefs = activity.getSharedPreferences(
            SamSprung.prefsValue, FragmentActivity.MODE_PRIVATE
        )
        fun bind(notice: StatusBarNotification) {
            (itemView as CardView).setCardBackgroundColor(prefs.getInt(
                SamSprung.prefColors, Color.rgb(255, 255, 255)).blended)
            this.notice = notice
            val notification = notice.notification
            val iconView = itemView.findViewById<AppCompatImageView>(R.id.icon)
            val imageView = itemView.findViewById<AppCompatImageView>(R.id.image)
            val titleText = itemView.findViewById<TextView>(R.id.title)
            val linesText = itemView.findViewById<TextView>(R.id.lines)
            val launch = itemView.findViewById<AppCompatImageView>(R.id.launch)
            val dismiss = itemView.findViewById<AppCompatImageView>(R.id.dismiss)

            titleText.text = ""
            linesText.text = ""
            Executors.newSingleThreadExecutor().execute {
                when {
                    null != notification.getLargeIcon() -> {
                        val icon = notification.getLargeIcon().loadDrawable(activity)
                        activity.runOnUiThread {
                            icon.applyTheme(activity.theme)
                            iconView.setImageDrawable(icon)
                        }
                    }
                    null != notification.smallIcon -> {
                        val icon = notification.smallIcon.loadDrawable(activity)
                        activity.runOnUiThread {
                            icon.applyTheme(activity.theme)
                            iconView.setImageDrawable(icon)
                        }
                    }
                }
                if (null != notification.extras) {
                    if (notification.extras.containsKey(NotificationCompat.EXTRA_LARGE_ICON_BIG)) {
                        val bitmap: Bitmap? = when (val image = notification.extras.get(
                            NotificationCompat.EXTRA_LARGE_ICON_BIG)) {
                            is Bitmap -> image
                            is Icon -> image.loadDrawable(activity).toBitmap()
                            else -> null
                        }
                        if (null != bitmap) {
                            activity.runOnUiThread {
                                imageView.setImageBitmap(getScaledBitmap(activity, bitmap))
                            }
                        }
                    }
                    if (notification.extras.containsKey(NotificationCompat.EXTRA_PICTURE)) {
                        val bitmap: Bitmap? = when (val image = notification.extras.get(
                            NotificationCompat.EXTRA_PICTURE)) {
                            is Bitmap -> image
                            is Icon -> image.loadDrawable(activity).toBitmap()
                            else -> null
                        }
                        if (null != bitmap) {
                            activity.runOnUiThread {
                                imageView.setImageBitmap(getScaledBitmap(activity, bitmap))
                            }
                        }
                    }
                    activity.runOnUiThread {
                        if (notification.extras.containsKey(NotificationCompat.EXTRA_TITLE_BIG)) {
                            val titleBig = notification.extras.get(NotificationCompat.EXTRA_TITLE_BIG)
                            if (null != titleBig) titleText.text = titleBig.toString()
                        }
                        if (titleText.text.isEmpty() && notification.extras
                                .containsKey(NotificationCompat.EXTRA_TITLE)) {
                            val textTitle = notification.extras.getCharSequence(
                                NotificationCompat.EXTRA_TITLE
                            )
                            if (null != textTitle) titleText.text = textTitle.toString()
                        }
                        if (notification.extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) {
                            val textBig = notification.extras.get(NotificationCompat.EXTRA_BIG_TEXT)
                            if (null != textBig) linesText.text = textBig.toString()
                        }
                        if (linesText.text.isEmpty() && notification.extras
                                .containsKey(NotificationCompat.EXTRA_TEXT_LINES)) {
                            val textArray = notification.extras.getCharSequenceArray(
                                NotificationCompat.EXTRA_TEXT_LINES
                            )
                            if (null != textArray) {
                                val textLines = Arrays.toString(textArray)
                                if (textLines != "[]") linesText.text = textLines
                            }
                        }
                        if (linesText.text.isEmpty() && notification.extras
                                .containsKey(NotificationCompat.EXTRA_TEXT)) {
                            val textExtra = notification.extras
                                .getCharSequence(NotificationCompat.EXTRA_TEXT)
                            if (null != textExtra) linesText.text = textExtra.toString()
                        }
                    }
                }
            }
//            if (linesText.text.isEmpty() && null != notification.tickerText) {
//                linesText.text = notification.tickerText.toString()
//            }
            if (null != notification.contentIntent) {
                launch.visibility = View.VISIBLE
                launch.setOnClickListener {
                    listener?.onLaunchClicked(notification.contentIntent)
                }
            } else {
                launch.visibility = View.GONE
            }
            if (notice.isClearable) {
                dismiss.visibility = View.VISIBLE
                dismiss.setOnClickListener {
                    NotificationReceiver.getReceiver()?.setNotificationsShown(arrayOf(notice.key))
                    NotificationReceiver.getReceiver()?.cancelNotification(notice.key)
                }
            } else {
                dismiss.visibility = View.GONE
            }
        }

        private inline val @receiver:ColorInt Int.blended
            @ColorInt
            get() = ColorUtils.blendARGB(this,
                if ((activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES) Color.BLACK else Color.WHITE, 0.4f)

        private  fun getScaledBitmap(activity: Activity, bitmap: Bitmap): Bitmap {
            val maxWidth = activity.window.decorView.width
            val maxHeight = activity.window.decorView.height
            return if (bitmap.width > activity.window.decorView.width) {
                val width = bitmap.width
                val height = bitmap.height
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
                var finalWidth = maxWidth
                var finalHeight = maxHeight
                if (ratioMax > ratioBitmap) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }
                Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
            } else {
                bitmap
            }
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

    private fun validateTextExtras(extras: Bundle, extra: String) : Boolean {
        return extras.containsKey(extra) &&
                extras.get(extra).toString().trim().isNotEmpty()
    }
    private fun validateCharExtras(extras: Bundle, extra: String) : Boolean {
        return extras.containsKey(extra) &&
                extras.getCharSequence(extra).toString().trim().isNotEmpty()
    }
    private fun validateArrayExtras(extras: Bundle, extra: String) : Boolean {
        if (extras.containsKey(extra)) {
            val textArray = extras.getCharSequenceArray(extra)
            if (null != textArray && Arrays.toString(textArray) != "[]") return true
        }
        return false
    }

    private fun isValidNotification(extras: Bundle?) : Boolean {
        return null != extras && (
                extras.containsKey(NotificationCompat.EXTRA_PICTURE) ||
                validateTextExtras(extras, NotificationCompat.EXTRA_TITLE_BIG) ||
                validateCharExtras(extras, NotificationCompat.EXTRA_TITLE) ||
                validateTextExtras(extras, NotificationCompat.EXTRA_BIG_TEXT) ||
                validateArrayExtras(extras, NotificationCompat.EXTRA_TEXT_LINES) ||
                validateCharExtras(extras, NotificationCompat.EXTRA_TEXT)
        )
    }

    private fun isGroupSummary(sbn: StatusBarNotification) : Boolean {
        return (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActiveNotifications(activeNotifications: ArrayList<StatusBarNotification>) {
        for (sbn in activeNotifications) {
            if (isValidNotification(sbn.notification.extras) && !isGroupSummary(sbn))
                sbNotifications.add(sbn)
        }
        this.notifyDataSetChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isGroupSummary(sbn) || !isValidNotification(sbn.notification.extras)) return
        var update = -1
        for (index in 0 until sbNotifications.size) {
            if (sbNotifications[index].key == sbn.key) {
                update = index
                break
            }
        }
        if (update != -1) {
            sbNotifications[update] = sbn
            this.notifyItemChanged(update, sbn)
        } else {
            sbNotifications.add(0, sbn)
            this.notifyItemInserted(0)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isGroupSummary(sbn) || !isValidNotification(sbn.notification.extras)) return
        var remove = -1
        for (index in 0 until sbNotifications.size) {
            if (sbNotifications[index].key == sbn.key) {
                remove = index
                break
            }
        }
        if (remove != -1) {
            sbNotifications.remove(sbNotifications[remove])
            this.notifyItemRemoved(remove)
        }
    }

    interface OnNoticeClickListener {
        fun onNoticeClicked(itemView: View, position: Int, notice: StatusBarNotification)
        fun onNoticeLongClicked(itemView: View, position: Int, notice: StatusBarNotification) : Boolean
        fun onLaunchClicked(pendingIntent: PendingIntent)
    }
}