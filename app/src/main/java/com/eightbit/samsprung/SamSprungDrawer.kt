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
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.os.*
import android.provider.Settings
import android.service.notification.NotificationListenerService.requestRebind
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import java.io.File
import java.util.*


class SamSprungDrawer : AppCompatActivity(),
    AppDrawerAdapater.OnAppClickListener,
    NotificationAdapter.OnNoticeClickListener {

    private lateinit var bReceiver: BroadcastReceiver
    private lateinit var pReceiver: BroadcastReceiver
    private var mReceiver: BroadcastReceiver? = null

    @SuppressLint("InflateParams", "CutPasteId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        // setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        // ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen)
        supportActionBar?.hide()
        setContentView(R.layout.apps_view_layout)

        requestRebind(ComponentName(
            applicationContext,
            NotificationListener::class.java
        ))

        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(this)
            findViewById<CoordinatorLayout>(R.id.rootLayout).background = wallpaperManager.drawable
            findViewById<BlurView>(R.id.blurContainer).setupWith(
                window.decorView.findViewById(R.id.rootLayout)
            )
                .setFrameClearDrawable(window.decorView.background)
                .setBlurRadius(1f)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(true)
                .setBlurAlgorithm(RenderScriptBlur(this))
        }

        val batteryLevel = findViewById<TextView>(R.id.battery_status)
        val bReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.quick_toggles)
        val noticesView = findViewById<RecyclerView>(R.id.notificationList)
        noticesView.layoutManager = LinearLayoutManager(this)
        noticesView.adapter = NotificationAdapter(arrayListOf(), this@SamSprungDrawer)

        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        val wifiEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (wifiManager.isWifiEnabled)
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_24)
            else
                toolbar.menu.findItem(R.id.toggle_wifi).setIcon(R.drawable.ic_baseline_wifi_off_24)
        }

        val nfcAdapter = (getSystemService(NFC_SERVICE) as NfcManager).defaultAdapter
        val nfcEnabler = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter.isEnabled)
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_24)
            else
                toolbar.menu.findItem(R.id.toggle_nfc).setIcon(R.drawable.ic_baseline_nfc_disabled_24)
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }.also {
            registerReceiver(bReceiver, it)
        }

        val bottomSheetBehavior: BottomSheetBehavior<View> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (hasAccessibility() || hasNotificationListener())
                        refreshNotifications(noticesView)

                    val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE)
                            as BluetoothManager).adapter
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE)
                            as NotificationManager
                    val camManager = getSystemService(CAMERA_SERVICE) as CameraManager
                    var isTorchEnabled = false

                    if (wifiManager.isWifiEnabled)
                        toolbar.menu.findItem(R.id.toggle_wifi)
                            .setIcon(R.drawable.ic_baseline_wifi_24)
                    else
                        toolbar.menu.findItem(R.id.toggle_wifi)
                            .setIcon(R.drawable.ic_baseline_wifi_off_24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(
                            this@SamSprungDrawer,
                            Manifest.permission.BLUETOOTH_CONNECT,
                        ) == PackageManager.PERMISSION_GRANTED) {
                        if (bluetoothAdapter.isEnabled)
                            toolbar.menu.findItem(R.id.toggle_bluetooth)
                                .setIcon(R.drawable.ic_baseline_bluetooth_24)
                        else
                            toolbar.menu.findItem(R.id.toggle_bluetooth)
                                .setIcon(R.drawable.ic_baseline_bluetooth_disabled_24)
                    } else {
                        toolbar.menu.findItem(R.id.toggle_bluetooth).isVisible = false
                    }

                    if (nfcAdapter.isEnabled)
                        toolbar.menu.findItem(R.id.toggle_nfc)
                            .setIcon(R.drawable.ic_baseline_nfc_24)
                    else
                        toolbar.menu.findItem(R.id.toggle_nfc)
                            .setIcon(R.drawable.ic_baseline_nfc_disabled_24)

                    toolbar.menu.findItem(R.id.toggle_gps).isVisible = false

                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL)
                        toolbar.menu.findItem(R.id.toggle_sound)
                            .setIcon(R.drawable.ic_baseline_hearing_24)
                    else
                        toolbar.menu.findItem(R.id.toggle_sound)
                            .setIcon(R.drawable.ic_baseline_hearing_disabled_24)

                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        if (notificationManager.currentInterruptionFilter ==
                            NotificationManager.INTERRUPTION_FILTER_ALL)
                            toolbar.menu.findItem(R.id.toggle_dnd)
                                .setIcon(R.drawable.ic_baseline_do_not_disturb_off_24)
                        else
                            toolbar.menu.findItem(R.id.toggle_dnd)
                                .setIcon(R.drawable.ic_baseline_do_not_disturb_on_24)
                    } else {
                        toolbar.menu.findItem(R.id.toggle_dnd).isVisible = false
                    }

                    if (Settings.System.canWrite(applicationContext)) {
                        try {
                            if (Settings.System.getInt(applicationContext.contentResolver,
                                    Settings.System.ACCELEROMETER_ROTATION).bool)
                                toolbar.menu.findItem(R.id.toggle_rotation)
                                    .setIcon(R.drawable.ic_baseline_screen_rotation_24)
                            else
                                toolbar.menu.findItem(R.id.toggle_rotation)
                                    .setIcon(R.drawable.ic_baseline_screen_lock_rotation_24)
                        } catch (e: Settings.SettingNotFoundException) {
                            toolbar.menu.findItem(R.id.toggle_rotation).isVisible = false
                        }
                    } else {
                        toolbar.menu.findItem(R.id.toggle_rotation).isVisible = false
                    }

                    if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                        camManager.setTorchMode(camManager.cameraIdList[0], isTorchEnabled)
                    } else {
                        toolbar.menu.findItem(R.id.toggle_torch).isVisible = isTorchEnabled
                    }

                    toolbar.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.toggle_wifi -> {
                                wifiEnabler.launch(Intent(Settings.Panel.ACTION_WIFI))
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_bluetooth -> {
                                if (bluetoothAdapter.isEnabled) {
                                    bluetoothAdapter.disable()
                                    toolbar.menu.findItem(R.id.toggle_bluetooth)
                                        .setIcon(R.drawable.ic_baseline_bluetooth_disabled_24)
                                } else {
                                    bluetoothAdapter.enable()
                                    toolbar.menu.findItem(R.id.toggle_bluetooth)
                                        .setIcon(R.drawable.ic_baseline_bluetooth_24)
                                }
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_nfc -> {
                                nfcEnabler.launch(Intent(Settings.Panel.ACTION_NFC))
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_gps -> {

                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_sound -> {
                                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                    toolbar.menu.findItem(R.id.toggle_sound)
                                        .setIcon(R.drawable.ic_baseline_hearing_disabled_24)
                                } else {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                    toolbar.menu.findItem(R.id.toggle_sound)
                                        .setIcon(R.drawable.ic_baseline_hearing_24)
                                }
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
                                return@setOnMenuItemClickListener true
                            }
                            R.id.toggle_rotation -> {
                                Settings.System.putInt(applicationContext.contentResolver,
                                    Settings.System.ACCELEROMETER_ROTATION,
                                    (!Settings.System.getInt(applicationContext.contentResolver,
                                        Settings.System.ACCELEROMETER_ROTATION).bool).int)
                            }
                            R.id.toggle_torch -> {
                                if (isTorchEnabled) {
                                    isTorchEnabled = false
                                    toolbar.menu.findItem(R.id.toggle_torch)
                                        .setIcon(R.drawable.ic_baseline_flashlight_off_24)
                                } else {
                                    isTorchEnabled = true
                                    toolbar.menu.findItem(R.id.toggle_torch)
                                        .setIcon(R.drawable.ic_baseline_flashlight_on_24)
                                }
                                camManager.setTorchMode(camManager.cameraIdList[0], isTorchEnabled)
                                return@setOnMenuItemClickListener true
                            }
                            else -> {
                                return@setOnMenuItemClickListener false
                            }
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        val launcherView = findViewById<RecyclerView>(R.id.appsList)

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        var packages: MutableList<ResolveInfo> = packageManager.queryIntentActivities(
            mainIntent, PackageManager.GET_RESOLVED_FILTER)
        packages.removeIf { item ->
            (null != item.filter && item.filter.hasCategory(Intent.CATEGORY_HOME))
                || SamSprung.prefs.getStringSet(SamSprung.prefHidden,
                HashSet())!!.contains(item.activityInfo.packageName)
        }
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))

        if (SamSprung.prefs.getBoolean(SamSprung.prefLayout, true))
            launcherView.layoutManager = GridLayoutManager(this, getColumnCount())
        else
            launcherView.layoutManager = LinearLayoutManager(this)
        launcherView.adapter = AppDrawerAdapater(packages, this, packageManager)

        val pReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                    packages = packageManager.queryIntentActivities(
                        mainIntent, PackageManager.GET_RESOLVED_FILTER)
                    packages.removeIf { item ->
                        (null != item.filter && item.filter.hasCategory(Intent.CATEGORY_HOME))
                            || SamSprung.prefs.getStringSet(SamSprung.prefHidden,
                        HashSet())!!.contains(item.activityInfo.packageName) }
                    Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))
                    (launcherView.adapter as AppDrawerAdapater).setPackages(packages)
                    (launcherView.adapter as AppDrawerAdapater).notifyDataSetChanged()
                }
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        packages = packageManager.queryIntentActivities(
                            mainIntent, PackageManager.GET_RESOLVED_FILTER)
                        packages.removeIf { item ->
                            (null != item.filter && item.filter.hasCategory(Intent.CATEGORY_HOME))
                                    || SamSprung.prefs.getStringSet(SamSprung.prefHidden,
                                HashSet())!!.contains(item.activityInfo.packageName)
                        }
                        (launcherView.adapter as AppDrawerAdapater).setPackages(packages)
                        (launcherView.adapter as AppDrawerAdapater).notifyDataSetChanged()
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

        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
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
                    finish()
                }
                if (direction == ItemTouchHelper.LEFT) {
                    finish()
                }
            }
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(launcherView)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshNotifications(noticesView: RecyclerView) {
        val groups: HashMap<String, SamSprungNotice> = hashMapOf()
        for (notification: Notification in SamSprung.notifications) {
            if (groups.containsKey(notification.group)
                && null != groups[notification.group]) {
                val notice : SamSprungNotice = groups[notification.group]!!
                if (null != notification.extras
                    && null != notification.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES)) {
                    notice.setString(Arrays.toString(
                        notification.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES)
                    ))
                } else if (null != notification.tickerText) {
                    notice.setString(notification.tickerText.toString())
                }
                groups.replace(notification.group, notice)
            } else {
                val notice = SamSprungNotice()
                when {
                    null != notification.getLargeIcon() -> notice.setDrawable(
                        notification.getLargeIcon().loadDrawable(this@SamSprungDrawer)
                    )
                    null != notification.smallIcon -> notice.setDrawable(
                        notification.smallIcon.loadDrawable(this@SamSprungDrawer)
                    )
                }
                if (null != notification.extras
                    && null != notification.extras.getCharSequenceArray(
                        NotificationCompat.EXTRA_TEXT_LINES)) {
                    notice.setString(Arrays.toString(
                        notification.extras.getCharSequenceArray(
                            NotificationCompat.EXTRA_TEXT_LINES)
                    ))
                } else if (null != notification.tickerText) {
                    notice.setString(notification.tickerText.toString())
                }
                if (null != notification.contentIntent)
                    notice.setIntentSender(notification.contentIntent.intentSender)
                if (null != notification.group) {
                    groups[notification.group] = notice
                } else {
                    groups[packageName] = notice
                }

            }
        }
        val notices: ArrayList<SamSprungNotice> = ArrayList<SamSprungNotice>(groups.values)
        (noticesView.adapter as NotificationAdapter).setNotices(notices)
        (noticesView.adapter as NotificationAdapter).notifyDataSetChanged()
    }

    private fun prepareLockOrientation() {
        with (SamSprung.prefs.edit()) {
            try {
                putBoolean(SamSprung.autoRotate,  Settings.System.getInt(
                    applicationContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION) == 1
                )
            } catch (e: Settings.SettingNotFoundException) {
                putBoolean(SamSprung.autoRotate, true)
            }
            apply()
        }

        if (Settings.System.canWrite(applicationContext)) {
            try {
                Settings.System.putInt(
                    applicationContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 1
                )
            } catch (ignored: Settings.SettingNotFoundException) { }
        }

        val mKeyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        SamSprung.isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if (SamSprung.isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }
    }

    override fun onAppClicked(appInfo: ResolveInfo, position: Int) {
        prepareLockOrientation()

        val serviceIntent = Intent(this, DisplayListenerService::class.java)
        val extras = Bundle()
        extras.putString("launchPackage", appInfo.activityInfo.packageName)
        extras.putString("launchActivity", appInfo.activityInfo.name)
        startForegroundService(serviceIntent.putExtras(extras))

        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            mReceiver = OffBroadcastReceiver(
                ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name)
            )
            applicationContext.registerReceiver(mReceiver, it)
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
        coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        coverIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        coverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(coverIntent.putExtras(extras), options.toBundle())
    }

    override fun onNoticeClicked(notice: SamSprungNotice, position: Int) {
        if (null != notice.getIntentSender()) {
            prepareLockOrientation()

            startForegroundService(Intent(this, DisplayListenerService::class.java)
                .putExtra("launchPackage", notice.getIntentSender()!!.creatorPackage))
            startIntentSender(notice.getIntentSender(),
                null, 0, 0, 0)
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
                + File.separator + AccessibilityHandler::class.java.name)
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

    private val Boolean.int get() = if (this) 1 else 0
    private val Int.bool:Boolean get() = this != 0


    override fun onDestroy() {
        super.onDestroy()
        if (this::bReceiver.isInitialized)
            unregisterReceiver(bReceiver)
        if (this::pReceiver.isInitialized)
            unregisterReceiver(pReceiver)
        try {
            if (null != mReceiver)
                unregisterReceiver(mReceiver)
        } catch (ignored: Exception) { }
    }
}