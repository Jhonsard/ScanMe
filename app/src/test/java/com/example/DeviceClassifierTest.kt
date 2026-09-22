package com.example

import com.example.domain.DeviceCategory
import com.example.domain.NetworkScanAggregator
import com.example.domain.ThreatLevel
import com.example.network.OuiRepository
import com.example.network.PingResult
import com.example.network.SubnetInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceClassifierTest {

    @Test
    fun testOuiVendorResolution() {
        val dahuaVendor = OuiRepository.resolveVendor("E4:AA:EA:11:22:33")
        assertEquals("Dahua", dahuaVendor)

        val appleVendor = OuiRepository.resolveVendor("AC:DE:48:00:11:22")
        assertEquals("Apple, Inc.", appleVendor)

        val tplinkVendor = OuiRepository.resolveVendor("C0:4A:00:99:88:77")
        assertEquals("TP-Link Technologies", tplinkVendor)
    }

    @Test
    fun testCameraDetectionAndThreatClassification() {
        val (category, threat) = OuiRepository.analyzeDevice(
            ip = "192.168.1.105",
            mac = "E4:AA:EA:11:22:33",
            vendor = "Dahua",
            openPorts = listOf(554, 80),
            isGateway = false,
            isLocalDevice = false
        )

        assertEquals(DeviceCategory.IP_CAMERA, category)
        assertEquals(ThreatLevel.SUSPICIOUS, threat)
    }

    @Test
    fun testAggregationAndSorting() {
        val subnet = SubnetInfo(
            localIp = "192.168.1.50",
            prefixLength = 24,
            gatewayIp = "192.168.1.1"
        )

        val pingResults = listOf(
            PingResult(ip = "192.168.1.1", isReachable = true, responseTimeMs = 2, openPorts = listOf(80)),
            PingResult(ip = "192.168.1.50", isReachable = true, responseTimeMs = 0),
            PingResult(ip = "192.168.1.120", isReachable = true, responseTimeMs = 15, openPorts = listOf(554))
        )

        val arpTable = mapOf(
            "192.168.1.1" to "C0:4A:00:01:02:03", // TP-Link
            "192.168.1.120" to "E4:AA:EA:99:88:77" // Dahua
        )

        val aggregator = NetworkScanAggregator()
        val devices = aggregator.aggregate(subnet, pingResults, arpTable)

        assertEquals(3, devices.size)

        // Le premier appareil doit être la caméra suspecte (192.168.1.120) car threatLevel == SUSPICIOUS
        val firstDevice = devices[0]
        assertEquals("192.168.1.120", firstDevice.ipAddress)
        assertEquals("Dahua", firstDevice.vendorName)
        assertEquals(ThreatLevel.SUSPICIOUS, firstDevice.threatLevel)
        assertEquals(DeviceCategory.IP_CAMERA, firstDevice.category)
        assertNotNull(firstDevice.suspicionReason)
        assertTrue(firstDevice.suspicionReason!!.contains("RTSP"))

        // La passerelle (192.168.1.1) doit venir ensuite
        val secondDevice = devices[1]
        assertEquals("192.168.1.1", secondDevice.ipAddress)
        assertTrue(secondDevice.isGateway)
    }
}
