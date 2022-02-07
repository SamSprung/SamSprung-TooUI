/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eightbit.samsprung

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Dialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.os.MessageQueue.IdleHandler
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.method.TextKeyListener
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.panels.*
import com.eightbit.samsprung.panels.CellLayout
import com.eightbit.samsprung.panels.WidgetSettings.Favorites
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.Executors


/**
 * Default launcher application.
 */
@SuppressLint("ClickableViewAccessibility")
class SamSprungPanels : AppCompatActivity(), View.OnClickListener, OnLongClickListener {
    private val mObserver: ContentObserver = FavoritesChangeObserver()
    private var mDragLayer: DragLayer? = null
    var workspace: Workspace? = null
        private set
    private var mAppWidgetManager: AppWidgetManager? = null
    var appWidgetHost: CoverWidgetHost? = null
        private set
    private var mAddItemCellInfo: CellLayout.CellInfo? = null
    private val mCellCoordinates = IntArray(2)

    /**
     * Returns true if the workspace is being loaded. When the workspace is loading,
     * no user interaction should be allowed to avoid any conflict.
     *
     * @return True if the workspace is locked, false otherwise.
     */
    var isWorkspaceLocked = true
        private set
    private var mSavedState: Bundle? = null
    private var mDefaultKeySsb: SpannableStringBuilder? = null
    private var mDestroyed = false
    private var mIsNewIntent = false
    private var mRestoring = false
    private var mWaitingForResult = false
    private var mLocaleChanged = false
    private var mSavedInstanceState: Bundle? = null
    private var mBinder: DesktopBinder? = null

