package com.example.network

/**
 * Modèle de données représentant l'infrastructure WAN et les informations du FAI (Fournisseur d'Accès Internet).
 * Réservé aux utilisateurs NetWard Pro (Mode Payant).
 */
data class IspInfo(
    val publicIp: String,
    val ispName: String,
    val organization: String,
    val asn: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val region: String,
    val reverseDns: String,
    val isVpnOrProxy: Boolean = false,
    val queryTimestamp: Long = System.currentTimeMillis()
) {
    val locationDisplay: String
        get() = if (city.isNotBlank() && country.isNotBlank()) "$city, $country" else country.ifBlank { "Inconnue" }

    val asnDisplay: String
        get() = if (asn.isNotBlank()) asn else "Non répertorié"

    val ispSummary: String
        get() = if (ispName.isNotBlank()) ispName else organization.ifBlank { "Fournisseur non identifié" }
}
