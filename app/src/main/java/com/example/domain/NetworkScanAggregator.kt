package com.example.domain

import com.example.network.ArpReader
import com.example.network.OuiRepository
import com.example.network.PingResult
import com.example.network.SubnetInfo

/**
 * Agrégateur reliant les résultats du Ping Sweep, les tables ARP Linux
 * et le dictionnaire OUI pour produire la liste enrichie des NetworkDevice.
 */
class NetworkScanAggregator {

    fun aggregate(
        subnetInfo: SubnetInfo,
        pingResults: List<PingResult>,
        arpTable: Map<String, String> // Map<IP, MAC>
    ): List<NetworkDevice> {
        val reachablePings = pingResults.filter { it.isReachable }.associateBy { it.ip }
        val allIps = (reachablePings.keys + arpTable.keys + listOf(subnetInfo.localIp)).distinct()

        val devices = mutableListOf<NetworkDevice>()

        for (ip in allIps) {
            val ping = reachablePings[ip]
            val mac = arpTable[ip] ?: (if (ip == subnetInfo.localIp) "LOCAL_DEVICE" else "INCONNUE")
            val isLocal = ip == subnetInfo.localIp
            val isGateway = ip == subnetInfo.gatewayIp

            val vendor = if (isLocal) {
                "Ce smartphone (Local)"
            } else if (mac != "INCONNUE") {
                OuiRepository.resolveVendor(mac)
            } else {
                "Équipement sans entrée ARP"
            }

            val openPorts = ping?.openPorts ?: emptyList()
            val rtt = ping?.responseTimeMs ?: -1L

            val (category, baseThreat) = OuiRepository.analyzeDevice(
                ip = ip,
                mac = mac,
                vendor = vendor,
                openPorts = openPorts,
                isGateway = isGateway,
                isLocalDevice = isLocal
            )

            val suspicionReason = when {
                openPorts.contains(554) && OuiRepository.isCameraVendor(vendor) ->
                    "Flux vidéo RTSP (port 554) actif sur matériel du fabricant $vendor"
                openPorts.contains(554) ->
                    "Port de streaming vidéo RTSP 554 ouvert (caractéristique des caméras de surveillance)"
                OuiRepository.isCameraVendor(vendor) ->
                    "Fabricant répertorié dans les équipements de surveillance / caméras ($vendor)"
                vendor.contains("Espressif", ignoreCase = true) && openPorts.isNotEmpty() ->
                    "Module microcontrôleur WiFi avec serveur actif (possible capteur ou caméra miniature DIY)"
                else -> null
            }

            devices.add(
                NetworkDevice(
                    ipAddress = ip,
                    macAddress = mac,
                    vendorName = vendor,
                    category = category,
                    threatLevel = baseThreat,
                    openPorts = openPorts,
                    responseTimeMs = rtt,
                    isGateway = isGateway,
                    isLocalDevice = isLocal,
                    suspicionReason = suspicionReason
                )
            )
        }

        // Tri : D'abord les menaces/suspects, puis la passerelle, puis par IP
        return devices.sortedWith(
            compareByDescending<NetworkDevice> { it.threatLevel == ThreatLevel.SUSPICIOUS }
                .thenByDescending { it.isGateway }
                .thenByDescending { it.isLocalDevice }
                .thenBy { SubnetInfo.ipToInt(it.ipAddress) }
        )
    }
}
