package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité de persistance Room pour un appareil réseau découvert.
 */
@Entity(tableName = "network_devices")
data class DeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val ipAddress: String,
    val vendorName: String,
    val customName: String? = null,
    val isTrusted: Boolean = false,
    val category: String,
    val threatLevel: String,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long,
    val isSuspiciousCamera: Boolean = false
)
