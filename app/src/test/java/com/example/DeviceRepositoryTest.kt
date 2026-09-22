package com.example

import com.example.data.DeviceDao
import com.example.data.DeviceEntity
import com.example.data.DeviceRepository
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRepositoryTest {

    private class FakeDeviceDao : DeviceDao {
        val memoryMap = mutableMapOf<String, DeviceEntity>()

        override fun getAllDevicesFlow(): Flow<List<DeviceEntity>> = flowOf(memoryMap.values.toList())
        override suspend fun getAllDevices(): List<DeviceEntity> = memoryMap.values.toList()
        override suspend fun getDeviceByMac(mac: String): DeviceEntity? = memoryMap[mac]
        override suspend fun insertOrUpdate(device: DeviceEntity) {
            memoryMap[device.macAddress] = device
        }
        override suspend fun insertOrUpdateAll(devices: List<DeviceEntity>) {
            devices.forEach { memoryMap[it.macAddress] = it }
        }
        override suspend fun setTrustedStatus(mac: String, trusted: Boolean) {
            memoryMap[mac]?.let { memoryMap[mac] = it.copy(isTrusted = trusted) }
        }
        override suspend fun updateCustomName(mac: String, name: String) {
            memoryMap[mac]?.let { memoryMap[mac] = it.copy(customName = name) }
        }
        override suspend fun deleteDevice(mac: String) {
            memoryMap.remove(mac)
        }
        override suspend fun clearAll() {
            memoryMap.clear()
        }
    }

    @Test
    fun testFirstScanDetectsNewDevices() = runBlocking {
        val fakeDao = FakeDeviceDao()
        val repo = DeviceRepository(fakeDao)

        val devices = listOf(
            NetworkDevice(
                ipAddress = "192.168.1.1",
                macAddress = "C0:4A:00:11:22:33",
                vendorName = "TP-Link",
                category = DeviceCategory.ROUTER_GATEWAY,
                threatLevel = ThreatLevel.SAFE,
                isGateway = true
            ),
            NetworkDevice(
                ipAddress = "192.168.1.100",
                macAddress = "AA:BB:CC:DD:EE:FF",
                vendorName = "Inconnu",
                category = DeviceCategory.UNKNOWN,
                threatLevel = ThreatLevel.WARNING
            )
        )

        val result = repo.processScanResults(devices)

        // Lors du 1er scan, AA:BB:CC:DD:EE:FF doit être identifié comme nouveau (intrus potentiel)
        assertEquals(2, result.allDevices.size)
        assertEquals(1, result.newDevices.size)
        assertEquals("AA:BB:CC:DD:EE:FF", result.newDevices.first().macAddress)
        assertTrue(result.hasIntrudersOrUnknown)

        // La passerelle doit avoir été automatiquement marquée comme de confiance
        val gatewayInDb = fakeDao.getDeviceByMac("C0:4A:00:11:22:33")
        assertTrue(gatewayInDb!!.isTrusted)
    }

    @Test
    fun testSecondScanPreservesCustomNameAndTrust() = runBlocking {
        val fakeDao = FakeDeviceDao()
        val repo = DeviceRepository(fakeDao)

        val dev = NetworkDevice(
            ipAddress = "192.168.1.80",
            macAddress = "3C:22:FB:00:11:22",
            vendorName = "Apple, Inc.",
            category = DeviceCategory.SMARTPHONE_TABLET,
            threatLevel = ThreatLevel.SAFE
        )

        // 1er passage
        repo.processScanResults(listOf(dev))
        // L'utilisateur personnalise le nom et lui accorde sa confiance
        repo.updateCustomName("3C:22:FB:00:11:22", "iPhone de Travail")
        repo.setTrustedStatus("3C:22:FB:00:11:22", true)

        // 2e passage avec le même appareil
        val secondScan = repo.processScanResults(listOf(dev))

        // Plus de nouvel intrus détecté
        assertEquals(0, secondScan.newDevices.size)
        assertFalse(secondScan.hasIntrudersOrUnknown)

        val preserved = secondScan.allDevices.first()
        assertEquals("iPhone de Travail", preserved.customName)
        assertTrue(preserved.isTrusted)
    }
}
