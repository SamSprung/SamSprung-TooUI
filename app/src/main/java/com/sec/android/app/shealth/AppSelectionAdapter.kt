package com.sec.android.app.shealth

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget


class AppSelectionAdapter(
    private val context: Context,
    private var packages: MutableList<ResolveInfo>,
    private var hide: HashSet<String>
) : BaseAdapter() {

    private val hidden = "hidden_packages"
    private var pacMan: PackageManager = context.packageManager
    private val sharedPref = context.getSharedPreferences(
        "samsprung.launcher.PREFS", Context.MODE_PRIVATE
    )

    override fun getCount(): Int {
        return packages.size //returns total of items in the list
    }

    override fun getItem(position: Int): Any {
        return packages[position] //returns list item at the specified position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.app_hidden_item, parent, false)
        }

        val application = packages[position]

        val appName = application.loadLabel(pacMan).toString()

        val detailView = convertView!!.findViewById<LinearLayout>(R.id.hiddenItemContainer)

        detailView.findViewById<ImageView>(R.id.hiddenItemImage).setImageBitmap(
            getBitmapFromDrawable(application.loadIcon(pacMan)))

        detailView.findViewById<TextView>(R.id.hiddenItemText).text = appName

        val hideSwitch = detailView.findViewById<SwitchCompat>(R.id.hiddenItemSwitch)
        hideSwitch.isChecked = !hide.contains(application.activityInfo.packageName)

        hideSwitch.setOnClickListener {
            val packageName = application.activityInfo.packageName
            if (hide.contains(packageName)) {
                hide.remove(packageName)
                with(sharedPref.edit()) {
                    putStringSet(hidden, hide)
                    apply()
                }
                Toast.makeText(
                    context, context.getString(
                        R.string.show_package, appName
                    ), Toast.LENGTH_SHORT
                ).show()
            } else {
                hide.add(packageName)
                with(sharedPref.edit()) {
                    putStringSet(hidden, hide)
                    apply()
                }
                Toast.makeText(
                    context, context.getString(
                        R.string.hide_package, appName
                    ), Toast.LENGTH_SHORT
                ).show()
            }
            updateAppWidget(context)
        }

        return convertView
    }

    private fun updateAppWidget(context: Context) {
        val updateIntent = Intent(context, StepCoverAppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_IDS,
            AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(
                ComponentName(context.applicationContext, StepCoverAppWidget::class.java)
            )
        )
        context.sendBroadcast(updateIntent)
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