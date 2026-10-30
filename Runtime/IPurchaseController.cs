using System;

namespace Superwall
{
    public interface IPurchaseController
    {
        void PurchaseFromAppStore(string productId, Action<PurchaseResult> completion);
        void PurchaseFromGooglePlay(string productId, string basePlanId, string offerId, Action<PurchaseResult> completion);
        void RestorePurchases(Action<RestorationResult> completion);

        /// <summary>
        /// Called when the user purchases a custom-store product (a product whose store is Custom in the
        /// Superwall dashboard, see <see cref="StoreProduct.IsCustomProduct"/>). Route the purchase through your
        /// own store or payment system and report its result. The SDK grants no entitlements for custom products:
        /// on success, update the subscription status yourself.
        ///
        /// Android only today. The default implementation fails the purchase, which is what happened before this
        /// method existed, so existing controllers keep compiling and behaving the same.
        /// </summary>
        void PurchaseFromCustomStore(string productId, Action<PurchaseResult> completion)
        {
            completion?.Invoke(PurchaseResult.Failed(
                "This purchase controller does not handle custom-store products. Implement " +
                "PurchaseFromCustomStore to purchase " + productId + "."));
        }
    }
}
