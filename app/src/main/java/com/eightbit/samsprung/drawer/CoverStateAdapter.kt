/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

package com.eightbit.samsprung.drawer

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CoverStateAdapter(fragmentActivity: AppCompatActivity) : FragmentStateAdapter(fragmentActivity) {
    private val mFragments = ArrayList<PanelViewFragment>()
    private val mFragmentIDs = ArrayList<Int>()
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
