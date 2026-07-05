package bpm.munkz.pulse_wear.os.bpm.presentation

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

internal class BillingUnlockCoordinator(
    context: Context,
    private val productIds: Set<String>,
    private val onProductOwned: (String) -> Unit,
    private val consumableProductIds: Set<String> = emptySet(),
    private val onProductConsumed: (String) -> Unit = onProductOwned,
) {
    private val appContext = context.applicationContext
    private val productDetailsById = mutableMapOf<String, ProductDetails>()

    var connected by mutableStateOf(false)
        private set
    var statusText by mutableStateOf("Connecting to Play")
        private set

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    statusText = "Purchase complete"
                } else {
                    processPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                statusText = "Purchase canceled"
            }
            else -> {
                statusText = "Play purchase error ${billingResult.responseCode}"
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            connected = true
            queryProducts()
            queryPurchases()
            return
        }

        statusText = "Connecting to Play"
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (connected) {
                        statusText = "Checking purchases"
                        queryProducts()
                        queryPurchases()
                    } else {
                        statusText = "Play unavailable ${billingResult.responseCode}"
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connected = false
                    statusText = "Play disconnected"
                }
            },
        )
    }

    fun stop() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun canBuy(productId: String): Boolean {
        return connected && productDetailsById.containsKey(productId)
    }

    fun buy(activity: Activity?, productId: String) {
        val hostActivity = activity
        if (hostActivity == null) {
            statusText = "Open app on watch"
            return
        }

        val productDetails = productDetailsById[productId]
        if (productDetails == null) {
            statusText = "Product not ready"
            queryProducts()
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let { offerToken ->
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }
        val productDetailsParams = productDetailsParamsBuilder.build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        val result = billingClient.launchBillingFlow(hostActivity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            statusText = "Play launch error ${result.responseCode}"
        }
    }

    private fun queryProducts() {
        if (!billingClient.isReady || productIds.isEmpty()) return

        val products = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsById.clear()
                productDetailsResult.productDetailsList.forEach { productDetails ->
                    productDetailsById[productDetails.productId] = productDetails
                }
                statusText = if (productDetailsById.isEmpty()) {
                    "Product not in Play Console"
                } else {
                    "Ready"
                }
            } else {
                statusText = "Product error ${billingResult.responseCode}"
            }
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            val ownedProductId = purchase.products.firstOrNull { productId -> productId in productIds }
                ?: return@forEach
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (ownedProductId in consumableProductIds) {
                        consumeOrComplete(purchase, ownedProductId)
                    } else {
                        acknowledgeOrUnlock(purchase, ownedProductId)
                    }
                }
                Purchase.PurchaseState.PENDING -> statusText = "Purchase pending"
                else -> Unit
            }
        }
    }

    private fun consumeOrComplete(purchase: Purchase, productId: String) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onProductConsumed(productId)
                statusText = "Thanks"
            } else {
                statusText = "Consume error ${billingResult.responseCode}"
            }
        }
    }

    private fun acknowledgeOrUnlock(purchase: Purchase, productId: String) {
        if (purchase.isAcknowledged) {
            onProductOwned(productId)
            statusText = "Unlocked"
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onProductOwned(productId)
                statusText = "Unlocked"
            } else {
                statusText = "Acknowledge error ${billingResult.responseCode}"
            }
        }
    }
}
