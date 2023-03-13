/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.samsprung.drawer

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.*
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.eightbit.content.ScaledContext
import com.eightbit.io.Debug
import com.eightbit.pm.PackageRetriever
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprungOverlay
import com.eightbit.samsprung.settings.Preferences
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.RecyclerViewTouch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class AppDrawerFragment : Fragment(), DrawerAppAdapter.OnAppClickListener {

    private val Number.toPx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        ScaledContext(requireActivity()).cover().resources.displayMetrics
    )

    private var launcherManager: LauncherManager? = null
    private lateinit var launcherView: RecyclerView
    private var packReceiver: BroadcastReceiver? = null
    private lateinit var prefs: SharedPreferences

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

        prefs = requireActivity().getSharedPreferences(
            Preferences.prefsValue, AppCompatActivity.MODE_PRIVATE)

        launcherManager = LauncherManager(requireActivity() as SamSprungOverlay)

        launcherView = view.findViewById(R.id.appsList)
        // launcherView.setHasFixedSize(true)

        val packageRetriever = PackageRetriever(requireActivity())
        val packages = packageRetriever.getFilteredPackageList()

        val adapter = DrawerAppAdapter(
            packages, this, requireActivity().packageManager, prefs
        )
        launcherView.adapter = adapter

        val searchView = (requireActivity() as SamSprungOverlay).getSearchView()
        if (null != searchView) {
            launcherView.updatePadding(bottom = 64)
            searchView.isSubmitButtonEnabled = false
            searchView.setIconifiedByDefault(true)
            searchView.findViewById<LinearLayout>(R.id.search_bar)?.run {
                this.layoutParams = this.layoutParams.apply {
                    height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        20f, resources.displayMetrics).toInt()
                }
            }
            (requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager).run {
                searchView.setSearchableInfo(getSearchableInfo(requireActivity().componentName))
            }
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    (launcherView.adapter as DrawerAppAdapter).setQuery(query)
                    return false
                }

                override fun onQueryTextChange(query: String): Boolean {
                    if (query.isEmpty()) searchView.isIconified = true
                    (launcherView.adapter as DrawerAppAdapter).setQuery(query)
                    return true
                }
            })
        } else {
            launcherView.updatePadding(bottom = 30)
            requireActivity().findViewById<SearchView>(R.id.package_search).isGone = true
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
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val packageRetriever = PackageRetriever(requireActivity())
            val packages = packageRetriever.getFilteredPackageList()
            withContext(Dispatchers.Main) {
                val adapter = launcherView.adapter as DrawerAppAdapter
                adapter.setPackages(packages)
                adapter.notifyDataSetChanged()
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
        val columnDefault = if (Debug.isOppoDevice) 3 else 6
        val columns = prefs.getInt(Preferences.prefLength, columnDefault)
        return if (columns < columnDefault) {
            columns
        } else {
            (requireActivity().windowManager.currentWindowMetrics
                .bounds.width() / 92.toPx + 0.5).toInt()
            // drawer_apps_icon: width - 48, margin - 4, scale - 1.5
        }
    }

    override fun onAppClicked(resolveInfo: ResolveInfo, position: Int) {
        launcherManager?.launchResolveInfo(resolveInfo)
        if (this::launcherView.isInitialized) getFilteredPackageList()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean(Preferences.prefLayout, true)) {
            launcherView.layoutManager = GridLayoutManager(activity, getColumnCount())
        } else {
            launcherView.layoutManager = LinearLayoutManager(activity)
            launcherView.addItemDecoration(DividerItemDecoration(activity,
                DividerItemDecoration.VERTICAL))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireActivity().unregisterReceiver(packReceiver) } catch (ignored: Exception) { }
    }
}
