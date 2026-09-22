package com.example.ai

import com.example.domain.NetworkDevice
import com.example.network.IspInfo

/**
 * Modèle représentant une source Web issue du Grounding Google Search.
 */
data class GroundingWebSource(
    val title: String,
    val url: String
)

/**
 * Résultat complet d'un audit de sécurité ou de réseau enrichi par Google Search.
 */
data class GroundingAuditResult(
    val queryPrompt: String,
    val summary: String,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingWebSource> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Cible analysée par le service de Grounding.
 */
sealed class AuditTarget {
    data class Device(val device: NetworkDevice) : AuditTarget()
    data class Isp(val isp: IspInfo) : AuditTarget()
    data class Custom(val prompt: String, val title: String) : AuditTarget()

    val displayTitle: String
        get() = when (this) {
            is Device -> "${device.displayName} (${device.ipAddress})"
            is Isp -> "Opérateur ${isp.ispSummary} (${isp.publicIp})"
            is Custom -> title
        }
}
