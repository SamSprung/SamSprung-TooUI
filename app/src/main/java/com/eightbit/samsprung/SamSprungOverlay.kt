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

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.eightbit.content.ScaledContext
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File

class SamSprungOverlay : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        // setTurnScreenOn(true)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val wlp: WindowManager.LayoutParams = window.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (SamSprung.prefs.getBoolean(SamSprung.prefScaled, false)) {
            ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen_NoActionBar)
            setContentView(R.layout.scaled_navigation)
        } else {
            setContentView(R.layout.navigation_layout)
        }

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val menu = findViewById<LinearLayout>(R.id.button_layout)!!
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (!menu.isVisible) menu.visibility = View.VISIBLE
                    menu.findViewById<VerticalStrokeTextView>(R.id.samsprung_logo)!!.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    menu.findViewById<AppCompatImageView>(R.id.button_recent)!!.setOnClickListener {
                        finish()
                        startActivity(
                            Intent(this@SamSprungOverlay, SamSprungDrawer::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    if (hasAccessibility()) {
                        menu.findViewById<ImageView>(R.id.button_screenshot)!!.setOnClickListener {
                            AccessibilityObserver.executeScreenshot()
                        }
                    } else {
                        menu.findViewById<ImageView>(R.id.button_screenshot)!!.visibility = View.GONE
                    }
                    menu.findViewById<AppCompatImageView>(R.id.button_recent)!!.setOnClickListener {
                        finish()
                        startActivity(
                            Intent(this@SamSprungOverlay, SamSprungDrawer::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    menu.findViewById<AppCompatImageView>(R.id.button_home)!!.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    menu.findViewById<AppCompatImageView>(R.id.button_back)!!.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (menu.isVisible) menu.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        findViewById<View>(R.id.coordinator)!!.setOnTouchListener(
            object: OnSwipeTouchListener(this@SamSprungOverlay) {
            override fun onSwipeTop() {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            override fun onSwipeBottom() {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    private fun hasAccessibility(): Boolean {
        val serviceString = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return serviceString != null && serviceString.contains(packageName
                + File.separator + AccessibilityObserver::class.java.name)
    }
}