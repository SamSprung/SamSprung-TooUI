package com.eightbit.samsprung

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
import kotlin.collections.ArrayList

class NotificationAdapter(
    private var notifications: ArrayList<StatusBarNotification>,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>() {
    override fun getItemCount(): Int {
        return notifications.size
    }

    override fun getItemId(i: Int): Long {
        return notifications[i].key.toLong()
    }

    private fun getItem(i: Int): StatusBarNotification {
        return notifications[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        return SimpleViewHolder(parent, listener)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notification!!, position)
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onNoticeClicked(holder.notification!!, position)
        }
        holder.bind(getItem(position))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.icon)
        private val tickerText: TextView = itemView.findViewById(R.id.ticker)
        private val linesText: TextView = itemView.findViewById(R.id.lines)
        var notification: Notification? = null
        fun bind(sbn: StatusBarNotification) {
            val notice: Notification = sbn.notification
            notification = notice
            when {
                null != notice.getLargeIcon() -> iconView.setImageDrawable(
                    notice.getLargeIcon().loadDrawable(iconView.context)
                )
                null != notice.smallIcon -> iconView.setImageDrawable(
                    notice.smallIcon.loadDrawable(iconView.context)
                )
            }
            if (null != notice.tickerText) {
                tickerText.text = notice.tickerText
                tickerText.visibility = View.VISIBLE
            }
            if (null != notice.extras) {
                if (null != notice.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES
                    )
                ) {
                    linesText.text = Arrays.toString(
                        notice.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES
                        )
                    )
                    if (tickerText.visibility == View.VISIBLE
                        && notice.tickerText != linesText.text
                    )
                        tickerText.visibility = View.GONE
                }
            }
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
        fun onNoticeClicked(notice: Notification, position: Int)
    }
}