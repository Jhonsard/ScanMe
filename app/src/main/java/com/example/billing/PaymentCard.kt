package com.example.billing

/**
 * Marques de cartes bancaires internationales supportées par NetWard.
 */
enum class CardBrand(val displayName: String, val cvcLength: Int, val maxNumberLength: Int) {
    VISA("Visa", 3, 16),
    MASTERCARD("Mastercard", 3, 16),
    AMERICAN_EXPRESS("American Express", 4, 15),
    UNKNOWN("Carte Bancaire", 3, 16)
}

/**
 * Modèle de données pour les cartes bancaires avec validation algorithmique.
 */
data class PaymentCard(
    val rawNumber: String,
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvc: String
) {
    val sanitizedNumber: String = rawNumber.replace("\\s".toRegex(), "").replace("-", "")

    val brand: CardBrand = detectBrand(sanitizedNumber)

    val last4: String = if (sanitizedNumber.length >= 4) sanitizedNumber.takeLast(4) else "****"

    /**
     * Valide l'ensemble des champs de la carte bancaire.
     */
    fun validate(): CardValidationResult {
        if (cardholderName.trim().length < 2) {
            return CardValidationResult.Invalid("Nom du titulaire incomplet")
        }

        if (sanitizedNumber.length < 13 || sanitizedNumber.length > 19) {
            return CardValidationResult.Invalid("Numéro de carte de longueur invalide")
        }

        if (!validateLuhn(sanitizedNumber)) {
            return CardValidationResult.Invalid("Numéro de carte bancaire invalide (Erreur Luhn)")
        }

        if (expiryMonth !in 1..12) {
            return CardValidationResult.Invalid("Mois d'expiration invalide")
        }

        val currentYear = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) % 100
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        val normalizedYear = if (expiryYear > 2000) expiryYear % 100 else expiryYear

        if (normalizedYear < currentYear || (normalizedYear == currentYear && expiryMonth < currentMonth)) {
            return CardValidationResult.Invalid("Carte bancaire expirée")
        }

        val requiredCvcLength = brand.cvcLength
        if (cvc.length != requiredCvcLength || !cvc.all { it.isDigit() }) {
            return CardValidationResult.Invalid("Code de sécurité CVC invalide ($requiredCvcLength chiffres requis)")
        }

        return CardValidationResult.Valid
    }

    companion object {
        /**
         * Détection de la marque par préfixe BIN international (Visa, Mastercard, Amex).
         */
        fun detectBrand(cleanedNumber: String): CardBrand {
            return when {
                cleanedNumber.startsWith("4") -> CardBrand.VISA
                cleanedNumber.startsWith("34") || cleanedNumber.startsWith("37") -> CardBrand.AMERICAN_EXPRESS
                isMastercardPrefix(cleanedNumber) -> CardBrand.MASTERCARD
                else -> CardBrand.UNKNOWN
            }
        }

        private fun isMastercardPrefix(number: String): Boolean {
            if (number.length < 2) return false
            val twoDigits = number.take(2).toIntOrNull() ?: return false
            if (twoDigits in 51..55) return true

            if (number.length >= 4) {
                val fourDigits = number.take(4).toIntOrNull() ?: return false
                if (fourDigits in 2221..2720) return true
            }
            return false
        }

        /**
         * Vérification de somme de contrôle par l'algorithme de Luhn (Norme ISO/IEC 7812).
         */
        fun validateLuhn(number: String): Boolean {
            if (number.isBlank() || !number.all { it.isDigit() }) return false

            var sum = 0
            var alternate = false
            for (i in number.length - 1 downTo 0) {
                var digit = number[i].digitToInt()
                if (alternate) {
                    digit *= 2
                    if (digit > 9) digit -= 9
                }
                sum += digit
                alternate = !alternate
            }
            return sum % 10 == 0
        }
    }
}

sealed class CardValidationResult {
    data object Valid : CardValidationResult()
    data class Invalid(val reason: String) : CardValidationResult()
}
