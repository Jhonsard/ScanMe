package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Moteur de balayage réseau (Ping Sweep) multithreadé non bloquant.
 * Utilise des coroutines régulées par un Semaphore pour envoyer des paquets ICMP/TCP légers.
 * But premier : réveiller les machines du sous-réseau pour que le noyau Linux
 * peuple sa table ARP (/proc/net/arp).
 */
class PingSweepEngine(
    private val maxConcurrency: Int = 35,
    private val socketTimeoutMs: Int = 180,
    private val icmpTimeoutMs: Int = 180
) {

    // Ports stratégiques pour le réveil réseau et l'audit de caméras/streamers
    private val targetPorts = listOf(
        PORT_HTTP,
        PORT_HTTPS,
        PORT_RTSP,
        PORT_ALT_HTTP
    )

    /**
     * Balaye une liste d'adresses IP et émet un flux de progression en temps réel.
     * Utilise channelFlow pour garantir la sécurité d'émission concurrente (Flow invariant).
     */
    fun scanSubnetFlow(hosts: List<String>): Flow<ScanProgressEvent> = channelFlow {
        val total = hosts.size
        val scannedCounter = AtomicInteger(0)
        val activeCounter = AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrency)

        send(ScanProgressEvent.Started(totalHosts = total))

        coroutineScope {
            val deferreds = hosts.map { ip ->
                async(Dispatchers.IO) {
                    val result = semaphore.withPermit {
                        probeHost(ip)
                    }

                    val scanned = scannedCounter.incrementAndGet()
                    if (result.isReachable) {
                        activeCounter.incrementAndGet()
                    }

                    send(
                        ScanProgressEvent.Progress(
                            progress = ScanProgress(
                                scannedHosts = scanned,
                                totalHosts = total,
                                activeHostsFound = activeCounter.get(),
                                currentIp = ip
                            ),
                            lastResult = result
                        )
                    )
                    result
                }
            }

            val allResults = deferreds.awaitAll()
            send(ScanProgressEvent.Completed(results = allResults))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sonde un hôte spécifique :
     * 1. Tentative ICMP echo / port 7 (InetAddress.isReachable)
     * 2. Probe de ports TCP ciblés (notamment port 554 RTSP caméra)
     */
    suspend fun probeHost(ip: String): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var reachable = false
        val openPorts = mutableListOf<Int>()

        try {
            val inetAddress = InetAddress.getByName(ip)
            // Test 1: Méthode standard Java isReachable
            reachable = inetAddress.isReachable(icmpTimeoutMs)
        } catch (_: Exception) {
            // Ignoré : l'hôte peut filtrer l'ICMP
        }

        // Test 2: Probe de ports TCP légers (réveille la table ARP même si ICMP est bloqué par un pare-feu)
        for (port in targetPorts) {
            if (isPortOpen(ip, port, socketTimeoutMs)) {
                openPorts.add(port)
                reachable = true
            }
        }

        val elapsed = if (reachable) (System.currentTimeMillis() - startTime) else -1L

        PingResult(
            ip = ip,
            isReachable = reachable,
            responseTimeMs = elapsed,
            openPorts = openPorts
        )
    }

    /**
     * Tente une connexion TCP synchrone avec un timeout très strict pour ne pas bloquer.
     */
    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    sealed class ScanProgressEvent {
        data class Started(val totalHosts: Int) : ScanProgressEvent()
        data class Progress(val progress: ScanProgress, val lastResult: PingResult) : ScanProgressEvent()
        data class Completed(val results: List<PingResult>) : ScanProgressEvent()
    }

    companion object {
        private const val TAG = "PingSweepEngine"
        const val PORT_HTTP = 80
        const val PORT_HTTPS = 443
        const val PORT_RTSP = 554      // Protocole clé des caméras IP (Real Time Streaming Protocol)
        const val PORT_ALT_HTTP = 8080
    }
}
