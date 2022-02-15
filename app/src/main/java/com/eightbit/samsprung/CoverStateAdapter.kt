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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eightbit.samsprung.launcher.AppDrawerFragment
import com.eightbit.samsprung.panels.PanelViewFragment
import java.util.ArrayList

class CoverStateAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
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