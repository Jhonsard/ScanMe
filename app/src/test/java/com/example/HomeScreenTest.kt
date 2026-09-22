package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.domain.DeviceCategory
import com.example.domain.NetworkDevice
import com.example.domain.ThreatLevel
import com.example.network.SubnetInfo
import com.example.ui.CategoryFilterChips
import com.example.ui.DeviceItemCard
import com.example.ui.NetworkTopologyCard
import com.example.ui.ScanMode
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNetworkTopologyCardDisplaysInfo() {
        val subnet = SubnetInfo(
            localIp = "192.168.1.15",
            prefixLength = 24,
            gatewayIp = "192.168.1.1",
            interfaceName = "wlan0"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                NetworkTopologyCard(subnet = subnet, scanMode = ScanMode.HOME)
            }
        }

        composeTestRule.onNodeWithTag("network_topology_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.1.15").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.1.1").assertIsDisplayed()
        composeTestRule.onNodeWithText("/24").assertIsDisplayed()
    }

    @Test
    fun testDeviceItemCardDisplaysSuspiciousBadge() {
        val camera = NetworkDevice(
            ipAddress = "192.168.1.102",
            macAddress = "E4:AA:EA:11:22:33",
            vendorName = "Dahua",
            category = DeviceCategory.IP_CAMERA,
            threatLevel = ThreatLevel.SUSPICIOUS,
            openPorts = listOf(554)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                DeviceItemCard(device = camera, onClick = {})
            }
        }

        composeTestRule.onNodeWithTag("device_card_192.168.1.102").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dahua").assertIsDisplayed()
        composeTestRule.onNodeWithText("SUSPECT").assertIsDisplayed()
    }
}
