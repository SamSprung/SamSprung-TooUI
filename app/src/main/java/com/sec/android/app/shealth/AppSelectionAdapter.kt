package com.sec.android.app.shealth

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat


class AppSelectionAdapter(
    private val context: Context,
    private var packages: MutableList<ResolveInfo>,
    private var hide: MutableSet<String>,
    private var pacMan: PackageManager
) :

    BaseAdapter() {

    override fun getCount(): Int {
        return packages.size //returns total of items in the list
    }

    override fun getItem(position: Int): Any {
        return packages[position] //returns list item at the specified position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // inflate the layout for each list row
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.app_hidden_item, parent, false)
        }

        convertView!!.isLongClickable = true

        val application = packages[position]

        val detailView = convertView.findViewById<LinearLayout>(R.id.hiddenItemContainer)

        detailView.findViewById<ImageView>(R.id.hiddenItemImage).setImageBitmap(
            getBitmapFromDrawable(application.loadIcon(pacMan)))

        detailView.findViewById<TextView>(R.id.hiddenItemText).text =
            application.loadLabel(pacMan).toString()

        val hideSwitch = detailView.findViewById<SwitchCompat>(R.id.hiddenItemSwitch)
        hideSwitch.isChecked = !hide.contains(application.activityInfo.packageName)

        // returns the view for the current row
        return convertView
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bitmapDrawable = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmapDrawable)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmapDrawable
    }
}