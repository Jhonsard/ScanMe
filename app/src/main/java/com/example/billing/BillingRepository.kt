package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubscriptionTier {
    FREE,
    PREMIUM
}

data class PricingPlan(
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val description: String,
    val isBestValue: Boolean = false,
    val trialPeriodDays: Int = 0
)

/**
 * Gestionnaire d'abonnement et de monétisation Play Billing pour NetWard Pro.
 */
class BillingRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _subscriptionTier = MutableStateFlow(
        if (prefs.getBoolean(KEY_IS_PREMIUM, false)) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
    )
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

    val availablePlans = listOf(
        PricingPlan(
            productId = "netward_annual_sub",
            title = "Annuel (Essai gratuit)",
            formattedPrice = "19,99 € / an",
            description = "Soit 1,66 €/mois • 3 jours d'essai gratuit offerts",
            isBestValue = true,
            trialPeriodDays = 3
        ),
        PricingPlan(
            productId = "netward_monthly_sub",
            title = "Mensuel",
            formattedPrice = "2,99 € / mois",
            description = "Sans engagement, résiliable à tout moment",
            isBestValue = false
        ),
        PricingPlan(
            productId = "netward_lifetime",
            title = "Licence À Vie",
            formattedPrice = "39,99 €",
            description = "Paiement unique pour un accès permanent",
            isBestValue = false
        )
    )

    fun isPremium(): Boolean {
        return _subscriptionTier.value == SubscriptionTier.PREMIUM
    }

    /**
     * Active le statut Premium (simulé ou validé via Google Play Billing)
     */
    fun activatePremium() {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, true).apply()
        _subscriptionTier.value = SubscriptionTier.PREMIUM
    }

    /**
     * Rétablit le statut gratuit (ex: expiration de l'abonnement ou tests)
     */
    fun deactivatePremium() {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, false).apply()
        _subscriptionTier.value = SubscriptionTier.FREE
    }

    companion object {
        private const val PREFS_NAME = "netward_billing_prefs"
        private const val KEY_IS_PREMIUM = "is_premium_active"
    }
}
