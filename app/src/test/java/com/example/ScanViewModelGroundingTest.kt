package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiSearchGroundingService
import com.example.ai.GroundingAuditResult
import com.example.ai.GroundingWebSource
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import com.example.network.IspInfo
import com.example.ui.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanViewModelGroundingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun auditDeviceWithGoogleSearch_updatesUiStateWithGroundedResult() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val fakeService = GeminiSearchGroundingService(
            apiKeyProvider = { "fake_key" },
            ioDispatcher = testDispatcher,
            httpExecutor = { _, _ ->
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Rapport de failles de sécurité Dahua : Firmware vulnérable CVE-2021-33044." }
                        ]
                      },
                      "groundingMetadata": {
                        "webSearchQueries": ["Dahua security bulletin 2025"],
                        "groundingChunks": [
                          {
                            "web": {
                              "uri": "https://example.com/advisory",
                              "title": "Advisory Dahua"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent()
            }
        )

        val viewModel = ScanViewModel(
            application = app,
            customGeminiGroundingService = fakeService
        )

        val device = NetworkDevice(
            ipAddress = "192.168.1.50",
            macAddress = "AA:BB:CC:DD:EE:FF",
            vendorName = "Dahua Technology",
            category = DeviceCategory.IP_CAMERA,
            threatLevel = ThreatLevel.SUSPICIOUS,
            openPorts = listOf(554)
        )

        viewModel.auditDeviceWithGoogleSearch(device)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("La boîte de dialogue d'audit doit être affichée", state.showGroundingDialog)
        assertFalse("Le chargement doit être terminé", state.isGroundingLoading)
        assertNotNull("Les résultats d'audit doivent être présents", state.groundingResult)
        assertEquals(1, state.groundingResult?.searchQueries?.size)
        assertEquals("Dahua security bulletin 2025", state.groundingResult?.searchQueries?.first())
        assertEquals(1, state.groundingResult?.sources?.size)
        assertEquals("https://example.com/advisory", state.groundingResult?.sources?.first()?.url)

        viewModel.closeGroundingDialog()
        assertFalse(viewModel.uiState.value.showGroundingDialog)
    }

    @Test
    fun auditIspWithGoogleSearch_updatesUiStateWithGroundedResult() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val fakeService = GeminiSearchGroundingService(
            apiKeyProvider = { "fake_key" },
            ioDispatcher = testDispatcher,
            httpExecutor = { _, _ ->
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Statut FAI : Aucune coupure majeure BGP détectée." }
                        ]
                      },
                      "groundingMetadata": {
                        "webSearchQueries": ["Orange outage report"],
                        "groundingChunks": [
                          {
                            "web": {
                              "uri": "https://example.com/outage",
                              "title": "Outage monitor"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent()
            }
        )

        val viewModel = ScanViewModel(
            application = app,
            customGeminiGroundingService = fakeService
        )

        val isp = IspInfo(
            publicIp = "90.10.20.30",
            ispName = "Orange",
            asn = "AS3215"
        )

        viewModel.auditIspWithGoogleSearch(isp)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showGroundingDialog)
        assertFalse(state.isGroundingLoading)
        assertNotNull(state.groundingResult)
        assertTrue(state.groundingResult?.summary?.contains("Aucune coupure") == true)
        assertEquals("Orange outage report", state.groundingResult?.searchQueries?.first())
    }
}
