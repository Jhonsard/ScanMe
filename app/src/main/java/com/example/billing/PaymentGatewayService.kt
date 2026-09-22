package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Modèle de transaction bancaire validée et enregistrée.
 */
data class PaymentTransaction(
    val transactionId: String,
    val planId: String,
    val amountFormatted: String,
    val cardLast4: String,
    val cardBrand: CardBrand,
    val cardholderName: String,
    val timestamp: Long,
    val licenseKey: String,
    val isSuccess: Boolean
)

/**
 * Service de traitement des paiements internationaux par carte bancaire
 * avec génération de licence et persistance pour le panneau d'administration.
 */
class PaymentGatewayService(
    context: Context,
    private val billingRepository: BillingRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_TRANSACTIONS, Context.MODE_PRIVATE)

    /**
     * Traite un paiement par carte bancaire pour le forfait sélectionné.
     */
    suspend fun processCardPayment(
        card: PaymentCard,
        plan: PricingPlan
    ): Result<PaymentTransaction> = withContext(Dispatchers.IO) {
        val validation = card.validate()
        if (validation is CardValidationResult.Invalid) {
            return@withContext Result.failure(IllegalArgumentException(validation.reason))
        }

        // Simulation d'appel à la passerelle de paiement internationale (Stripe) et 3D Secure
        delay(800)

        val transactionId = "TXN-" + UUID.randomUUID().toString().take(12).uppercase()
        val licenseKey = "NW-PRO-" + UUID.randomUUID().toString().take(8).uppercase() + "-" + (1000..9999).random()

        val transaction = PaymentTransaction(
            transactionId = transactionId,
            planId = plan.productId,
            amountFormatted = plan.formattedPrice,
            cardLast4 = card.last4,
            cardBrand = card.brand,
            cardholderName = card.cardholderName,
            timestamp = System.currentTimeMillis(),
            licenseKey = licenseKey,
            isSuccess = true
        )

        // Sauvegarde locale de la transaction et mise à jour des droits Premium
        saveTransaction(transaction)
        billingRepository.activatePremium()

        Result.success(transaction)
    }

    private fun saveTransaction(txn: PaymentTransaction) {
        prefs.edit()
            .putString(KEY_LAST_TXN_ID, txn.transactionId)
            .putString(KEY_LAST_LICENSE, txn.licenseKey)
            .putString(KEY_LAST_PLAN, txn.planId)
            .putLong(KEY_LAST_TIMESTAMP, txn.timestamp)
            .apply()
    }

    fun getActiveLicenseKey(): String? {
        return prefs.getString(KEY_LAST_LICENSE, null)
    }

    companion object {
        private const val PREFS_TRANSACTIONS = "netward_transactions_prefs"
        private const val KEY_LAST_TXN_ID = "last_transaction_id"
        private const val KEY_LAST_LICENSE = "last_license_key"
        private const val KEY_LAST_PLAN = "last_plan_id"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    }
}
