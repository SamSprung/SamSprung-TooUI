package com.eightbit.samsprung

/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.*
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.*
import com.eightbit.samsprung.launcher.AppDisplayListener
import com.eightbit.samsprung.launcher.NotificationAdapter
import com.eightbit.samsprung.panels.*
import com.eightbit.widget.RecyclerViewTouch
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SamSprungOverlay : FragmentActivity(), NotificationAdapter.OnNoticeClickListener {

    private lateinit var prefs: SharedPreferences
    private var mDisplayListener: DisplayManager.DisplayListener? = null
    private var widgetHandler: WidgetHandler? = null

    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var camManager: CameraManager

    private var isTorchEnabled = false

    private lateinit var battReceiver: BroadcastReceiver
    private lateinit var offReceiver: BroadcastReceiver
    private lateinit var noticesView: RecyclerView

    private lateinit var bottomHandle: View
    private lateinit var bottomSheetBehaviorMain: BottomSheetBehavior<View>
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: FragmentStateAdapter
    private lateinit var searchView: SearchView

    private val mObserver: ContentObserver = FavoritesChangeObserver()

    private var mDestroyed = false
    private var mBinder: DesktopBinder? = null
    private var textSpeech: TextToSpeech? = null


    fun getBottomSheetMain() : BottomSheetBehavior<View> {
        return bottomSheetBehaviorMain
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)

        super.onCreate(savedInstanceState)
        actionBar?.hide()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT)

        window.attributes.width = ViewGroup.LayoutParams.MATCH_PARENT
        window.attributes.gravity = Gravity.BOTTOM
        // window.setBackgroundDrawable(null)

        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)

        ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen_NoActionBar)
        setContentView(R.layout.home_main_view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mDisplayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(display: Int) {}
            override fun onDisplayChanged(display: Int) {
                if (display == 0) {
                    onDismiss()
                }
            }

            override fun onDisplayRemoved(display: Int) {}
        }
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(
            mDisplayListener, Handler(Looper.getMainLooper())
        )

        val coordinator = findViewById<CoordinatorLayout>(R.id.coordinator)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            coordinator.background =
                WallpaperManager.getInstance(this).drawable
        }

        val blurView = findViewById<BlurView>(R.id.blurContainer)
        blurView.setupWith(
            window.decorView.findViewById(R.id.coordinator))
            .setFrameClearDrawable(window.decorView.background)
            .setBlurRadius(1f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
            .setBlurAlgorithm(RenderScriptBlur(this))

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager).adapter
        nfcAdapter = (getSystemService(NFC_SERVICE) as NfcManager).defaultAdapter
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        camManager = getSystemService(CAMERA_SERVICE) as CameraManager
        camManager.registerTorchCallback(object: CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchEnabled = enabled
            }
        }, null)

        val batteryLevel = findViewById<TextView>(R.id.battery_status)
        battReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    Handler(Looper.getMainLooper()).post {
                        batteryLevel.text = String.format("%d%%",
                            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100))
                    }
                }
            }
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }.also {
            registerReceiver(battReceiver, it)
        }
        val toggleStats = findViewById<LinearLayout>(R.id.toggle_status)
        val clock = findViewById<TextClock>(R.id.clock_status)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.cover_quick_toggles)

        var color = configureMenuIcons(toolbar)
        batteryLevel.setTextColor(color)
        clock.setTextColor(color)

        val wifiEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (wifiManager.isWifiEnabled)
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_on_24)
            else
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_off_24)
            toolbar.menu.findItem(R.id.toggle_wifi).icon.setTint(color)
        }

        val nfcEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter.isEnabled)
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_on_24)
            else
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_off_24)
            toolbar.menu.findItem(R.id.toggle_nfc).icon.setTint(color)
        }

        textSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int? = textSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    textSpeech = null
                }
            }
        }

        noticesView = findViewById(R.id.notificationList)
        noticesView.layoutManager = LinearLayoutManager(this)
        val noticeAdapter = NotificationAdapter(this, this@SamSprungOverlay)
        noticeAdapter.setHasStableIds(true)
        noticesView.adapter = noticeAdapter

        val bottomSheetBehavior: BottomSheetBehavior<View> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var hasConfigured = false
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    toolbar.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.toggle_wifi -> {
                                wifiEnabler.launch(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_bluetooth -> {
                                if (bluetoothAdapter.isEnabled) {
                                    bluetoothAdapter.disable()
                                    item.setIcon(R.drawable.ic_baseline_bluetooth_off_24)
                                } else {
                                    bluetoothAdapter.enable()
                                    item.setIcon(R.drawable.ic_baseline_bluetooth_on_24)
                                }
                                item.icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_nfc -> {
                                nfcEnabler.launch(Intent(Settings.Panel.ACTION_NFC))
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_sound -> {
                                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                    item.setIcon(R.drawable.ic_baseline_sound_off_24)
                                } else {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                    item.setIcon(R.drawable.ic_baseline_sound_on_24)
                                }
                                item.icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_dnd -> {
                                if (notificationManager.currentInterruptionFilter ==
                                    NotificationManager.INTERRUPTION_FILTER_ALL) {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_NONE)
                                    item.setIcon(R.drawable.ic_baseline_do_not_disturb_on_24)
                                } else {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_ALL)
                                    item.setIcon(R.drawable.ic_baseline_do_not_disturb_off_24)
                                }
                                item.icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_torch -> {
                                if (isTorchEnabled) {
                                    item.setIcon(R.drawable.ic_baseline_flashlight_off_24)
                                } else {
                                    item.setIcon(R.drawable.ic_baseline_flashlight_on_24)
                                }
                                item.icon.setTint(color)
                                camManager.setTorchMode(camManager.cameraIdList[0], !isTorchEnabled)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_widgets -> {
                                widgetHandler?.showAddDialog()
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                                return@setOnMenuItemClickListener false
                            }
                            else -> {
                                return@setOnMenuItemClickListener false
                            }
                        }
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    hasConfigured = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val info = findViewById<LinearLayout>(R.id.bottom_info)
                if (slideOffset > 0) {
                    if (slideOffset > 0.75) {
                        info.visibility = View.GONE
                        if (!hasConfigured) {
                            hasConfigured = true
                            color = configureMenuIcons(toolbar)
                            batteryLevel.setTextColor(color)
                            clock.setTextColor(color)
                        }
                    }
                } else {
                    toggleStats.removeAllViews()
                    for (i in 0 until toolbar.menu.size()) {
                        val enabled = prefs.getBoolean(toolbar.menu.getItem(i).title.toPref, true)
                        if (enabled) {
                            toolbar.menu.getItem(i).isVisible = true
                            toolbar.menu.getItem(i).icon.setTint(color)
                            val icon = layoutInflater.inflate(
                                R.layout.toggle_state_icon, null) as AppCompatImageView
                            icon.findViewById<AppCompatImageView>(R.id.toggle_icon)
                            icon.setImageDrawable(toolbar.menu.getItem(i).icon)
                            toggleStats.addView(icon)
                        } else {
                            toolbar.menu.getItem(i).isVisible = false
                        }
                    }
                    info.visibility = View.VISIBLE
                }
            }
        })

        for (i in 0 until toolbar.menu.size()) {
            val enabled = prefs.getBoolean(toolbar.menu.getItem(i).title.toPref, true)
            if (enabled) {
                toolbar.menu.getItem(i).isVisible = true
                val icon = layoutInflater.inflate(
                    R.layout.toggle_state_icon, null) as AppCompatImageView
                icon.findViewById<AppCompatImageView>(R.id.toggle_icon)
                icon.setImageDrawable(toolbar.menu.getItem(i).icon)
                toggleStats.addView(icon)
            } else {
                toolbar.menu.getItem(i).isVisible = false
            }
        }

        val delete: View = toolbar.findViewById(R.id.toggle_widgets)
        delete.setOnLongClickListener(View.OnLongClickListener {
            if (viewPager.currentItem != 0) {
                toolbar.menu.findItem(R.id.toggle_widgets)
                    .setIcon(R.drawable.ic_baseline_delete_forever_24)
                tactileFeedback()
                val index = viewPager.currentItem
                val fragment = (pagerAdapter as CoverStateAdapter)
                    .getFragment(index)
                val widget = fragment.getLayout()!!.getChildAt(0).tag
                if (widget is CoverWidgetInfo) {
                    viewPager.setCurrentItem(index - 1, true)
                    model.removeDesktopAppWidget(widget)
                    if (null != widgetHandler?.getAppWidgetHost()) {
                        widgetHandler!!.getAppWidgetHost()
                            .deleteAppWidgetId(widget.appWidgetId)
                    }
                    WidgetModel.deleteItemFromDatabase(
                        applicationContext, widget
                    )
                    (pagerAdapter as CoverStateAdapter).removeFragment(index)
                }
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                toolbar.menu.findItem(R.id.toggle_widgets)
                    .setIcon(R.drawable.ic_baseline_widgets_24)
            } else {
                Toast.makeText(this,
                    R.string.incompatible_fragment,
                    Toast.LENGTH_LONG).show()
            }
            return@OnLongClickListener true
        })

        RecyclerViewTouch(noticesView).setSwipeCallback(
            ItemTouchHelper.START or ItemTouchHelper.END,
            object: RecyclerViewTouch.SwipeCallback {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.START || direction == ItemTouchHelper.END) {
                    val notice = (viewHolder as NotificationAdapter.NoticeViewHolder).notice
                    if (notice.isClearable)
                        NotificationReceiver.getReceiver()?.cancelNotification(notice.key)
                }
            }
        })

        viewPager = findViewById(R.id.pager)
        pagerAdapter = CoverStateAdapter(this)
        viewPager.adapter = pagerAdapter

        searchView = findViewById(R.id.package_search)
        searchView.findViewById<LinearLayout>(R.id.search_bar)?.run {
            this.layoutParams = this.layoutParams.apply {
                height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    18f, resources.displayMetrics).toInt()
            }
        }

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    searchView.visibility = View.VISIBLE
                } else {
                    searchView.visibility = View.GONE
                }
            }
        })

        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            widgetHandler = WidgetHandler(this, viewPager, pagerAdapter as CoverStateAdapter,
                ScaledContext.getDisplayParams(this))
        }

        val handler = Handler(Looper.getMainLooper())
        val menuButton = findViewById<FloatingActionButton>(R.id.menu_fab)
        val fakeOverlay = findViewById<LinearLayout>(R.id.fake_overlay)
        bottomHandle = findViewById(R.id.bottom_handle)
        bottomSheetBehaviorMain = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_main))
        bottomSheetBehaviorMain.isHideable = false
        bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehaviorMain.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    coordinator.keepScreenOn = true
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    menuButton.visibility = View.GONE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    coordinator.keepScreenOn = false
                    coordinator.visibility = View.GONE
                    bottomSheetBehaviorMain.isDraggable = true
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                color = prefs.getInt(SamSprung.prefColors,
                    Color.rgb(255, 255, 255))
                if (slideOffset > 0) {
                    coordinator.visibility = View.VISIBLE
                    toggleStats.invalidate()
                    if (slideOffset > 0.5) {
                        bottomSheetBehaviorMain.isDraggable = false
                        fakeOverlay.visibility = View.GONE
                    }
                    if (bottomHandle.visibility != View.INVISIBLE) {
                        handler.removeCallbacksAndMessages(null)
                        bottomHandle.visibility = View.INVISIBLE
                    }
                } else {
                    fakeOverlay.visibility = View.VISIBLE
                    bottomHandle.setBackgroundColor(color)
                    menuButton.drawable.setTint(color)
                    bottomHandle.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
                    menuButton.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
                    if (!bottomHandle.isVisible) {
                        handler.postDelayed({
                            runOnUiThread {
                                bottomHandle.visibility = View.VISIBLE
                                menuButton.visibility = View.VISIBLE
                            }
                        }, 250)
                    }
                }
            }
        })


        menuButton.setOnClickListener {
            bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
        }
        menuButton.drawable.setTint(color)

        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            contentResolver.registerContentObserver(
                WidgetSettings.Favorites.CONTENT_URI, true, mObserver
            )
            model.loadUserItems(true, this)
        }
        coordinator.visibility = View.GONE
        initializeDrawer(null != intent?.action && SamSprung.launcher == intent.action)

        offReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    onDismiss()
                }
            }
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }.also {
            registerReceiver(offReceiver, it)
        }
    }

    fun getSearch() : SearchView {
        return searchView
    }

    companion object {
        val model = WidgetModel()
        const val APPWIDGET_HOST_ID = SamSprung.request_code
    }

    private fun prepareConfiguration() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        val mKeyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        (application as SamSprung).isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if ((application as SamSprung).isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }

        mKeyguardManager.requestDismissKeyguard(this,
            object : KeyguardManager.KeyguardDismissCallback() { })
    }

    private fun configureMenuIcons(toolbar: Toolbar) : Int {
        val color = prefs.getInt(SamSprung.prefColors,
            Color.rgb(255, 255, 255))

        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager

        if (wifiManager.isWifiEnabled)
            toolbar.menu.findItem(R.id.toggle_wifi)
                .setIcon(R.drawable.ic_baseline_wifi_on_24)
        else
            toolbar.menu.findItem(R.id.toggle_wifi)
                .setIcon(R.drawable.ic_baseline_wifi_off_24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                this@SamSprungOverlay,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (bluetoothAdapter.isEnabled)
                toolbar.menu.findItem(R.id.toggle_bluetooth)
                    .setIcon(R.drawable.ic_baseline_bluetooth_on_24)
            else
                toolbar.menu.findItem(R.id.toggle_bluetooth)
                    .setIcon(R.drawable.ic_baseline_bluetooth_off_24)
        } else {
            toolbar.menu.findItem(R.id.toggle_bluetooth).isVisible = false
        }

        if (nfcAdapter.isEnabled)
            toolbar.menu.findItem(R.id.toggle_nfc)
                .setIcon(R.drawable.ic_baseline_nfc_on_24)
        else
            toolbar.menu.findItem(R.id.toggle_nfc)
                .setIcon(R.drawable.ic_baseline_nfc_off_24)

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            toolbar.menu.findItem(R.id.toggle_sound)
                .setIcon(R.drawable.ic_baseline_sound_on_24)
        else
            toolbar.menu.findItem(R.id.toggle_sound)
                .setIcon(R.drawable.ic_baseline_sound_off_24)

        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (notificationManager.currentInterruptionFilter ==
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
                toolbar.menu.findItem(R.id.toggle_dnd)
                    .setIcon(R.drawable.ic_baseline_do_not_disturb_off_24)
            else
                toolbar.menu.findItem(R.id.toggle_dnd)
                    .setIcon(R.drawable.ic_baseline_do_not_disturb_on_24)
        } else {
            toolbar.menu.findItem(R.id.toggle_dnd).isVisible = false
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            if (isTorchEnabled) {
                toolbar.menu.findItem(R.id.toggle_torch)
                    .setIcon(R.drawable.ic_baseline_flashlight_on_24)
            } else {
                toolbar.menu.findItem(R.id.toggle_torch)
                    .setIcon(R.drawable.ic_baseline_flashlight_off_24)
            }
        } else {
            toolbar.menu.findItem(R.id.toggle_torch).isVisible = false
        }
        for (i in 0 until toolbar.menu.size()) {
            toolbar.menu.getItem(i).icon.setTint(color)
        }
        return color
    }

    private fun initializeDrawer(launcher: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            runOnUiThread {
                bottomHandle = findViewById(R.id.bottom_handle)
                bottomHandle.visibility = View.VISIBLE
                bottomHandle.setBackgroundColor(prefs.getInt(
                    SamSprung.prefColors,
                    Color.rgb(255, 255, 255)))
                bottomHandle.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
                if (launcher) {
                    bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            if (this::noticesView.isInitialized) {
                if (hasNotificationListener()) {
                    NotificationReceiver.getReceiver()?.setNotificationsListener(
                        noticesView.adapter as NotificationAdapter
                    )
                }
            }
        }, 150)
    }

    fun processIntentSender(intentSender: IntentSender) {
        prepareConfiguration()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        startIntentSender(intentSender, null, 0, 0, 0,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())

        Handler(Looper.getMainLooper()).postDelayed({
            runOnUiThread {
                val extras = Bundle()
                extras.putString("launchPackage", intentSender.creatorPackage)
                extras.putBoolean("intentSender", true)

                startForegroundService(
                    Intent(this,
                    AppDisplayListener::class.java).putExtras(extras))

                onDismiss()
            }
        }, 100)
    }

    private fun setNotificationAction(position: Int, actionsPanel: LinearLayout,
                                      action: Notification.Action) {
        val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
        val actionEntries = actionsPanel.findViewById<LinearLayout>(R.id.entries)
        val button = AppCompatButton(
            ContextThemeWrapper(this,
                R.style.Theme_SecondScreen_NoActionBar
        )
        )
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        button.setSingleLine()
        button.text = action.title
        button.setOnClickListener {
            actionEntries.visibility = View.GONE
            if (null != action.remoteInputs && action.remoteInputs.isNotEmpty()) {
                for (remoteInput in action.remoteInputs) {
                    if (remoteInput.allowFreeFormInput) {
                        val toolbar = findViewById<Toolbar>(R.id.toolbar)
                        actionEntries.visibility = View.VISIBLE
                        val reply = actionEntries.findViewById<EditText>(R.id.reply)
                        val send = actionEntries.findViewById<AppCompatImageView>(R.id.send)
                        reply.setOnFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                toolbar.visibility = View.GONE
                                (noticesView.layoutManager as LinearLayoutManager)
                                    .scrollToPositionWithOffset(position,
                                        -(actionButtons.height + reply.height)
                                    )
                            } else {
                                toolbar.visibility = View.VISIBLE
                            }
                        }
                        send.setOnClickListener {
                            val replyIntent = Intent()
                            val replyBundle = Bundle()
                            replyBundle.putCharSequence(remoteInput.resultKey, reply.text.toString())
                            RemoteInput.addResultsToIntent(action.remoteInputs, replyIntent, replyBundle)
                            startIntentSender(action.actionIntent.intentSender,
                                replyIntent, 0, 0, 0,
                                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
                            actionEntries.visibility = View.GONE
                        }
                    }
                }
            } else {
                processIntentSender(action.actionIntent.intentSender)
                actionsPanel.visibility = View.GONE
            }
        }
        actionButtons.addView(button, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT,
            if (action.title.length > 10) 1.5f else 1.0f
        ))
    }

    private fun setNotificationCancel(actionsPanel: LinearLayout, sbn: StatusBarNotification) {
        val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
        val button = AppCompatImageView(
            ContextThemeWrapper(this,
                R.style.Theme_SecondScreen_NoActionBar)
        )
        button.setImageDrawable(ContextCompat
            .getDrawable(this, R.drawable.ic_baseline_cancel_presentation_24))
        button.setOnClickListener {
            NotificationReceiver.getReceiver()?.setNotificationsShown(arrayOf(sbn.key))
            NotificationReceiver.getReceiver()?.cancelNotification(sbn.key)
            actionsPanel.visibility = View.GONE
        }
        val params = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f
        )
        params.gravity = Gravity.CENTER
        actionButtons.addView(button, params)
    }

    override fun onNoticeClicked(itemView: View, position: Int, notice: StatusBarNotification) {
        val actionsPanel = itemView.findViewById<LinearLayout>(R.id.action_panel)
        if (actionsPanel.isVisible) {
            actionsPanel.visibility = View.GONE
        } else {
            val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
            if (actionButtons.childCount > 0) {
                actionsPanel.visibility = View.VISIBLE
            } else {
                actionsPanel.visibility = View.VISIBLE
                if (null != notice.notification.actions) {
                    for (action in notice.notification.actions) {
                        setNotificationAction(position, actionsPanel, action)
                    }
                    (noticesView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(position,
                            -(actionButtons.height))
                }
                if (notice.isClearable) {
                    setNotificationCancel(actionsPanel, notice)
                    (noticesView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(position,
                            -(actionButtons.height))
                }
            }
        }
    }

    override fun onNoticeLongClicked(itemView: View, position: Int,
                                     notice: StatusBarNotification
    ) : Boolean {
        tactileFeedback()
        textSpeech?.speak(itemView.findViewById<TextView>(R.id.lines).text,
            TextToSpeech.QUEUE_ADD, null, SamSprung.notification)
        return true
    }

    private fun tactileFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(
                    VibrationEffect
                        .createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect
                    .createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun hasNotificationListener(): Boolean {
        val myNotificationListenerComponentName = ComponentName(
            applicationContext, NotificationReceiver::class.java)
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners")
        if (enabledListeners.isEmpty()) return false
        return enabledListeners.split(":").map {
            ComponentName.unflattenFromString(it)
        }.any {componentName->
            myNotificationListenerComponentName == componentName
        }
    }

    private fun onFavoritesChanged() {
        model.loadUserItems(false, this)
    }

    @SuppressLint("InflateParams")
    fun bindAppWidgets(
        binder: DesktopBinder,
        appWidgets: LinkedList<CoverWidgetInfo>
    ) {
        widgetHandler?.bindAppWidgets(binder, appWidgets)
    }

    fun bindItems(
        binder: DesktopBinder,
        shortcuts: ArrayList<WidgetInfo?>?, start: Int, count: Int
    ) {
        val end = (start + DesktopBinder.ITEMS_COUNT).coerceAtMost(count)
        var i = start
        while (i < end) {
            shortcuts!![i]
            i++
        }
        if (end >= count) {
            binder.startBindingAppWidgetsWhenIdle()
        } else {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_ITEMS, i, count).sendToTarget()
        }
    }

    fun onDesktopItemsLoaded(
    shortcuts: ArrayList<WidgetInfo?>?,
    appWidgets: ArrayList<CoverWidgetInfo>?
    ) {
        if (mDestroyed) {
            return
        }
        // Flag any old binder to terminate early
        if (mBinder != null) {
            mBinder!!.mTerminate = true
        }
        mBinder = DesktopBinder(this, shortcuts, appWidgets)
        mBinder!!.startBindingItems()
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        if (mBinder != null) {
            mBinder!!.mTerminate = true
        }
        // return lastNonConfigurationInstance
        return null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initializeDrawer(null != intent?.action && SamSprung.launcher == intent.action)
    }

    private val requestCreateAppWidgetHost = 9001

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // We have special handling for widgets
        if (requestCode == requestCreateAppWidgetHost) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == RESULT_CANCELED) {
                if (appWidgetId != -1) {
                    widgetHandler?.getAppWidgetHost()?.deleteAppWidgetId(appWidgetId)
                }
            } else {
                widgetHandler?.completeAddAppWidget(appWidgetId)
            }
            return
        }
    }

    fun onDismiss() {
        if (null != mDisplayListener) {
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(mDisplayListener)
        }
        try {
            if (this::battReceiver.isInitialized)
                unregisterReceiver(battReceiver)
        } catch (ignored: Exception) { }
        try {
            if (this::offReceiver.isInitialized)
                unregisterReceiver(offReceiver)
        } catch (ignored: Exception) { }
        finish()
    }

    public override fun onDestroy() {
        mDestroyed = true
        onDismiss()
        super.onDestroy()
        if (null != textSpeech) {
            textSpeech?.stop()
            textSpeech?.shutdown()
        }
        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            widgetHandler?.onDestroy()
            model.unbind()
            model.abortLoaders()
            contentResolver.unregisterContentObserver(mObserver)
        }
    }

    private inner class FavoritesChangeObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            onFavoritesChanged()
        }
    }

    private val CharSequence.toPref get() = this.toString()
        .lowercase().replace(" ", "_")

    override fun startActivities(intents: Array<out Intent>?, options: Bundle?) {
        if (null != options)
            super.startActivities(intents, options)
        else
            super.startActivities(intents,
                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivities(intents: Array<out Intent>?) {
        super.startActivities(intents,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivityIfNeeded(
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ): Boolean {
        return if (null != options)
            super.startActivityIfNeeded(intent, requestCode, options)
        else
            super.startActivityIfNeeded(intent, requestCode,
                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return super.startActivityIfNeeded(intent, requestCode,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        if (null != options)
            super.startActivity(intent, options)
        else
            super.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivityFromFragment(
        fragment: Fragment,
        intent: Intent?,
        requestCode: Int,
        options: Bundle?
    ) {
        if (null != options)
            super.startActivityFromFragment(fragment, intent, requestCode, options)
        else
            super.startActivityFromFragment(fragment, intent, requestCode,
                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int) {
        super.startActivityFromFragment(fragment, intent, requestCode,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startIntentSender(
        intent: IntentSender?,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
        if (null != options)
            super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options)
        else
            super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags,
                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }

    override fun startIntentSender(
        intent: IntentSender?,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int
    ) {
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
    }
}