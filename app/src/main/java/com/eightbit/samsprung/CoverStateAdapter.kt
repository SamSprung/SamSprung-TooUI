package com.eightbit.samsprung

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eightbit.samsprung.launcher.AppDrawerFragment
import com.eightbit.samsprung.panels.PanelViewFragment
import java.util.ArrayList

class CoverStateAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    private val mFragments = ArrayList<PanelViewFragment>()
    private val mFragmentIDs = ArrayList<Int>()
    private val appDrawer = AppDrawerFragment()

    fun addFragment() : Int {
        val fragment = PanelViewFragment()
        val position = mFragmentIDs.size + 1
        mFragments.add(fragment)
        notifyItemInserted(position)
        mFragmentIDs.add(position)
        return position
    }

    fun removeFragment(fragmentID: Int) {
        val position = fragmentID - 1
        mFragments.removeAt(position)
        mFragmentIDs.removeAt(position)
        mFragments.trimToSize()
        mFragmentIDs.trimToSize()
        notifyItemRemoved(fragmentID)
    }

    fun getFragment(fragmentID: Int) : PanelViewFragment {
        return mFragments[fragmentID - 1]
    }

    fun getDrawer() : AppDrawerFragment {
        return appDrawer
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) appDrawer else mFragments[position - 1]
    }

    override fun getItemId(position: Int): Long {
        return if (position == 0) 0L else mFragmentIDs[position - 1].toLong()
    }

    override fun getItemCount(): Int {
        return mFragmentIDs.size + 1
    }

    override fun containsItem(itemId: Long): Boolean {
        return mFragmentIDs.contains(itemId.toInt())
    }
}