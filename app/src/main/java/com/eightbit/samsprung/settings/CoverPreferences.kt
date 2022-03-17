package com.eightbit.samsprung.settings

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
import android.app.Dialog
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.eightbit.content.ScaledContext
import com.eightbit.io.Debug
import com.eightbit.material.IconifiedSnackbar
import com.eightbit.pm.PackageRetriever
import com.eightbit.samsprung.*
import com.eightbit.view.AnimatedLinearLayout
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CoverPreferences : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var coordinator: CoordinatorLayout
    private var updateCheck : CheckUpdatesTask? = null

    private var hasPremiumSupport = false
    private lateinit var mainSwitch: SwitchCompat
    private lateinit var permissionList: LinearLayout
    private lateinit var accessibility: SwitchCompat
    private lateinit var notifications: SwitchCompat
    private lateinit var keyboard: SwitchCompat

    private lateinit var hiddenList: RecyclerView

    private lateinit var billingClient: BillingClient
    private val iapList = ArrayList<String>()
    private val subList = ArrayList<String>()
    private val iapSkuDetails = ArrayList<SkuDetails>()
    private val subSkuDetails = ArrayList<SkuDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        ScaledContext.screen(this).setTheme(R.style.Theme_SecondScreen)
        setContentView(R.layout.cover_settings_layout)
        permissionList = findViewById(R.id.permissions)

        val componentName = ComponentName(applicationContext, NotificationReceiver::class.java)
        packageManager.setComponentEnabledSetting(componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )

        retrieveDonationMenu()

        coordinator = findViewById(R.id.coordinator)
        findViewById<BlurView>(R.id.blurContainer).setupWith(coordinator)
            .setFrameClearDrawable(coordinator.background)
            .setBlurRadius(10f).setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(false)
            .setBlurAlgorithm(RenderScriptBlur(this))

        findViewById<TextView>(R.id.build_info).text = (getString(R.string.build_hash, BuildConfig.COMMIT))
        findViewById<LinearLayout>(R.id.build_layout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/SamSprung/SamSprung-TooUI/wiki")))
        }

        initializeLayout()

        val paypal = findViewById<LinearLayout>(R.id.paypal)
        paypal.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/donate/?hosted_button_id=Q2LFH2SC8RHRN")))
        }
        paypal.isVisible = BuildConfig.FLAVOR != "google"

        val googlePlay = findViewById<LinearLayout>(R.id.google_play)
        googlePlay.setOnClickListener {
            val view: View = layoutInflater.inflate(R.layout.donation_layout, null)
            val dialog = AlertDialog.Builder(
                ContextThemeWrapper(this, R.style.DialogTheme_NoActionBar)
            )
            val donations = view.findViewById<LinearLayout>(R.id.donation_layout)
            for (skuDetail: SkuDetails in iapSkuDetails) {
                val button = Button(applicationContext)
                button.setBackgroundResource(R.drawable.button_rippled)
                button.elevation = 10f.toPx
                button.text = getString(R.string.iap_button, skuDetail.price)
                button.setOnClickListener {
                    billingClient.launchBillingFlow(
                        this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build()
                    )
                }
                donations.addView(button)
            }
            val subscriptions = view.findViewById<LinearLayout>(R.id.subscription_layout)
            for (skuDetail: SkuDetails in subSkuDetails) {
                val button = Button(applicationContext)
                button.setBackgroundResource(R.drawable.button_rippled)
                button.elevation = 10f.toPx
                button.text = getString(R.string.sub_button, skuDetail.price)
                button.setOnClickListener {
                    billingClient.launchBillingFlow(
                        this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build()
                    )
                }
                subscriptions.addView(button)
            }
            dialog.setOnCancelListener {
                donations.removeAllViewsInLayout()
                subscriptions.removeAllViewsInLayout()
            }
            val donateDialog: Dialog = dialog.setView(view).show()
            donateDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        findViewById<LinearLayout>(R.id.logcat).setOnClickListener {
            captureLogcat(findViewById(R.id.coordinator))
        }

        notifications = findViewById(R.id.notifications_switch)
        notifications.isChecked = hasNotificationListener()
        findViewById<LinearLayout>(R.id.notifications).setOnClickListener {
            notificationLauncher.launch(Intent(
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ))
        }

        findViewById<LinearLayout>(R.id.usage_layout).setOnClickListener {
            usageLauncher.launch(Intent(
                Settings.ACTION_USAGE_ACCESS_SETTINGS
            ))
        }

        accessibility = findViewById(R.id.accessibility_switch)
        accessibility.isChecked = hasAccessibility()
        findViewById<LinearLayout>(R.id.accessibility).setOnClickListener {
            if (BuildConfig.FLAVOR == "google" && !accessibility.isChecked) {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.accessibility_disclaimer))
                    .setPositiveButton(R.string.button_confirm) { dialog, _ ->
                        accessibilityLauncher.launch(Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS,
                        ))
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                        accessibility.isChecked = false
                        dialog.dismiss()
                    }.show()
            } else {
                accessibilityLauncher.launch(Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS,
                ))
            }
        }

        keyboard = findViewById(R.id.keyboard_switch)
        keyboard.isClickable = false
        keyboard.isChecked = hasKeyboardInstalled()
        findViewById<LinearLayout>(R.id.keyboard_layout).setOnClickListener {
            try {
                keyboardLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=" + BuildConfig.APPLICATION_ID + ".ime"
                )))
            } catch (exception: ActivityNotFoundException) {
                keyboardLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://play.google.com/store/apps/details?id="
                                + BuildConfig.APPLICATION_ID + ".ime"
                )))
            }
        }

        val updates = findViewById<SwitchCompat>(R.id.updates_switch)
        updates.isChecked = prefs.getBoolean(SamSprung.prefTester, false)
        updates.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                putBoolean(SamSprung.prefTester, isChecked)
                apply()
            }
        }
        val updatesPanel = findViewById<LinearLayout>(R.id.updates)
        updatesPanel.setOnClickListener {
            updates.isChecked = !updates.isChecked
        }
        updatesPanel.isVisible = BuildConfig.FLAVOR != "google"

        val draggable = findViewById<SwitchCompat>(R.id.draggable_switch)
        draggable.isChecked = prefs.getBoolean(SamSprung.prefSlider, true)
        draggable.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                putBoolean(SamSprung.prefSlider, isChecked)
                apply()
            }
        }
        findViewById<LinearLayout>(R.id.draggable).setOnClickListener {
            draggable.isChecked = !draggable.isChecked
        }

        val vibration = findViewById<SwitchCompat>(R.id.vibration_switch)
        vibration.isChecked = prefs.getBoolean(SamSprung.prefReacts, true)
        vibration.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                putBoolean(SamSprung.prefReacts, isChecked)
                apply()
            }
        }
        findViewById<LinearLayout>(R.id.vibration).setOnClickListener {
            vibration.isChecked = !vibration.isChecked
        }

        val placementBar = findViewById<SeekBar>(R.id.placement_bar)
        placementBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                with(prefs.edit()) {
                    putInt(SamSprung.prefShifts, progress)
                    apply()
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) { }

            override fun onStopTrackingTouch(seek: SeekBar) { }
        })
        placementBar.progress = prefs.getInt(SamSprung.prefShifts, 2)

        val themeSpinner = findViewById<Spinner>(R.id.theme_spinner)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.theme_options))
        spinnerAdapter.setDropDownViewResource(R.layout.dropdown_item_1)
        themeSpinner.adapter = spinnerAdapter
        themeSpinner.setSelection(prefs.getInt(SamSprung.prefThemes, 0))
        themeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                with (prefs.edit()) {
                    putInt(SamSprung.prefThemes, position)
                    apply()
                }
                (application as SamSprung).setThemePreference()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        val isGridView = prefs.getBoolean(SamSprung.prefLayout, true)
        findViewById<ToggleButton>(R.id.swapViewType).isChecked = isGridView
        findViewById<ToggleButton>(R.id.swapViewType).setOnCheckedChangeListener { _, isChecked ->
            with (prefs.edit()) {
                putBoolean(SamSprung.prefLayout, isChecked)
                apply()
            }
        }

        val packageRetriever = PackageRetriever(this)
        val packages = packageRetriever.getPackageList()
        for (installed in packages) {
            if (installed.resolvePackageName == "apps.ijp.coveros") {
                val compatDialog = AlertDialog.Builder(this)
                    .setMessage(getString(R.string.incompatibility_warning))
                    .setPositiveButton(R.string.button_uninstall) { dialog, _ ->
                        try {
                            startActivity(Intent(Intent.ACTION_DELETE)
                                .setData(Uri.parse("package:apps.ijp.coveros")))
                            dialog.dismiss()
                        } catch (ignored: Exception) { }
                    }
                    .setNegativeButton(R.string.button_disable) { dialog, _ ->
                        startActivity(Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:apps.ijp.coveros")
                        ))
                        dialog.dismiss()
                    }.create()
                compatDialog.setCancelable(false)
                compatDialog.show()
            }
        }
        val unlisted = packageRetriever.getHiddenPackages()

        hiddenList = findViewById(R.id.app_toggle_list)
        hiddenList.layoutManager = LinearLayoutManager(this)
        hiddenList.addItemDecoration(
            DividerItemDecoration(this,
            DividerItemDecoration.VERTICAL)
        )
        hiddenList.adapter = FilteredAppsAdapter(packageManager, packages, unlisted, prefs)

        val bottomSheetBehavior: BottomSheetBehavior<View> =
            BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    findViewById<LinearLayout>(R.id.bottom_sheet)
                        .setBackgroundColor(Color.TRANSPARENT)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0.1) {
                    findViewById<LinearLayout>(R.id.bottom_sheet)
                        .setBackgroundColor(getColor(R.color.backgroundFlat))
                }
            }
        })

        findViewById<LinearLayout>(R.id.visibility_handle).setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val color = prefs.getInt(SamSprung.prefColors, Color.rgb(255, 255, 255))

        val textRed = findViewById<TextView>(R.id.color_red_text)
        val colorRedBar = findViewById<SeekBar>(R.id.color_red_bar)
        colorRedBar.progress = color.red

        colorRedBar.progressTintList = ColorStateList
            .valueOf(Color.rgb(colorRedBar.progress, 0,0))

        val textGreen = findViewById<TextView>(R.id.color_green_text)
        val colorGreenBar = findViewById<SeekBar>(R.id.color_green_bar)
        colorGreenBar.progress = color.green

        colorGreenBar.progressTintList = ColorStateList
            .valueOf(Color.rgb(0, colorGreenBar.progress, 0))

        val textBlue = findViewById<TextView>(R.id.color_blue_text)
        val colorBlueBar = findViewById<SeekBar>(R.id.color_blue_bar)
        colorBlueBar.progress = color.blue

        colorBlueBar.progressTintList = ColorStateList
            .valueOf(Color.rgb(0, 0, colorBlueBar.progress))

        val alphaFloat = prefs.getFloat(SamSprung.prefAlphas, 1f)
        val alphaPreview = findViewById<View>(R.id.alpha_preview)
        val alphaView = findViewById<LinearLayout>(R.id.color_alpha_view)
        val colorAlphaBar = findViewById<SeekBar>(R.id.color_alpha_bar)
        alphaPreview.setBackgroundColor(color)
        alphaPreview.alpha = alphaFloat
        colorAlphaBar.progress = (alphaFloat * 100).toInt()

        val colorPanel = findViewById<AnimatedLinearLayout>(R.id.color_panel)
        val colorComposite = findViewById<View>(R.id.color_composite)
        colorComposite.setBackgroundColor(color)

        findViewById<LinearLayout>(R.id.innerLayout).viewTreeObserver.addOnGlobalLayoutListener {
            bottomSheetBehavior.peekHeight = window.decorView.height -
                     (findViewById<LinearLayout>(R.id.innerLayout).height + 141.toScalePx.toInt())
        }

        val colorHandler = Handler(Looper.getMainLooper())
        colorComposite.setOnClickListener {
            if (colorPanel.isVisible) {
                val animate = TranslateAnimation(
                    0f, 0f, 0f, -colorPanel.height.toFloat()
                )
                animate.duration = 750
                animate.fillAfter = false
                colorPanel.setAnimationListener(object : AnimatedLinearLayout.AnimationListener {
                    override fun onAnimationStart(layout: AnimatedLinearLayout) {
                        colorHandler.postDelayed({
                            textRed.visibility = View.INVISIBLE
                            colorRedBar.visibility = View.INVISIBLE
                        }, 125)
                        colorHandler.postDelayed({
                            textGreen.visibility = View.INVISIBLE
                            colorGreenBar.visibility = View.INVISIBLE
                        }, 250)
                        colorHandler.postDelayed({
                            textBlue.visibility = View.INVISIBLE
                            colorBlueBar.visibility = View.INVISIBLE
                        }, 375)
                        colorHandler.postDelayed({
                            alphaView.visibility = View.INVISIBLE
                            colorAlphaBar.visibility = View.INVISIBLE
                        }, 500)
                    }
                    override fun onAnimationEnd(layout: AnimatedLinearLayout) {
                        colorPanel.clearAnimation()
                        layout.setAnimationListener(null)
                        textRed.visibility = View.GONE
                        colorRedBar.visibility = View.GONE
                        textGreen.visibility = View.GONE
                        colorGreenBar.visibility = View.GONE
                        textBlue.visibility = View.GONE
                        colorBlueBar.visibility = View.GONE
                        alphaView.visibility = View.GONE
                        colorAlphaBar.visibility = View.GONE
                        colorPanel.visibility = View.GONE
                    }
                })
                colorPanel.startAnimation(animate)
                val divider = findViewById<View>(R.id.color_divider)
                val follow = TranslateAnimation(
                    0f, 0f, 0f, -colorPanel.height.toFloat()
                )
                follow.duration = 750
                follow.fillAfter = false
                follow.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation?) {
                        divider.clearAnimation()
                        animation?.setAnimationListener(null)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}

                    override fun onAnimationStart(animation: Animation?) {}
                })
                divider.startAnimation(follow)
            } else {
                colorPanel.visibility = View.VISIBLE
                colorHandler.postDelayed({
                    colorAlphaBar.visibility = View.VISIBLE
                    alphaView.visibility = View.VISIBLE
                }, 100)
                colorHandler.postDelayed({
                    colorBlueBar.visibility = View.VISIBLE
                    textBlue.visibility = View.VISIBLE
                }, 200)
                colorHandler.postDelayed({
                    colorGreenBar.visibility = View.VISIBLE
                    textGreen.visibility = View.VISIBLE
                }, 300)
                colorHandler.postDelayed({
                    colorRedBar.visibility = View.VISIBLE
                    textRed.visibility = View.VISIBLE
                }, 400)
            }
        }

        colorRedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val newColor = Color.rgb(
                    progress,
                    colorGreenBar.progress,
                    colorBlueBar.progress
                )
                with(prefs.edit()) {
                    putInt(SamSprung.prefColors, newColor)
                    apply()
                }
                colorComposite.setBackgroundColor(newColor)
                alphaPreview.setBackgroundColor(newColor)
                colorRedBar.progressTintList = ColorStateList
                    .valueOf(Color.rgb(progress, 0, 0))
            }

            override fun onStartTrackingTouch(seek: SeekBar) { }

            override fun onStopTrackingTouch(seek: SeekBar) { }
        })

        colorGreenBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val newColor = Color.rgb(
                    colorRedBar.progress,
                    progress,
                    colorBlueBar.progress
                )
                with(prefs.edit()) {
                    putInt(SamSprung.prefColors, newColor)
                    apply()
                }
                colorComposite.setBackgroundColor(newColor)
                alphaPreview.setBackgroundColor(newColor)
                colorGreenBar.progressTintList = ColorStateList
                    .valueOf(Color.rgb(0, progress, 0))
            }

            override fun onStartTrackingTouch(seek: SeekBar) { }

            override fun onStopTrackingTouch(seek: SeekBar) { }
        })

        colorBlueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val newColor = Color.rgb(
                    colorRedBar.progress,
                    colorGreenBar.progress,
                    progress
                )
                with(prefs.edit()) {
                    putInt(SamSprung.prefColors, newColor)
                    apply()
                }
                colorComposite.setBackgroundColor(newColor)
                alphaPreview.setBackgroundColor(newColor)
                colorBlueBar.progressTintList = ColorStateList
                    .valueOf(Color.rgb(0, 0, progress))
            }

            override fun onStartTrackingTouch(seek: SeekBar) { }

            override fun onStopTrackingTouch(seek: SeekBar) { }
        })

        colorAlphaBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val alpha = progress.toFloat() / 100
                with(prefs.edit()) {
                    putFloat(SamSprung.prefAlphas, alpha)
                    apply()
                }
                alphaPreview.alpha = alpha
            }

            override fun onStartTrackingTouch(seek: SeekBar) { }

            override fun onStopTrackingTouch(seek: SeekBar) { }
        })

        textRed.visibility = View.GONE
        colorRedBar.visibility = View.GONE
        textGreen.visibility = View.GONE
        colorGreenBar.visibility = View.GONE
        textBlue.visibility = View.GONE
        colorBlueBar.visibility = View.GONE
        alphaView.visibility = View.GONE
        colorAlphaBar.visibility = View.GONE
        colorPanel.visibility = View.GONE

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.cover_quick_toggles)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val pref = item.title.toPref
            with(prefs.edit()) {
                putBoolean(pref, !prefs.getBoolean(pref, true))
                apply()
            }
            when (item.itemId) {
                R.id.toggle_wifi -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_wifi_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_wifi_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_bluetooth -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_bluetooth_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_bluetooth_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_nfc -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_nfc_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_nfc_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_sound -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_sound_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_sound_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_dnd -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_do_not_disturb_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_do_not_disturb_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_torch -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_flashlight_on_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_flashlight_off_24dp)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggle_widgets -> {
                    if (prefs.getBoolean(pref, true))
                        item.setIcon(R.drawable.ic_baseline_widgets_24dp)
                    else
                        item.setIcon(R.drawable.ic_baseline_insert_page_break_24dp)
                    return@setOnMenuItemClickListener true
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
        }

        val wifi = toolbar.menu.findItem(R.id.toggle_wifi)
        if (prefs.getBoolean(wifi.title.toPref, true))
            wifi.setIcon(R.drawable.ic_baseline_wifi_on_24dp)
        else
            wifi.setIcon(R.drawable.ic_baseline_wifi_off_24dp)

        val bluetooth = toolbar.menu.findItem(R.id.toggle_bluetooth)
        if (prefs.getBoolean(bluetooth.title.toPref, true))
            bluetooth.setIcon(R.drawable.ic_baseline_bluetooth_on_24dp)
        else
            bluetooth.setIcon(R.drawable.ic_baseline_bluetooth_off_24dp)

        val nfc = toolbar.menu.findItem(R.id.toggle_nfc)
        if (prefs.getBoolean(nfc.title.toPref, true))
            nfc.setIcon(R.drawable.ic_baseline_nfc_on_24dp)
        else
            nfc.setIcon(R.drawable.ic_baseline_nfc_off_24dp)

        val sound = toolbar.menu.findItem(R.id.toggle_sound)
        if (prefs.getBoolean(sound.title.toPref, true))
            sound.setIcon(R.drawable.ic_baseline_sound_on_24dp)
        else
            sound.setIcon(R.drawable.ic_baseline_sound_off_24dp)

        val dnd = toolbar.menu.findItem(R.id.toggle_dnd)
        if (prefs.getBoolean(dnd.title.toPref, true))
            dnd.setIcon(R.drawable.ic_baseline_do_not_disturb_on_24dp)
        else
            dnd.setIcon(R.drawable.ic_baseline_do_not_disturb_off_24dp)

        val torch = toolbar.menu.findItem(R.id.toggle_torch)
        if (prefs.getBoolean(torch.title.toPref, true))
            torch.setIcon(R.drawable.ic_baseline_flashlight_on_24dp)
        else
            torch.setIcon(R.drawable.ic_baseline_flashlight_off_24dp)

        val widgets = toolbar.menu.findItem(R.id.toggle_widgets)
        if (prefs.getBoolean(widgets.title.toPref, true))
            widgets.setIcon(R.drawable.ic_baseline_widgets_24dp)
        else
            widgets.setIcon(R.drawable.ic_baseline_insert_page_break_24dp)
    }

    private val permissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BIND_APPWIDGET
            )
        else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BIND_APPWIDGET
            )

    private val requestUnchecked = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("MissingPermission")
    private val requestStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        if (it) coordinator.background = WallpaperManager.getInstance(this).drawable
    }

    private fun setAnimatedUpdateNotice(appUpdateInfo: AppUpdateInfo?, downloadUrl: String?) {
        runOnUiThread {
            val buildIcon = findViewById<AppCompatImageView>(R.id.build_icon)
            buildIcon.setImageDrawable(ContextCompat.getDrawable(
                this@CoverPreferences, R.drawable.ic_baseline_browser_updated_24dp))
            val buildInfo = findViewById<TextView>(R.id.build_info)
            val colorStateList = buildInfo.textColors
            buildInfo.setTextColor(Color.RED)
            val anim: Animation = AlphaAnimation(0.2f, 1.0f)
            anim.duration = 500
            anim.repeatMode = Animation.REVERSE
            anim.repeatCount = Animation.INFINITE
            buildInfo.startAnimation(anim)
            findViewById<LinearLayout>(R.id.build_layout).setOnClickListener {
                buildIcon.setImageDrawable(ContextCompat.getDrawable(
                    this@CoverPreferences, R.drawable.ic_github_octocat_24dp))
                anim.cancel()
                buildInfo.setTextColor(colorStateList)
                if (null != appUpdateInfo) {
                    updateCheck?.downloadPlayUpdate(appUpdateInfo)
                } else if (null != downloadUrl) {
                    updateCheck?.downloadUpdate(downloadUrl)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            if (it.key == Manifest.permission.BLUETOOTH_CONNECT && !it.value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestUnchecked.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else if (it.key == Manifest.permission.READ_EXTERNAL_STORAGE) {
                if (it.value)
                    coordinator.background = WallpaperManager.getInstance(this).drawable
                else
                    requestStorage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else if (it.key == Manifest.permission.RECORD_AUDIO && !it.value) {
                requestUnchecked.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        updateCheck = CheckUpdatesTask(this@CoverPreferences)
        if (BuildConfig.FLAVOR == "google") {
            updateCheck?.setPlayUpdateListener(object: CheckUpdatesTask.CheckPlayUpdateListener {
                override fun onPlayUpdateFound(appUpdateInfo: AppUpdateInfo) {
                    setAnimatedUpdateNotice(appUpdateInfo, null)
                }
            })
        } else {
            updateCheck?.setUpdateListener(object: CheckUpdatesTask.CheckUpdateListener {
                override fun onUpdateFound(downloadUrl: String) {
                    setAnimatedUpdateNotice(null, downloadUrl)
                }
            })
        }
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::notifications.isInitialized)
            notifications.isChecked = hasNotificationListener()
    }

    @SuppressLint("NotifyDataSetChanged")
    private val usageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        Executors.newSingleThreadExecutor().execute {
            val packageRetriever = PackageRetriever(this)
            val packages = packageRetriever.getPackageList()
            val unlisted = packageRetriever.getHiddenPackages()
            runOnUiThread {
                (hiddenList.adapter as FilteredAppsAdapter).setPackages(packages, unlisted)
                (hiddenList.adapter as FilteredAppsAdapter).notifyDataSetChanged()
            }
        }
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::accessibility.isInitialized)
            accessibility.isChecked = hasAccessibility()
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::mainSwitch.isInitialized) {
            mainSwitch.isChecked = Settings.canDrawOverlays(applicationContext)
            startForegroundService(Intent(
                ScaledContext.cover(this), OnBroadcastService::class.java
            ))
        }
    }

    private val keyboardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::keyboard.isInitialized)
            keyboard.isChecked = hasKeyboardInstalled()
    }

    private fun isDeviceSecure(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    private fun hasAccessibility(): Boolean {
        val serviceString = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return serviceString != null && serviceString.contains(packageName
                + File.separator + AccessibilityObserver::class.java.name)
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

    private fun hasKeyboardInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(BuildConfig.APPLICATION_ID + ".ime", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun captureLogcat(parent: ViewGroup) {
        if (!Debug(this).captureLogcat(isDeviceSecure())) {
            IconifiedSnackbar(this, parent).buildSnackbar(
                R.string.logcat_failed, R.drawable.ic_android_studio_24dp, Snackbar.LENGTH_LONG
            ).show()
        }
    }

    val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls())
            updateCheck?.retrieveUpdate()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.cover_settings_menu, menu)
        val actionSwitch: MenuItem = menu.findItem(R.id.switch_action_bar)
        actionSwitch.setActionView(R.layout.configure_switch)
        mainSwitch = menu.findItem(R.id.switch_action_bar).actionView
            .findViewById(R.id.switch2) as SwitchCompat
        mainSwitch.setOnCheckedChangeListener { _, isChecked ->
            permissionList.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        mainSwitch.isChecked = Settings.canDrawOverlays(applicationContext)
        mainSwitch.setOnClickListener {
            overlayLauncher.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        return true
    }

    private fun initializeLayout() {
        startForegroundService(Intent(
            ScaledContext.cover(this), OnBroadcastService::class.java
        ))

        if (!prefs.getBoolean(SamSprung.prefWarned, false)) {
            val view: View = layoutInflater.inflate(R.layout.setup_notice_view, null)
            val dialog = AlertDialog.Builder(
                ContextThemeWrapper(this, R.style.DialogTheme_NoActionBar)
            )
            val setupDialog: Dialog = dialog.setView(view).create()
            view.findViewById<AppCompatButton>(R.id.button_wiki).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/SamSprung/SamSprung-TooUI/wiki")))
            }
            view.findViewById<AppCompatButton>(R.id.setup_confirm).setOnClickListener {
                with(prefs.edit()) {
                    putBoolean(SamSprung.prefWarned, true)
                    apply()
                }
                setupDialog.dismiss()
            }
            setupDialog.setOnDismissListener {
                verifyCompatibility()
            }
            setupDialog.setCancelable(false)
            setupDialog.show()
            setupDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_layout_themed)
            setupDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            verifyCompatibility()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initializeLayout()
    }

    private fun getIAP(amount: Int) : String {
        return String.format("subscription_%02d", amount)
    }

    private fun getSub(amount: Int) : String {
        return String.format("monthly_%02d", amount)
    }

    private val responseListenerIAP = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            iapSkuDetails.clear()
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                iapSkuDetails.add(skuDetail)
            }
        }
        billingClient.queryPurchaseHistoryAsync(
            BillingClient.SkuType.INAPP, iapHistoryListener)
    }

    private val responseListenerSub = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            subSkuDetails.clear()
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                subSkuDetails.add(skuDetail)
            }
        }
        billingClient.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS, subHistoryListener)
    }

    private val consumeResponseListener = ConsumeResponseListener { _, _ ->
        Snackbar.make(
            findViewById(R.id.donation_wrapper),
            R.string.donation_thanks, Snackbar.LENGTH_LONG
        ).show()
    }

    private fun handlePurchaseIAP(purchase : Purchase) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
        billingClient.consumeAsync(consumeParams.build(), consumeResponseListener)
    }

    private var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener {
        IconifiedSnackbar(this).buildTickerBar(getString(R.string.donation_thanks)).show()
        hasPremiumSupport = true
    }

    private fun handlePurchaseSub(purchase : Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build(),
            acknowledgePurchaseResponseListener)
    }

    private fun handlePurchase(purchase : Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                for (iap: String in iapList) {
                    if (purchase.skus.contains(iap))
                        handlePurchaseIAP(purchase)
                }
                for (sub: String in subList) {
                    if (purchase.skus.contains(sub))
                        handlePurchaseSub(purchase)
                }
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private lateinit var subsPurchased: ArrayList<String>

    private val iapOwnedListener = PurchasesResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in purchases) {
                for (sku in purchase.skus) {
                    if (subsPurchased.contains(sku)) {
                        hasPremiumSupport = true
                        break
                    }
                }
            }
        }
    }

    private val subHistoryListener = PurchaseHistoryResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases)
                subsPurchased.addAll(purchase.skus)
            billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, iapOwnedListener)
        }
    }

    private val iapHistoryListener = PurchaseHistoryResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases) {
                for (sku in purchase.skus) {
                    if (sku.split("_")[1].toInt() >= 10) {
                        hasPremiumSupport = true
                        break
                    }
                }
            }
        }
    }

    private fun verifyCompatibility() {
        if (isDeviceSecure() && !prefs.getBoolean(SamSprung.prefSecure, false)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.secure_notice)
                .setMessage(R.string.lock_screen_warning)
                .setPositiveButton(R.string.button_confirm) { dialog, _ ->
                    with (prefs.edit()) {
                        putBoolean(SamSprung.prefSecure,  true)
                        apply()
                    }
                    dialog.dismiss()
                    runOnUiThread { requestPermissions.launch(permissions) }
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }.show()
        } else {
            runOnUiThread { requestPermissions.launch(permissions) }
        }
    }

    private val billingConnectionMutex = Mutex()

    private val resultAlreadyConnected = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .setDebugMessage("Billing client is already connected")
        .build()

    /**
     * Returns immediately if this BillingClient is already connected, otherwise
     * initiates the connection and suspends until this client is connected.
     * If a connection is already in the process of being established, this
     * method just suspends until the billing client is ready.
     */
    private suspend fun BillingClient.connect(): BillingResult = billingConnectionMutex.withLock {
        if (isReady) {
            // fast path: avoid suspension if already connected
            resultAlreadyConnected
        } else {
            unsafeConnect()
        }
    }

    private suspend fun BillingClient.unsafeConnect() = suspendCoroutine<BillingResult> { cont ->
        startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                cont.resume(billingResult)
            }
            override fun onBillingServiceDisconnected() {
                // no need to setup reconnection logic here, call ensureReady()
                // before each purchase to reconnect as necessary
            }
        })
    }

    private fun retrieveDonationMenu() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener).enablePendingPurchases().build()

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val clientResponseCode = billingClient.connect().responseCode
            if (clientResponseCode == BillingClient.BillingResponseCode.OK) {
                iapList.add(getIAP(1))
                iapList.add(getIAP(5))
                iapList.add(getIAP(10))
                iapList.add(getIAP(25))
                iapList.add(getIAP(50))
                iapList.add(getIAP(75))
                iapList.add(getIAP(99))
                billingClient.querySkuDetailsAsync(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(iapList)
                        .setType(BillingClient.SkuType.INAPP)
                        .build(), responseListenerIAP
                )

                subList.add(getSub(1))
                subList.add(getSub(5))
                subList.add(getSub(10))
                subList.add(getSub(25))
                subList.add(getSub(50))
                subList.add(getSub(75))
                subList.add(getSub(99))
                billingClient.querySkuDetailsAsync(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(subList)
                        .setType(BillingClient.SkuType.SUBS)
                        .build(), responseListenerSub
                )
            }
        }
    }

    private val CharSequence.toPref get() = this.toString()
        .lowercase().replace(" ", "_")

    private val Number.toPx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        Resources.getSystem().displayMetrics
    )

    private val Number.toScalePx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        ScaledContext.screen(this@CoverPreferences).resources.displayMetrics
    )
}