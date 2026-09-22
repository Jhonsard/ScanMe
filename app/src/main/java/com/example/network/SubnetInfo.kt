package com.example.network

import java.net.InetAddress
import kotlin.math.min

/**
 * Représente la topologie IPv4 du sous-réseau local auquel le smartphone est connecté.
 */
data class SubnetInfo(
    val localIp: String,
    val prefixLength: Int,
    val gatewayIp: String?,
    val interfaceName: String = "wlan0"
) {
    /**
     * Masque binaire sous forme d'entier 32 bits non signé.
     * Exemple : pour un /24 -> 0xFFFFFF00 (-256)
     */
    val netmaskInt: Int = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))

    val localIpInt: Int = ipToInt(localIp)

    val networkAddressInt: Int = localIpInt and netmaskInt
    val networkAddress: String = intToIp(networkAddressInt)

    val broadcastAddressInt: Int = networkAddressInt or netmaskInt.inv()
    val broadcastAddress: String = intToIp(broadcastAddressInt)

    /**
     * Calcule la liste ordonnée des adresses IP hôtes à balayer.
     * Exclut l'adresse réseau et l'adresse de diffusion (broadcast).
     * Par sécurité sur mobile, si le réseau est plus grand qu'un /23 (ex: /16),
     * on limite le scan au /24 local autour de l'IP du smartphone pour éviter d'épuiser les sockets.
     */
    fun getHostIps(maxHosts: Int = 254): List<String> {
        val effectivePrefix = if (prefixLength < 23) 24 else prefixLength
        val effectiveMask = if (effectivePrefix == 0) 0 else (-1 shl (32 - effectivePrefix))
        val startNet = localIpInt and effectiveMask
        val endBroadcast = startNet or effectiveMask.inv()

        val startHost = startNet + 1
        val endHost = endBroadcast - 1

        if (startHost > endHost) return emptyList()

        val totalHosts = (endHost.toLong() and 0xFFFFFFFFL) - (startHost.toLong() and 0xFFFFFFFFL) + 1
        val count = min(totalHosts, maxHosts.toLong()).toInt()

        val list = ArrayList<String>(count)
        var current = startHost
        for (i in 0 until count) {
            list.add(intToIp(current))
            current++
        }
        return list
    }

    companion object {
        fun ipToInt(ip: String): Int {
            val parts = ip.split(".")
            if (parts.size != 4) return 0
            var result = 0
            for (i in 0..3) {
                val byteVal = parts[i].toIntOrNull() ?: 0
                result = (result shl 8) or (byteVal and 0xFF)
            }
            return result
        }

        fun intToIp(value: Int): String {
            return "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}.${(value ushr 8) and 0xFF}.${value and 0xFF}"
        }
    }
}
