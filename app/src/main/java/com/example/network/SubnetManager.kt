package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Gère la détection de la topologie réseau de l'appareil (IP locale, masque de sous-réseau, passerelle).
 */
class SubnetManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Détermine la configuration du sous-réseau actif via ConnectivityManager et LinkProperties.
     * En cas d'indisponibilité (ex: appareil déconnecté ou restrictions de l'OS),
     * interroge les NetworkInterfaces en fallback.
     */
    fun getActiveSubnetInfo(): SubnetInfo? {
        val activeNetwork = connectivityManager.activeNetwork ?: return getFallbackFromInterfaces()
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return getFallbackFromInterfaces()

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

        if (!isWifi && !isEthernet) {
            Log.w(TAG, "Le réseau actif n'est ni WiFi ni Ethernet (possiblement réseau mobile)")
        }

        // Recherche de la première adresse IPv4
        var localIp: String? = null
        var prefixLength = 24
        var interfaceName = linkProperties.interfaceName ?: "wlan0"

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                localIp = address.hostAddress
                prefixLength = linkAddress.prefixLength
                break
            }
        }

        if (localIp == null) {
            return getFallbackFromInterfaces()
        }

        // Recherche de la passerelle (Gateway / Route par défaut 0.0.0.0/0)
        var gatewayIp: String? = null
        for (route in linkProperties.routes) {
            if (route.isDefaultRoute && route.gateway is Inet4Address) {
                gatewayIp = route.gateway?.hostAddress
                break
            }
        }

        return SubnetInfo(
            localIp = localIp,
            prefixLength = prefixLength,
            gatewayIp = gatewayIp,
            interfaceName = interfaceName
        )
    }

    /**
     * Mécanisme de secours par interrogation directe des interfaces réseau (NetworkInterface Linux)
     */
    private fun getFallbackFromInterfaces(): SubnetInfo? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name
                // Priorité aux interfaces sans-fil wlan ou eth
                if (!name.startsWith("wlan") && !name.startsWith("eth") && !name.startsWith("en")) continue

                for (interfaceAddress in intf.interfaceAddresses) {
                    val addr = interfaceAddress.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        val prefix = interfaceAddress.networkPrefixLength.toInt()
                        return SubnetInfo(
                            localIp = ip,
                            prefixLength = if (prefix in 8..30) prefix else 24,
                            gatewayIp = null,
                            interfaceName = name
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture des interfaces réseau", e)
        }
        return null
    }

    companion object {
        private const val TAG = "SubnetManager"
    }
}
