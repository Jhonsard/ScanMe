package com.example.network

import java.util.Locale

/**
 * Entrée extraite de la table ARP du noyau Linux (/proc/net/arp).
 */
data class ArpEntry(
    val ipAddress: String,
    val macAddress: String,
    val hardwareType: String = "0x1",
    val flags: String = "0x2",
    val deviceInterface: String = "wlan0"
) {
    /**
     * Une entrée est valide si :
     * - Les flags ne sont pas 0x0 (qui correspondent à un échec de résolution ARP)
     * - L'adresse MAC n'est pas l'adresse nulle 00:00:00:00:00:00
     * - Le format de la MAC comporte bien 6 octets
     */
    val isValid: Boolean
        get() {
            if (flags == "0x0" || flags == "0x00") return false
            if (macAddress.equals("00:00:00:00:00:00", ignoreCase = true)) return false
            return MAC_REGEX.matches(macAddress)
        }

    /**
     * Retourne les 3 premiers octets (OUI) sous la forme XX:XX:XX
     */
    val ouiPrefix: String?
        get() {
            val clean = macAddress.replace("-", ":").uppercase(Locale.ROOT)
            val parts = clean.split(":")
            return if (parts.size >= 3) {
                "${parts[0]}:${parts[1]}:${parts[2]}"
            } else null
        }

    companion object {
        private val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")

        fun normalizeMac(rawMac: String): String {
            return rawMac.trim().replace("-", ":").uppercase(Locale.ROOT)
        }
    }
}
