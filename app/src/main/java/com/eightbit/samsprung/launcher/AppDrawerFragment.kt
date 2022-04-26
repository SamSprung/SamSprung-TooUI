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
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.eightbit.content.ScaledContext
import com.eightbit.pm.PackageRetriever
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.RecyclerViewTouch
import java.util.concurrent.Executors

class AppDrawerFragment : Fragment(), DrawerAppAdapater.OnAppClickListener {

    private val Number.toPx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        ScaledContext.cover(requireActivity()).resources.displayMetrics
    )

    private var launcherManager: LauncherManager? = null
    private lateinit var launcherView: RecyclerView
    private var packReceiver: BroadcastReceiver? = null

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

        launcherManager = LauncherManager(requireActivity() as SamSprungOverlay)

        launcherView = view.findViewById(R.id.appsList)
        launcherView.setHasFixedSize(true)

        val packageRetriever = PackageRetriever(requireActivity())
        val packages = packageRetriever.getFilteredPackageList()

        if (prefs.getBoolean(SamSprung.prefLayout, true)) {
            launcherView.layoutManager = GridLayoutManager(activity, getColumnCount())
        } else {
            launcherView.layoutManager = LinearLayoutManager(activity)
            launcherView.addItemDecoration(DividerItemDecoration(activity,
                DividerItemDecoration.VERTICAL))
        }
        launcherView.adapter = DrawerAppAdapater(
            packages, this, requireActivity().packageManager, prefs
        )

        val searchView = (requireActivity() as SamSprungOverlay).getSearchView()
        if (null != searchView) {
            launcherView.updatePadding(bottom = 60)
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
        } else {
            launcherView.updatePadding(bottom = 30)
        }

        RecyclerViewTouch(launcherView).setSwipeCallback(ItemTouchHelper.DOWN,
            object: RecyclerViewTouch.SwipeCallback {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.DOWN) {
                    onSwipeClosed(launcherView)
                }
            }
        })

        launcherView.setOnTouchListener(object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeBottom() : Boolean {
                onSwipeClosed(launcherView)
                return false
            }
        })

        packReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                    getFilteredPackageList()
                } else if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        getFilteredPackageList()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun getFilteredPackageList() {
        Executors.newSingleThreadExecutor().execute {
            val packageRetriever = PackageRetriever(requireActivity())
            val packages = packageRetriever.getFilteredPackageList()
            requireActivity().runOnUiThread {
                (launcherView.adapter as DrawerAppAdapater).setPackages(packages)
                (launcherView.adapter as DrawerAppAdapater).notifyDataSetChanged()
            }
        }
    }

    private fun onSwipeClosed(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager is LinearLayoutManager) {
            val manager = recyclerView.layoutManager as LinearLayoutManager
            if (manager.itemCount == 0 || manager.findFirstCompletelyVisibleItemPosition() == 0) {
                (requireActivity() as SamSprungOverlay).closeMainDrawer()
            }
        }
        if (recyclerView.layoutManager is GridLayoutManager) {
            val manager = recyclerView.layoutManager as GridLayoutManager
            if (manager.itemCount == 0 || manager.findFirstCompletelyVisibleItemPosition() == 0) {
                (requireActivity() as SamSprungOverlay).closeMainDrawer()
            }
        }
    }

    private fun getColumnCount(): Int {
        return (requireActivity().windowManager.currentWindowMetrics
            .bounds.width() / 92.toPx + 0.5).toInt()
    }

    override fun onAppClicked(resolveInfo: ResolveInfo, position: Int) {
        launcherManager?.launchResolveInfo(resolveInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (null != packReceiver) requireActivity().unregisterReceiver(packReceiver)
        } catch (ignored: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        if (this::launcherView.isInitialized) getFilteredPackageList()
    }
}