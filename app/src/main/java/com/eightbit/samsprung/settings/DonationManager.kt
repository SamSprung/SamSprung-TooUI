package com.eightbit.samsprung.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.billingclient.api.*
import com.eightbit.material.IconifiedSnackbar
import com.eightbit.samsprung.BuildConfig
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DonationManager internal constructor(private val activity: CoverPreferences) {

    private val Number.toPx get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        Resources.getSystem().displayMetrics
    )

    private lateinit var billingClient: BillingClient
    private val iapSkuDetails = ArrayList<ProductDetails>()
    private val subSkuDetails = ArrayList<ProductDetails>()

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    private fun getIAP(amount: Int) : String {
        return String.format("subscription_%02d", amount)
    }

    private fun getSub(amount: Int) : String {
        return String.format("monthly_%02d", amount)
    }

    private val iapList = ArrayList<String>()
    private val subList = ArrayList<String>()

    private val consumeResponseListener = ConsumeResponseListener { _, _ ->
        IconifiedSnackbar(activity).buildTickerBar(
            activity.getString(R.string.donation_thanks)
        ).show()
    }

    private fun handlePurchaseIAP(purchase : Purchase) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
        billingClient.consumeAsync(consumeParams.build(), consumeResponseListener)
    }

    private var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener {
        IconifiedSnackbar(activity).buildTickerBar(activity.getString(R.string.donation_thanks)).show()
        SamSprung.hasSubscription = true
    }

    private fun handlePurchaseSub(purchase : Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build(),
            acknowledgePurchaseResponseListener)
    }

    private fun handlePurchase(purchase : Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                for (iap: String in iapList) {
                    if (purchase.products.contains(iap))
                        handlePurchaseIAP(purchase)
                }
                for (sub: String in subList) {
                    if (purchase.products.contains(sub))
                        handlePurchaseSub(purchase)
                }
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private lateinit var subsPurchased: ArrayList<String>

    private val subsOwnedListener = PurchasesResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in purchases) {
                for (sku in purchase.products) {
                    if (subsPurchased.contains(sku)) {
                        SamSprung.hasSubscription = true
                        break
                    }
                }
            }
        }
    }

    private val subHistoryListener = PurchaseHistoryResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases)
                subsPurchased.addAll(purchase.products)
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS).build(), subsOwnedListener)
        }
    }

    private val iapHistoryListener = PurchaseHistoryResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && null != purchases) {
            for (purchase in purchases) {
                for (sku in purchase.products) {
                    if (sku.split("_")[1].toInt() >= 10) {
                        break
                    }
                }
            }
        }
    }

    private val billingConnectionMutex = Mutex()

    private val resultAlreadyConnected = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .setDebugMessage("Billing client is already connected")
        .build()

    /**
     * Returns immediately if this BillingClient is already connected, otherwise
     * initiates the connection and suspends until this client is connected.
     * If a connection is already in the process of being established, this
     * method just suspends until the billing client is ready.
     */
    private suspend fun BillingClient.connect(): BillingResult = billingConnectionMutex.withLock {
        if (isReady) {
            // fast path: avoid suspension if already connected
            resultAlreadyConnected
        } else {
            unsafeConnect()
        }
    }

    private suspend fun BillingClient.unsafeConnect() = suspendCoroutine { cont ->
        startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                cont.resume(billingResult)
            }
            override fun onBillingServiceDisconnected() {
                // no need to setup reconnection logic here, call ensureReady()
                // before each purchase to reconnect as necessary
            }
        })
    }

    fun retrieveDonationMenu() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener).enablePendingPurchases().build()

        iapSkuDetails.clear()
        subSkuDetails.clear()

        backgroundScope.launch(Dispatchers.IO) {
            val clientResponseCode = billingClient.connect().responseCode
            if (clientResponseCode == BillingClient.BillingResponseCode.OK) {
                iapList.add(getIAP(1))
                iapList.add(getIAP(5))
                iapList.add(getIAP(10))
                iapList.add(getIAP(25))
                iapList.add(getIAP(50))
                iapList.add(getIAP(75))
                iapList.add(getIAP(99))
                iapList.forEach {
                    val productList = QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP).build()
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(listOf(productList))
                    billingClient.queryProductDetailsAsync(params.build()) { _, productDetailsList ->
                        iapSkuDetails.addAll(productDetailsList)
                        billingClient.queryPurchaseHistoryAsync(
                            QueryPurchaseHistoryParams.newBuilder().setProductType(
                                BillingClient.ProductType.INAPP
                            ).build(), iapHistoryListener
                        )
                    }
                }
                if (BuildConfig.GOOGLE_PLAY) return@launch
                subList.add(getSub(1))
                subList.add(getSub(5))
                subList.add(getSub(10))
                subList.add(getSub(25))
                subList.add(getSub(50))
                subList.add(getSub(75))
                subList.add(getSub(99))
                subList.forEach {
                    val productList = QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS).build()
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(listOf(productList))
                    billingClient.queryProductDetailsAsync(params.build()) { _, productDetailsList ->
                        subSkuDetails.addAll(productDetailsList)
                        billingClient.queryPurchaseHistoryAsync(
                            QueryPurchaseHistoryParams.newBuilder().setProductType(
                                BillingClient.ProductType.SUBS
                            ).build(), subHistoryListener
                        )
                    }
                }
            }
        }
    }

    private fun getDonationButton(skuDetail: ProductDetails): Button {
        val button = Button(activity.applicationContext)
        button.setBackgroundResource(R.drawable.button_rippled)
        button.elevation = 10f.toPx
        button.setTextColor(ContextCompat.getColor(activity, android.R.color.black))
        button.textSize = 8f.toPx
        button.text = activity.getString(
            R.string.iap_button, skuDetail.oneTimePurchaseOfferDetails!!.formattedPrice
        )
        button.setOnClickListener {
            val productDetailsParamsList = BillingFlowParams.ProductDetailsParams
                .newBuilder().setProductDetails(skuDetail).build()
            billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsList)).build()
            )
        }
        return button
    }

    private fun getSubscriptionButton(skuDetail: ProductDetails): Button {
        val button = Button(activity.applicationContext)
        button.setBackgroundResource(R.drawable.button_rippled)
        button.elevation = 10f.toPx
        button.setTextColor(ContextCompat.getColor(activity, android.R.color.black))
        button.textSize = 8f.toPx
        button.text = activity.getString(
            R.string.sub_button,
            skuDetail.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice
        )
        button.setOnClickListener {
            val productDetailsParamsList = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setOfferToken(skuDetail.subscriptionOfferDetails!![0]!!.offerToken)
                .setProductDetails(skuDetail).build()
            billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsList)).build()
            )
        }
        return button
    }

    fun onSendDonationClicked() {
        val view: LinearLayout = activity.layoutInflater
            .inflate(R.layout.donation_layout, null) as LinearLayout
        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(activity, R.style.Theme_Overlay_NoActionBar)
        )
        val donations = view.findViewById<LinearLayout>(R.id.donation_layout)
        donations.removeAllViewsInLayout()
        for (skuDetail: ProductDetails in iapSkuDetails
            .sortedBy { skuDetail -> skuDetail.productId }) {
            if (null == skuDetail.oneTimePurchaseOfferDetails) continue
            donations.addView(getDonationButton(skuDetail))
        }
        val subscriptions = view.findViewById<LinearLayout>(R.id.subscription_layout)
        if (BuildConfig.GOOGLE_PLAY) {
            subscriptions.isVisible = false
        } else {
            subscriptions.isVisible = true
            subscriptions.removeAllViewsInLayout()
            for (skuDetail: ProductDetails in subSkuDetails
                .sortedBy { skuDetail -> skuDetail.productId }) {
                if (null == skuDetail.subscriptionOfferDetails) continue
                subscriptions.addView(getSubscriptionButton(skuDetail))
            }
        }
        dialog.setOnCancelListener {
            donations.removeAllViewsInLayout()
            if (!BuildConfig.GOOGLE_PLAY) subscriptions.removeAllViewsInLayout()
        }
        dialog.setOnDismissListener {
            donations.removeAllViewsInLayout()
            if (!BuildConfig.GOOGLE_PLAY) subscriptions.removeAllViewsInLayout()
        }
        val donateDialog: Dialog = dialog.setView(view).show()

        if (SamSprung.hasSubscription) {
            val padding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                Resources.getSystem().displayMetrics
            ).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, padding, 0, padding)

            @SuppressLint("InflateParams") val manage =
                activity.layoutInflater.inflate(R.layout.button_cancel_sub, null)
            manage.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://support.google.com/googleplay/workflow/9827184"
                )))
                donateDialog.cancel()
            }
            manage.layoutParams = params
            view.addView(manage)
        }

        if (!BuildConfig.GOOGLE_PLAY) {
            @SuppressLint("InflateParams")
            val sponsor: View = activity.layoutInflater.inflate(R.layout.button_sponsor, null)
            sponsor.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/sponsors/AbandonedCart"
                )))
                donateDialog.cancel()
            }
            view.addView(sponsor)

            @SuppressLint("InflateParams")
            val paypal: View = activity.layoutInflater.inflate(R.layout.button_paypal, null)
            paypal.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.paypal.com/donate/?hosted_button_id=Q2LFH2SC8RHRN"
                )))
                donateDialog.cancel()
            }
            view.addView(paypal)
        }
        donateDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}