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
import android.content.pm.*
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.*
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.*
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.eightbit.content.ScaledContext
import com.eightbit.material.IconifiedSnackbar
import com.eightbit.samsprung.launcher.CoverStateAdapter
import com.eightbit.samsprung.launcher.LauncherManager
import com.eightbit.samsprung.launcher.PanelWidgetManager
import com.eightbit.samsprung.panels.*
import com.eightbit.samsprung.speech.VoiceRecognizer
import com.eightbit.samsprung.settings.CheckUpdatesTask
import com.eightbit.view.AnimatedLinearLayout
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.*
import java.util.concurrent.Executors

class SamSprungOverlay : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var launcherManager: LauncherManager? = null
    private var widgetManager: PanelWidgetManager? = null
    val model = WidgetModel()
    private var appWidgetHost: WidgetHost? = null

    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var camManager: CameraManager

    private var isTorchEnabled = false

    private lateinit var battReceiver: BroadcastReceiver
    private lateinit var offReceiver: BroadcastReceiver

    private lateinit var bottomSheetBehaviorMain: BottomSheetBehavior<View>
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: FragmentStateAdapter
    private lateinit var searchView: SearchView

    private inner class FavoritesChangeObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            onFavoritesChanged()
        }
    }
    private val mObserver: ContentObserver = FavoritesChangeObserver()

    private var mBinder: DesktopBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        // setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        )

        window.attributes.width = ViewGroup.LayoutParams.MATCH_PARENT
        window.attributes.gravity = Gravity.BOTTOM
        // window.setBackgroundDrawable(null)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        ScaledContext.wrap(this).setTheme(R.style.Theme_Launcher_NoActionBar)
        setContentView(R.layout.home_main_view)

        offReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) onStopOverlay()
            }
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }.also {
            registerReceiver(offReceiver, it)
        }

        val mAppWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetHost = WidgetHost(applicationContext, APPWIDGET_HOST_ID)
        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            Executors.newSingleThreadExecutor().execute {
                recreateWidgetPreviewDb()
            }
            appWidgetHost.startListening()
        }

        val coordinator = findViewById<CoordinatorLayout>(R.id.coordinator)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            coordinator.background = WallpaperManager
                .getInstance(ScaledContext.cover(this)).drawable
        }

        val blurView = findViewById<BlurView>(R.id.blurContainer)
        blurView.setupWith(coordinator)
            .setFrameClearDrawable(coordinator.background)
            .setBlurRadius(1f).setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(false)
            .setBlurAlgorithm(RenderScriptBlur(this))

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        nfcAdapter = (getSystemService(NFC_SERVICE) as NfcManager).defaultAdapter
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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

        val clock = findViewById<TextClock>(R.id.clock_status)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.cover_quick_toggles)

        var color = configureMenuIcons(toolbar)
        batteryLevel.setTextColor(color)
        clock.setTextColor(color)

        val wifiEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (wifiManager.isWifiEnabled)
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_on_24dp)
            else
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_off_24dp)
            toolbar.menu.findItem(R.id.toggle_wifi).icon.setTint(color)
        }

        val nfcEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter.isEnabled)
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_on_24dp)
            else
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_off_24dp)
            toolbar.menu.findItem(R.id.toggle_nfc).icon.setTint(color)
        }

        val buttonAuth = findViewById<AppCompatImageView>(R.id.button_auth)
        val keyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        buttonAuth.isVisible = keyguardManager.isDeviceLocked

        val toggleStats = findViewById<LinearLayout>(R.id.toggle_status)
        val info = findViewById<LinearLayout>(R.id.bottom_info)
        val bottomSheetBehavior: BottomSheetBehavior<View> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var hasConfigured = false
            @SuppressLint("MissingPermission")
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
                                    item.setIcon(R.drawable.ic_baseline_bluetooth_off_24dp)
                                } else {
                                    bluetoothAdapter.enable()
                                    item.setIcon(R.drawable.ic_baseline_bluetooth_on_24dp)
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
                                    item.setIcon(R.drawable.ic_baseline_sound_off_24dp)
                                } else {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                    item.setIcon(R.drawable.ic_baseline_sound_on_24dp)
                                }
                                item.icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_dnd -> {
                                if (notificationManager.currentInterruptionFilter ==
                                    NotificationManager.INTERRUPTION_FILTER_ALL) {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_NONE)
                                    item.setIcon(R.drawable.ic_baseline_do_not_disturb_on_24dp)
                                } else {
                                    notificationManager.setInterruptionFilter(
                                        NotificationManager.INTERRUPTION_FILTER_ALL)
                                    item.setIcon(R.drawable.ic_baseline_do_not_disturb_off_24dp)
                                }
                                item.icon.setTint(color)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_torch -> {
                                if (isTorchEnabled) {
                                    item.setIcon(R.drawable.ic_baseline_flashlight_off_24dp)
                                } else {
                                    item.setIcon(R.drawable.ic_baseline_flashlight_on_24dp)
                                }
                                item.icon.setTint(color)
                                camManager.setTorchMode(camManager.cameraIdList[0], !isTorchEnabled)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_widgets -> {
                                widgetManager?.showAddDialog(viewPager)
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
                    toggleStats.removeAllViews()
                    configureMenuVisibility(toolbar)
                    info.visibility = View.VISIBLE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    if (!hasConfigured) {
                        hasConfigured = true
                        info.visibility = View.GONE
                        color = configureMenuIcons(toolbar)
                        batteryLevel.setTextColor(color)
                        clock.setTextColor(color)
                        buttonAuth.isVisible = keyguardManager.isDeviceLocked
                    }
                }
            }
        })

        configureMenuVisibility(toolbar)

        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            toolbar.findViewById<View>(R.id.toggle_widgets)
                .setOnLongClickListener(View.OnLongClickListener {
                if (viewPager.currentItem > 1) {
                    toolbar.menu.findItem(R.id.toggle_widgets)
                        .setIcon(R.drawable.ic_baseline_delete_forever_24dp)
                    tactileFeedback()
                    val index = viewPager.currentItem
                    val fragment = (pagerAdapter as CoverStateAdapter).getFragment(index)
                    val widget = fragment.getLayout()!!.getChildAt(0).tag
                    if (widget is PanelWidgetInfo) {
                        viewPager.setCurrentItem(index - 1, true)
                        model.removeDesktopAppWidget(widget)
                        appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                        WidgetModel.deleteItemFromDatabase(
                            applicationContext, widget
                        )
                        (pagerAdapter as CoverStateAdapter).removeFragment(index)
                    }
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    toolbar.menu.findItem(R.id.toggle_widgets)
                        .setIcon(R.drawable.ic_baseline_widgets_24dp)
                } else {
                    Toast.makeText(this,
                        R.string.incompatible_fragment,
                        Toast.LENGTH_LONG).show()
                }
                return@OnLongClickListener true
            })
        }
        coordinator.visibility = View.GONE
        info.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        launcherManager = LauncherManager(this)

        viewPager = findViewById(R.id.pager)
        pagerAdapter = CoverStateAdapter(this)
        viewPager.adapter = pagerAdapter

        searchView = findViewById(R.id.package_search)
        searchView.isSubmitButtonEnabled = false
        searchView.setIconifiedByDefault(true)
        searchView.findViewById<LinearLayout>(R.id.search_bar)?.run {
            this.layoutParams = this.layoutParams.apply {
                height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f, resources.displayMetrics).toInt()
            }
        }

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isNotDrawer = position != 1
                searchView.isGone = isNotDrawer
                if (bottomSheetBehaviorMain.state == BottomSheetBehavior.STATE_EXPANDED)
                    findViewById<View>(R.id.bottom_sheet_main).keepScreenOn = isNotDrawer
                with(prefs.edit()) {
                    putInt(SamSprung.prefViewer, position)
                    apply()
                }
            }
        })

        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            widgetManager = PanelWidgetManager(this, mAppWidgetManager,
                appWidgetHost, pagerAdapter as CoverStateAdapter
            )
            contentResolver.registerContentObserver(
                WidgetSettings.Favorites.CONTENT_URI, true, mObserver
            )
            model.loadUserItems(true, this)
        }

        val menuButton = findViewById<FloatingActionButton>(R.id.menu_fab)
        val fakeOverlay = findViewById<LinearLayout>(R.id.fake_overlay)
        val bottomHandle = findViewById<View>(R.id.bottom_handle)
        bottomSheetBehaviorMain = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_main))
        bottomSheetBehaviorMain.isFitToContents = false
        bottomSheetBehaviorMain.isDraggable = prefs.getBoolean(SamSprung.prefSlider, true)
        bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehaviorMain.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var hasConfigured = false
            val handler = Handler(Looper.getMainLooper())
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (viewPager.currentItem != 1) bottomSheet.keepScreenOn = true
                    bottomSheetBehaviorMain.isDraggable = false
                    bottomHandle.visibility = View.INVISIBLE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    hasConfigured = false
                    bottomSheet.keepScreenOn = false
                    coordinator.visibility = View.GONE
                    fakeOverlay.visibility = View.VISIBLE
                    bottomHandle.setBackgroundColor(color)
                    setMenuButtonGravity(menuButton)
                    menuButton.drawable.setTint(color)
                    bottomHandle.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
                    menuButton.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
                    handler.postDelayed({
                        bottomHandle.visibility = View.VISIBLE
                        menuButton.visibility = View.VISIBLE
                    }, 500)
                    bottomSheetBehaviorMain.isDraggable =
                        prefs.getBoolean(SamSprung.prefSlider, true)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                color = prefs.getInt(SamSprung.prefColors,
                    Color.rgb(255, 255, 255))
                if (slideOffset > 0) {
                    if (!hasConfigured) {
                        hasConfigured = true
                        handler.removeCallbacksAndMessages(null)
                        menuButton.visibility = View.GONE
                        fakeOverlay.visibility = View.GONE
                        coordinator.visibility = View.VISIBLE
                        val index = prefs.getInt(SamSprung.prefViewer, viewPager.currentItem)
                        viewPager.setCurrentItem(if (index < 2) 1 else index, false)
                    }
                }
            }
        })
        bottomHandle.visibility = View.VISIBLE
        bottomHandle.setBackgroundColor(prefs.getInt(
            SamSprung.prefColors,
            Color.rgb(255, 255, 255)))
        bottomHandle.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)

        setMenuButtonGravity(menuButton)
        menuButton.setOnClickListener {
            bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
        }
        menuButton.drawable.setTint(color)

        findViewById<AnimatedLinearLayout>(R.id.update_notice).visibility = View.GONE

        if (null != intent?.action && SamSprung.launcher == intent.action) {
            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
            }, 150)
        }

        val voice = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        val recognizer = VoiceRecognizer(object : VoiceRecognizer.SpeechResultsListener {
            override fun onSpeechResults(suggested: String) {
                menuButton.keepScreenOn = true
                launchApplication(suggested, menuButton)
            }
        })
        voice?.setRecognitionListener(recognizer)
        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            menuButton.setOnLongClickListener {
                tactileFeedback()
                voice?.startListening(recognizer.getSpeechIntent(false))
                return@setOnLongClickListener true
            }
        }
    }

    fun getBottomSheetMain() : BottomSheetBehavior<View> {
        return bottomSheetBehaviorMain
    }

    fun getSearch() : SearchView {
        return searchView
    }

    private var keyguardListener: KeyguardListener? = null

    interface KeyguardListener {
        fun onKeyguardCheck(unlocked: Boolean)
    }

    private fun dismissKeyguard(keyguardManager: KeyguardManager, authDialog: AlertDialog?) {
        keyguardManager.requestDismissKeyguard(this,
            object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissCancelled() {
                super.onDismissCancelled()
                authDialog?.dismiss()
                keyguardListener?.onKeyguardCheck(true)
            }

            override fun onDismissError() {
                super.onDismissError()
                if (null != authDialog) {
                    tactileFeedback()
                    authDialog.dismiss()
                }
                keyguardListener?.onKeyguardCheck(false)
            }

            override fun onDismissSucceeded() {
                super.onDismissSucceeded()
                if (null != authDialog) {
                    tactileFeedback()
                    authDialog.dismiss()
                }
                keyguardListener?.onKeyguardCheck(true)
            }
        })
        setTurnScreenOn(false)
    }

    @SuppressLint("InflateParams")
    fun setKeyguardListener(listener: KeyguardListener?) {
        this.keyguardListener = listener
        setTurnScreenOn(true)
        val keyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        if (keyguardManager.isDeviceLocked) {
            val authView = layoutInflater.inflate(R.layout.fingerprint_auth, null)
            val authDialog = AlertDialog.Builder(
                ContextThemeWrapper(this, R.style.DialogTheme_NoActionBar)
            ).setView(authView).create()
            authDialog.setCancelable(false)
            authDialog.window?.attributes?.windowAnimations = R.style.SlidingDialogAnimation
            authDialog.show()
            authDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            authDialog.window?.setLayout(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                authView.findViewById<RelativeLayout>(R.id.auth_view).background =
                    WallpaperManager.getInstance(ScaledContext.cover(this)).drawable
            }
            try {
                HiddenApiBypass.invoke(Class.forName("android.app.KeyguardManager"),
                    keyguardManager, "semStartLockscreenFingerprintAuth")
                dismissKeyguard(keyguardManager, authDialog)
            } catch (ite: Exception) {
                ite.printStackTrace()
                authDialog.dismiss()
                IconifiedSnackbar(this@SamSprungOverlay, viewPager).buildTickerBar(
                    getString(R.string.auth_unavailable),
                    R.drawable.ic_baseline_fingerprint_24dp,
                    Snackbar.LENGTH_LONG
                ).show()
                keyguardListener?.onKeyguardCheck(false)
            }
        } else {
            dismissKeyguard(keyguardManager, null)
            keyguardListener?.onKeyguardCheck(true)
        }
    }

    companion object {
        const val APPWIDGET_HOST_ID = SamSprung.request_code
    }

    private fun launchApplication(launchCommand: String, menuButton: FloatingActionButton) {
        val matchedApps: ArrayList<ApplicationInfo> = ArrayList()
        val packages: List<ApplicationInfo> = packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            var ai: ApplicationInfo
            try {
                ai = packageManager.getApplicationInfo(
                    packageInfo.packageName, 0
                )
                if (packageManager.getApplicationLabel(ai).contains(launchCommand, true)) {
                    matchedApps.add(packageInfo)
                }
            } catch (ignored: PackageManager.NameNotFoundException) { }
        }
        if (matchedApps.isNotEmpty()) {
            if (matchedApps.size == 1) {
                launcherManager?.launchDefaultActivity(matchedApps[0])
            } else {
                bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
                getSearch().setQuery(launchCommand, true)
            }
        }
        menuButton.keepScreenOn = false
    }

    private fun configureMenuVisibility(toolbar: Toolbar) {
        val toggleStats = findViewById<LinearLayout>(R.id.toggle_status)
        for (i in 0 until toolbar.menu.size()) {
            val enabled = prefs.getBoolean(toolbar.menu.getItem(i).title.toPref, true)
            if (enabled) {
                toolbar.menu.getItem(i).isVisible = true
                val icon = layoutInflater.inflate(R.layout.toggle_state_icon, toggleStats,
                    false).findViewById<AppCompatImageView>(R.id.toggle_icon)
                icon.setImageDrawable(toolbar.menu.getItem(i).icon)
                toggleStats.addView(icon)
            } else {
                toolbar.menu.getItem(i).isVisible = false
            }
        }
        toggleStats.requestLayout()
    }

    private fun configureMenuIcons(toolbar: Toolbar) : Int {
        val color = prefs.getInt(SamSprung.prefColors,
            Color.rgb(255, 255, 255))

        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager

        if (wifiManager.isWifiEnabled)
            toolbar.menu.findItem(R.id.toggle_wifi)
                .setIcon(R.drawable.ic_baseline_wifi_on_24dp)
        else
            toolbar.menu.findItem(R.id.toggle_wifi)
                .setIcon(R.drawable.ic_baseline_wifi_off_24dp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                this@SamSprungOverlay,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (bluetoothAdapter.isEnabled)
                toolbar.menu.findItem(R.id.toggle_bluetooth)
                    .setIcon(R.drawable.ic_baseline_bluetooth_on_24dp)
            else
                toolbar.menu.findItem(R.id.toggle_bluetooth)
                    .setIcon(R.drawable.ic_baseline_bluetooth_off_24dp)
        } else {
            toolbar.menu.findItem(R.id.toggle_bluetooth).isVisible = false
        }

        if (nfcAdapter.isEnabled)
            toolbar.menu.findItem(R.id.toggle_nfc)
                .setIcon(R.drawable.ic_baseline_nfc_on_24dp)
        else
            toolbar.menu.findItem(R.id.toggle_nfc)
                .setIcon(R.drawable.ic_baseline_nfc_off_24dp)

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            toolbar.menu.findItem(R.id.toggle_sound)
                .setIcon(R.drawable.ic_baseline_sound_on_24dp)
        else
            toolbar.menu.findItem(R.id.toggle_sound)
                .setIcon(R.drawable.ic_baseline_sound_off_24dp)

        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (notificationManager.currentInterruptionFilter ==
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
                toolbar.menu.findItem(R.id.toggle_dnd)
                    .setIcon(R.drawable.ic_baseline_do_not_disturb_off_24dp)
            else
                toolbar.menu.findItem(R.id.toggle_dnd)
                    .setIcon(R.drawable.ic_baseline_do_not_disturb_on_24dp)
        } else {
            toolbar.menu.findItem(R.id.toggle_dnd).isVisible = false
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            if (isTorchEnabled) {
                toolbar.menu.findItem(R.id.toggle_torch)
                    .setIcon(R.drawable.ic_baseline_flashlight_on_24dp)
            } else {
                toolbar.menu.findItem(R.id.toggle_torch)
                    .setIcon(R.drawable.ic_baseline_flashlight_off_24dp)
            }
        } else {
            toolbar.menu.findItem(R.id.toggle_torch).isVisible = false
        }
        for (i in 0 until toolbar.menu.size()) {
            toolbar.menu.getItem(i).icon.setTint(color)
        }
        return color
    }

    private fun setMenuButtonGravity(menuButton: FloatingActionButton) {
        var gravity = GravityCompat.END
        when (prefs.getInt(SamSprung.prefShifts, 2)) {
            0 -> gravity = GravityCompat.START
            1 -> gravity = Gravity.CENTER_HORIZONTAL
            2 -> gravity = GravityCompat.END
        }
        val menuParams = menuButton.layoutParams as CoordinatorLayout.LayoutParams
        menuParams.anchorGravity = Gravity.TOP or gravity
        menuButton.layoutParams = menuParams
    }

    private fun tactileFeedback() {
        if (!prefs.getBoolean(SamSprung.prefReacts, true)) return
        val vibe = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(vibe)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(vibe)
        }
    }

    fun hasNotificationListener(): Boolean {
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
        appWidgets: LinkedList<PanelWidgetInfo>
    ) {
        widgetManager?.bindAppWidgets(binder, appWidgets)
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
    appWidgets: ArrayList<PanelWidgetInfo>?
    ) {
        // Flag any old binder to terminate early
        if (null != mBinder) mBinder!!.mTerminate = true
        mBinder = DesktopBinder(this, shortcuts, appWidgets)
        mBinder!!.startBindingItems()
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        if (null != mBinder) mBinder!!.mTerminate = true
        // return lastNonConfigurationInstance
        return null
    }

    fun showUpdateNotice() {
        runOnUiThread {
            val fakeSnackbar = findViewById<AnimatedLinearLayout>(R.id.update_notice)
            findViewById<TextView>(R.id.update_text).text =
                getString(R.string.update_service, getString(R.string.samsprung))
            val animate = TranslateAnimation(
                0f, 0f, 0f, -fakeSnackbar.height.toFloat()
            )
            animate.duration = 500
            animate.fillAfter = true
            fakeSnackbar.setAnimationListener(object : AnimatedLinearLayout.AnimationListener {
                override fun onAnimationStart(layout: AnimatedLinearLayout) {}
                override fun onAnimationEnd(layout: AnimatedLinearLayout) {
                    layout.setAnimationListener(null)
                }
            })
            fakeSnackbar.visibility = View.VISIBLE
            fakeSnackbar.startAnimation(animate)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Handler(Looper.getMainLooper()).postDelayed({
            val bottomHandle = findViewById<View>(R.id.bottom_handle)
            bottomHandle.visibility = View.VISIBLE
            bottomHandle.setBackgroundColor(prefs.getInt(
                SamSprung.prefColors,
                Color.rgb(255, 255, 255)))
            bottomHandle.alpha = prefs.getFloat(SamSprung.prefAlphas, 1f)
            if (null != intent?.action && SamSprung.launcher == intent.action) {
                bottomSheetBehaviorMain.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }, 150)
        val updateCheck = CheckUpdatesTask(this)
        if (BuildConfig.FLAVOR == "google") {
            updateCheck.setPlayUpdateListener(object: CheckUpdatesTask.CheckPlayUpdateListener {
                override fun onPlayUpdateFound(appUpdateInfo: AppUpdateInfo) {
                    showUpdateNotice()
                }
            })
        } else {
            updateCheck.setUpdateListener(object: CheckUpdatesTask.CheckUpdateListener {
                override fun onUpdateFound(downloadUrl: String) {
                    showUpdateNotice()
                }
            })
        }
    }

    fun onStopOverlay() {
        findViewById<View>(R.id.bottom_sheet_main).keepScreenOn = false
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
        super.onDestroy()
        onStopOverlay()
        if (prefs.getBoolean(getString(R.string.toggle_widgets).toPref, true)) {
            try {
                appWidgetHost?.stopListening()
            } catch (ignored: NullPointerException) { }
            model.unbind()
            model.abortLoaders()
            contentResolver.unregisterContentObserver(mObserver)
        }
    }

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    private val CharSequence.toPref get() = this.toString()
        .lowercase().replace(" ", "_")

    val requestCreateAppWidget = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val appWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == FragmentActivity.RESULT_CANCELED) {
            if (appWidgetId != -1) {
                appWidgetHost?.deleteAppWidgetId(appWidgetId)
            }
        } else {
            widgetManager?.completeAddAppWidget(appWidgetId, viewPager)
        }
    }

    private var mWidgetPreviewCacheDb: WidgetPreviewLoader.CacheDb? = null
    fun recreateWidgetPreviewDb() {
        mWidgetPreviewCacheDb = WidgetPreviewLoader.CacheDb(this)
    }
    fun getWidgetPreviewCacheDb(): WidgetPreviewLoader.CacheDb? {
        return mWidgetPreviewCacheDb
    }
}