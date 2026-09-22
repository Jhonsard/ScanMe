package com.example

import com.example.ai.GeminiSearchGroundingService
import com.example.ai.GroundingAuditResult
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import com.example.network.IspInfo
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiSearchGroundingServiceTest {

    private val sampleMockGeminiResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "Audit de vulnérabilité : L'équipement Dahua Camera présente le port 554 RTSP exposé. Des failles CVE-2021-33044 ont été répertoriées dans le passé permettant un contournement d'authentification. Recommandations : 1. Mettre à jour le firmware. 2. Isoler sur un VLAN IoT. 3. Désactiver RTSP si non utilisé."
                  }
                ],
                "role": "model"
              },
              "finishReason": "STOP",
              "groundingMetadata": {
                "webSearchQueries": [
                  "Dahua IP camera port 554 CVE security vulnerability",
                  "Dahua firmware exploit advisory 2025"
                ],
                "groundingChunks": [
                  {
                    "web": {
                      "uri": "https://nvd.nist.gov/vuln/detail/CVE-2021-33044",
                      "title": "NVD - CVE-2021-33044 Detail"
                    }
                  },
                  {
                    "web": {
                      "uri": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
                      "title": "CISA Known Exploited Vulnerabilities"
                    }
                  }
                ]
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parseGroundingResponse_extractsSummaryQueriesAndSources() {
        val service = GeminiSearchGroundingService(apiKeyProvider = { "test_key" })
        val result = service.parseGroundingResponse("test prompt", sampleMockGeminiResponse)

        assertTrue(result.summary.contains("Dahua Camera"))
        assertTrue(result.summary.contains("CVE-2021-33044"))

        assertEquals(2, result.searchQueries.size)
        assertEquals("Dahua IP camera port 554 CVE security vulnerability", result.searchQueries[0])
        assertEquals("Dahua firmware exploit advisory 2025", result.searchQueries[1])

        assertEquals(2, result.sources.size)
        assertEquals("https://nvd.nist.gov/vuln/detail/CVE-2021-33044", result.sources[0].url)
        assertEquals("NVD - CVE-2021-33044 Detail", result.sources[0].title)
        assertEquals("https://www.cisa.gov/known-exploited-vulnerabilities-catalog", result.sources[1].url)
        assertEquals("CISA Known Exploited Vulnerabilities", result.sources[1].title)
    }

    @Test
    fun executeGroundingQuery_sendsGoogleSearchToolAndGemini35FlashModel() = runTest {
        var capturedUrl = ""
        var capturedBody = ""

        val service = GeminiSearchGroundingService(
            apiKeyProvider = { "AIzaSyTest123" },
            ioDispatcher = StandardTestDispatcher(testScheduler),
            httpExecutor = { url, jsonBody ->
                capturedUrl = url
                capturedBody = jsonBody
                sampleMockGeminiResponse
            }
        )

        val device = NetworkDevice(
            ipAddress = "192.168.1.102",
            macAddress = "E0:50:8B:11:22:33",
            vendorName = "Dahua Technology",
            category = DeviceCategory.IP_CAMERA,
            threatLevel = ThreatLevel.SUSPICIOUS,
            openPorts = listOf(80, 554)
        )

        val result = service.auditDevice(device)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)

        // Vérification de l'URL contenant gemini-3.5-flash
        assertTrue("L'URL doit cibler gemini-3.5-flash", capturedUrl.contains("models/gemini-3.5-flash:generateContent"))
        assertTrue("L'URL doit contenir la clé API", capturedUrl.contains("key=AIzaSyTest123"))

        // Vérification du corps de requête contenant l'outil googleSearch
        val jsonRequest = JSONObject(capturedBody)
        val tools = jsonRequest.getJSONArray("tools")
        assertEquals(1, tools.length())
        assertTrue("L'outil googleSearch doit être configuré", tools.getJSONObject(0).has("googleSearch"))
    }

    @Test
    fun auditIsp_executesSearchGroundingForNetworkOperator() = runTest {
        var capturedPrompt = ""

        val service = GeminiSearchGroundingService(
            apiKeyProvider = { "AIzaSyTest123" },
            ioDispatcher = StandardTestDispatcher(testScheduler),
            httpExecutor = { _, jsonBody ->
                capturedPrompt = jsonBody
                sampleMockGeminiResponse
            }
        )

        val isp = IspInfo(
            publicIp = "82.65.12.34",
            ispName = "Free SAS",
            organization = "Free SAS",
            asn = "AS12322",
            city = "Paris",
            country = "France"
        )

        val result = service.auditIsp(isp)
        assertTrue(result.isSuccess)
        assertTrue("Le corps doit mentionner le FAI Free SAS", capturedPrompt.contains("Free SAS"))
        assertTrue("Le corps doit mentionner l'ASN AS12322", capturedPrompt.contains("AS12322"))
    }

    @Test
    fun auditDevice_returnsFailureWhenApiKeyIsPlaceholder() = runTest {
        val service = GeminiSearchGroundingService(
            apiKeyProvider = { "MY_GEMINI_API_KEY" }
        )

        val device = NetworkDevice(
            ipAddress = "192.168.1.1",
            macAddress = "00:11:22:33:44:55",
            vendorName = "Netgear",
            category = DeviceCategory.ROUTER_GATEWAY,
            threatLevel = ThreatLevel.SAFE
        )

        val result = service.auditDevice(device)
        assertFalse(result.isSuccess)
        assertTrue(
            "Le message d'erreur doit guider l'utilisateur sur la configuration de GEMINI_API_KEY",
            result.exceptionOrNull()?.message?.contains("GEMINI_API_KEY") == true
        )
    }
}
