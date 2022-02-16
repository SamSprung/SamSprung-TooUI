package com.eightbit.samsprung.launcher

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

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.SearchManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.eightbit.pm.PackageRetriever
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.RecyclerViewTouch
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.concurrent.Executors

class AppDrawerFragment : Fragment(), DrawerAppAdapater.OnAppClickListener {

    private lateinit var packReceiver: BroadcastReceiver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_drawer, container, false
        ) as ViewGroup
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences(
            SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)

        val launcherView = view.findViewById<RecyclerView>(R.id.appsList)

        val packageRetriever = PackageRetriever(requireActivity())
        var packages = packageRetriever.getFilteredPackageList()

        launcherView.setHasFixedSize(true)
        if (prefs.getBoolean(SamSprung.prefLayout, true)) {
            launcherView.layoutManager = GridLayoutManager(activity, getColumnCount())
        } else {
            launcherView.layoutManager = LinearLayoutManager(activity)
            launcherView.addItemDecoration(DividerItemDecoration(activity,
                DividerItemDecoration.VERTICAL))
        }
        launcherView.adapter = DrawerAppAdapater(
            packages, this, requireActivity().packageManager, prefs)

        val searchView = (requireActivity() as SamSprungOverlay).getSearch()
        val searchManager = requireActivity().getSystemService(AppCompatActivity.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
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

        val bottomSheetMain = (requireActivity() as SamSprungOverlay).getBottomSheetMain()

        RecyclerViewTouch(launcherView).setSwipeCallback(
            ItemTouchHelper.END or ItemTouchHelper.DOWN,
            object: RecyclerViewTouch.SwipeCallback {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.DOWN) {
                    onSwipeDown(launcherView, bottomSheetMain)
                }
            }
        })

        launcherView.setOnTouchListener(object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeBottom() : Boolean {
                onSwipeDown(launcherView, bottomSheetMain)
                return false
            }
        })

        packReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                    Executors.newSingleThreadExecutor().execute {
                        packages = packageRetriever.getFilteredPackageList()
                        requireActivity().runOnUiThread {
                            (launcherView.adapter as DrawerAppAdapater).setPackages(packages)
                            (launcherView.adapter as DrawerAppAdapater).notifyDataSetChanged()
                        }
                    }
                } else if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        Executors.newSingleThreadExecutor().execute {
                            packages = packageRetriever.getFilteredPackageList()
                            requireActivity().runOnUiThread {
                                (launcherView.adapter as DrawerAppAdapater).setPackages(packages)
                                (launcherView.adapter as DrawerAppAdapater).notifyDataSetChanged()
                            }
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
            requireActivity().registerReceiver(packReceiver, it)
        }
    }

    private fun onSwipeDown(launcherView: RecyclerView, bottomSheet: BottomSheetBehavior<View>) {
        if (launcherView.layoutManager is LinearLayoutManager) {
            if ((launcherView.layoutManager as LinearLayoutManager)
                    .findFirstCompletelyVisibleItemPosition() == 0) {
                bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        if (launcherView.layoutManager is GridLayoutManager) {
            if ((launcherView.layoutManager as GridLayoutManager)
                    .findFirstCompletelyVisibleItemPosition() == 0) {
                bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun getColumnCount(): Int {
        return (requireActivity().windowManager.currentWindowMetrics.bounds.width() / 96 + 0.5).toInt()
    }

    private fun prepareConfiguration() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        val mKeyguardManager = (requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        (requireActivity().application as SamSprung).isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if ((requireActivity().application as SamSprung).isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }

        mKeyguardManager.requestDismissKeyguard(requireActivity(),
            object : KeyguardManager.KeyguardDismissCallback() { })
    }

    override fun onAppClicked(appInfo: ResolveInfo, position: Int) {
        prepareConfiguration()

        (requireActivity().getSystemService(AppCompatActivity
            .LAUNCHER_APPS_SERVICE) as LauncherApps).startMainActivity(
            ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name),
            Process.myUserHandle(),
            requireActivity().windowManager.currentWindowMetrics.bounds,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
        )

        val extras = Bundle()
        extras.putString("launchPackage", appInfo.activityInfo.packageName)
        extras.putString("launchActivity", appInfo.activityInfo.name)

        val orientationChanger = LinearLayout((requireActivity().application as SamSprung).getScaledContext())
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowManager = (requireActivity().application as SamSprung).getScaledContext()?.getSystemService(
            Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(orientationChanger, orientationLayout)
        orientationChanger.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().runOnUiThread {
                windowManager.removeViewImmediate(orientationChanger)
                (requireActivity() as SamSprungOverlay).onDismiss()
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                requireActivity().startForegroundService(
                    Intent(requireActivity(),
                    AppDisplayListener::class.java).putExtras(extras))
            }
        }, 50)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (this::packReceiver.isInitialized)
                requireActivity().unregisterReceiver(packReceiver)
        } catch (ignored: Exception) { }
    }
}