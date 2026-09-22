package com.example

import com.example.network.SubnetInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetUtilsTest {

    @Test
    fun testIpConversionRoundTrip() {
        val testIp = "192.168.1.100"
        val intVal = SubnetInfo.ipToInt(testIp)
        val backToIp = SubnetInfo.intToIp(intVal)
        assertEquals(testIp, backToIp)
    }

    @Test
    fun testStandardSlash24Subnet() {
        val subnet = SubnetInfo(
            localIp = "192.168.1.45",
            prefixLength = 24,
            gatewayIp = "192.168.1.1"
        )

        assertEquals("192.168.1.0", subnet.networkAddress)
        assertEquals("192.168.1.255", subnet.broadcastAddress)

        val hosts = subnet.getHostIps()
        assertEquals(254, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertTrue(hosts.contains("192.168.1.45"))
    }

    @Test
    fun testClassASlash24Segment() {
        val subnet = SubnetInfo(
            localIp = "10.0.0.15",
            prefixLength = 24,
            gatewayIp = "10.0.0.1"
        )

        assertEquals("10.0.0.0", subnet.networkAddress)
        assertEquals("10.0.0.255", subnet.broadcastAddress)

        val hosts = subnet.getHostIps()
        assertEquals(254, hosts.size)
        assertEquals("10.0.0.1", hosts.first())
        assertEquals("10.0.0.254", hosts.last())
    }

    @Test
    fun testLargeSubnetSafetyCap() {
        // Un réseau d'entreprise /16 (65 534 hôtes) doit être bridé à 254 hôtes autour du téléphone
        val subnet = SubnetInfo(
            localIp = "172.16.5.30",
            prefixLength = 16,
            gatewayIp = "172.16.0.1"
        )

        val hosts = subnet.getHostIps()
        assertEquals(254, hosts.size)
        assertEquals("172.16.5.1", hosts.first())
        assertEquals("172.16.5.254", hosts.last())
    }
}
