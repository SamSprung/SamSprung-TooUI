package com.eightbit.samsprung

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.AppDrawerAdapater.AppViewHolder

class AppDrawerAdapater(
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
        return if (SamSprung.prefs.getBoolean(SamSprung.prefLayout, true))
            SimpleGridHolder(parent, listener, packageManager)
        else
        return SimpleViewHolder(parent, listener, packageManager)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener) holder.listener.onAppClicked(
                holder.appInfo!!,
                position
            )
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener) {
                holder.listener.onAppClicked(holder.appInfo!!, position)
            }
        }
        holder.bind(getItem(position))
    }

    abstract class AppViewHolder(
        itemView: View, val listener: OnAppClickListener?,
        private val packageManager: PackageManager
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.widgetItemImage)
        var appInfo: ResolveInfo? = null
        fun bind(appInfo: ResolveInfo) {
            this.appInfo = appInfo
            if (null != appInfo.loadIcon(packageManager)) {
                iconView.setImageDrawable(appInfo.loadIcon(packageManager))
            }
            if (!SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)) {
                if (null != appInfo.loadLabel(packageManager)) {
                    itemView.findViewById<TextView>(R.id.widgetItemText).text =
                        appInfo.loadLabel(packageManager).toString()
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
            R.layout.app_drawer_item,
            parent, false
        ), listener, packageManager
    )

    internal class SimpleGridHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.app_drawer_icon,
            parent, false
        ), listener, packageManager
    )

    interface OnAppClickListener {
        fun onAppClicked(appInfo: ResolveInfo, position: Int)
    }
}