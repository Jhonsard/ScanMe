package com.example.network

import com.example.domain.DeviceCategory
import com.example.domain.ThreatLevel
import java.util.Locale

/**
 * Base de données locale intégrée des préfixes constructeurs OUI (IEEE)
 * et moteur d'heuristique pour la détection de caméras espionnes / IoT.
 */
object OuiRepository {

    // Liste noire des fabricants de vidéosurveillance, caméras IP et microcontrôleurs vidéo IoT
    val CAMERA_VENDORS = setOf(
        "Hikvision",
        "Hangzhou Hikvision Digital Technology",
        "Dahua",
        "Zhejiang Dahua Technology",
        "Reolink",
        "Tuya",
        "Tuya Smart",
        "Wyze",
        "Wyze Labs",
        "Espressif",
        "Espressif Systems", // Très utilisé par les caméras espionnes miniatures WiFi DIY (ESP32-CAM)
        "Axis",
        "Axis Communications",
        "Amcrest",
        "Foscam",
        "Shenzhen Foscam Intelligent",
        "Vivotek",
        "Hanwha",
        "Hanwha Techwin",
        "Uniview",
        "Zhejiang Uniview Technologies",
        "EZVIZ",
        "TP-Link Tapo",
        "Xiaomi Camera",
        "Yi Technology",
        "Ring",
        "Arlo"
    )

    private val OUI_MAP = mapOf(
        // Caméras & Surveillance
        "00:12:12" to "Dahua",
        "3C:EF:8C" to "Dahua",
        "90:02:A9" to "Dahua",
        "E4:AA:EA" to "Dahua",
        "BC:51:FE" to "Hikvision",
        "44:19:B6" to "Hikvision",
        "C0:56:E3" to "Hikvision",
        "40:2C:76" to "Hikvision",
        "EC:71:DB" to "Reolink",
        "48:E7:DA" to "Reolink",
        "94:A1:A2" to "Tuya Smart",
        "D8:1F:12" to "Tuya Smart",
        "70:89:71" to "Tuya Smart",
        "24:6F:28" to "Espressif Systems", // ESP32-CAM
        "30:AE:A4" to "Espressif Systems",
        "84:F3:EB" to "Espressif Systems",
        "2C:3A:E8" to "Wyze Labs",
        "7C:78:B2" to "Wyze Labs",
        "00:40:8C" to "Axis Communications",
        "AC:CC:8E" to "Axis Communications",
        "00:02:D1" to "Vivotek",
        "00:62:6E" to "Amcrest",
        "00:1A:6B" to "Ring",
        "B0:09:DA" to "Ring",
        "84:D6:D0" to "Arlo Technologies",

        // Réseau & Routeurs
        "00:14:22" to "Dell",
        "C0:4A:00" to "TP-Link Technologies",
        "50:C7:BF" to "TP-Link Technologies",
        "00:1D:7E" to "Cisco-Linksys",
        "00:1F:12" to "Cisco",
        "10:05:01" to "Netgear",
        "9C:3D:CF" to "Netgear",
        "04:D4:C4" to "ASUSTek Computer",
        "00:26:18" to "ASUSTek Computer",
        "F0:9F:C2" to "Ubiquiti Networks",
        "78:8A:20" to "Ubiquiti Networks",
        "34:8A:AE" to "Sagemcom Broadband",
        "44:E9:DD" to "Freebox SAS",
        "00:07:CB" to "Freebox SAS",
        "D0:84:B0" to "AVM GmbH (FRITZ!Box)",

        // Smartphones, Tablettes & PC
        "00:17:F2" to "Apple, Inc.",
        "AC:DE:48" to "Apple, Inc.",
        "BC:92:6B" to "Apple, Inc.",
        "F0:18:98" to "Apple, Inc.",
        "3C:22:FB" to "Apple, Inc.",
        "A4:83:E7" to "Apple, Inc.",
        "00:1A:8A" to "Samsung Electronics",
        "50:77:05" to "Samsung Electronics",
        "98:3B:8F" to "Samsung Electronics",
        "A8:7C:01" to "Samsung Electronics",
        "3C:5A:37" to "Google, Inc.",
        "D8:6C:63" to "Google, Inc.",
        "54:60:09" to "Google, Inc.",
        "64:16:66" to "Xiaomi Communications",
        "58:44:98" to "Xiaomi Communications",
        "40:4E:36" to "Huawei Technologies",
        "00:15:5D" to "Microsoft Corporation",
        "98:5F:D3" to "Microsoft Corporation",
        "00:1E:67" to "Intel Corporate",
        "B4:2E:99" to "Intel Corporate",

        // Divertissement, Smart TV & IoT
        "00:04:20" to "Slim Devices (Logitech)",
        "B8:27:EB" to "Raspberry Pi Foundation",
        "DC:A6:32" to "Raspberry Pi Foundation",
        "E4:5F:01" to "Raspberry Pi Foundation",
        "00:09:B0" to "Onkyo Corporation",
        "48:A6:B8" to "Sonos, Inc.",
        "00:0E:58" to "Sonos, Inc.",
        "B8:3E:59" to "Roku, Inc.",
        "DC:3A:5E" to "Amazon Technologies",
        "68:54:5A" to "Amazon Technologies",
        "FC:65:DE" to "Amazon Technologies",
        "A0:02:DC" to "Sony Corporation",
        "00:1C:26" to "LG Electronics",
        "A8:23:FE" to "LG Electronics"
    )

