package com.victorious.mango.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.victorious.mango.router.*
import com.victorious.mango.vpn.VpnStateCache
import kotlinx.coroutines.launch

@Composable
private fun <T> load(client: RouterClient, block: suspend RouterClient.() -> T): T? {
    var v by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) { v = runCatching { client.block() }.getOrNull() }
    return v
}

@Composable
fun StatusScreen(client: RouterClient) = ScreenScaffold("Status", "Router overview") {
    RouterLoginCard(client)
    val s = load(client) { systemStatus() }
    NeonCard("System") {
        InfoRow("Model", s?.model ?: "—"); InfoRow("Hostname", s?.hostname ?: "—")
        InfoRow("Uptime", "${s?.uptimeSec ?: 0}s"); InfoRow("CPU load", "${s?.cpuLoad ?: 0.0}")
        InfoRow("Memory", "${s?.memUsedPct ?: 0}%")
    }
    NeonCard("Network") { InfoRow("WAN", if (s?.wanUp == true) "Up" else "Down"); InfoRow("Clients", "${s?.clients ?: 0}") }
}

@Composable
private fun RouterLoginCard(client: RouterClient) {
    val context = LocalContext.current
    val store = remember { RouterStore(context) }
    val saved = remember { store.load() }
    var host by remember { mutableStateOf(saved?.host ?: RouterStore.DEFAULT_HOST) }
    var user by remember { mutableStateOf(saved?.username ?: "root") }
    var pass by remember { mutableStateOf(saved?.password ?: "") }
    var message by remember { mutableStateOf(if (saved != null) "Saved login for ${saved.host}" else "Not connected") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    NeonCard("Router") {
        OutlinedTextField(host, { host = it }, label = { Text("Host") }, singleLine = true)
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true)
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Text(message)
        Button(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val result = runCatching {
                    client.login(host, user, pass)
                    store.save(host, user, pass)
                    val snap = client.vpnSnapshot()
                    VpnStateCache.update(context, snap)
                    "Connected to ${snap.displayRouter}"
                }
                message = result.getOrElse { "Login failed: ${it.message ?: it.javaClass.simpleName}" }
                busy = false
            }
        }) { Text("Connect") }
    }
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
fun VpnScreen(client: RouterClient) = ScreenScaffold("VPN", "Router WireGuard / OpenVPN client") {
    val snap = load(client) { vpnSnapshot() }
    NeonCard("Router VPN") {
        InfoRow("Router", snap?.displayRouter ?: "—")
        InfoRow("State", snap?.statusLabel ?: "—")
        InfoRow("Profile", snap?.displayProfile ?: "—")
        val v = snap?.active ?: snap?.wireguard ?: snap?.openvpn
        InfoRow("RX / TX", "${v?.rxBytes ?: 0} / ${v?.txBytes ?: 0} B")
        Text("Use the Router VPN on/off shortcuts or VictorLaunch to change this. Each change asks for confirmation.")
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
