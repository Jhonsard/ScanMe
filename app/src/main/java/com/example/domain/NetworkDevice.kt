package com.example.domain

/**
 * Catégorie fonctionnelle estimée d'un équipement réseau.
 */
enum class DeviceCategory {
    ROUTER_GATEWAY,
    SMARTPHONE_TABLET,
    COMPUTER,
    IP_CAMERA,
    SMART_HOME_IOT,
    ENTERTAINMENT,
    PRINTER,
    UNKNOWN
}

/**
 * Niveau de menace de sécurité ou de confidentialité.
 */
enum class ThreatLevel {
    SAFE,           // Équipement de confiance ou standard
    INFORMATIONAL,  // Équipement habituel mais non labellisé
    SUSPICIOUS,     // Équipement présentant des attributs de surveillance/espionnage
    WARNING         // Appareil non répertorié ou port inhabituel
}

/**
 * Représentation unifiée d'un équipement découvert sur le sous-réseau.
 */
data class NetworkDevice(
    val ipAddress: String,
    val macAddress: String,
    val vendorName: String,
    val category: DeviceCategory,
    val threatLevel: ThreatLevel,
    val openPorts: List<Int> = emptyList(),
    val responseTimeMs: Long = -1L,
    val isGateway: Boolean = false,
    val isLocalDevice: Boolean = false,
    val suspicionReason: String? = null,
    val customName: String? = null,
    val isTrusted: Boolean = false
) {
    val displayName: String
        get() = customName ?: if (isGateway) "Passerelle / Routeur" else if (isLocalDevice) "Cet appareil" else vendorName

    val hasCameraAttributes: Boolean
        get() = category == DeviceCategory.IP_CAMERA || threatLevel == ThreatLevel.SUSPICIOUS
}