    private var widgetContext: Context? = null
    private lateinit var offReceiver: BroadcastReceiver

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)

        widgetContext = ScaledContext.widget(applicationContext)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        ScaledContext.widget(this).setTheme(R.style.Theme_AppCompat)

        mAppWidgetManager = AppWidgetManager.getInstance(widgetContext)
        appWidgetHost = CoverWidgetHost(
            widgetContext,
            APPWIDGET_HOST_ID
        )
        appWidgetHost!!.startListening()

        val localeConfiguration = LocaleConfiguration()
        readConfiguration(applicationContext, localeConfiguration)
        val configuration = resources.configuration
        val previousLocale = localeConfiguration.locale
        val locale = configuration.locales[0].toString()
        val previousMcc = localeConfiguration.mcc
        val mcc = configuration.mcc
        val previousMnc = localeConfiguration.mnc
        val mnc = configuration.mnc
        mLocaleChanged = locale != previousLocale || mcc != previousMcc || mnc != previousMnc
        if (mLocaleChanged) {
            localeConfiguration.locale = locale
            localeConfiguration.mcc = mcc
            localeConfiguration.mnc = mnc
            writeConfiguration(applicationContext, localeConfiguration)
        }

        setContentView(R.layout.panel_main_layout)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            findViewById<View>(R.id.coordinator).background =
                WallpaperManager.getInstance(this).drawable
        }

        (findViewById<View>(R.id.blurContainer) as BlurView).setupWith(
            window.decorView.findViewById(R.id.coordinator)
        )
            .setFrameClearDrawable(window.decorView.background)
            .setBlurRadius(1f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
            .setBlurAlgorithm(RenderScriptBlur(this))

        val menuHandle = findViewById<AppCompatImageView>(R.id.menu_zone)
        menuHandle.setOnClickListener {
            showAddDialog(CellLayout.CellInfo())
        }
        menuHandle.setOnLongClickListener {
            finish()
            startForegroundService(
                Intent(
                    applicationContext,
                    OnBroadcastService::class.java
                ).setAction(SamSprung.launcher)
            )
            return@setOnLongClickListener true
        }

        mDragLayer = findViewById(R.id.drag_layer)
        val dragLayer = mDragLayer
        workspace = dragLayer!!.findViewById(R.id.workspace)
        val workspace = workspace
        val deleteZone: DeleteZone = dragLayer.findViewById(R.id.delete_zone)
        workspace!!.setOnLongClickListener(this)
        workspace.setDragger(dragLayer)
        workspace.setLauncher(this)
        deleteZone.setLauncher(this)
        deleteZone.setDragController(dragLayer)
        dragLayer.setDragScoller(workspace)
        dragLayer.setDragListener(deleteZone)

        contentResolver.registerContentObserver(Favorites.CONTENT_URI, true, mObserver)

        if (!mRestoring) {
            startLoaders()
        }
        mSavedState = savedInstanceState
        restoreState(mSavedState)

        // For handling default keys
        mDefaultKeySsb = SpannableStringBuilder()
        Selection.setSelection(mDefaultKeySsb, 0)

        offReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    finish()
                    startForegroundService(
                        Intent(
                            applicationContext,
                            OnBroadcastService::class.java
                        ).setAction(SamSprung.services)
                    )
                }
            }
        }
        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            registerReceiver(offReceiver, it)
        }
    }

    class LocaleConfiguration {
        var locale: String? = null
        var mcc = -1
        var mnc = -1
    }

    private fun startLoaders() {
        model.loadUserItems(!mLocaleChanged, this, mLocaleChanged)
        mRestoring = false
    }

    override fun onResume() {
        super.onResume()
        if (mRestoring) {
            startLoaders()
        }
        mIsNewIntent = false
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        if (mBinder != null) {
            mBinder!!.mTerminate = true
        }
        // return lastNonConfigurationInstance
        return null
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private fun restoreState(savedState: Bundle?) {
        if (savedState == null) {
            return
        }
        val currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1)
        if (currentScreen > -1) {
            workspace!!.currentScreen = currentScreen
        }
        val addScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1)
        if (addScreen > -1) {
            mAddItemCellInfo = CellLayout.CellInfo()
            val addItemCellInfo = mAddItemCellInfo!!
            addItemCellInfo.valid = true
            addItemCellInfo.screen = addScreen
            addItemCellInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X)
            addItemCellInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y)
            addItemCellInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X)
            addItemCellInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y)
            addItemCellInfo.findVacantCellsFromOccupied(
                savedState.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS),
                savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X),
                savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y)
            )
            mRestoring = true
        }
    }

    @Suppress("UNUSED")
    fun onScreenChanged(workspace: Workspace, mCurrentScreen: Int) {

    }

    private fun completeAddAppWidget(
        appWidgetId: Int, cellInfo: CellLayout.CellInfo?,
        insertAtFirst: Boolean
    ) {
        mWaitingForResult = false
        val appWidgetInfo = mAppWidgetManager!!.getAppWidgetInfo(appWidgetId)

        // Build Launcher-specific widget info and save to database
        val launcherInfo = CoverWidgetInfo(appWidgetId)

        var spanX = appWidgetInfo.minWidth
        var spanY = appWidgetInfo.minHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            spanX = max(appWidgetInfo.minWidth, appWidgetInfo.maxResizeWidth)
            spanY = max(appWidgetInfo.minHeight, appWidgetInfo.maxResizeHeight)
        }
        val layout = workspace!!.getChildAt(cellInfo!!.screen) as CellLayout
        val spans = layout.rectToCell(spanX, spanY)
        launcherInfo.spanX = spans[0]
        launcherInfo.spanY = spans[1]

        // Try finding open space on Launcher screen
        val xy = mCellCoordinates

        Executors.newSingleThreadExecutor().execute {
            WidgetModel.addItemToDatabase(
                widgetContext, launcherInfo,
                Favorites.CONTAINER_DESKTOP,
                workspace!!.currentScreen, xy[0], xy[1], false
            )
        }
        if (!mRestoring) {
            model.addDesktopAppWidget(launcherInfo)

            // Perform actual inflation because we're live
            launcherInfo.hostView = appWidgetHost!!.createView(
                widgetContext, appWidgetId, appWidgetInfo)
            launcherInfo.hostView!!.setAppWidget(appWidgetId, appWidgetInfo)
            launcherInfo.hostView!!.tag = launcherInfo
            workspace!!.addInCurrentScreen(
                launcherInfo.hostView, xy[0], xy[1],
                launcherInfo.spanX, launcherInfo.spanY, insertAtFirst
            )

        } else if (model.isDesktopLoaded) {
            model.addDesktopAppWidget(launcherInfo)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Close the menu
        if (Intent.ACTION_MAIN == intent.action) {
            window.closeAllPanels()

            // Set this flag so that onResume knows to close the search dialog if it's open,
            // because this was a new intent (thus a press of 'home' or some such) rather than
            // for example onResume being called when the user pressed the 'back' button.
            mIsNewIntent = true
            workspace!!.unlock()
            if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT !=
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            ) {
                if (!workspace!!.isDefaultScreenShowing) {
                    workspace!!.moveToDefaultScreen()
                }
                val v = window.peekDecorView()
                if (v != null && v.windowToken != null) {
                    val imm = getSystemService(
                        INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Do not call super here
        mSavedInstanceState = savedInstanceState
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, workspace!!.currentScreen)
        super.onSaveInstanceState(outState)
        if (mAddItemCellInfo != null && mAddItemCellInfo!!.valid && mWaitingForResult) {
            val addItemCellInfo: CellLayout.CellInfo = mAddItemCellInfo as CellLayout.CellInfo
            val layout = workspace!!.getChildAt(addItemCellInfo.screen) as CellLayout
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, addItemCellInfo.screen)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, addItemCellInfo.cellX)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, addItemCellInfo.cellY)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, addItemCellInfo.spanX)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, addItemCellInfo.spanY)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X, layout.countX)
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y, layout.countY)
            outState.putBooleanArray(
                RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS,
                layout.occupiedCells
            )
        }
    }

    public override fun onDestroy() {
        mDestroyed = true
        super.onDestroy()
        try {
            appWidgetHost!!.stopListening()
        } catch (ex: NullPointerException) {
            Log.w(LogTag, "problem while stopping AppWidgetHost during Launcher destruction", ex)
        }
        TextKeyListener.getInstance().release()
        model.unbind()
        model.abortLoaders()
        contentResolver.unregisterContentObserver(mObserver)
        try {
            if (this::offReceiver.isInitialized)
                unregisterReceiver(offReceiver)
        } catch (ignored: Exception) { }
    }

    private val requestCreateAppWidget = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        mWaitingForResult = false
        val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == RESULT_CANCELED) {
            if (appWidgetId != -1) {
                appWidgetHost!!.deleteAppWidgetId(appWidgetId)
            }
        } else {
            completeAddAppWidget(
                appWidgetId, mAddItemCellInfo, !isWorkspaceLocked
            )
        }
    }

    private fun addAppWidget(appWidgetId: Int) {
        mWaitingForResult = true
        val appWidget = mAppWidgetManager!!.getAppWidgetInfo(appWidgetId) ?: return
        if (null != appWidget.configure) {
            try {
                appWidgetHost?.startAppWidgetConfigureActivityForResult(
                    this, appWidgetId, 0, requestCreateAppWidgetHost,
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
            completeAddAppWidget(appWidgetId, mAddItemCellInfo, !isWorkspaceLocked)
        }
    }

    private fun findSingleSlot(cellInfo: CellLayout.CellInfo): Boolean {
        val xy = IntArray(2)
        if (findSlot(cellInfo, xy, 1, 1)) {
            cellInfo.cellX = xy[0]
            cellInfo.cellY = xy[1]
            return true
        }
        return false
    }

    private fun findSlot(
        info: CellLayout.CellInfo?,
        xy: IntArray,
        spanX: Int,
        spanY: Int
    ): Boolean {
        var cellInfo = info
        if (!cellInfo!!.findCellForSpan(xy, spanX, spanY)) {
            val occupied = if (mSavedState != null) mSavedState!!.getBooleanArray(
                RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS
            ) else null
            cellInfo = workspace!!.findAllVacantCells(occupied)
            if (!cellInfo!!.findCellForSpan(xy, spanX, spanY)) {
                Toast.makeText(ScaledContext.wrap(this), getString(R.string.out_of_space), Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    /**
     * When the notification that favorites have changed is received, requests
     * a favorites list refresh.
     */
    private fun onFavoritesChanged() {
        isWorkspaceLocked = true
        model.loadUserItems(false, this, false)
    }

    fun onDesktopItemsLoaded(
        shortcuts: ArrayList<WidgetInfo?>?,
        appWidgets: ArrayList<CoverWidgetInfo>?
    ) {
        if (mDestroyed) {
            if (WidgetModel.DEBUG_LOADERS) {
                Log.d(
                    WidgetModel.LOG_TAG,
                    "  ------> destroyed, ignoring desktop items"
                )
            }
            return
        }
        bindDesktopItems(shortcuts, appWidgets)
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     */
    private fun bindDesktopItems(
        shortcuts: ArrayList<WidgetInfo?>?,
        appWidgets: ArrayList<CoverWidgetInfo>?
    ) {
        val workspace = workspace
        val count = workspace!!.childCount
        for (i in 0 until count) {
            (workspace.getChildAt(i) as ViewGroup).removeAllViewsInLayout()
        }

        // Flag any old binder to terminate early
        if (mBinder != null) {
            mBinder!!.mTerminate = true
        }
        mBinder = DesktopBinder(this, shortcuts, appWidgets)
        mBinder!!.startBindingItems()
    }

    private fun bindItems(
        binder: DesktopBinder,
        shortcuts: ArrayList<WidgetInfo?>?, start: Int, count: Int
    ) {
        val end = (start + DesktopBinder.ITEMS_COUNT).coerceAtMost(count)
        var i = start
        while (i < end) {
            shortcuts!![i]
            i++
        }
        workspace!!.requestLayout()
        if (end >= count) {
            finishBindDesktopItems()
            binder.startBindingAppWidgetsWhenIdle()
        } else {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_ITEMS, i, count).sendToTarget()
        }
    }

    private fun finishBindDesktopItems() {
        if (mSavedState != null) {
            if (!workspace!!.hasFocus()) {
                workspace!!.getChildAt(workspace!!.currentScreen).requestFocus()
            }
            mSavedState = null
        }
        if (mSavedInstanceState != null) {
            super.onRestoreInstanceState(mSavedInstanceState!!)
            mSavedInstanceState = null
        }
        isWorkspaceLocked = false
    }

    private fun bindAppWidgets(
        binder: DesktopBinder,
        appWidgets: LinkedList<CoverWidgetInfo>
    ) {
        val workspace = workspace
        val desktopLocked = isWorkspaceLocked
        if (!appWidgets.isEmpty()) {
            val item = appWidgets.removeFirst()
            val appWidgetId = item.appWidgetId
            val appWidgetInfo = mAppWidgetManager!!.getAppWidgetInfo(appWidgetId)
            item.hostView = appWidgetHost!!.createView(
                widgetContext, appWidgetId, appWidgetInfo)
            if (LOGD) {
                Log.d(
                    LogTag, String.format(
                        "about to setAppWidget for id=%d, info=%s",
                        appWidgetId, appWidgetInfo
                    )
                )
            }
            item.hostView!!.setAppWidget(appWidgetId, appWidgetInfo)
            item.hostView!!.tag = item
            workspace!!.addInScreen(
                item.hostView, item.screen, item.cellX,
                item.cellY, item.spanX, item.spanY, !desktopLocked
            )
            workspace.requestLayout()
        }
        if (!appWidgets.isEmpty()) {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_APPWIDGETS).sendToTarget()
        }
    }

    override fun onClick(v: View) {
        v.tag
    }

    override fun onLongClick(v: View): Boolean {
        var view = v
        if (isWorkspaceLocked) {
            return false
        }
        if (view !is CellLayout) {
            view = view.parent as View
        }
        val cellInfo = view.tag as CellLayout.CellInfo

        if (workspace!!.allowLongPress()) {
            if (cellInfo.cell == null) {
                if (cellInfo.valid) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                                .defaultVibrator.vibrate(VibrationEffect.createOneShot(
                                30, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                            .vibrate(VibrationEffect.createOneShot(
                            30, VibrationEffect.DEFAULT_AMPLITUDE))
                    }

                    // User long pressed on empty space
                    workspace!!.setAllowLongPress(false)
                    showAddDialog(cellInfo)
                }
            } else {
                workspace!!.startDrag(cellInfo)
            }
        }
        return true
    }

    private fun showAddDialog(cellInfo: CellLayout.CellInfo) {

        mWaitingForResult = true
        mAddItemCellInfo = cellInfo
        workspace?.unlock()
        val appWidgetId = appWidgetHost!!.allocateAppWidgetId()

        val view: View = layoutInflater.inflate(R.layout.panel_preview_dialog, null)
        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(this, R.style.DialogTheme_NoActionBar)
        )
        val previews = view.findViewById<LinearLayout>(R.id.previews_layout)
        dialog.setOnCancelListener {
            previews.removeAllViews()
        }
        val widgetDialog: Dialog = dialog.setView(view).show()
        val mWidgetPreviewLoader = WidgetPreviewLoader(this)
        val infoList: List<AppWidgetProviderInfo> = mAppWidgetManager!!.installedProviders
        for (info: AppWidgetProviderInfo in infoList) {
            var spanX: Int = info.minWidth
            var spanY: Int = info.minHeight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                spanX = max(info.minWidth, info.maxResizeWidth)
                spanY = max(info.minHeight, info.maxResizeHeight)
            }
            val maxWidth: Int = workspace!!.getChildAt(cellInfo!!.screen).width
            val maxHeight: Int = workspace!!.getChildAt(cellInfo!!.screen).height
            val previewSizeBeforeScale = IntArray(1)
            val preview = mWidgetPreviewLoader.generateWidgetPreview(
                info, spanX, spanY, maxWidth, maxHeight, null, previewSizeBeforeScale
            )
            val previewImage = layoutInflater.inflate(
                R.layout.panel_preview_image, null) as AppCompatImageView
            previewImage.adjustViewBounds = true
            previewImage.setImageBitmap(preview)
            previewImage.setOnClickListener {
                val success = mAppWidgetManager!!.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
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
                widgetDialog.dismiss()
            }
            previews.addView(previewImage)
        }
        widgetDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private inner class FavoritesChangeObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            onFavoritesChanged()
        }
    }

    private class DesktopBinder(
        launcher: SamSprungPanels, shortcuts: ArrayList<WidgetInfo?>?,
        appWidgets: ArrayList<CoverWidgetInfo>?
    ) : Handler(Looper.getMainLooper()), IdleHandler {
        private val mShortcuts: ArrayList<WidgetInfo?>? = shortcuts
        private val mAppWidgets: LinkedList<CoverWidgetInfo>
        private val mLauncher: SoftReference<SamSprungPanels> = SoftReference(launcher)
        var mTerminate = false
        fun startBindingItems() {
            if (WidgetModel.DEBUG_LOADERS) Log.d(LogTag, "------> start binding items")
            obtainMessage(MESSAGE_BIND_ITEMS, 0, mShortcuts!!.size).sendToTarget()
        }

        fun startBindingAppWidgetsWhenIdle() {
            // Ask for notification when message queue becomes idle
            val messageQueue = Looper.myQueue()
            messageQueue.addIdleHandler(this)
        }

        override fun queueIdle(): Boolean {
            // Queue is idle, so start binding items
            startBindingAppWidgets()
            return false
        }

        fun startBindingAppWidgets() {
            obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget()
        }

        override fun handleMessage(msg: Message) {
            val launcher = mLauncher.get()
            if (launcher == null || mTerminate) {
                return
            }
            when (msg.what) {
                MESSAGE_BIND_ITEMS -> {
                    launcher.bindItems(this, mShortcuts, msg.arg1, msg.arg2)
                }
                MESSAGE_BIND_APPWIDGETS -> {
                    launcher.bindAppWidgets(this, mAppWidgets)
                }
            }
        }

        companion object {
            const val MESSAGE_BIND_ITEMS = 0x1
            const val MESSAGE_BIND_APPWIDGETS = 0x2

            // Number of items to bind in every pass
            const val ITEMS_COUNT = 6
        }

        init {
            // Sort widgets so active workspace is bound first
            val currentScreen = launcher.workspace!!.currentScreen
            val size = appWidgets!!.size
            mAppWidgets = LinkedList()
            for (i in 0 until size) {
                val appWidgetInfo = appWidgets[i]
                if (appWidgetInfo.screen == currentScreen) {
                    mAppWidgets.addFirst(appWidgetInfo)
                } else {
                    mAppWidgets.addLast(appWidgetInfo)
                }
            }
            if (WidgetModel.DEBUG_LOADERS) {
                Log.d(LogTag, "------> binding " + shortcuts!!.size + " items")
                Log.d(LogTag, "------> binding " + appWidgets.size + " widgets")
            }
        }
    }

    private fun readConfiguration(context: Context, configuration: LocaleConfiguration) {
        Executors.newSingleThreadExecutor().execute {
            var `in`: DataInputStream? = null
            try {
                `in` = DataInputStream(context.openFileInput(PREFERENCES))
                configuration.locale = `in`.readUTF()
                configuration.mcc = `in`.readInt()
                configuration.mnc = `in`.readInt()
            } catch (e: FileNotFoundException) {
                // Ignore
            } catch (e: IOException) {
                // Ignore
            } finally {
                if (`in` != null) {
                    try {
                        `in`.close()
                    } catch (e: IOException) {
                        // Ignore
                    }
                }
            }
        }
    }

    private fun writeConfiguration(context: Context, configuration: LocaleConfiguration) {
        Executors.newSingleThreadExecutor().execute {
            var out: DataOutputStream? = null
            try {
                out = DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE))
                out.writeUTF(configuration.locale)
                out.writeInt(configuration.mcc)
                out.writeInt(configuration.mnc)
                out.flush()
            } catch (e: FileNotFoundException) {
                // Ignore
            } catch (e: IOException) {
                context.getFileStreamPath(PREFERENCES).delete()
            } finally {
                if (out != null) {
                    try {
                        out.close()
                    } catch (e: IOException) {
                        // Ignore
                    }
                }
            }
        }
    }

    companion object {
        val LogTag: String = SamSprungPanels::class.java.name
        const val LOGD = false

        private const val PREFERENCES = "widget.preferences"

        // Type: int
        private const val RUNTIME_STATE_CURRENT_SCREEN = "widget.current_screen"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_SCREEN = "widget.add_screen"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_CELL_X = "widget.add_cellX"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_CELL_Y = "widget.add_cellY"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_SPAN_X = "widget.add_spanX"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_SPAN_Y = "widget.add_spanY"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_COUNT_X = "widget.add_countX"

        // Type: int
        private const val RUNTIME_STATE_PENDING_ADD_COUNT_Y = "widget.add_countY"

        // Type: int[]
        private const val RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS = "widget.add_occupied_cells"

        val model = WidgetModel()
        private val sLock = Any()

        const val APPWIDGET_HOST_ID = SamSprung.request_code

        private var sScreen = 1 // DEFAULT_SCREEN
        var screen: Int
            get() {
                synchronized(sLock) { return sScreen }
            }
            set(screen) {
                synchronized(sLock) { sScreen = screen }
            }
    }

    private fun getSpanForWidget(
        context: Context, component: ComponentName?, minWidth: Int,
        minHeight: Int
    ): IntArray {
        val padding: Rect = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null)
        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested
        val requiredWidth: Int = minWidth + padding.left + padding.right
        val requiredHeight: Int = minHeight + padding.top + padding.bottom
        return intArrayOf(requiredWidth, requiredHeight)
    }

    fun getSpanForWidget(context: Context, info: AppWidgetProviderInfo): IntArray {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight)
    }

    @Suppress("UNUSED")
    fun getMinSpanForWidget(context: Context, info: AppWidgetProviderInfo): IntArray {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight)
    }

    private val requestCreateAppWidgetHost = 9001

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mWaitingForResult = false

        // We have special handling for widgets
        if (requestCode == requestCreateAppWidgetHost) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == RESULT_CANCELED) {
                if (appWidgetId != -1) {
                    appWidgetHost!!.deleteAppWidgetId(appWidgetId)
                }
            } else {
                completeAddAppWidget(
                    appWidgetId, mAddItemCellInfo, !isWorkspaceLocked
                )
            }
            return
        }
    }
}