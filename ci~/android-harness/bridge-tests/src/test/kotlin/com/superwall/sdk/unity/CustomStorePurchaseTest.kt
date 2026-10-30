package com.superwall.sdk.unity

import android.app.Activity
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.store.abstractions.product.ApiStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.models.SuperwallProductPrice
import com.unity3d.player.UnityPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Custom-store products (store == CUSTOM, superwall-android 2.8+) have no Google Play `ProductDetails`, so the SDK
 * calls `purchase(activity, product: StoreProduct, ...)`. The bridge must send them to C# as
 * `purchaseFromCustomStore`; before, it overrode only the ProductDetails variant and the SDK's default failed every
 * custom purchase without C# ever hearing of it.
 */
class CustomStorePurchaseTest {
    private val controller = SuperwallUnityBridge.UnityPurchaseController()

    @Before
    fun clearMessages() = UnityPlayer.sentMessages.clear()

    private fun apiProduct(id: String, platform: SuperwallProductPlatform) = StoreProduct.custom(
        ApiStoreProduct(
            SuperwallProduct(identifier = id, platform = platform, price = SuperwallProductPrice(amount = 999, currency = "USD")),
        ),
    )

    private fun sentCallbacks(): List<JSONObject> =
        UnityPlayer.sentMessages.toList().map { JSONObject(it[2]) }

    private suspend fun awaitRequest(): JSONObject {
        while (true) {
            sentCallbacks().firstOrNull()?.let { return it }
            delay(5)
        }
    }

    @Test
    fun customProductIsSentToCSharpAsPurchaseFromCustomStoreAndTheReplyIsReturned() = runBlocking {
        val purchase = async(Dispatchers.Default) {
            controller.purchase(Activity(), apiProduct("pro.yearly", SuperwallProductPlatform.CUSTOM), null, null)
        }

        val request = withTimeout(5_000) { awaitRequest() }
        assertEquals("purchaseFromCustomStore", request.getString("method"))
        val data = request.getJSONObject("data")
        assertEquals("pro.yearly", data.getString("productId"))
        val callbackId = data.getString("callbackId")
        assertTrue(controller.isPending(callbackId))

        controller.completePurchase(callbackId, PurchaseResult.Purchased())

        assertTrue(withTimeout(5_000) { purchase.await() } is PurchaseResult.Purchased)
        assertFalse(controller.isPending(callbackId))
    }

    @Test
    fun anApiProductThatIsNotCustomKeepsTheSdkDefaultAndNeverReachesCSharp() = runBlocking {
        // A test-mode product: API-backed like a custom one, but its platform is not CUSTOM and it has no
        // ProductDetails. The SDK's default answer (a failure) is unchanged and nothing is sent to C#.
        val result = controller.purchase(Activity(), apiProduct("pro.monthly", SuperwallProductPlatform.SUPERWALL), null, null)

        assertTrue(result is PurchaseResult.Failed)
        assertTrue(sentCallbacks().isEmpty())
    }

    @Test
    fun customStoreEntitlementsAreSerializedAsCustom() {
        val method = SuperwallUnityBridge::class.java.getDeclaredMethod("serializeEntitlement", Entitlement::class.java)
        method.isAccessible = true
        val json = method.invoke(SuperwallUnityBridge(), Entitlement(id = "premium", store = Store.CUSTOM)) as JSONObject

        assertEquals("custom", json.getString("store"))
    }

    @Test
    fun storeProductSerializationCarriesIsCustomProduct() {
        val method = SuperwallUnityBridge::class.java.getDeclaredMethod("serializeStoreProduct", StoreProduct::class.java)
        method.isAccessible = true
        val bridge = SuperwallUnityBridge()

        val custom = method.invoke(bridge, apiProduct("pro.yearly", SuperwallProductPlatform.CUSTOM)) as JSONObject
        val notCustom = method.invoke(bridge, apiProduct("pro.monthly", SuperwallProductPlatform.SUPERWALL)) as JSONObject

        assertTrue(custom.getBoolean("isCustomProduct"))
        assertFalse(notCustom.getBoolean("isCustomProduct"))
    }
}
