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
import android.app.Dialog
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.android.billingclient.api.*
import com.eightbit.material.IconifiedSnackbar
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import com.google.android.material.snackbar.Snackbar
import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashSet

class CoverSettingsActivity : AppCompatActivity() {

    private lateinit var updates : CheckUpdatesTask

    private lateinit var switch: SwitchCompat
    private lateinit var accessibility: SwitchCompat
    private lateinit var notifications: SwitchCompat
    private lateinit var settings: SwitchCompat

    private lateinit var billingClient: BillingClient
    private val iapList = ArrayList<String>()
    private val subList = ArrayList<String>()
    private val buttonsIAP = ArrayList<Button>()
    private val buttonsSub = ArrayList<Button>()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_editor_layout)

        onNewIntent(intent)

        if (SamSprung.prefs.contains(SamSprung.autoRotate))
            with(SamSprung.prefs.edit()) {
                remove(SamSprung.autoRotate)
                apply()
            }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            findViewById<CoordinatorLayout>(R.id.coordinator).background =
                WallpaperManager.getInstance(this).drawable
        }

        findViewById<BlurView>(R.id.blurContainer).setupWith(
            window.decorView.findViewById(R.id.coordinator))
            .setFrameClearDrawable(window.decorView.background)
            .setBlurRadius(10f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
            .setBlurAlgorithm(RenderScriptBlur(this))

        val isGridView = SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)
        findViewById<ToggleButton>(R.id.swapViewType).isChecked = isGridView
        findViewById<ToggleButton>(R.id.swapViewType).setOnCheckedChangeListener { _, isChecked ->
            with (SamSprung.prefs.edit()) {
                putBoolean(SamSprung.prefLayout, isChecked)
                apply()
            }
        }

        accessibility = findViewById(R.id.accessibility_switch)
        accessibility.isChecked = hasAccessibility()
        accessibility.setOnClickListener {
            if (accessibility.isChecked) {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.aceessibility_details))
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

        notifications = findViewById(R.id.notifications_switch)
        notifications.isChecked = hasNotificationListener()
        notifications.setOnClickListener {
            notificationLauncher.launch(Intent(
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ))
        }

        settings = findViewById(R.id.settings_switch)
        settings.isChecked = Settings.System.canWrite(applicationContext)
        settings.setOnClickListener {
            settingsLauncher.launch(Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            ))
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = packageManager.queryIntentActivities(
            mainIntent, PackageManager.GET_RESOLVED_FILTER)
        packages.removeIf { item -> null != item.filter
                && item.filter.hasCategory(Intent.CATEGORY_HOME) }
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))

        val unlisted: HashSet<String> = HashSet()
        val hide: Set<String> = SamSprung.prefs.getStringSet(
            SamSprung.prefHidden, setOf<String>()) as Set<String>
        unlisted.addAll(hide)

        val listView: ListView = findViewById(R.id.selectionListView)
        listView.adapter = FilteredAppsAdapter(this, packages, unlisted)

        val scalingNavGroup = findViewById<LinearLayout>(R.id.scaling_nav)
        val scalingNav : SwitchCompat = findViewById(R.id.scaling_nav_switch)
        scalingNav.isChecked = SamSprung.prefs.getBoolean(SamSprung.prefReader, false)
        scalingNav.setOnClickListener {
            with(SamSprung.prefs.edit()) {
                putBoolean(SamSprung.prefReader, scalingNav.isChecked)
                apply()
            }
        }

        val scalingGen : SwitchCompat = findViewById(R.id.scaling_gen_switch)
        scalingGen.isChecked = SamSprung.prefs.getBoolean(SamSprung.prefScaled, false)
        scalingNavGroup.visibility = if (scalingGen.isChecked) View.VISIBLE else View.GONE
        scalingGen.setOnClickListener {
            with(SamSprung.prefs.edit()) {
                putBoolean(SamSprung.prefScaled, scalingGen.isChecked)
                apply()
            }
            scalingNavGroup.visibility = if (scalingGen.isChecked) View.VISIBLE else View.GONE
        }

        startForegroundService(Intent(this, OnBroadcastService::class.java))

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener).enablePendingPurchases().build()

        Executors.newSingleThreadExecutor().execute {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        iapList.add(getIAP(1))
                        iapList.add(getIAP(5))
                        iapList.add(getIAP(10))
                        iapList.add(getIAP(25))
                        iapList.add(getIAP(50))
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
                        subList.add(getSub(99))
                        billingClient.querySkuDetailsAsync(
                            SkuDetailsParams.newBuilder()
                                .setSkusList(subList)
                                .setType(BillingClient.SkuType.SUBS)
                                .build(), responseListenerSub
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }

    private val permissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

    private val requestBluetooth = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("MissingPermission")
    private val requestStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        if (it) findViewById<CoordinatorLayout>(R.id.coordinator).background =
            WallpaperManager.getInstance(this).drawable
        checkApplicationUpdates()
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            if (it.key == Manifest.permission.BLUETOOTH_CONNECT && !it.value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else if (it.key == Manifest.permission.READ_EXTERNAL_STORAGE && !it.value) {
                requestStorage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                checkApplicationUpdates()
            }
        }

    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::notifications.isInitialized)
            notifications.isChecked = hasNotificationListener()
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::settings.isInitialized)
            settings.isChecked = Settings.System.canWrite(applicationContext)
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::accessibility.isInitialized)
            accessibility.isChecked = hasAccessibility()
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (this::switch.isInitialized) {
            switch.isChecked = Settings.canDrawOverlays(applicationContext)
            if (switch.isChecked)
                startForegroundService(Intent(this, OnBroadcastService::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.logcat -> {
            captureLogcat()
            true
        }
        R.id.subscribe -> {
            val view: View = layoutInflater.inflate(R.layout.donation_layout, null)
            val dialog = AlertDialog.Builder(
                ContextThemeWrapper(this, R.style.DialogTheme_NoActionBar)
            )
            val donations = view.findViewById<LinearLayout>(R.id.donation_layout)
            for (button: Button in buttonsIAP)
                donations.addView(button)
            val subscriptions = view.findViewById<LinearLayout>(R.id.subscription_layout)
            for (button: Button in buttonsSub)
                subscriptions.addView(button)
            dialog.setOnCancelListener {
                donations.removeAllViewsInLayout()
                subscriptions.removeAllViewsInLayout()
            }
            val donateDialog: Dialog = dialog.setView(view).show()
            donateDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_view)
            true
        }
        R.id.donate -> {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/donate/?hosted_button_id=Q2LFH2SC8RHRN")))
            true
        } else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuWithIcon(item: MenuItem, color: Int) {
        val builder = SpannableStringBuilder().append("*").append("    ").append(item.title)
        if (item.icon != null && item.icon.constantState != null) {
            val drawable = item.icon.constantState!!.newDrawable()
            if (-1 != color) drawable.mutate().setTint(color)
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = ImageSpan(drawable)
            builder.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            item.title = builder
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        updateMenuWithIcon(menu.findItem(R.id.logcat), -1)
        updateMenuWithIcon(menu.findItem(R.id.subscribe), -1)
        if (BuildConfig.FLAVOR == "github")
            updateMenuWithIcon(menu.findItem(R.id.donate), -1)
        else
            menu.findItem(R.id.donate).isVisible = false
        menu.findItem(R.id.version).title = (getString(R.string.build_hash, BuildConfig.COMMIT))
        updateMenuWithIcon(menu.findItem(R.id.version), -1)
        val actionSwitch: MenuItem = menu.findItem(R.id.switch_action_bar)
        actionSwitch.setActionView(R.layout.configure_switch)
        switch = menu.findItem(R.id.switch_action_bar).actionView
            .findViewById(R.id.switch2) as SwitchCompat
        switch.isChecked = Settings.canDrawOverlays(applicationContext)
        switch.setOnClickListener {
            overlayLauncher.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        return true
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

    private fun getRepositoryToken(): String {
        val hex = "6768705f7666375663347a52574b396165634c33703431524c596d39716950617766323150626c47"
        val output = java.lang.StringBuilder()
        var i = 0
        while (i < hex.length) {
            val str = hex.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    private fun captureLogcat() {
        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        log.append(getString(R.string.build_hash, BuildConfig.COMMIT))
        try {
            var line: String?
            val mLogcatProc: Process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d", "-t", "512", BuildConfig.APPLICATION_ID, "AndroidRuntime",
                "AppIconSolution:S", "ViewRootImpl:S", "IssueReporterActivity:S",
            ))
            val reader = BufferedReader(InputStreamReader(mLogcatProc.inputStream))
            log.append(separator).append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        IssueReporterLauncher.forTarget("SamSprung", "SamSprung-TooUI")
            .theme(R.style.Theme_SecondScreen_NoActionBar)
            .guestToken(getRepositoryToken())
            .guestEmailRequired(true)
            .guestAllowUsername(true)
            .titleTextDefault(getString(R.string.build_hash, BuildConfig.COMMIT))
            .minDescriptionLength(50)
            .putExtraInfo("logcat", log.toString())
            .homeAsUpEnabled(false).launch(this)
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls())
            updates.retrieveUpdate()
    }

    private fun checkApplicationUpdates() {
        updates = CheckUpdatesTask(applicationContext)
        if (BuildConfig.FLAVOR != "google") {
            if (packageManager.canRequestPackageInstalls()) {
                updates.retrieveUpdate()
            } else {
                updateLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse(String.format("package:%s", packageName))))
            }
        }
    }

    private fun getIAP(amount: Int) : String {
        return String.format("subscription_%02d", amount)
    }

    private fun getSub(amount: Int) : String {
        return String.format("monthly_%02d", amount)
    }

    private val responseListenerIAP = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                val button = Button(this)
                button.setBackgroundResource(R.drawable.button_rippled)
                button.text = skuDetail.title
                button.setOnClickListener {
                    billingClient.launchBillingFlow(this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build())
                }
                buttonsIAP.add(button)
            }
        }
    }

    private val responseListenerSub = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                val button = Button(this)
                button.setBackgroundResource(R.drawable.button_rippled)
                button.text = skuDetail.title
                button.setOnClickListener {
                    billingClient.launchBillingFlow(this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build())
                }
                buttonsSub.add(button)
            }
        }
    }

    private val consumeResponseListener = ConsumeResponseListener { _, _ ->
        Snackbar.make(
            findViewById(R.id.donation_wrapper),
            R.string.donation_thanks, Snackbar.LENGTH_LONG
        ).show()
    }

    private fun handlePurchaseIAP(purchase : Purchase) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
        billingClient.consumeAsync(consumeParams.build(), consumeResponseListener
        )
    }

    private var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener {
        IconifiedSnackbar(this).buildTickerBar(getString(R.string.donation_thanks)).show()
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
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        requestPermissions.launch(permissions)
        if (isDeviceSecure() && !SamSprung.prefs.getBoolean(SamSprung.prefSecure, false)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.caveats_title)
                .setMessage(R.string.caveats_warning)
                .setPositiveButton(R.string.button_confirm) { dialog, _ ->
                    with (SamSprung.prefs.edit()) {
                        putBoolean(SamSprung.prefSecure,  true)
                        apply()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }.show()
        }
    }
}