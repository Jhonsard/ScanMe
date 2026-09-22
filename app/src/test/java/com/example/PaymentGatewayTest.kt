package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingRepository
import com.example.billing.CardBrand
import com.example.billing.CardValidationResult
import com.example.billing.PaymentCard
import com.example.billing.PaymentGatewayService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentGatewayTest {

    private lateinit var context: Context
    private lateinit var billingRepository: BillingRepository
    private lateinit var gatewayService: PaymentGatewayService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        billingRepository = BillingRepository(context)
        billingRepository.deactivatePremium()
        gatewayService = PaymentGatewayService(context, billingRepository)
    }

    @Test
    fun testDetectCardBrands() {
        assertEquals(CardBrand.VISA, PaymentCard.detectBrand("4111111111111111"))
        assertEquals(CardBrand.MASTERCARD, PaymentCard.detectBrand("5105105105105100"))
        assertEquals(CardBrand.MASTERCARD, PaymentCard.detectBrand("2221000000000000"))
        assertEquals(CardBrand.AMERICAN_EXPRESS, PaymentCard.detectBrand("378282246310005"))
        assertEquals(CardBrand.UNKNOWN, PaymentCard.detectBrand("6011000000000000"))
    }

    @Test
    fun testLuhnAlgorithmValidation() {
        assertTrue(PaymentCard.validateLuhn("49927398716"))
        assertTrue(PaymentCard.validateLuhn("4111111111111111"))
        assertTrue(!PaymentCard.validateLuhn("4111111111111112"))
    }

    @Test
    fun testValidateCardWithValidVisa() {
        val card = PaymentCard(
            rawNumber = "4111 1111 1111 1111",
            cardholderName = "Jean Dupont",
            expiryMonth = 12,
            expiryYear = 2028,
            cvc = "123"
        )
        val result = card.validate()
        assertTrue(result is CardValidationResult.Valid)
        assertEquals("1111", card.last4)
        assertEquals(CardBrand.VISA, card.brand)
    }

    @Test
    fun testValidateCardWithInvalidCvc() {
        val card = PaymentCard(
            rawNumber = "4111 1111 1111 1111",
            cardholderName = "Jean Dupont",
            expiryMonth = 12,
            expiryYear = 2028,
            cvc = "12"
        )
        val result = card.validate()
        assertTrue(result is CardValidationResult.Invalid)
        assertEquals("Code de sécurité CVC invalide (3 chiffres requis)", (result as CardValidationResult.Invalid).reason)
    }

    @Test
    fun testProcessCardPaymentSuccessAndActivatesPremium() = runBlocking {
        val card = PaymentCard(
            rawNumber = "4111 1111 1111 1111",
            cardholderName = "Jean Dupont",
            expiryMonth = 12,
            expiryYear = 2029,
            cvc = "123"
        )
        val plan = billingRepository.availablePlans.first { it.productId == "netward_annual_sub" }

        val result = gatewayService.processCardPayment(card, plan)
        assertTrue(result.isSuccess)

        val txn = result.getOrNull()!!
        assertTrue(txn.isSuccess)
        assertTrue(txn.transactionId.startsWith("TXN-"))
        assertTrue(txn.licenseKey.startsWith("NW-PRO-"))
        assertEquals("1111", txn.cardLast4)
        assertEquals(CardBrand.VISA, txn.cardBrand)

        // Statut Premium actif
        assertTrue(billingRepository.isPremium())
        assertNotNull(gatewayService.getActiveLicenseKey())
    }
}
