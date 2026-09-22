package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM network_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM network_devices")
    suspend fun getAllDevices(): List<DeviceEntity>

    @Query("SELECT * FROM network_devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(devices: List<DeviceEntity>)

    @Query("UPDATE network_devices SET isTrusted = :trusted WHERE macAddress = :mac")
    suspend fun setTrustedStatus(mac: String, trusted: Boolean)

    @Query("UPDATE network_devices SET customName = :name WHERE macAddress = :mac")
    suspend fun updateCustomName(mac: String, name: String)

    @Query("DELETE FROM network_devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("DELETE FROM network_devices")
    suspend fun clearAll()
}