    /**
     * Résout le nom du fabricant à partir de l'adresse MAC.
     */
    fun resolveVendor(macAddress: String): String {
        val clean = macAddress.trim().replace("-", ":").uppercase(Locale.ROOT)
        val parts = clean.split(":")
        if (parts.size >= 3) {
            val prefix = "${parts[0]}:${parts[1]}:${parts[2]}"
            return OUI_MAP[prefix] ?: "Fabricant non répertorié"
        }
        return "Inconnu"
    }

    /**
     * Analyse les attributs de l'appareil pour déterminer sa catégorie et son niveau de menace.
     */
    fun analyzeDevice(
        ip: String,
        mac: String,
        vendor: String,
        openPorts: List<Int>,
        isGateway: Boolean,
        isLocalDevice: Boolean
    ): Pair<DeviceCategory, ThreatLevel> {
        val hasRtsp = openPorts.contains(554)
        val isKnownCameraVendor = isCameraVendor(vendor)

        // Cas 1 : Passerelle / Routeur
        if (isGateway) {
            return Pair(DeviceCategory.ROUTER_GATEWAY, ThreatLevel.SAFE)
        }

        // Cas 2 : Caméra de surveillance / Appareil espion potentiel
        if (hasRtsp && isKnownCameraVendor) {
            return Pair(DeviceCategory.IP_CAMERA, ThreatLevel.SUSPICIOUS)
        }
        if (hasRtsp) {
            return Pair(DeviceCategory.IP_CAMERA, ThreatLevel.SUSPICIOUS)
        }
        if (isKnownCameraVendor) {
            return Pair(DeviceCategory.IP_CAMERA, ThreatLevel.SUSPICIOUS)
        }

        // Cas 3 : Microcontrôleur IoT (ESP32) avec ports Web ouverts (souvent utilisé pour des modules DIY cachés)
        if (vendor.contains("Espressif", ignoreCase = true)) {
            val threat = if (openPorts.isNotEmpty()) ThreatLevel.SUSPICIOUS else ThreatLevel.WARNING
            return Pair(DeviceCategory.SMART_HOME_IOT, threat)
        }

        // Cas 4 : Appareils grand public courants
        val vendorUpper = vendor.uppercase(Locale.ROOT)
        return when {
            vendorUpper.contains("APPLE") || vendorUpper.contains("SAMSUNG") || vendorUpper.contains("GOOGLE") || vendorUpper.contains("XIAOMI") || vendorUpper.contains("HUAWEI") -> {
                Pair(DeviceCategory.SMARTPHONE_TABLET, ThreatLevel.SAFE)
            }
            vendorUpper.contains("INTEL") || vendorUpper.contains("DELL") || vendorUpper.contains("MICROSOFT") || vendorUpper.contains("ASUSTEK") || vendorUpper.contains("LENOVO") -> {
                Pair(DeviceCategory.COMPUTER, ThreatLevel.SAFE)
            }
            vendorUpper.contains("TP-LINK") || vendorUpper.contains("NETGEAR") || vendorUpper.contains("CISCO") || vendorUpper.contains("UBIQUITI") || vendorUpper.contains("SAGEMCOM") || vendorUpper.contains("FREEBOX") || vendorUpper.contains("FRITZ") -> {
                Pair(DeviceCategory.ROUTER_GATEWAY, ThreatLevel.SAFE)
            }
            vendorUpper.contains("SONOS") || vendorUpper.contains("ROKU") || vendorUpper.contains("SONY") || vendorUpper.contains("AMAZON") || vendorUpper.contains("LG ELECTRONICS") -> {
                Pair(DeviceCategory.ENTERTAINMENT, ThreatLevel.SAFE)
            }
            vendorUpper.contains("RASPBERRY") -> {
                Pair(DeviceCategory.COMPUTER, ThreatLevel.INFORMATIONAL)
            }
            else -> {
                Pair(DeviceCategory.UNKNOWN, ThreatLevel.INFORMATIONAL)
            }
        }
    }

    fun isCameraVendor(vendor: String): Boolean {
        return CAMERA_VENDORS.any { vendor.contains(it, ignoreCase = true) }
    }
}
