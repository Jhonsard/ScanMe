package com.example.network

/**
 * Résultat du sondage actif d'une adresse IP lors du ping sweep.
 */
data class PingResult(
    val ip: String,
    val isReachable: Boolean,
    val responseTimeMs: Long = -1L,
    val openPorts: List<Int> = emptyList()
) {
    val isRtspDetected: Boolean
        get() = openPorts.contains(554)
}

/**
 * Événement de progression émis pendant l'analyse asynchrone.
 */
data class ScanProgress(
    val scannedHosts: Int,
    val totalHosts: Int,
    val activeHostsFound: Int,
    val currentIp: String
) {
    val progressPercentage: Float
        get() = if (totalHosts > 0) scannedHosts.toFloat() / totalHosts.toFloat() else 0f
}
