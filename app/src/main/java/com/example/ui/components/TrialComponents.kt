package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.TrialState
import com.example.billing.TrialStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

/**
 * Bandeau dynamique affichant le décompte des 30 jours d'essai gratuit.
 */
@Composable
fun TrialBanner(
    trialState: TrialState,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val (bgContainerColor, contentColor, statusText) = when (trialState.status) {
        TrialStatus.ACTIVE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Essai gratuit : ${trialState.daysRemaining} jours restants"
        )
        TrialStatus.EXPIRING_SOON -> Triple(
            WarningAmberContainer,
            WarningAmber,
            "Attention : Votre essai expire dans ${trialState.daysRemaining} jour(s)"
        )
        TrialStatus.EXPIRED -> Triple(
            AlertRedContainer,
            AlertRed,
            if (trialState.isTampered) "Période d'essai révoquée (horloge modifiée)"
            else "Période d'essai de 30 jours expirée"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgContainerColor)
            .clickable { onUpgradeClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("trial_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (trialState.status == TrialStatus.EXPIRED) Icons.Default.Warning
                    else Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = statusText,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (trialState.status == TrialStatus.EXPIRED) "Passez à NetWard Pro pour relancer les scans"
                        else "Accès de base complet pendant 30 jours",
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "S'abonner",
                    color = bgContainerColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Dialogue d'alerte et de blocage affiché si la période d'essai de 30 jours est expirée.
 */
@Composable
fun TrialExpiredDialog(
    trialState: TrialState,
    onDismiss: () -> Unit,
    onSubscribeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Essai expiré",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (trialState.isTampered) "Accès restreint" else "Essai de 30 jours terminé",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (trialState.isTampered)
                        "Une anomalie de date système a été constatée. La période d'essai de 30 jours a été clôturée."
                    else
                        "Votre période d'évaluation gratuite de 30 jours de NetWard est arrivée à terme. Pour continuer à effectuer des audits réseau et débloquer l'analyse WAN/FAI avancée, veuillez souscrire à NetWard Pro."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Déblocage immédiat\n✓ Analyse FAI, IP WAN & ASN\n✓ Support carte bancaire (Visa, Mastercard, Amex)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubscribeClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                modifier = Modifier.testTag("expired_dialog_subscribe_button")
            ) {
                Text("Voir les forfaits Pro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
