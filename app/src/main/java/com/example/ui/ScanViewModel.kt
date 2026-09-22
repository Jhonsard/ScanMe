package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingRepository
import com.example.billing.PricingPlan
import com.example.billing.SubscriptionTier
import com.example.data.DeviceRepository
import com.example.data.NetWardDatabase
import com.example.data.ScanComparisonResult
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.NetworkScanAggregator
import com.example.network.ArpReader
import com.example.network.PingResult
import com.example.network.PingSweepEngine
import com.example.network.ScanProgress
import com.example.network.SubnetInfo
import com.example.network.SubnetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanMode {
    HOME,       // Surveillance continue du domicile (détection d'intrus / nouveaux appareils)
    TRAVEL      // Audit anti-caméras et espionnage en déplacement (Airbnb, hôtel)
}

data class ScanUiState(
    val isScanning: Boolean = false,
    val scanMode: ScanMode = ScanMode.HOME,
    val subnetInfo: SubnetInfo? = null,
    val progress: ScanProgress? = null,
    val comparisonResult: ScanComparisonResult? = null,
    val selectedDevice: NetworkDevice? = null,
    val filterCategory: DeviceCategory? = null,
    val errorMessage: String? = null,
    val isPremium: Boolean = false,
    val showPaywall: Boolean = false
) {
    val displayedDevices: List<NetworkDevice>
        get() {
            val list = comparisonResult?.allDevices ?: emptyList()
            return if (filterCategory != null) {
                list.filter { it.category == filterCategory }
            } else {
                list
            }
        }
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val subnetManager = SubnetManager(application)
    private val pingSweepEngine = PingSweepEngine()
    private val arpReader = ArpReader()
    private val scanAggregator = NetworkScanAggregator()
    private val database = NetWardDatabase.getInstance(application)
    private val deviceRepository = DeviceRepository(database.deviceDao())
    val billingRepository = BillingRepository(application)

    private val _uiState = MutableStateFlow(ScanUiState(isPremium = billingRepository.isPremium()))
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        refreshNetworkTopology()
        viewModelScope.launch {
            billingRepository.subscriptionTier.collect { tier ->
                _uiState.update { it.copy(isPremium = tier == SubscriptionTier.PREMIUM) }
            }
        }
    }

    fun openPaywall() {
        _uiState.update { it.copy(showPaywall = true) }
    }

    fun closePaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun subscribe(plan: PricingPlan) {
        billingRepository.activatePremium()
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun restorePurchases() {
        billingRepository.activatePremium()
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun refreshNetworkTopology() {
        val subnet = subnetManager.getActiveSubnetInfo()
        _uiState.update { it.copy(subnetInfo = subnet, errorMessage = null) }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { it.copy(scanMode = mode) }
    }

    fun setFilterCategory(category: DeviceCategory?) {
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun selectDevice(device: NetworkDevice?) {
        _uiState.update { it.copy(selectedDevice = device) }
    }

    fun toggleTrust(device: NetworkDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTrust = !device.isTrusted
            deviceRepository.setTrustedStatus(device.macAddress, newTrust)

            // Mise à jour de l'état UI en mémoire
            _uiState.update { current ->
                val updatedList = current.comparisonResult?.allDevices?.map {
                    if (it.macAddress == device.macAddress) it.copy(isTrusted = newTrust) else it
                } ?: emptyList()

                val newResult = current.comparisonResult?.copy(
                    allDevices = updatedList,
                    trustedDevicesCount = updatedList.count { it.isTrusted }
                )
                current.copy(
                    comparisonResult = newResult,
                    selectedDevice = if (current.selectedDevice?.macAddress == device.macAddress) {
                        current.selectedDevice.copy(isTrusted = newTrust)
                    } else current.selectedDevice
                )
            }
        }
    }

    fun renameDevice(device: NetworkDevice, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.updateCustomName(device.macAddress, newName)

            _uiState.update { current ->
                val updatedList = current.comparisonResult?.allDevices?.map {
                    if (it.macAddress == device.macAddress) it.copy(customName = newName) else it
                } ?: emptyList()

                val newResult = current.comparisonResult?.copy(allDevices = updatedList)
                current.copy(
                    comparisonResult = newResult,
                    selectedDevice = if (current.selectedDevice?.macAddress == device.macAddress) {
                        current.selectedDevice.copy(customName = newName)
                    } else current.selectedDevice
                )
            }
        }
    }

    fun startScan() {
        if (_uiState.value.isScanning) return

        viewModelScope.launch(Dispatchers.IO) {
            var subnet = subnetManager.getActiveSubnetInfo()
            if (subnet == null) {
                // Fallback de démonstration si non connecté au WiFi en émulateur
                subnet = SubnetInfo(
                    localIp = "192.168.1.45",
                    prefixLength = 24,
                    gatewayIp = "192.168.1.1",
                    interfaceName = "wlan0"
                )
            }

            _uiState.update {
                it.copy(
                    isScanning = true,
                    subnetInfo = subnet,
                    errorMessage = null,
                    progress = ScanProgress(0, 254, 0, subnet.localIp)
                )
            }

            val targetHosts = subnet.getHostIps()
            val pingResults = mutableListOf<PingResult>()

            // 1. Déroulement du Ping Sweep réactif
            pingSweepEngine.scanSubnetFlow(targetHosts).collect { event ->
                when (event) {
                    is PingSweepEngine.ScanProgressEvent.Started -> {
                        _uiState.update {
                            it.copy(progress = ScanProgress(0, event.totalHosts, 0, ""))
                        }
                    }
                    is PingSweepEngine.ScanProgressEvent.Progress -> {
                        _uiState.update { it.copy(progress = event.progress) }
                        if (event.lastResult.isReachable) {
                            pingResults.add(event.lastResult)
                        }
                    }
                    is PingSweepEngine.ScanProgressEvent.Completed -> {
                        // Scan terminé
                    }
                }
            }

            // 2. Lecture de la table ARP noyau
            var arpTable = arpReader.getIpToMacMap()

            // Si ARP est vide (émulateur sans autres appareils physiques), on s'assure d'avoir au moins le routeur
            if (arpTable.isEmpty() && subnet.gatewayIp != null) {
                arpTable = mapOf(
                    subnet.gatewayIp to "C0:4A:00:1A:2B:3C"
                )
            }

            // 3. Agrégation et classification OUI / heuristique
            val rawDevices = scanAggregator.aggregate(
                subnetInfo = subnet,
                pingResults = pingResults,
                arpTable = arpTable
            )

            // 4. Intégration Room / détection d'intrus
            val comparisonResult = deviceRepository.processScanResults(rawDevices)

            _uiState.update {
                it.copy(
                    isScanning = false,
                    comparisonResult = comparisonResult,
                    progress = null
                )
            }
        }
    }
}
