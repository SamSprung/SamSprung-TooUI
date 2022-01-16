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

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class SamSprungHomeView : AppCompatActivity(), NotificationsAdapter.OnNoticeClickListener  {

    @SuppressLint("InflateParams", "CutPasteId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_view_layout)

        findViewById<LinearLayout>(R.id.rootLayout).setOnTouchListener(
            object: OnSwipeTouchListener(this@SamSprungHomeView) {
            override fun onSwipeLeft() {
                val coverIntent = Intent(SamSprung.context, CoverListenerService::class.java)
                coverIntent.putExtra("dismissListener", "dismissListener")
                startService(coverIntent)
                finish()
            }

            override fun onSwipeRight() {
                val coverIntent = Intent(SamSprung.context, SamSprungAppsView::class.java)
                coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
                coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                coverIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                coverIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                coverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(coverIntent, options.toBundle())
                finish()
            }
        })

        val noticesView = findViewById<RecyclerView>(R.id.notificationList)

        noticesView.layoutManager = LinearLayoutManager(this)
        noticesView.adapter = NotificationsAdapter(this)

        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.RIGHT) {
                    val coverIntent = Intent(SamSprung.context, SamSprungAppsView::class.java)
                    coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
                    coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    coverIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    coverIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    coverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(coverIntent, options.toBundle())
                    finish()
                }
                if (direction == ItemTouchHelper.LEFT) {
                    val coverIntent = Intent(SamSprung.context, CoverListenerService::class.java)
                    coverIntent.putExtra("dismissListener", "dismissListener")
                    startService(coverIntent)
                    finish()
                }
            }
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(noticesView)
    }

    override fun onNoticeClicked(notice: Notification, position: Int) {
        startIntentSender(
            notice.contentIntent.intentSender,
            null, 0, 0, 0)
    }
}