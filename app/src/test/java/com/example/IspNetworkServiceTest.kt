package com.example

import com.example.network.IspNetworkService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IspNetworkServiceTest {

    private val sampleJsonResponse = """
        {
          "status": "success",
          "country": "France",
          "countryCode": "FR",
          "region": "IDF",
          "regionName": "Île-de-France",
          "city": "Paris",
          "isp": "Free SAS",
          "org": "Free SAS Proxi",
          "as": "AS12322 Free SAS",
          "reverse": "82-64-120-45.subs.proxad.net",
          "query": "82.64.120.45",
          "proxy": false,
          "hosting": false
        }
    """.trimIndent()

    @Test
    fun testParseIspJsonCorrectlyExtractsAllFields() {
        val service = IspNetworkService()
        val ispInfo = service.parseIspJson(sampleJsonResponse)

        assertEquals("82.64.120.45", ispInfo.publicIp)
        assertEquals("Free SAS", ispInfo.ispName)
        assertEquals("Free SAS Proxi", ispInfo.organization)
        assertEquals("AS12322 Free SAS", ispInfo.asn)
        assertEquals("France", ispInfo.country)
        assertEquals("FR", ispInfo.countryCode)
        assertEquals("Paris", ispInfo.city)
        assertEquals("Île-de-France", ispInfo.region)
        assertEquals("82-64-120-45.subs.proxad.net", ispInfo.reverseDns)
        assertFalse(ispInfo.isVpnOrProxy)

        assertEquals("Paris, France", ispInfo.locationDisplay)
        assertEquals("AS12322 Free SAS", ispInfo.asnDisplay)
        assertEquals("Free SAS", ispInfo.ispSummary)
    }

    @Test
    fun testFetchIspDetailsWithCustomHttpClientSuccess() = runBlocking {
        val mockService = IspNetworkService(httpClient = { sampleJsonResponse })
        val result = mockService.fetchIspDetails()

        assertTrue(result.isSuccess)
        val info = result.getOrNull()!!
        assertEquals("82.64.120.45", info.publicIp)
        assertEquals("Free SAS", info.ispName)
    }

    @Test
    fun testFetchIspDetailsHandlesHttpErrorGracefully() = runBlocking {
        val failingService = IspNetworkService(httpClient = {
            throw java.io.IOException("Network unreachable")
        })

        val result = failingService.fetchIspDetails()
        assertTrue(result.isFailure)
    }
}
