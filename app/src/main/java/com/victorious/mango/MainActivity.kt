package com.victorious.mango

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import com.victorious.mango.router.GlInetRpcClient
import com.victorious.mango.router.RouterClient
import com.victorious.mango.ui.screens.*
import com.victorious.mango.ui.theme.*

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Status("status", "Status", Icons.Filled.Dashboard),
    Wifi("wifi", "Wi-Fi", Icons.Filled.Wifi),
    Vpn("vpn", "VPN", Icons.Filled.VpnKey),
    AdGuard("adguard", "AdGuard", Icons.Filled.Shield),
    Tether("tether", "Tether", Icons.Filled.Usb),
    Firmware("firmware", "Firmware", Icons.Filled.SystemUpdate),
}

class MainActivity : ComponentActivity() {
    private val client: RouterClient = GlInetRpcClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fleet = (application as VictorMangoApp).fleetTheme
        setContent {
            val fleetState by fleet.state.collectAsState()
            VictorMangoTheme(fleetState) { App(client) }
        }
    }
}

@Composable
fun App(client: RouterClient) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            Dest.entries.forEach { d ->
                NavigationBarItem(
                    selected = current == d.route,
                    onClick = { nav.navigate(d.route) { launchSingleTop = true; popUpTo(Dest.Status.route) } },
                    icon = { Icon(d.icon, d.label) }, label = { Text(d.label) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                )
            }
        }
    }) { pad ->
        NavHost(nav, Dest.Status.route, Modifier.padding(pad)) {
            composable(Dest.Status.route) { StatusScreen(client) }
            composable(Dest.Wifi.route) { WifiScreen(client) }
            composable(Dest.Vpn.route) { VpnScreen(client) }
            composable(Dest.AdGuard.route) { AdGuardScreen(client) }
            composable(Dest.Tether.route) { TetherScreen(client) }
            composable(Dest.Firmware.route) { FirmwareScreen(client) }
        }
    }
}
