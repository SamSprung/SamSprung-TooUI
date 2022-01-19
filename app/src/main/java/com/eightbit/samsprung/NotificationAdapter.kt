package com.eightbit.samsprung

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private var notices: ArrayList<SamSprungNotice>,
    private var listener: OnNoticeClickListener
) : RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder>() {
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
}