package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingRepository
import com.example.billing.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BillingRepositoryTest {

    @Test
    fun testInitialSubscriptionStateIsFree() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val billing = BillingRepository(context)

        // Réinitialise pour le test
        billing.deactivatePremium()

        assertEquals(SubscriptionTier.FREE, billing.subscriptionTier.value)
        assertFalse(billing.isPremium())
    }

    @Test
    fun testActivatePremiumUpdatesStateAndPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val billing = BillingRepository(context)

        billing.activatePremium()

        assertEquals(SubscriptionTier.PREMIUM, billing.subscriptionTier.value)
        assertTrue(billing.isPremium())

        // Nouvelle instance pour vérifier la persistance SharedPreferences
        val newBillingInstance = BillingRepository(context)
        assertTrue(newBillingInstance.isPremium())
    }

    @Test
    fun testPricingPlansAreConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val billing = BillingRepository(context)

        val plans = billing.availablePlans
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.any { it.isBestValue && it.trialPeriodDays > 0 })
        assertTrue(plans.any { it.productId == "netward_lifetime" })
    }
}
