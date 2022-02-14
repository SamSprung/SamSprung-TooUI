package com.eightbit.samsprung.launcher

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
import android.util.TypedValue
import android.view.*
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.pm.PackageRetriever
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import java.util.concurrent.Executors

class AppDrawerFragment : Fragment(), DrawerAppAdapater.OnAppClickListener {

    private lateinit var launcherView: RecyclerView
    private lateinit var searchView: SearchView

    private var listener: AppDrawerListener? = null

    fun setListener(listener: AppDrawerListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_drawer, container, false
        ) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       val prefs = requireActivity().getSharedPreferences(
           SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)

        launcherView = view.findViewById<RecyclerView>(R.id.appsList)

        val packageRetriever = PackageRetriever(requireActivity())
        var packages = packageRetriever.getFilteredPackageList()

        if (prefs.getBoolean(SamSprung.prefLayout, true))
            launcherView.layoutManager = GridLayoutManager(activity, getColumnCount())
        else
            launcherView.layoutManager = LinearLayoutManager(activity)
        launcherView.adapter = DrawerAppAdapater(
            packages, this, requireActivity().packageManager, prefs)

        searchView = view.findViewById<SearchView>(R.id.package_search)
        searchView.findViewById<LinearLayout>(R.id.search_bar)?.run {
            this.layoutParams = this.layoutParams.apply {
                height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    24f, resources.displayMetrics).toInt()
            }
        }
        searchView.gravity = Gravity.CENTER_VERTICAL

        val searchManager = requireActivity().getSystemService(AppCompatActivity.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.isSubmitButtonEnabled = false
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                (launcherView.adapter as DrawerAppAdapater).setQuery(query)
                searchView.visibility = View.GONE
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                (launcherView.adapter as DrawerAppAdapater).setQuery(query)
                return true
            }
        })
        searchView.visibility = View.GONE

        listener?.onDrawerCreated(launcherView)

        val viewReceiver = object : BroadcastReceiver() {
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
            requireActivity().registerReceiver(viewReceiver, it)
        }
    }

    private fun getColumnCount(): Int {
        return (requireActivity().windowManager.currentWindowMetrics.bounds.width() / 96 + 0.5).toInt()
    }

    private fun animateSearchReveal(view: View, anchor: View) {
        val animate = TranslateAnimation(
            -anchor.width.toFloat(), 0f, 0f, 0f
        )
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
    }

    fun clearSearchOrOpen(anchor: View) {
        if (searchView.query.isNotBlank()) {
            searchView.setQuery("", true)
            if (searchView.isVisible)
                searchView.visibility = View.GONE
        } else if (!searchView.isVisible) {
            searchView.visibility = View.VISIBLE
            animateSearchReveal(searchView, anchor)
        }
    }

    fun clearSearchOrClose() {
        if (searchView.query.isNotBlank()) {
            searchView.setQuery("", true)
            if (searchView.isVisible)
                searchView.visibility = View.GONE
        }
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

        (requireActivity().getSystemService(AppCompatActivity.LAUNCHER_APPS_SERVICE) as LauncherApps).startMainActivity(
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

    interface AppDrawerListener {
        fun onDrawerCreated(launcherView: RecyclerView)
    }
}