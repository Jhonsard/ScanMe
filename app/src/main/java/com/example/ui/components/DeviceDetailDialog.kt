package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun DeviceDetailDialog(
    device: NetworkDevice,
    onDismiss: () -> Unit,
    onToggleTrust: (NetworkDevice) -> Unit,
    onRename: (NetworkDevice, String) -> Unit,
    onGoogleSearchAudit: ((NetworkDevice) -> Unit)? = null
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(device.customName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text("Fermer")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryBackgroundColor(device.category, device.threatLevel)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(device.category),
                            contentDescription = null,
                            tint = getCategoryIconColor(device.threatLevel),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.vendorName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Alerte de menace si suspect
                if (device.threatLevel == ThreatLevel.SUSPICIOUS && device.suspicionReason != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = device.suspicionReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Édition du nom personnalisé
                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Nom personnalisé") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_rename_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditingName = false }) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                onRename(device, editedName.trim())
                                isEditingName = false
                            },
                            modifier = Modifier.testTag("device_rename_save_button")
                        ) {
                            Text("Enregistrer")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (device.customName != null) "Nom : ${device.customName}" else "Aucun nom personnalisé",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { isEditingName = true },
                            modifier = Modifier.testTag("device_rename_toggle_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Renommer")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Détails techniques réseau
                DetailRow(label = "Adresse IP", value = device.ipAddress)
                DetailRow(label = "Adresse MAC", value = device.macAddress)
                DetailRow(label = "Fabricant", value = device.vendorName)
                DetailRow(
                    label = "Latence (RTT)",
                    value = if (device.responseTimeMs >= 0) "${device.responseTimeMs} ms" else "Non mesuré"
                )
                DetailRow(
                    label = "Ports ouverts",
                    value = if (device.openPorts.isNotEmpty()) device.openPorts.joinToString(", ") else "Aucun port standard ouvert"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton d'audit de sécurité Web avec Google Search Grounding (gemini-3.5-flash)
                Button(
                    onClick = { onGoogleSearchAudit?.invoke(device) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_google_search_audit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNavy,
                        contentColor = AccentCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audit Failles Web (Google Search AI)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bouton de marquage Appareil de confiance
                OutlinedButton(
                    onClick = { onToggleTrust(device) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_trust_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (device.isTrusted) SafeGreen else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (device.isTrusted) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (device.isTrusted) "Appareil de confiance (Validé)" else "Marquer comme appareil fiable"
                    )
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getCategoryIcon(category: DeviceCategory): ImageVector {
    return when (category) {
        DeviceCategory.ROUTER_GATEWAY -> Icons.Default.Router
        DeviceCategory.SMARTPHONE_TABLET -> Icons.Default.PhoneAndroid
        DeviceCategory.COMPUTER -> Icons.Default.Computer
        DeviceCategory.IP_CAMERA -> Icons.Default.CameraAlt
        DeviceCategory.SMART_HOME_IOT -> Icons.Default.Security
        DeviceCategory.ENTERTAINMENT -> Icons.Default.Computer
        DeviceCategory.PRINTER -> Icons.Default.DeviceUnknown
        DeviceCategory.UNKNOWN -> Icons.Default.DeviceUnknown
    }
}

fun getCategoryBackgroundColor(category: DeviceCategory, threat: ThreatLevel): Color {
    return when (threat) {
        ThreatLevel.SUSPICIOUS -> AlertRedContainer
        ThreatLevel.WARNING -> WarningAmberContainer
        ThreatLevel.SAFE -> SafeGreenContainer
        ThreatLevel.INFORMATIONAL -> Color(0xFFF1F5F9)
    }
}

fun getCategoryIconColor(threat: ThreatLevel): Color {
    return when (threat) {
        ThreatLevel.SUSPICIOUS -> AlertRed
        ThreatLevel.WARNING -> WarningAmber
        ThreatLevel.SAFE -> SafeGreen
        ThreatLevel.INFORMATIONAL -> Color(0xFF475569)
    }
}
