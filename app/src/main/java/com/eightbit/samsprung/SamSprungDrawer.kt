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
import android.app.KeyguardManager.KeyguardDismissCallback
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.content.ScaledContext
import com.eightbit.view.OnSwipeTouchListener
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import java.io.File
import java.util.*
import java.util.concurrent.Executors


class SamSprungDrawer : AppCompatActivity(),
    DrawerAppAdapater.OnAppClickListener,
    NotificationAdapter.OnNoticeClickListener {

    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var camManager: CameraManager

    private var isTorchEnabled = false

    private lateinit var prefs: SharedPreferences
    private lateinit var oReceiver: BroadcastReceiver
    private lateinit var bReceiver: BroadcastReceiver
    private lateinit var pReceiver: BroadcastReceiver
    private lateinit var noticesView: RecyclerView

    private val SCREW_YOU_JAGAN2 = "SCREW YOU JAGAN2 ;)"

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)

        if (null != DisplayListener.getFakeOrientationLock()
            && DisplayListener.getFakeOrientationLock()!!.isAttachedToWindow) {
            (SamSprung.getCoverContext()?.getSystemService(Context.WINDOW_SERVICE)
                    as WindowManager).removeView(DisplayListener.getFakeOrientationLock())
        }

        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        supportActionBar?.hide()
        ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen_NoActionBar)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        setContentView(R.layout.drawer_layout)

        oReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    finish()
                }
            }
        }
        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            registerReceiver(oReceiver, it)
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            findViewById<CoordinatorLayout>(R.id.coordinator).background =
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

        val batteryLevel = findViewById<TextView>(R.id.battery_status)
        bReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    Handler(Looper.getMainLooper()).post {
                        batteryLevel.text = String.format("%d%%",
                            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100))
                    }
                }
            }
        }

        IntentFilter(Intent.ACTION_BATTERY_CHANGED).also {
            registerReceiver(bReceiver, it)
        }

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager).adapter
        nfcAdapter = (getSystemService(NFC_SERVICE) as NfcManager).defaultAdapter
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        camManager = getSystemService(CAMERA_SERVICE) as CameraManager
        camManager.registerTorchCallback(object: TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchEnabled = enabled
            }
        }, null)

        val toggleStats = findViewById<LinearLayout>(R.id.toggle_status)
        val clock = findViewById<TextClock>(R.id.clock_status)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.cover_quick_toggles)

        var color = configureMenuIcons(toolbar)
        batteryLevel.setTextColor(color)
        clock.setTextColor(color)

        for (i in 0 until toolbar.menu.size()) {
            val icon = layoutInflater.inflate(
                R.layout.toggle_status, null) as AppCompatImageView
            icon.findViewById<AppCompatImageView>(R.id.toggle_icon)
            icon.background = toolbar.menu.getItem(i).icon
            toggleStats.addView(icon)
        }

        noticesView = findViewById(R.id.notificationList)


        noticesView.layoutManager = LinearLayoutManager(this)
        noticesView.adapter = NotificationAdapter(this, this@SamSprungDrawer)
        val noticeTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) { }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.RIGHT) {
                    val notice = (viewHolder as NotificationAdapter.NoticeViewHolder).notice
                    if (null != notice.getKey()) {
                        NotificationObserver.getObserver()
                            ?.setNotificationsShown(arrayOf(notice.getKey()))
                    }
                }
            }
        }
        ItemTouchHelper(noticeTouchCallback).attachToRecyclerView(noticesView)
        onNewIntent(intent)

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

        val bottomSheetBehavior: BottomSheetBehavior<View> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {

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
                                    toolbar.menu.findItem(R.id.toggle_bluetooth)
                                        .setIcon(R.drawable.ic_baseline_bluetooth_off_24)
                                } else {
                                    bluetoothAdapter.enable()
                                    toolbar.menu.findItem(R.id.toggle_bluetooth)
                                        .setIcon(R.drawable.ic_baseline_bluetooth_on_24)
                                }
                                toolbar.menu.findItem(R.id.toggle_bluetooth).icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_nfc -> {
                                nfcEnabler.launch(Intent(Settings.Panel.ACTION_NFC))
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_sound -> {
                                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                    toolbar.menu.findItem(R.id.toggle_sound)
                                        .setIcon(R.drawable.ic_baseline_sound_off_24)
                                } else {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                    toolbar.menu.findItem(R.id.toggle_sound)
                                        .setIcon(R.drawable.ic_baseline_sound_on_24)
                                }
                                toolbar.menu.findItem(R.id.toggle_sound).icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_dnd -> {
                                if (notificationManager.currentInterruptionFilter ==
                                    NotificationManager.INTERRUPTION_FILTER_ALL) {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_NONE)
                                    toolbar.menu.findItem(R.id.toggle_dnd)
                                        .setIcon(R.drawable.ic_baseline_do_not_disturb_on_24)
                                } else {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_ALL)
                                    toolbar.menu.findItem(R.id.toggle_dnd)
                                        .setIcon(R.drawable.ic_baseline_do_not_disturb_off_24)
                                }
                                toolbar.menu.findItem(R.id.toggle_dnd).icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
