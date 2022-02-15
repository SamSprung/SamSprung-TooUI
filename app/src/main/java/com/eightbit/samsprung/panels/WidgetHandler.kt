package com.eightbit.samsprung.panels

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.eightbit.samsprung.CoverStateAdapter
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprungOverlay
import java.util.*
import java.util.concurrent.Executors

class WidgetHandler(
    private var overlay: SamSprungOverlay,
    private var viewPager: ViewPager2,
    private var pagerAdapter: CoverStateAdapter,
    private var displayMetrics: IntArray
) {

    private var mAppWidgetManager: AppWidgetManager =
        AppWidgetManager.getInstance(overlay.applicationContext)
    private var appWidgetHost: AppWidgetHost = CoverWidgetHost(
        overlay.applicationContext,
        SamSprungOverlay.APPWIDGET_HOST_ID
    )
    private val requestCreateAppWidgetHost = 9001

    init {
        appWidgetHost.startListening()
    }

    fun getAppWidgetHost() : AppWidgetHost {
        return appWidgetHost
    }

    @SuppressLint("InflateParams")
    fun completeAddAppWidget(appWidgetId: Int) {
        val appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId)

        // Build Launcher-specific widget info and save to database
        val launcherInfo = CoverWidgetInfo(appWidgetId)

        var spanX = appWidgetInfo.minWidth
        var spanY = appWidgetInfo.minHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            spanX = Integer.max(appWidgetInfo.minWidth, appWidgetInfo.maxResizeWidth)
            spanY = Integer.max(appWidgetInfo.minHeight, appWidgetInfo.maxResizeHeight)
        }
        if (spanX > displayMetrics[0])
            spanX = displayMetrics[0]
        if (spanY > displayMetrics[1])
            spanY = displayMetrics[1]
        val spans = intArrayOf(spanX, spanY)

        launcherInfo.spanX = spans[0]
        launcherInfo.spanY = spans[1]

        Executors.newSingleThreadExecutor().execute {
            WidgetModel.addItemToDatabase(
                overlay.applicationContext, launcherInfo,
                WidgetSettings.Favorites.CONTAINER_DESKTOP,
                spans[0],  spans[1], false
            )
        }
        SamSprungOverlay.model.addDesktopAppWidget(launcherInfo)

        launcherInfo.hostView = appWidgetHost.createView(
            overlay.applicationContext, appWidgetId, appWidgetInfo)
        launcherInfo.hostView!!.setAppWidget(appWidgetId, appWidgetInfo)
        launcherInfo.hostView!!.tag = launcherInfo

        val id = pagerAdapter.addFragment()
        val fragment = pagerAdapter.getFragment(id)
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            displayMetrics[0], displayMetrics[1]
        )
        params.gravity = Gravity.CENTER
        fragment.setListener(object: PanelViewFragment.ViewCreatedListener {
            override fun onViewCreated(view: View) {
                (view as LinearLayout).addView(launcherInfo.hostView, params)
            }
        })
        viewPager.setCurrentItem(id + 1, true)
    }

    private fun addAppWidget(appWidgetId: Int) {
        val appWidget = mAppWidgetManager.getAppWidgetInfo(appWidgetId) ?: return
        if (null != appWidget.configure) {
            try {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    overlay, appWidgetId, 0, requestCreateAppWidgetHost,
                    ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                )
            } catch (ignored: ActivityNotFoundException) {
                // Launch over to configure widget, if needed
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                intent.component = appWidget.configure
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                requestCreateAppWidget.launch(intent)
            }
        } else {
            completeAddAppWidget(appWidgetId)
        }
    }

    @SuppressLint("InflateParams")
    fun showAddDialog() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val mWidgetPreviewLoader = WidgetPreviewLoader(overlay)

        val view: View = overlay.layoutInflater.inflate(R.layout.panel_picker_view, null)
        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(overlay, R.style.DialogTheme_NoActionBar)
        )
        val previews = view.findViewById<LinearLayout>(R.id.previews_layout)
        dialog.setOnCancelListener {
            for (previewImage in previews.children) {
                mWidgetPreviewLoader.recycleBitmap(previewImage.tag,
                    (previewImage as AppCompatImageView).drawable.toBitmap())
            }
            previews.removeAllViewsInLayout()
        }
        dialog.setOnDismissListener {
            for (previewImage in previews.children) {
                mWidgetPreviewLoader.recycleBitmap(previewImage.tag,
                    (previewImage as AppCompatImageView).drawable.toBitmap())
            }
            previews.removeAllViewsInLayout()
        }
        val widgetDialog = dialog.setView(view).show()
        val infoList: List<AppWidgetProviderInfo> = mAppWidgetManager.installedProviders
        for (info: AppWidgetProviderInfo in infoList) {
            val previewSizeBeforeScale = IntArray(1)
            val preview = mWidgetPreviewLoader.generateWidgetPreview(
                info, overlay.window.decorView.width, overlay.window.decorView.height,
                null, previewSizeBeforeScale
            )
            val previewImage = overlay.layoutInflater.inflate(
                R.layout.widget_preview, null) as AppCompatImageView
            previewImage.adjustViewBounds = true
            previewImage.setImageBitmap(preview)
            previewImage.setOnClickListener {
                val success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
                if (success) {
                    addAppWidget(appWidgetId)
                } else {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    intent.putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile
                    )
                    requestCreateAppWidget.launch(intent)
                }
                widgetDialog?.dismiss()
            }
            previewImage.tag = info
            previews.addView(previewImage)
        }
        widgetDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private val requestCreateAppWidget = overlay.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val appWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == FragmentActivity.RESULT_CANCELED) {
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        } else {
            completeAddAppWidget(appWidgetId)
        }
    }

    @SuppressLint("InflateParams")
    fun bindAppWidgets(
        binder: DesktopBinder,
        appWidgets: LinkedList<CoverWidgetInfo>
    ) {
        if (!appWidgets.isEmpty()) {
            val item = appWidgets.removeFirst()
            val appWidgetId = item.appWidgetId
            val appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId)
            item.hostView = appWidgetHost.createView(
                overlay.applicationContext, appWidgetId, appWidgetInfo)
            item.hostView!!.setAppWidget(appWidgetId, appWidgetInfo)
            item.hostView!!.tag = item

            val id = pagerAdapter.addFragment()
            val fragment = pagerAdapter.getFragment(id)
            val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                displayMetrics[0], displayMetrics[1]
            )
            params.gravity = Gravity.CENTER
            fragment.setListener(object: PanelViewFragment.ViewCreatedListener {
                override fun onViewCreated(view: View) {
                    (view as LinearLayout).addView(item.hostView, params)
                }
            })
        }
        if (!appWidgets.isEmpty()) {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_APPWIDGETS).sendToTarget()
        }
    }

    fun onDestroy() {
        try {
            appWidgetHost.stopListening()
        } catch (ignored: NullPointerException) { }
    }
}