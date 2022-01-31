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
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.eightbit.content.ScaledContext
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.VerticalStrokeTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior


class SamSprungOverlay : AppCompatActivity() {

    private lateinit var  prefs: SharedPreferences
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT)

        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        supportActionBar?.hide()
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val wlp: WindowManager.LayoutParams = window.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen_NoActionBar)
        setContentView(R.layout.navigation_menu)

        val bottomHandle = findViewById<View>(R.id.bottom_handle)
        bottomHandle.setBackgroundColor(prefs.getInt(SamSprung.prefColors,
            Color.rgb(255, 255, 255)))
        bottomHandle.visibility = View.VISIBLE
        val handler = Handler(Looper.getMainLooper())

        val coordinator = findViewById<CoordinatorLayout>(R.id.coordinator)
        val menu = findViewById<LinearLayout>(R.id.button_layout)
        val menuLogo = menu.findViewById<VerticalStrokeTextView>(R.id.samsprung_logo)
        val menuRecent = menu.findViewById<ImageView>(R.id.button_recent)
        val menuHome = menu.findViewById<ImageView>(R.id.button_home)
        val menuBack = menu.findViewById<ImageView>(R.id.button_back)
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    menu.visibility = View.VISIBLE
                    menuRecent.setOnClickListener {
                        finish()
                        startActivity(
                            Intent(this@SamSprungOverlay, SamSprungDrawer::class.java),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    menuHome.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    menuBack.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    menu.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val color = prefs.getInt(SamSprung.prefColors,
                    Color.rgb(255, 255, 255))
                if (slideOffset > 0) {
                    if (bottomHandle.visibility != View.INVISIBLE) {
                        val icons = menu.findViewById<LinearLayout>(R.id.icons_layout)
                        for (i in 0 until icons.childCount) {
                            (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
                        }
                        menuLogo.setTextColor(color)
                        handler.removeCallbacksAndMessages(null)
                        bottomHandle.visibility = View.INVISIBLE
                    }
                } else {
                    bottomHandle.setBackgroundColor(color)
                    if (bottomHandle.visibility != View.VISIBLE) {
                        handler.postDelayed({
                            runOnUiThread { bottomHandle.visibility = View.VISIBLE }
                        }, 500)
                    }
                }
            }
        })

        coordinator.setOnTouchListener(
            object: OnSwipeTouchListener(this@SamSprungOverlay) {
            override fun onSwipeTop() : Boolean {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                return true
            }
            override fun onSwipeBottom() : Boolean {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                return true
            }
        })
    }
}