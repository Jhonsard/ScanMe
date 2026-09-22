package com.example.network

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Lecteur et analyseur de la table ARP du noyau Linux (/proc/net/arp).
 */
class ArpReader(
    private val arpFilePath: String = ARP_FILE_PATH
) {

    /**
     * Lit le fichier virtuel /proc/net/arp du système et retourne la liste des entrées valides.
     */
    fun readArpTable(): List<ArpEntry> {
        val file = File(arpFilePath)
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "Le fichier $arpFilePath n'existe pas ou n'est pas accessible en lecture")
            return emptyList()
        }

        return try {
            val content = file.readText()
            parseArpContent(content)
        } catch (e: SecurityException) {
            Log.e(TAG, "Restriction SELinux lors de la lecture de $arpFilePath", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur E/S lors de la lecture de $arpFilePath", e)
            emptyList()
        }
    }

    /**
     * Parse le texte brut issu de /proc/net/arp.
     * Cette méthode est pure et peut être testée unitairement avec des échantillons textuels.
     */
    fun parseArpContent(content: String): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()
        val lines = content.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Ignorer la ligne d'en-tête Linux
            // "IP address       HW type     Flags       HW address            Mask     Device"
            if (trimmed.startsWith("IP", ignoreCase = true) || trimmed.contains("HW address", ignoreCase = true)) {
                continue
            }

            // Découpage par espaces multiples (\s+)
            val tokens = trimmed.split(SPLIT_REGEX)
            if (tokens.size >= 6) {
                val ip = tokens[0].trim()
                val hwType = tokens[1].trim()
                val flags = tokens[2].trim()
                val rawMac = tokens[3].trim()
                val dev = tokens[5].trim()

                val normalizedMac = ArpEntry.normalizeMac(rawMac)
                val entry = ArpEntry(
                    ipAddress = ip,
                    macAddress = normalizedMac,
                    hardwareType = hwType,
                    flags = flags,
                    deviceInterface = dev
                )

                if (entry.isValid) {
                    entries.add(entry)
                }
            }
        }

        return entries
    }

    /**
     * Retourne une table de correspondance rapide Map<IP, Adresse MAC>
     */
    fun getIpToMacMap(): Map<String, String> {
        return readArpTable().associate { it.ipAddress to it.macAddress }
    }

    companion object {
        private const val TAG = "ArpReader"
        const val ARP_FILE_PATH = "/proc/net/arp"
        private val SPLIT_REGEX = Regex("\\s+")
    }
}
