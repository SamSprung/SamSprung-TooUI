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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class PackageRetriever(val context: Context) {

    private var prefs: SharedPreferences = context.getSharedPreferences(
        SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE
    )

    private fun getPackageList() : MutableList<ResolveInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: MutableList<ResolveInfo> = context.packageManager.queryIntentActivities(
            mainIntent, PackageManager.GET_RESOLVED_FILTER
        )
        packages.removeIf { item ->
            null != item.filter && item.filter.hasCategory(Intent.CATEGORY_HOME)
        }
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(context.packageManager))
        return packages
    }

    fun getRecentPackageList(recentFirst: Boolean) : MutableList<ResolveInfo> {
        val packages = getPackageList()
        val statsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            statsManager.isAppInactive(context.packageName)
        } catch (ignored: Exception) {
            return packages
        }
        val recent: HashSet<ResolveInfo> = HashSet()
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60 * 60 * 1000
        val usageEvents: UsageEvents = statsManager.queryEvents(startTime, endTime)
        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (UsageEvents.Event.ACTIVITY_RESUMED == event.eventType) {
                val iterator: MutableIterator<ResolveInfo> = packages.iterator()
                while (iterator.hasNext()) {
                    val info = iterator.next()
                    if (context.packageName != event.packageName
                        && event.packageName == info.activityInfo.packageName) {
                        iterator.remove()
                        recent.add(info)
                    }
                }
            }
        }
        if (recentFirst)
            packages.addAll(0, recent)
        else
            packages.addAll(recent.reversed())
        return packages
    }

    fun getHiddenPackages() : HashSet<String> {
        val unlisted: HashSet<String> = HashSet()
        val hide: Set<String> = prefs.getStringSet(
            SamSprung.prefHidden, setOf<String>()) as Set<String>
        unlisted.addAll(hide)
        return unlisted
    }

    fun getFilteredPackageList(): MutableList<ResolveInfo> {
        val packages = getRecentPackageList(true)
        packages.removeIf { item ->
            prefs.getStringSet(SamSprung.prefHidden, HashSet())!!
                .contains(item.activityInfo.packageName)
        }
        return packages
    }
}