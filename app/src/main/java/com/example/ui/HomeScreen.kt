package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import com.example.network.SubnetInfo
import com.example.ui.components.CreditCardPaymentModal
import com.example.ui.components.DeviceDetailDialog
import com.example.ui.components.IspDetailsCard
import com.example.ui.components.PaywallDialog
import com.example.ui.components.TrialBanner
import com.example.ui.components.TrialExpiredDialog
import com.example.ui.components.getCategoryBackgroundColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.getCategoryIconColor
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Logo NetWard",
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NetWard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.scanMode == ScanMode.HOME) "Protection Domicile" else "Audit Voyage & Airbnb",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentCyan
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.isPremium) SafeGreen else AccentCyan)
                            .clickable { viewModel.openPaywall() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("topbar_pro_badge")
                    ) {
                        Text(
                            text = if (uiState.isPremium) "PRO ACTIF" else "PASSER PRO",
                            color = PrimaryNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshNetworkTopology() },
                        enabled = !uiState.isScanning,
                        modifier = Modifier.testTag("refresh_network_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rafraîchir le réseau",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sélecteur de mode : Domicile vs Voyage / Airbnb
            TabRow(
                selectedTabIndex = if (uiState.scanMode == ScanMode.HOME) 0 else 1,
                containerColor = PrimaryNavy,
                contentColor = AccentCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.scanMode == ScanMode.HOME,
                    onClick = { viewModel.setScanMode(ScanMode.HOME) },
                    text = { Text("Mode Domicile", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.testTag("mode_home_tab")
                )
                Tab(
                    selected = uiState.scanMode == ScanMode.TRAVEL,
                    onClick = { viewModel.setScanMode(ScanMode.TRAVEL) },
                    text = { Text("Audit Voyage / Airbnb", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) },
                    modifier = Modifier.testTag("mode_travel_tab")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. Bandeau de période d'essai de 30 jours
                uiState.trialState?.let { trial ->
                    item {
                        TrialBanner(
                            trialState = trial,
                            isPremium = uiState.isPremium,
                            onUpgradeClick = { viewModel.openPaywall() }
                        )
                    }
                }

                // 1. Carte d'état de la topologie réseau
                item {
                    NetworkTopologyCard(
                        subnet = uiState.subnetInfo,
                        scanMode = uiState.scanMode
                    )
                }

                // 1bis. Carte d'analyse WAN / FAI Pro
                item {
                    IspDetailsCard(
                        ispInfo = uiState.ispInfo,
                        isLoading = uiState.isLoadingIsp,
                        isPremium = uiState.isPremium,
                        errorMessage = uiState.ispErrorMessage,
                        onRefreshIsp = { viewModel.fetchIspDetails() },
                        onUnlockPro = { viewModel.openPaywall() }
                    )
                }

                // 2. Bannière de verdict ou barre de progression
                item {
                    SecurityStatusSection(
                        uiState = uiState,
                        onStartScan = { viewModel.startScan() }
                    )
                }

                // 3. Filtres par catégorie
                if (uiState.comparisonResult != null) {
                    item {
                        CategoryFilterChips(
                            selected = uiState.filterCategory,
                            onSelect = { viewModel.setFilterCategory(it) }
                        )
                    }
                }

                // 4. Liste des équipements découverts
                val devices = uiState.displayedDevices
                if (devices.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Appareils détectés (${devices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cliquez pour inspecter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(devices, key = { it.macAddress }) { device ->
                        DeviceItemCard(
                            device = device,
                            onClick = { viewModel.selectDevice(device) }
                        )
                    }
                } else if (!uiState.isScanning && uiState.comparisonResult == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucun scan effectué",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Lancez un audit pour identifier les hôtes et caméras.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Boîte de dialogue détaillée de l'appareil
        uiState.selectedDevice?.let { device ->
            DeviceDetailDialog(
                device = device,
                onDismiss = { viewModel.selectDevice(null) },
                onToggleTrust = { viewModel.toggleTrust(it) },
                onRename = { dev, name -> viewModel.renameDevice(dev, name) }
            )
        }

        // Fenêtre d'abonnement / Paywall NetWard Pro
        if (uiState.showPaywall) {
            PaywallDialog(
                plans = viewModel.billingRepository.availablePlans,
                isCurrentlyPremium = uiState.isPremium,
                onDismiss = { viewModel.closePaywall() },
                onSubscribe = { plan -> viewModel.openCardPayment(plan) },
                onRestore = { viewModel.restorePurchases() }
            )
        }

        // Fenêtre de paiement par carte bancaire (Visa, Mastercard, Amex)
        if (uiState.showCardPaymentModal && uiState.selectedPlanForPayment != null) {
            CreditCardPaymentModal(
                selectedPlan = uiState.selectedPlanForPayment!!,
                isProcessing = uiState.isProcessingPayment,
                errorMessage = uiState.paymentErrorMessage,
                onDismiss = { viewModel.closeCardPayment() },
                onSubmitPayment = { card ->
                    viewModel.processCardPayment(card, uiState.selectedPlanForPayment!!)
                }
            )
        }

        // Fenêtre d'alerte expiration essai 30 jours
        if (uiState.showTrialExpiredDialog && uiState.trialState != null) {
            TrialExpiredDialog(
                trialState = uiState.trialState!!,
                onDismiss = { viewModel.dismissTrialExpiredDialog() },
                onSubscribeClick = {
                    viewModel.dismissTrialExpiredDialog()
                    viewModel.openPaywall()
                }
            )
        }
    }
}

@Composable
fun NetworkTopologyCard(
    subnet: SubnetInfo?,
    scanMode: ScanMode
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network_topology_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Réseau Local Actif",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Interface : ${subnet?.interfaceName ?: "wlan0"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(title = "IP Téléphone", value = subnet?.localIp ?: "192.168.1.45")
                InfoColumn(title = "Passerelle Box", value = subnet?.gatewayIp ?: "192.168.1.1")
                InfoColumn(title = "Masque", value = "/${subnet?.prefixLength ?: 24}")
            }
        }
    }
}

@Composable
private fun InfoColumn(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SecurityStatusSection(
    uiState: ScanUiState,
    onStartScan: () -> Unit
) {
    val comparison = uiState.comparisonResult
    val progress = uiState.progress

    Column(modifier = Modifier.fillMaxWidth()) {
        if (uiState.isScanning && progress != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanning_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audit réseau en cours...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progress.progressPercentage * 100).toInt()} %",
                            color = AccentCyan,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentCyan,
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hôte sondé : ${progress.currentIp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${progress.activeHostsFound} actifs trouvés",
                            style = MaterialTheme.typography.bodySmall,
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (comparison != null) {
            // Bannière de verdict après analyse
            val hasCamera = comparison.hasSuspiciousCameras
            val hasIntruders = comparison.hasIntrudersOrUnknown

            val bannerColor = when {
                hasCamera -> AlertRedContainer
                hasIntruders -> WarningAmberContainer
                else -> SafeGreenContainer
            }

            val iconColor = when {
                hasCamera -> AlertRed
                hasIntruders -> WarningAmber
                else -> SafeGreen
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bannerColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_verdict_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            hasCamera -> Icons.Default.CameraAlt
                            hasIntruders -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                hasCamera -> "Alerte : Caméra suspecte détectée !"
                                hasIntruders -> "Attention : Nouveaux appareils inconnus"
                                else -> "Réseau protégé et sain"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                        Text(
                            text = when {
                                hasCamera -> "${comparison.suspiciousCameras.size} équipement(s) de streaming/surveillance identifié(s)."
                                hasIntruders -> "${comparison.newDevices.size} nouvel(s) intrus potentiel(s) détecté(s) sur la box."
                                else -> "Tous les appareils connectés (${comparison.totalDevicesCount}) sont authentifiés."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bouton d'action principal
        Button(
            onClick = onStartScan,
            enabled = !uiState.isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryNavy,
                contentColor = AccentCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.isScanning) "Scan en cours..." else if (uiState.scanMode == ScanMode.TRAVEL) "Lancer l'audit anti-caméras" else "Lancer l'analyse du réseau",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CategoryFilterChips(
    selected: DeviceCategory?,
    onSelect: (DeviceCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filter_chips_row")
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Tous") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue
                )
            )
        }
        item {
            FilterChip(
                selected = selected == DeviceCategory.IP_CAMERA,
                onClick = { onSelect(if (selected == DeviceCategory.IP_CAMERA) null else DeviceCategory.IP_CAMERA) },
                label = { Text("Caméras & Espion") },
                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selected == DeviceCategory.ROUTER_GATEWAY,
                onClick = { onSelect(if (selected == DeviceCategory.ROUTER_GATEWAY) null else DeviceCategory.ROUTER_GATEWAY) },
                label = { Text("Routeurs") }
            )
        }
        item {
            FilterChip(
                selected = selected == DeviceCategory.SMARTPHONE_TABLET,
                onClick = { onSelect(if (selected == DeviceCategory.SMARTPHONE_TABLET) null else DeviceCategory.SMARTPHONE_TABLET) },
                label = { Text("Mobiles") }
            )
        }
        item {
            FilterChip(
                selected = selected == DeviceCategory.COMPUTER,
                onClick = { onSelect(if (selected == DeviceCategory.COMPUTER) null else DeviceCategory.COMPUTER) },
                label = { Text("Ordinateurs") }
            )
        }
    }
}

@Composable
fun DeviceItemCard(
    device: NetworkDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("device_card_${device.ipAddress}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône avec badge de menace
            Box(
                modifier = Modifier
                    .size(44.dp)
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (device.threatLevel == ThreatLevel.SUSPICIOUS) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AlertRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SUSPECT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text(
                    text = "${device.ipAddress} • ${device.macAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (device.openPorts.isNotEmpty()) {
                    Text(
                        text = "Ports ouverts : ${device.openPorts.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.openPorts.contains(554)) AlertRed else AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Statut de confiance
            if (device.isTrusted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Appareil approuvé",
                    tint = SafeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
