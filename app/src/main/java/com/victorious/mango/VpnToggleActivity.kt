package com.victorious.mango

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.victorious.mango.router.GlInetRpcClient
import com.victorious.mango.router.PlannedToggle
import com.victorious.mango.router.RouterStore
import com.victorious.mango.router.planToggle
import com.victorious.mango.ui.theme.VictorMangoTheme
import com.victorious.mango.vpn.VpnStateCache
import com.victorious.mango.vpn.VpnToggleContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Public, confirm-gated entry used by static shortcuts and by VictorLaunch.
 * It never starts or stops the router's VPN client until the user confirms.
 */
class VpnToggleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = VpnToggleContract.requestOf(intent.action)
        if (request == null) {
            finish()
            return
        }
        val fleet = (application as VictorMangoApp).fleetTheme
        setContent {
            val fleetState by fleet.state.collectAsState()
            VictorMangoTheme(fleetState) {
                VpnToggleGate(request, onFinished = { finish() })
            }
        }
    }
}

private sealed interface GateState {
    data object Loading : GateState
    data class Confirm(val plan: PlannedToggle) : GateState
    data class Message(val title: String, val body: String) : GateState
}

@Composable
private fun VpnToggleGate(request: VpnToggleContract.Request, onFinished: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember { mutableStateOf<GateState>(GateState.Loading) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(request) {
        state = withContext(Dispatchers.IO) { prepare(context, request) }
    }

    when (val s = state) {
        GateState.Loading -> Dialog(onDismissRequest = { if (!busy) onFinished() }) {
            CircularProgressIndicator()
        }
        is GateState.Message -> AlertDialog(
            onDismissRequest = onFinished,
            title = { Text(s.title) },
            text = { Text(s.body) },
            confirmButton = { TextButton(onClick = onFinished) { Text("OK") } },
        )
        is GateState.Confirm -> AlertDialog(
            onDismissRequest = { if (!busy) onFinished() },
            title = { Text(s.plan.title) },
            text = { Text(s.plan.message) },
            confirmButton = {
                TextButton(
                    enabled = !busy && s.plan.blockedReason == null,
                    onClick = {
                        if (s.plan.rpc == null) {
                            onFinished()
                            return@TextButton
                        }
                        busy = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { applyConfirmed(context, s.plan) }
                            state = result
                            busy = false
                        }
                    },
                ) { Text(s.plan.confirmLabel) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = onFinished) { Text("Cancel") } },
        )
    }
}

private suspend fun prepare(context: android.content.Context, request: VpnToggleContract.Request): GateState {
    val store = RouterStore(context)
    val creds = store.load()
        ?: return GateState.Message(
            "Router VPN",
            "VictorMango has no saved router login, so it cannot name the router or VPN profile yet. Open VictorMango, connect to your GL.iNet router, then try again.",
        )
    return runCatching {
        val client = GlInetRpcClient()
        client.login(creds.host, creds.username, creds.password)
        val snap = client.vpnSnapshot()
        VpnStateCache.update(context, snap)
        val plan = planToggle(request, snap)
        if (plan.blockedReason != null) GateState.Message(plan.title, plan.message)
        else GateState.Confirm(plan)
    }.getOrElse { e ->
        GateState.Message(
            "Router VPN",
            "Could not reach the router at ${creds.host} to look up its name and VPN profile. ${e.message ?: e.javaClass.simpleName}",
        )
    }
}

private suspend fun applyConfirmed(context: android.content.Context, plan: PlannedToggle): GateState {
    val rpc = plan.rpc ?: return GateState.Message("Router VPN", "No change was made.")
    val store = RouterStore(context)
    val creds = store.load() ?: return GateState.Message("Router VPN", "The router login is no longer saved.")
    return runCatching {
        val client = GlInetRpcClient()
        client.login(creds.host, creds.username, creds.password)
        client.applyVpn(rpc)
        val snap = client.vpnSnapshot()
        VpnStateCache.update(context, snap)
        val verb = if (rpc.intent == com.victorious.mango.router.VpnIntent.START) "started" else "stopped"
        GateState.Message("Router VPN", "The VPN client on ${snap.displayRouter} was $verb (profile ${snap.displayProfile}).")
    }.getOrElse { e ->
        GateState.Message("Router VPN", "The router did not apply the change. ${e.message ?: e.javaClass.simpleName}")
    }
}
