package com.example.ai

import com.example.BuildConfig
import com.example.domain.NetworkDevice
import com.example.network.IspInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service d'intelligence cybersécurité exploitant le modèle gemini-3.5-flash
 * avec l'outil googleSearch (Search Grounding) pour fournir des alertes,
 * vulnérabilités (CVE) et incidents récents vérifiés sur le Web.
 */
class GeminiSearchGroundingService(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpExecutor: ((url: String, jsonBody: String) -> String)? = null
) {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Analyse en direct les vulnérabilités de sécurité et avis CERT/CVE connus
     * pour un équipement réseau détecté.
     */
    suspend fun auditDevice(device: NetworkDevice): Result<GroundingAuditResult> = withContext(ioDispatcher) {
        val portsDesc = if (device.openPorts.isNotEmpty()) {
            device.openPorts.joinToString(", ")
        } else {
            "aucun port standard exposé"
        }

        val prompt = buildString {
            appendLine("Tu es un expert senior en cybersécurité offensive et défensive.")
            appendLine("Effectue un audit de sécurité précis et à jour en exploitant la recherche Google (Google Search Grounding).")
            appendLine("Appareil analysé :")
            appendLine("- Fabricant / Constructeur : ${device.vendorName}")
            appendLine("- Nom détecté : ${device.displayName}")
            appendLine("- Catégorie : ${device.category.name}")
            appendLine("- Ports réseau ouverts : $portsDesc")
            appendLine("- Adresse IP locale : ${device.ipAddress}")
            appendLine()
            appendLine("Tâches requises :")
            appendLine("1. Recherche sur le Web les vulnérabilités récentes (failles CVE, firmware non sécurisé, exploits connus) pour ce fabricant/modèle et ces ports.")
            appendLine("2. Indique si cet appareil présente un risque d'espionnage, d'écoute ou de caméra cachée s'il s'agit d'un équipement vidéo/IoT.")
            appendLine("3. Fournis 3 recommandations concrètes et immédiates pour sécuriser cet équipement sur le réseau local.")
            appendLine("Sois synthétique, professionnel et cite les faits découverts.")
        }

        executeGroundingQuery(prompt)
    }

    /**
     * Analyse la réputation, les incidents de routage BGP et les pannes récentes
     * du fournisseur d'accès Internet (FAI) détecté.
     */
    suspend fun auditIsp(isp: IspInfo): Result<GroundingAuditResult> = withContext(ioDispatcher) {
        val prompt = buildString {
            appendLine("Tu es un spécialiste en ingénierie réseau et sécurité WAN.")
            appendLine("Effectue une analyse à jour avec Google Search (Search Grounding) sur l'opérateur et la liaison Internet suivante :")
            appendLine("- Fournisseur d'accès Internet (FAI) : ${isp.ispName}")
            appendLine("- Système Autonome (ASN) : ${isp.asn}")
            appendLine("- Organisation : ${isp.organization}")
            appendLine("- Localisation déclarée : ${isp.city}, ${isp.country}")
            appendLine("- IP Publique : ${isp.publicIp}")
            appendLine("- VPN / Proxy / Datacenter : ${if (isp.isVpnOrProxy) "OUI" else "NON"}")
            appendLine()
            appendLine("Tâches requises :")
            appendLine("1. Recherche les pannes récentes signalées ou coupures majeures de service pour cet opérateur.")
            appendLine("2. Vérifie la réputation de sécurité de cette plage IP / ASN (signalements spam, abuse, fuites de routes BGP).")
            appendLine("3. Donne un avis de confiance pour un usage professionnel ou sécurisé.")
        }

        executeGroundingQuery(prompt)
    }

    /**
     * Exécute une requête personnalisée sur le Web via gemini-3.5-flash avec l'outil googleSearch.
     */
    suspend fun executeGroundingQuery(prompt: String): Result<GroundingAuditResult> = withContext(ioDispatcher) {
        try {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Clé API Gemini non configurée. Veuillez renseigner GEMINI_API_KEY dans le panneau Secrets d'AI Studio pour activer l'audit Google Search Grounding."
                    )
                )
            }

            // Construction du payload JSON avec gemini-3.5-flash et l'outil googleSearch
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Activation explicite de Google Search Grounding
                val toolsArray = JSONArray().apply {
                    val searchTool = JSONObject().apply {
                        put("googleSearch", JSONObject())
                    }
                    put(searchTool)
                }
                put("tools", toolsArray)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val responseString = if (httpExecutor != null) {
                httpExecutor.invoke(url, requestJson.toString())
            } else {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorDetail = parseErrorMessage(body)
                        throw IllegalStateException("Erreur API Gemini (${response.code}) : $errorDetail")
                    }
                    body
                }
            }

            val parsedResult = parseGroundingResponse(prompt, responseString)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyse le JSON retourné par Gemini contenant les textes et les métadonnées de Grounding.
     */
    fun parseGroundingResponse(prompt: String, jsonString: String): GroundingAuditResult {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)

        // Extraction du texte de la réponse
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textBuilder = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i)
                val text = part?.optString("text", "") ?: ""
                if (text.isNotEmpty()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(text)
                }
            }
        }
        val summary = if (textBuilder.isNotEmpty()) {
            textBuilder.toString()
        } else {
            "Aucune analyse textuelle reçue de Gemini."
        }

        // Extraction des requêtes de recherche réelles (Grounding Metadata)
        val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
        val searchQueries = mutableListOf<String>()
        val webSearchQueries = groundingMetadata?.optJSONArray("webSearchQueries")
        if (webSearchQueries != null) {
            for (i in 0 until webSearchQueries.length()) {
                val query = webSearchQueries.optString(i, "").trim()
                if (query.isNotEmpty()) {
                    searchQueries.add(query)
                }
            }
        }

        // Extraction des sources web (Grounding Chunks)
        val sources = mutableListOf<GroundingWebSource>()
        val groundingChunks = groundingMetadata?.optJSONArray("groundingChunks")
        if (groundingChunks != null) {
            for (i in 0 until groundingChunks.length()) {
                val chunk = groundingChunks.optJSONObject(i)
                val web = chunk?.optJSONObject("web")
                if (web != null) {
                    val uri = web.optString("uri", "").trim()
                    val title = web.optString("title", uri).trim()
                    if (uri.isNotEmpty()) {
                        sources.add(GroundingWebSource(title = title.ifEmpty { uri }, url = uri))
                    }
                }
            }
        }

        return GroundingAuditResult(
            queryPrompt = prompt,
            summary = summary,
            searchQueries = searchQueries,
            sources = sources
        )
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val json = JSONObject(errorBody)
            val error = json.optJSONObject("error")
            error?.optString("message", errorBody) ?: errorBody
        } catch (_: Exception) {
            errorBody.take(200)
        }
    }
}
