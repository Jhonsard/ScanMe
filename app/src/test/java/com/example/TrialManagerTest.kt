package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.TrialManager
import com.example.billing.TrialStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrialManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val trialManager = TrialManager(context)
        trialManager.resetTrialForTesting()
    }

    @Test
    fun testInitialTrialStateIsActiveWith30Days() {
        val now = 1_700_000_000_000L
        val trialManager = TrialManager(context) { now }
        trialManager.resetTrialForTesting()

        val state = trialManager.refreshTrialState()
        assertEquals(TrialStatus.ACTIVE, state.status)
        assertEquals(30, state.daysRemaining)
        assertFalse(state.isTampered)
        assertTrue(state.isScanAllowed(isPremium = false))
    }

    @Test
    fun testTrialStateAfter10DaysIsActiveWith20Days() {
        var simulatedNow = 1_700_000_000_000L
        val trialManager = TrialManager(context) { simulatedNow }
        trialManager.resetTrialForTesting()

        // Avance de 10 jours
        simulatedNow += TimeUnit.DAYS.toMillis(10)
        val state = trialManager.refreshTrialState()

        assertEquals(TrialStatus.ACTIVE, state.status)
        assertEquals(20, state.daysRemaining)
        assertTrue(state.isScanAllowed(isPremium = false))
    }

    @Test
    fun testTrialStateAfter28DaysIsExpiringSoonWith2Days() {
        var simulatedNow = 1_700_000_000_000L
        val trialManager = TrialManager(context) { simulatedNow }
        trialManager.resetTrialForTesting()

        // Avance de 28 jours
        simulatedNow += TimeUnit.DAYS.toMillis(28)
        val state = trialManager.refreshTrialState()

        assertEquals(TrialStatus.EXPIRING_SOON, state.status)
        assertEquals(2, state.daysRemaining)
        assertTrue(state.isScanAllowed(isPremium = false))
    }

    @Test
    fun testTrialStateAfter31DaysIsExpired() {
        var simulatedNow = 1_700_000_000_000L
        val trialManager = TrialManager(context) { simulatedNow }
        trialManager.resetTrialForTesting()

        // Avance de 31 jours
        simulatedNow += TimeUnit.DAYS.toMillis(31)
        val state = trialManager.refreshTrialState()

        assertEquals(TrialStatus.EXPIRED, state.status)
        assertEquals(0, state.daysRemaining)
        // Scan bloqué pour utilisateur gratuit
        assertFalse(state.isScanAllowed(isPremium = false))
        // Scan autorisé si l'utilisateur est Premium
        assertTrue(state.isScanAllowed(isPremium = true))
    }

    @Test
    fun testClockTamperingIsDetectedAndBlocksScan() {
        var simulatedNow = 1_700_000_000_000L
        val trialManager = TrialManager(context) { simulatedNow }
        trialManager.resetTrialForTesting()

        // Le temps avance normalement à J+5
        simulatedNow += TimeUnit.DAYS.toMillis(5)
        trialManager.refreshTrialState()

        // L'utilisateur tente de reculer artificiellement son horloge de 10 jours
        simulatedNow -= TimeUnit.DAYS.toMillis(10)
        val tamperedState = trialManager.refreshTrialState()

        assertTrue(tamperedState.isTampered)
        assertEquals(TrialStatus.EXPIRED, tamperedState.status)
        assertFalse(tamperedState.isScanAllowed(isPremium = false))
    }
}
