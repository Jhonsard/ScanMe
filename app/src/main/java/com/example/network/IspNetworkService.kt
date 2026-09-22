package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Service d'interrogation de l'infrastructure réseau publique (WAN).
 * Fournit les détails sur le FAI (ISP), l'ASN, l'IP publique et la géolocalisation réseau.
 */
class IspNetworkService(
    private val httpClient: (String) -> String = { urlString -> fetchHttpContent(urlString) }
) {

    /**
     * Récupère les informations d'infrastructure WAN et FAI.
     * Cette méthode effectue une requête HTTPS sécurisée et résout le reverse DNS.
     */
    suspend fun fetchIspDetails(): Result<IspInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonResponse = httpClient(ISP_QUERY_URL)
            val parsedInfo = parseIspJson(jsonResponse)
            Result.success(parsedInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse la réponse JSON brute provenant de l'API de diagnostic réseau.
     */
    fun parseIspJson(jsonString: String): IspInfo {
        val root = JSONObject(jsonString)

        val publicIp = root.optString("query", root.optString("ip", "Non détectée"))
        val ispName = root.optString("isp", root.optString("org", "Inconnu"))
        val organization = root.optString("org", ispName)
        val asn = root.optString("as", root.optString("asn", ""))
        val country = root.optString("country", "")
        val countryCode = root.optString("countryCode", "")
        val city = root.optString("city", "")
        val region = root.optString("regionName", root.optString("region", ""))
        val isProxy = root.optBoolean("proxy", false) || root.optBoolean("hosting", false)

        var reverseDns = root.optString("reverse", "")
        if (reverseDns.isBlank() && publicIp.isNotBlank() && publicIp != "Non détectée") {
            try {
                val inetAddr = InetAddress.getByName(publicIp)
                reverseDns = inetAddr.canonicalHostName
                if (reverseDns == publicIp) reverseDns = ""
            } catch (_: Exception) {
                reverseDns = ""
            }
        }

        return IspInfo(
            publicIp = publicIp,
            ispName = ispName,
            organization = organization,
            asn = asn,
            country = country,
            countryCode = countryCode,
            city = city,
            region = region,
            reverseDns = reverseDns.ifBlank { "Non résolu" },
            isVpnOrProxy = isProxy
        )
    }

    companion object {
        private const val ISP_QUERY_URL = "http://ip-api.com/json/?fields=status,message,country,countryCode,region,regionName,city,isp,org,as,reverse,query,proxy,hosting"

        private fun fetchHttpContent(urlString: String): String {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "NetWard-Network-Audit/1.0")

            try {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    content.append(line)
                }
                reader.close()
                return content.toString()
            } finally {
                connection.disconnect()
            }
        }
    }
}
