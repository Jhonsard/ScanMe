package com.example

import com.example.network.ArpEntry
import com.example.network.ArpReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArpReaderTest {

    private val sampleArpOutput = """
        IP address       HW type     Flags       HW address            Mask     Device
        192.168.1.1      0x1         0x2         c0:4a:00:11:22:33     *        wlan0
        192.168.1.50     0x1         0x0         00:00:00:00:00:00     *        wlan0
        192.168.1.65     0x1         0x2         00-14-22-01-23-45     *        wlan0
        192.168.1.99     0x1         0x2         e4:aa:ea:ab:cd:ef     *        wlan0
        invalid_line_without_enough_tokens
    """.trimIndent()

    @Test
    fun testParseArpContent() {
        val reader = ArpReader()
        val entries = reader.parseArpContent(sampleArpOutput)

        // Doit filtrer la ligne d'en-tête, la ligne incomplète 0x0/00:00:00:00:00:00, et la ligne invalide
        assertEquals(3, entries.size)

        val first = entries[0]
        assertEquals("192.168.1.1", first.ipAddress)
        assertEquals("C0:4A:00:11:22:33", first.macAddress)
        assertEquals("C0:4A:00", first.ouiPrefix)
        assertEquals("wlan0", first.deviceInterface)
        assertTrue(first.isValid)

        val second = entries[1]
        assertEquals("192.168.1.65", second.ipAddress)
        assertEquals("00:14:22:01:23:45", second.macAddress)
        assertEquals("00:14:22", second.ouiPrefix)

        val third = entries[2]
        assertEquals("192.168.1.99", third.ipAddress)
        assertEquals("E4:AA:EA:AB:CD:EF", third.macAddress)
        assertEquals("E4:AA:EA", third.ouiPrefix)
    }

    @Test
    fun testArpEntryValidation() {
        val validEntry = ArpEntry(
            ipAddress = "192.168.1.10",
            macAddress = "AA:BB:CC:DD:EE:FF",
            flags = "0x2"
        )
        assertTrue(validEntry.isValid)

        val zeroMac = ArpEntry(
            ipAddress = "192.168.1.11",
            macAddress = "00:00:00:00:00:00",
            flags = "0x2"
        )
        assertFalse(zeroMac.isValid)

        val zeroFlags = ArpEntry(
            ipAddress = "192.168.1.12",
            macAddress = "AA:BB:CC:DD:EE:FF",
            flags = "0x0"
        )
        assertFalse(zeroFlags.isValid)
    }
}
