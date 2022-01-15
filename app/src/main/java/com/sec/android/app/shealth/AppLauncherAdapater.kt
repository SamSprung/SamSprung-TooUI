package com.sec.android.app.shealth

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sec.android.app.shealth.AppLauncherAdapter.AppViewHolder

class AppLauncherAdapter(
    private val packages: MutableList<ResolveInfo>,
    private val listener: OnAppClickListener,
    private val packageManager: PackageManager
) : RecyclerView.Adapter<AppViewHolder>() {
    override fun getItemCount(): Int {
        return packages.size
    }

    override fun getItemId(i: Int): Long {
        return packages[i].labelRes.toLong()
    }

    private fun getItem(i: Int): ResolveInfo {
        return packages[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return SimpleViewHolder(parent, listener, packageManager)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener) holder.listener.onAppClicked(
                holder.appItem,
                position
            )
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener) {
                holder.listener.onAppClicked(holder.appItem, position)
            }
        }
        holder.bind(getItem(position))
    }

    abstract class AppViewHolder(
        itemView: View, val listener: OnAppClickListener?,
        private val packageManager: PackageManager
    ) : RecyclerView.ViewHolder(itemView) {
        private val isGridView = SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)
        private val widgetListContainer = itemView.findViewById<LinearLayout>(
            R.id.widgetListContainer)
        val iconView: ImageView =  itemView.findViewById(
            if (isGridView) R.id.widgetGridImage else R.id.widgetItemImage)
        private val widgetItemText: TextView = itemView.findViewById(R.id.widgetItemText)
        var appItem: ResolveInfo? = null
        @SuppressLint("SetTextI18n")
        fun bind(appInfo: ResolveInfo) {
            widgetListContainer.visibility = if (isGridView) View.GONE else View.VISIBLE
            itemView.findViewById<ImageView>(R.id.widgetGridImage).visibility =
                if (isGridView) View.VISIBLE else View.GONE
            if (null != appInfo.loadIcon(packageManager)) {
                iconView.setImageDrawable(appInfo.loadIcon(packageManager))
            }
            if (!isGridView) {
                if (null != appInfo.loadLabel(packageManager)) {
                    widgetItemText.text = appInfo.loadLabel(packageManager).toString()
                }
            }
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.step_widget_item,
            parent, false
        ), listener, packageManager
    )

    interface OnAppClickListener {
        fun onAppClicked(appInfo: ResolveInfo?, position: Int)
    }
}