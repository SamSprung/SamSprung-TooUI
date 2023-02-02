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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CoverStateAdapter(fragmentActivity: AppCompatActivity) : FragmentStateAdapter(fragmentActivity) {
    private val mFragments: ArrayList<PanelViewFragment> = arrayListOf()
    private val mFragmentIDs: ArrayList<Int> = arrayListOf()
    private val noticeList = NotificationFragment()
    private val appDrawer = AppDrawerFragment()

    fun addFragment() : Int {
        val fragment = PanelViewFragment()
        val position = mFragmentIDs.size + 2
        mFragments.add(fragment)
        notifyItemInserted(position)
        mFragmentIDs.add(position)
        return position
    }

    fun removeFragment(fragmentID: Int) {
        val position = fragmentID - 2
        mFragments.removeAt(position)
        mFragmentIDs.removeAt(position)
        mFragments.trimToSize()
        mFragmentIDs.trimToSize()
        notifyItemRemoved(fragmentID)
    }

    fun getFragment(fragmentID: Int) : PanelViewFragment {
        return mFragments[fragmentID - 2]
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> noticeList
            1 -> appDrawer
            else -> mFragments[position - 2]
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < 2) position.toLong() else mFragmentIDs[position - 2].toLong()
    }

    override fun getItemCount(): Int {
        return mFragmentIDs.size + 2
    }

    override fun containsItem(itemId: Long): Boolean {
        return mFragmentIDs.contains(itemId.toInt())
    }
}
