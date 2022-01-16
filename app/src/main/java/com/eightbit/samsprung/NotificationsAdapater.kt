package com.eightbit.samsprung

import android.app.Notification
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NotificationsAdapter(
    private val listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationsAdapter.NoticeViewHolder>() {
    override fun getItemCount(): Int {
        return SamSprung.notices.size
    }

    override fun getItemId(i: Int): Long {
        return SamSprung.notices[i].number.toLong()
    }

    private fun getItem(i: Int): Notification {
        return SamSprung.notices[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        return SimpleViewHolder(parent, listener)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener) holder.listener.onNoticeClicked(
                holder.notice!!,
                position
            )
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener) {
                holder.listener.onNoticeClicked(holder.notice!!, position)
            }
        }
        holder.bind(getItem(position))
    }

    abstract class NoticeViewHolder(
        itemView: View, val listener: OnNoticeClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.icon)
        private val tickerText: TextView =  itemView.findViewById(R.id.ticker)
        private val linesText: TextView =  itemView.findViewById(R.id.lines)
        var notice: Notification? = null
        fun bind(notice: Notification) {
            this.notice = notice

            val bigPicture: Bitmap? = notice.extras.getParcelable(Notification.EXTRA_PICTURE)
            val bigLargeIcon: Any? = notice.extras.get(Notification.EXTRA_LARGE_ICON_BIG)
            when {
                null != notice.smallIcon -> iconView.setImageDrawable(
                    notice.smallIcon.loadDrawable(itemView.context))
                null != notice.getLargeIcon() -> iconView.setImageDrawable(
                    notice.getLargeIcon().loadDrawable(itemView.context))
                null != bigPicture -> iconView.setImageBitmap(bigPicture)
                bigLargeIcon is Bitmap -> iconView.setImageBitmap(bigLargeIcon)
            }

            if (null != notice.tickerText)
                tickerText.text = notice.tickerText
            else
                tickerText.visibility = View.GONE
            if (null != notice.extras) {
                if (null != notice.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES)) {
                    linesText.text = Arrays.toString(
                        notice.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES)
                    )
                    if (View.VISIBLE == tickerText.visibility
                        && notice.tickerText == linesText.text)
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