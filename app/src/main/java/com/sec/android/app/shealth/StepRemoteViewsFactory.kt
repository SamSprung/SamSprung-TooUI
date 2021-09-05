package com.sec.android.app.shealth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import java.util.*


class StepRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {
    private val packages: MutableList<ResolveInfo>
    private val pacMan = context.packageManager
    override fun onCreate() {}
    override fun onDataSetChanged() {}
    override fun onDestroy() {
        packages.clear()
    }

    override fun getCount(): Int {
        return packages.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val application = packages[position]
        val rv = RemoteViews(context.packageName, R.layout.step_widget_item)
        rv.setTextViewText(R.id.widgetItemText, application.loadLabel(pacMan).toString())
        try {
            val drawable = pacMan.getApplicationIcon(
                application.activityInfo.packageName
            )
            rv.setImageViewBitmap(R.id.widgetItemImage, getBitmapFromDrawable(drawable))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            rv.setImageViewBitmap(
                R.id.widgetItemImage,
                getBitmapFromDrawable(application.loadIcon(pacMan))
            )
        }
        val extras = Bundle()
        extras.putString("launchPackage", application.activityInfo.packageName)
        extras.putString("launchActivity", application.activityInfo.name)
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        rv.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    init {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.removeCategory(Intent.CATEGORY_HOME)
        packages = pacMan.queryIntentActivities(mainIntent, 0)
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(pacMan))
    }
}