package com.sec.android.app.shealth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.util.*

class AppLauncherService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StepRemoteViewsFactory(applicationContext)
    }

    class StepRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {
        private var isGridView = true
        private var packages: MutableList<ResolveInfo> = arrayListOf()
        private val pacMan = context.packageManager

        override fun onCreate() {
            val sharedPref = context.getSharedPreferences(
                "samsprung.launcher.PREFS", Context.MODE_PRIVATE
            )
            isGridView = sharedPref.getBoolean("gridview", isGridView)
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.removeCategory(Intent.CATEGORY_HOME)
            packages = pacMan.queryIntentActivities(mainIntent, 0)
            packages.removeIf { item -> item.activityInfo.packageName == context.packageName }
            Collections.sort(packages, ResolveInfo.DisplayNameComparator(pacMan))
        }
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

            rv.setViewVisibility(R.id.widgetListContainer,
                if (isGridView) View.GONE else View.VISIBLE)
            rv.setViewVisibility(R.id.widgetGridImage,
                if (isGridView) View.VISIBLE else View.GONE)

            val icon = if (isGridView) R.id.widgetGridImage else R.id.widgetItemImage

            try {
                val drawable = pacMan.getApplicationIcon(
                    application.activityInfo.packageName
                )
                rv.setImageViewBitmap(icon, getBitmapFromDrawable(drawable))
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                rv.setImageViewBitmap(
                    icon, getBitmapFromDrawable(application.loadIcon(pacMan))
                )
            }

            val extras = Bundle()
            extras.putString("launchPackage", application.activityInfo.packageName)
            extras.putString("launchActivity", application.activityInfo.name)
            val fillInIntent = Intent()
            fillInIntent.putExtras(extras)
                rv.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent)
            if (!isGridView) {
                rv.setTextViewText(R.id.widgetItemText, application.loadLabel(pacMan).toString())
            }
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
}