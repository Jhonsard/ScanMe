package com.example.data

import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Résultat d'une analyse comparée entre le scan courant et l'historique enregistré.
 */
data class ScanComparisonResult(
    val allDevices: List<NetworkDevice>,
    val newDevices: List<NetworkDevice>,
    val suspiciousCameras: List<NetworkDevice>,
    val trustedDevicesCount: Int,
    val totalDevicesCount: Int
) {
    val hasIntrudersOrUnknown: Boolean
        get() = newDevices.isNotEmpty()

    val hasSuspiciousCameras: Boolean
        get() = suspiciousCameras.isNotEmpty()
}

class DeviceRepository(private val deviceDao: DeviceDao) {

    val allDevicesFlow: Flow<List<NetworkDevice>> = deviceDao.getAllDevicesFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun getKnownDevices(): List<DeviceEntity> {
        return deviceDao.getAllDevices()
    }

    /**
     * Compare le scan instantané avec la base de données :
     * 1. Détecte les nouveaux équipements jamais observés auparavant (intrus potentiels).
     * 2. Conserve les noms personnalisés et le statut de confiance (isTrusted).
     * 3. Met à jour les horodatages et synchronise la base de données.
     */
    suspend fun processScanResults(currentDevices: List<NetworkDevice>): ScanComparisonResult {
        val now = System.currentTimeMillis()
        val knownMap = deviceDao.getAllDevices().associateBy { it.macAddress }

        val newDevicesList = mutableListOf<NetworkDevice>()
        val suspiciousCamerasList = mutableListOf<NetworkDevice>()
        val updatedDevices = mutableListOf<NetworkDevice>()
        val entitiesToSave = mutableListOf<DeviceEntity>()

        for (device in currentDevices) {
            val existing = knownMap[device.macAddress]
            val isFirstTime = existing == null && 
                    !device.isGateway && 
                    !device.isLocalDevice && 
                    device.macAddress != "LOCAL_DEVICE" && 
                    device.macAddress != "INCONNUE"

            val customName = existing?.customName ?: device.customName
            val isTrusted = existing?.isTrusted ?: (device.isGateway || device.isLocalDevice)

            val updatedDevice = device.copy(
                customName = customName,
                isTrusted = isTrusted
            )

            updatedDevices.add(updatedDevice)

            if (isFirstTime) {
                newDevicesList.add(updatedDevice)
            }

            if (updatedDevice.hasCameraAttributes) {
                suspiciousCamerasList.add(updatedDevice)
            }

            entitiesToSave.add(
                DeviceEntity(
                    macAddress = device.macAddress,
                    ipAddress = device.ipAddress,
                    vendorName = device.vendorName,
                    customName = customName,
                    isTrusted = isTrusted,
                    category = device.category.name,
                    threatLevel = device.threatLevel.name,
                    firstSeenTimestamp = existing?.firstSeenTimestamp ?: now,
                    lastSeenTimestamp = now,
                    isSuspiciousCamera = updatedDevice.hasCameraAttributes
                )
            )
        }

        // Sauvegarde asynchrone dans Room
        deviceDao.insertOrUpdateAll(entitiesToSave)

        return ScanComparisonResult(
            allDevices = updatedDevices,
            newDevices = newDevicesList,
            suspiciousCameras = suspiciousCamerasList,
            trustedDevicesCount = updatedDevices.count { it.isTrusted },
            totalDevicesCount = updatedDevices.size
        )
    }

    suspend fun setTrustedStatus(mac: String, trusted: Boolean) {
        deviceDao.setTrustedStatus(mac, trusted)
    }

    suspend fun updateCustomName(mac: String, name: String) {
        deviceDao.updateCustomName(mac, name)
    }

    suspend fun deleteDevice(mac: String) {
        deviceDao.deleteDevice(mac)
    }

    private fun DeviceEntity.toDomainModel(): NetworkDevice {
        val cat = try {
            DeviceCategory.valueOf(category)
        } catch (_: Exception) {
            DeviceCategory.UNKNOWN
        }
        val threat = try {
            ThreatLevel.valueOf(threatLevel)
        } catch (_: Exception) {
            ThreatLevel.INFORMATIONAL
        }

        return NetworkDevice(
            ipAddress = ipAddress,
            macAddress = macAddress,
            vendorName = vendorName,
            category = cat,
            threatLevel = threat,
            customName = customName,
            isTrusted = isTrusted
        )
    }
}
