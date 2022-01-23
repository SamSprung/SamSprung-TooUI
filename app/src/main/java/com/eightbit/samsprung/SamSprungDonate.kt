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

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import java.util.*
import java.util.concurrent.Executors

class SamSprungDonate : AppCompatActivity() {

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        // setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        // ScaledContext.wrap(this).setTheme(R.style.Theme_SecondScreen)
        supportActionBar?.hide()
        setContentView(R.layout.donation_layout)

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener).enablePendingPurchases().build()

        Executors.newSingleThreadExecutor().execute {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val iapList = ArrayList<String>()
                        iapList.add(getIAP(1))
                        iapList.add(getIAP(5))
                        iapList.add(getIAP(10))
                        iapList.add(getIAP(25))
                        iapList.add(getIAP(50))
                        billingClient.querySkuDetailsAsync(
                            SkuDetailsParams.newBuilder()
                                .setSkusList(iapList)
                                .setType(BillingClient.SkuType.INAPP)
                                .build(), iapResponseListener
                        )

                        val subList = ArrayList<String>()
                        subList.add(getSub(1))
                        subList.add(getSub(5))
                        subList.add(getSub(10))
                        subList.add(getSub(25))
                        subList.add(getSub(50))
                        billingClient.querySkuDetailsAsync(
                            SkuDetailsParams.newBuilder()
                                .setSkusList(subList)
                                .setType(BillingClient.SkuType.SUBS)
                                .build(), subResponseListener
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }

    private fun getIAP(amount: Int) : String {
        return String.format("subscription_%d", amount)
    }

    private fun getSub(amount: Int) : String {
        return String.format("monthly_%d", amount)
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
                TODO()
        }
    }

    private val iapResponseListener = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            val layout: LinearLayout = findViewById(R.id.donation_layout)
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                val button = Button(this)
                button.text = skuDetail.title
                button.setOnClickListener {
                    billingClient.launchBillingFlow(this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build())
                }
                runOnUiThread { layout.addView(button) }
            }
        }
    }

    private val subResponseListener = SkuDetailsResponseListener { _, skuDetails ->
        if (null != skuDetails) {
            val layout: LinearLayout = findViewById(R.id.subscription_layout)
            for (skuDetail: SkuDetails in skuDetails.sortedBy { skuDetail -> skuDetail.sku }) {
                val button = Button(this)
                button.text = skuDetail.title
                button.setOnClickListener {
                    billingClient.launchBillingFlow(this,
                        BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build())
                }
                runOnUiThread { layout.addView(button) }
            }
        }
    }

    private var acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { TODO() }

    private fun handlePurchase(purchase : Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                val ackPurchaseResult = billingClient.acknowledgePurchase(
                    acknowledgePurchaseParams.build(),
                    acknowledgePurchaseResponseListener)
            }
        }
    }
}