//                            R.id.toggle_rotation -> {
//                                if (prefs.getInt(SamSprung.autoRotate, 1) == 1) {
//                                    toolbar.menu.findItem(R.id.toggle_rotation)
//                                        .setIcon(R.drawable.ic_baseline_screen_lock_rotation_24)
//                                    with(prefs.edit()) {
//                                        putInt(SamSprung.autoRotate, 0)
//                                        apply()
//                                    }
//                                } else {
//                                    toolbar.menu.findItem(R.id.toggle_rotation)
//                                        .setIcon(R.drawable.ic_baseline_screen_rotation_24)
//                                    with(prefs.edit()) {
//                                        putInt(SamSprung.autoRotate, 1)
//                                        apply()
//                                    }
//                                }
//                                toolbar.menu.findItem(R.id.toggle_rotation).icon.setTint(color)
//                                return@setOnMenuItemClickListener true
//                            }
                            R.id.toggle_torch -> {
                                if (isTorchEnabled) {
                                    toolbar.menu.findItem(R.id.toggle_torch)
                                        .setIcon(R.drawable.ic_baseline_flashlight_off_24)
                                } else {
                                    toolbar.menu.findItem(R.id.toggle_torch)
                                        .setIcon(R.drawable.ic_baseline_flashlight_on_24)
                                }
                                toolbar.menu.findItem(R.id.toggle_torch).icon.setTint(color)
                                camManager.setTorchMode(camManager.cameraIdList[0], !isTorchEnabled)
                                return@setOnMenuItemClickListener true
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
                if (slideOffset > 0.75) {
                    info.visibility = View.GONE
                    if (!hasConfigured) {
                        hasConfigured = true
                        color = configureMenuIcons(toolbar)
                        batteryLevel.setTextColor(color)
                        clock.setTextColor(color)
                    }
                } else {
                    toggleStats.removeAllViewsInLayout()
                    for (i in 0 until toolbar.menu.size()) {
                        toolbar.menu.getItem(i).icon.setTint(color)
                        val icon = layoutInflater.inflate(
                            R.layout.toggle_status, null) as AppCompatImageView
                        icon.findViewById<AppCompatImageView>(R.id.toggle_icon)
                        icon.background = toolbar.menu.getItem(i).icon
                        toggleStats.addView(icon)
                    }
                    info.visibility = View.VISIBLE
                }
            }
        })

        val launcherView = findViewById<RecyclerView>(R.id.appsList)

        val packageRetriever = PackageRetriever(this)
        var packages = packageRetriever.getFilteredPackageList()

        if (prefs.getBoolean(SamSprung.prefLayout, true))
            launcherView.layoutManager = GridLayoutManager(this, getColumnCount())
        else
            launcherView.layoutManager = LinearLayoutManager(this)
        launcherView.adapter = DrawerAppAdapater(packages, this, packageManager, prefs)

        pReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                    Executors.newSingleThreadExecutor().execute {
                        packages = packageRetriever.getFilteredPackageList()
                        runOnUiThread { (launcherView.adapter as DrawerAppAdapater)
                            .setPackages(packages) }
                    }
                }
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        Executors.newSingleThreadExecutor().execute {
                            packages = packageRetriever.getFilteredPackageList()
                            runOnUiThread { (launcherView.adapter as DrawerAppAdapater)
                                .setPackages(packages) }
                        }
                    }
                }
            }
        }
        IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }.also {
            registerReceiver(pReceiver, it)
        }

        val searchWrapper = findViewById<FrameLayout>(R.id.search_wrapper)
        val searchView = findViewById<SearchView>(R.id.package_search)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.isSubmitButtonEnabled = false
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                (launcherView.adapter as DrawerAppAdapater).setQuery(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                (launcherView.adapter as DrawerAppAdapater).setQuery(query)
                return true
            }
        })
        searchWrapper.visibility = View.GONE

        val mKeyboardView = if (hasAccessibility())
            getKeyboard(searchWrapper, ScaledContext.wrap(this)) else null

        SamSprungInput.setInputListener(object : SamSprungInput.InputMethodListener {
            override fun onInputRequested(instance: SamSprungInput) {
                if (!mKeyboardView!!.isShown)
                    searchWrapper.addView(mKeyboardView, 0)
            }

            override fun onKeyboardHidden(isHidden: Boolean?) {
                if (searchWrapper.isVisible)
                    searchWrapper.visibility = View.GONE
            }
        })

        val drawerTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) { }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    if (searchView.query.isNotBlank()) {
                        (launcherView.adapter as DrawerAppAdapater).setQuery("")
                        if (searchWrapper.isVisible)
                            searchWrapper.visibility = View.GONE
                    } else {
                        finish()
                        startActivity(
                            Intent(applicationContext, SamSprungOverlay::class.java),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                }
                if (direction == ItemTouchHelper.RIGHT) {
                    if (searchView.query.isNotBlank()) {
                        (launcherView.adapter as DrawerAppAdapater).setQuery("")
                        if (searchWrapper.isVisible)
                            searchWrapper.visibility = View.GONE
                    } else {
                        searchWrapper.visibility = View.VISIBLE
                    }
                }
            }
        }
        ItemTouchHelper(drawerTouchCallback).attachToRecyclerView(launcherView)
        launcherView.setOnTouchListener(object : OnSwipeTouchListener(this@SamSprungDrawer) {
            override fun onSwipeLeft() : Boolean {
                if (searchView.query.isNotBlank()) {
                    (launcherView.adapter as DrawerAppAdapater).setQuery("")
                    if (searchWrapper.isVisible)
                        searchWrapper.visibility = View.GONE
                } else {
                    finish()
                    startActivity(
                        Intent(applicationContext, SamSprungOverlay::class.java),
                        ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                    )
                }
                return true
            }
            override fun onSwipeRight() : Boolean {
                if (searchView.query.isNotBlank()) {
                    (launcherView.adapter as DrawerAppAdapater).setQuery("")
                    if (searchWrapper.isVisible)
                        searchWrapper.visibility = View.GONE
                } else {
                    searchWrapper.visibility = View.VISIBLE
                }
                return true
            }
            override fun onSwipeBottom() : Boolean {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) return false
                if (launcherView.layoutManager is LinearLayoutManager) {
                    if ((launcherView.layoutManager as LinearLayoutManager)
                            .findFirstCompletelyVisibleItemPosition() == 0) {
                        finish()
                        startActivity(
                            Intent(this@SamSprungDrawer, SamSprungWidget::class.java),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                        return true
                    }
                }
                if (launcherView.layoutManager is GridLayoutManager) {
                    if ((launcherView.layoutManager as GridLayoutManager)
                            .findFirstCompletelyVisibleItemPosition() == 0) {
                        finish()
                        startActivity(
                            Intent(this@SamSprungDrawer, SamSprungWidget::class.java),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun fakeOrientationLock() {
        val orientationChanger = LinearLayout(SamSprung.getCoverContext())
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowManager = SamSprung.getCoverContext()?.getSystemService(
            Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(orientationChanger, orientationLayout)
        orientationChanger.visibility = View.VISIBLE
        if (prefs.getInt(SamSprung.autoRotate, 1) == 1) {
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    windowManager.removeView(orientationChanger)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    finish()
                }
            }, 100)
        } else {
            DisplayListener.setFakeOrientationLock(orientationChanger)
            finish()
        }
    }

    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun getKeyboard (parent: ViewGroup, displayContext: Context) : KeyboardView {
        val mKeyboard = Keyboard(parent.context, R.xml.keyboard_qwerty)
        val mKeyboardView = LayoutInflater.from(displayContext)
            .inflate(R.layout.keyboard_view, null) as KeyboardView
        mKeyboardView.isPreviewEnabled = false
        mKeyboardView.keyboard = mKeyboard
        SamSprungInput.setInputMethod(mKeyboardView, mKeyboard, parent)
        AccessibilityObserver.enableKeyboard(displayContext)
        mKeyboardView.elevation = 1F
        return mKeyboardView
    }

    private fun prepareConfiguration() {
        val mKeyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        SamSprung.isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if (SamSprung.isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }

        mKeyguardManager.requestDismissKeyguard(this,
            object : KeyguardDismissCallback() {
            override fun onDismissError() {
                super.onDismissError()
            }

            override fun onDismissSucceeded() {
                super.onDismissSucceeded()
            }

            override fun onDismissCancelled() {
                super.onDismissCancelled()
            }
        })
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
                this@SamSprungDrawer,
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

//        if (prefs.getInt(SamSprung.autoRotate, 1) == 1)
//            toolbar.menu.findItem(R.id.toggle_rotation)
//                .setIcon(R.drawable.ic_baseline_screen_rotation_24)
//        else
//            toolbar.menu.findItem(R.id.toggle_rotation)
//                .setIcon(R.drawable.ic_baseline_screen_lock_rotation_24)

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

    override fun onAppClicked(appInfo: ResolveInfo, position: Int) {
        prepareConfiguration()

        val extras = Bundle()
        extras.putString("launchPackage", appInfo.activityInfo.packageName)
        extras.putString("launchActivity", appInfo.activityInfo.name)

        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            applicationContext.registerReceiver(OffBroadcastReceiver(
                ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name)
            ), it)
        }
        val coverIntent = Intent(Intent.ACTION_MAIN)
        coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        coverIntent.component = ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
        try {
            val applicationInfo: ApplicationInfo =
                packageManager.getApplicationInfo(
                    appInfo.activityInfo.packageName, PackageManager.GET_META_DATA
                )
            applicationInfo.metaData.putString(
                "com.samsung.android.activity.showWhenLocked", "true"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        coverIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(coverIntent.putExtras(extras), options.toBundle())

        fakeOrientationLock()

        val serviceIntent = Intent(this, DisplayListener::class.java)
        startForegroundService(serviceIntent.putExtras(extras))
    }

    override fun onNoticeClicked(notice: SamSprungNotice, position: Int) {
        if (null != notice.getIntentSender()) {
            prepareConfiguration()

            startIntentSender(notice.getIntentSender(),
                null, 0, 0, 0)
            startForegroundService(Intent(this, DisplayListener::class.java)
                .putExtra("launchPackage", notice.getIntentSender()!!.creatorPackage))
        }
    }

    private fun getColumnCount(): Int {
        return (windowManager.currentWindowMetrics.bounds.width() / 96 + 0.5).toInt()
    }

    private fun hasAccessibility(): Boolean {
        val serviceString = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return serviceString != null && serviceString.contains(packageName
                + File.separator + AccessibilityObserver::class.java.name)
    }

    private fun hasNotificationListener(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (null != cn && TextUtils.equals(packageName, cn.packageName)) return true
            }
        }
        return false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        CheckUpdatesTask(this)
        if (!this::noticesView.isInitialized) return
        if (hasNotificationListener()) {
            NotificationObserver.getObserver()?.setNotificationsChangedListener(
                noticesView.adapter as NotificationAdapter
            )
        }
        if (hasAccessibility()) {
            AccessibilityObserver.getObserver()?.setEventsChangedListener(
                noticesView.adapter as NotificationAdapter
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityObserver.disableKeyboard(this)
        try {
            if (this::oReceiver.isInitialized)
                unregisterReceiver(oReceiver)
        } catch (ignored: Exception) { }
        try {
            if (this::bReceiver.isInitialized)
                unregisterReceiver(bReceiver)
        } catch (ignored: Exception) { }
        try {
            if (this::pReceiver.isInitialized)
                unregisterReceiver(pReceiver)
        } catch (ignored: Exception) { }
    }
}