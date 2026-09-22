package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.CardBrand
import com.example.billing.CardValidationResult
import com.example.billing.PaymentCard
import com.example.billing.PricingPlan
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PrimaryNavy

/**
 * Boîte de dialogue modale permettant le paiement par carte bancaire internationale
 * (Visa, Mastercard, American Express) avec validation algorithmique (Luhn).
 */
@Composable
fun CreditCardPaymentModal(
    selectedPlan: PricingPlan,
    isProcessing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmitPayment: (PaymentCard) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var expiryInput by remember { mutableStateOf("") } // MM/YY
    var cvcInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val detectedBrand by remember(cardNumber) {
        derivedStateOf { PaymentCard.detectBrand(cardNumber.replace(" ", "")) }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = PrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Paiement Carte Bancaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Offre : ${selectedPlan.title} (${selectedPlan.formattedPrice})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badge de marque détectée
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Carte acceptée :", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = detectedBrand.displayName,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Champ Numéro de Carte
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(16)
                        cardNumber = digits.chunked(4).joinToString(" ")
                    },
                    label = { Text("Numéro de carte") },
                    placeholder = { Text("4111 2222 3333 4444") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("card_number_input")
                )

                // Champ Nom du Titulaire
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Nom sur la carte") },
                    placeholder = { Text("JEAN DUPONT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("card_name_input")
                )

                // Ligne Date d'expiration & CVC
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiryInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(4)
                            expiryInput = if (digits.length >= 3) {
                                "${digits.take(2)}/${digits.drop(2)}"
                            } else {
                                digits
                            }
                        },
                        label = { Text("Date (MM/AA)") },
                        placeholder = { Text("12/28") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("card_expiry_input")
                    )

                    OutlinedTextField(
                        value = cvcInput,
                        onValueChange = { input ->
                            cvcInput = input.filter { it.isDigit() }.take(detectedBrand.cvcLength)
                        },
                        label = { Text("CVC") },
                        placeholder = { Text(if (detectedBrand == CardBrand.AMERICAN_EXPRESS) "4 chiffres" else "3 chiffres") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("card_cvc_input")
                    )
                }

                // Affichage d'erreur
                val displayError = localError ?: errorMessage
                if (displayError != null) {
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Mention sécurité
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transactions sécurisées chiffrées selon les normes bancaires ISO/IEC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localError = null
                    val cleanDigits = cardNumber.replace(" ", "")
                    val parts = expiryInput.split("/")
                    val month = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val year = parts.getOrNull(1)?.toIntOrNull() ?: 0

                    val card = PaymentCard(
                        rawNumber = cleanDigits,
                        cardholderName = cardholderName,
                        expiryMonth = month,
                        expiryYear = year,
                        cvc = cvcInput
                    )

                    val validation = card.validate()
                    if (validation is CardValidationResult.Invalid) {
                        localError = validation.reason
                    } else {
                        onSubmitPayment(card)
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNavy,
                    contentColor = AccentCyan
                ),
                modifier = Modifier.testTag("submit_card_payment_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentCyan, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Traitement...")
                } else {
                    Text("Payer ${selectedPlan.formattedPrice}")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Annuler")
            }
        }
    )
}
