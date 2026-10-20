package com.victorious.mango.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.victorious.mango.router.*

@Composable
private fun <T> load(client: RouterClient, block: suspend RouterClient.() -> T): T? {
    var v by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) { v = runCatching { client.block() }.getOrNull() }
    return v
}

@Composable
fun StatusScreen(client: RouterClient) = ScreenScaffold("Status", "Router overview") {
    val s = load(client) { systemStatus() }
    NeonCard("System") {
        InfoRow("Model", s?.model ?: "—"); InfoRow("Hostname", s?.hostname ?: "—")
        InfoRow("Uptime", "${s?.uptimeSec ?: 0}s"); InfoRow("CPU load", "${s?.cpuLoad ?: 0.0}")
        InfoRow("Memory", "${s?.memUsedPct ?: 0}%")
    }
    NeonCard("Network") { InfoRow("WAN", if (s?.wanUp == true) "Up" else "Down"); InfoRow("Clients", "${s?.clients ?: 0}") }
}

@Composable
fun WifiScreen(client: RouterClient) = ScreenScaffold("Wi-Fi", "Access points & radios") {
    val list = load(client) { wifiInterfaces() }.orEmpty()
    list.forEach { w ->
        var on by remember { mutableStateOf(w.enabled) }
        NeonCard(w.ssid) {
            InfoRow("Interface", w.name); InfoRow("Band", w.band); InfoRow("Channel", "${w.channel}"); InfoRow("Security", w.encryption)
            ToggleRow("Enabled", on) { on = it } // TODO: client.setWifiEnabled
        }
    }
}

@Composable
fun VpnScreen(client: RouterClient) = ScreenScaffold("VPN", "WireGuard client") {
    val v = load(client) { wireGuardStatus() }
    var on by remember(v) { mutableStateOf(v?.enabled ?: false) }
    NeonCard("WireGuard") {
        ToggleRow("Enabled", on) { on = it } // TODO: client.setWireGuardEnabled
        InfoRow("State", if (v?.connected == true) "Connected" else "Disconnected")
        InfoRow("Peer", v?.peer ?: "—"); InfoRow("RX / TX", "${v?.rxBytes ?: 0} / ${v?.txBytes ?: 0} B")
    }
    NeonCard("Profiles") { Text("TODO: import .conf / QR profile") }
}

@Composable
fun AdGuardScreen(client: RouterClient) = ScreenScaffold("AdGuard", "AdGuard Home DNS filtering") {
    val a = load(client) { adGuardStatus() }
    var on by remember(a) { mutableStateOf(a?.enabled ?: false) }
    NeonCard("AdGuard Home") {
        ToggleRow("Enabled", on) { on = it } // TODO: client.setAdGuardEnabled
        InfoRow("DNS queries", "${a?.dnsQueries ?: 0}"); InfoRow("Blocked", "${a?.blocked ?: 0}")
    }
}

@Composable
fun TetherScreen(client: RouterClient) = ScreenScaffold("Tether", "USB / phone tethering as WAN") {
    val t = load(client) { tetherStatus() }
    NeonCard("Tethering") { InfoRow("Mode", t?.mode ?: "—"); InfoRow("Active", "${t?.active ?: false}"); InfoRow("Device", t?.device ?: "—") }
}

@Composable
fun FirmwareScreen(client: RouterClient) = ScreenScaffold("Firmware", "GL.iNet / OpenWrt firmware") {
    val f = load(client) { firmwareInfo() }
    NeonCard("Version") {
        InfoRow("Installed", f?.current ?: "—"); InfoRow("Latest", f?.latest ?: "—")
        Button(onClick = { /* TODO: online upgrade */ }, enabled = f?.updateAvailable == true) { Text("Upgrade") }
    }
    NeonCard("Security") { Text("Firmware < 4.5.0 is affected by a login bypass (gl-inet/CVE-issues). Keep updated.") }
}
