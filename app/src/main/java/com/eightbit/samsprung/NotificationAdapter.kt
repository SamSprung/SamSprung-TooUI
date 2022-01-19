package com.eightbit.samsprung

import android.app.Notification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NotificationAdapter(
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>() {
    override fun getItemCount(): Int {
        return SamSprung.notifications.size
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private fun getItem(i: Int): Notification {
        return SamSprung.notifications[i]
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
        private val linesText: TextView = itemView.findViewById(R.id.lines)
        var notification: Notification? = null
        fun bind(notice: Notification) {
            this.notification = notice
            when {
                null != notice.getLargeIcon() -> iconView.setImageDrawable(
                    notice.getLargeIcon().loadDrawable(iconView.context)
                )
                null != notice.smallIcon -> iconView.setImageDrawable(
                    notice.smallIcon.loadDrawable(iconView.context)
                )
            }
            if (null != notice.extras && null != notice.extras.getCharSequenceArray(
                    NotificationCompat.EXTRA_TEXT_LINES)) {
                linesText.text = Arrays.toString(notice.extras.getCharSequenceArray(
                    NotificationCompat.EXTRA_TEXT_LINES
                ))
            } else if (null != notice.tickerText) {
                linesText.text = notice.tickerText
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