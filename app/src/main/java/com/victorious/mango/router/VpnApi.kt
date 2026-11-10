package com.victorious.mango.router

import com.victorious.mango.vpn.VpnToggleContract
import org.json.JSONObject

/**
 * Pure helpers for the official GL.iNet 4.x WireGuard / OpenVPN *client* RPC
 * (`wg-client` / `ovpn-client` `start`, `stop`, `get_status`).
 *
 * Status codes from the official SDK 4.0 examples: 0 not connected, 1 connected, 2 connecting.
 * Start/stop parameter shapes also come from those examples:
 *  - wg-client.start {group_id, peer_id}; wg-client.stop {}
 *  - ovpn-client.start {group_id, client_id}; ovpn-client.stop {group_id, client_id}
 *  - both get_status {}
 */
enum class VpnProtocol(val wireName: String, val module: String) {
    WIREGUARD("wireguard", "wg-client"),
    OPENVPN("openvpn", "ovpn-client"),
}

enum class VpnLink { DISCONNECTED, CONNECTED, CONNECTING }

data class VpnClientReport(
    val protocol: VpnProtocol,
    val link: VpnLink,
    val profileName: String? = null,
    val groupId: Int? = null,
    val configId: Int? = null,
    val domain: String? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
) {
    val isUp: Boolean get() = link == VpnLink.CONNECTED || link == VpnLink.CONNECTING
    val hasStartIds: Boolean get() = (groupId ?: 0) != 0 && (configId ?: 0) != 0
    val displayProfile: String
        get() = profileName?.takeIf { it.isNotBlank() }
            ?: domain?.takeIf { it.isNotBlank() }
            ?: if (hasStartIds) "${protocolLabel()} #$configId" else "No profile selected"

    fun protocolLabel(): String = when (protocol) {
        VpnProtocol.WIREGUARD -> "WireGuard"
        VpnProtocol.OPENVPN -> "OpenVPN"
    }
}

data class RouterVpnSnapshot(
    val host: String,
    val routerName: String,
    val wireguard: VpnClientReport? = null,
    val openvpn: VpnClientReport? = null,
    val error: String? = null,
) {
    val active: VpnClientReport? get() = listOfNotNull(wireguard, openvpn).firstOrNull { it.isUp }

    val lastUsed: VpnClientReport?
        get() = active ?: listOfNotNull(wireguard, openvpn).firstOrNull { it.hasStartIds }

    val displayRouter: String get() = routerName.ifBlank { host }.ifBlank { "Unknown router" }

    val displayProfile: String
        get() = (active ?: lastUsed)?.let { "${it.protocolLabel()} · ${it.displayProfile}" }
            ?: "No profile selected"

    val connected: Boolean get() = active?.link == VpnLink.CONNECTED
    val enabled: Boolean get() = active?.isUp == true
    val statusLabel: String
        get() = when (active?.link) {
            VpnLink.CONNECTED -> "connected"
            VpnLink.CONNECTING -> "connecting"
            VpnLink.DISCONNECTED, null -> "disconnected"
        }
}

enum class VpnIntent { START, STOP }

/**
 * What the confirm dialog should offer, and which official RPC (if any) to run *after* confirm.
 * [rpc] is null when the API must not be called (already in the requested state, or blocked).
 */
data class PlannedToggle(
    val request: VpnToggleContract.Request,
    val title: String,
    val message: String,
    val confirmLabel: String,
    val rpc: PlannedRpc?,
    val blockedReason: String? = null,
    val alreadyInState: Boolean = false,
)

data class PlannedRpc(
    val intent: VpnIntent,
    val protocol: VpnProtocol,
    val groupId: Int?,
    val configId: Int?,
)

fun parseVpnLink(status: Int?): VpnLink = when (status) {
    0 -> VpnLink.DISCONNECTED
    1 -> VpnLink.CONNECTED
    2 -> VpnLink.CONNECTING
    else -> VpnLink.DISCONNECTED
}

fun parseVpnClientStatus(protocol: VpnProtocol, result: JSONObject): VpnClientReport {
    val link = parseVpnLink(if (result.has("status") && !result.isNull("status")) result.optInt("status") else null)
    val groupId = presentPositiveInt(result, "group_id")
    val configId = when (protocol) {
        VpnProtocol.WIREGUARD -> presentPositiveInt(result, "peer_id")
        VpnProtocol.OPENVPN -> presentPositiveInt(result, "client_id")
    }
    return VpnClientReport(
        protocol = protocol,
        link = link,
        profileName = presentString(result, "name"),
        groupId = groupId,
        configId = configId,
        domain = presentString(result, "domain"),
        rxBytes = result.optLong("rx_bytes", 0),
        txBytes = result.optLong("tx_bytes", 0),
    )
}

fun parseVpnClientStatus(protocol: VpnProtocol, json: String): VpnClientReport =
    parseVpnClientStatus(protocol, JSONObject(json))

/** First usable profile from official `get_all_config_list` when get_status has no last-used ids. */
fun firstProfileFromConfigList(protocol: VpnProtocol, result: JSONObject): VpnClientReport? {
    val list = result.optJSONArray("config_list") ?: return null
    for (i in 0 until list.length()) {
        val group = list.optJSONObject(i) ?: continue
        val groupId = presentPositiveInt(group, "group_id") ?: continue
        val groupName = presentString(group, "group_name")
        when (protocol) {
            VpnProtocol.WIREGUARD -> {
                val peers = group.optJSONArray("peers") ?: continue
                for (j in 0 until peers.length()) {
                    val peer = peers.optJSONObject(j) ?: continue
                    val peerId = presentPositiveInt(peer, "peer_id") ?: continue
                    val name = presentString(peer, "name") ?: groupName
                    return VpnClientReport(protocol, VpnLink.DISCONNECTED, name, groupId, peerId)
                }
            }
            VpnProtocol.OPENVPN -> {
                val clients = group.optJSONArray("clients") ?: continue
                for (j in 0 until clients.length()) {
                    val client = clients.optJSONObject(j) ?: continue
                    val clientId = presentPositiveInt(client, "client_id") ?: continue
                    val name = presentString(client, "name") ?: groupName
                    return VpnClientReport(protocol, VpnLink.DISCONNECTED, name, groupId, clientId)
                }
            }
        }
    }
    return null
}

fun startParams(protocol: VpnProtocol, groupId: Int, configId: Int): JSONObject = JSONObject().apply {
    put("group_id", groupId)
    when (protocol) {
        VpnProtocol.WIREGUARD -> put("peer_id", configId)
        VpnProtocol.OPENVPN -> put("client_id", configId)
    }
}

/** Official ovpn-client.stop takes group_id+client_id; wg-client.stop takes no params. */
fun stopParams(protocol: VpnProtocol, groupId: Int?, configId: Int?): JSONObject = when (protocol) {
    VpnProtocol.WIREGUARD -> JSONObject()
    VpnProtocol.OPENVPN -> JSONObject().apply {
        if (groupId != null) put("group_id", groupId)
        if (configId != null) put("client_id", configId)
    }
}

fun parseRouterIdentity(info: JSONObject, fallbackHost: String): String {
    val board = info.optJSONObject("board_info")
    val hostname = presentString(board, "hostname")
    val model = presentString(info, "model") ?: presentString(board, "model")
    return hostname ?: model ?: fallbackHost
}

fun planToggle(request: VpnToggleContract.Request, snap: RouterVpnSnapshot): PlannedToggle {
    val wantOn = when (request) {
        VpnToggleContract.Request.ON -> true
        VpnToggleContract.Request.OFF -> false
        VpnToggleContract.Request.TOGGLE -> snap.active == null
    }
    val router = snap.displayRouter
    val profile = snap.displayProfile
    return if (wantOn) {
        val target = snap.lastUsed
        when {
            snap.active != null -> PlannedToggle(
                request, "Turn Router VPN on?",
                "Router VPN is already on for $router using profile $profile.",
                "Turn on", rpc = null, alreadyInState = true,
            )
            target == null || !target.hasStartIds -> PlannedToggle(
                request, "Turn Router VPN on?",
                "No WireGuard or OpenVPN client profile was found on $router.",
                "Turn on", rpc = null, blockedReason = "No VPN profile on $router",
            )
            else -> PlannedToggle(
                request, "Turn Router VPN on?",
                "This will start the VPN client on $router using profile ${target.protocolLabel()} · ${target.displayProfile}.",
                "Turn on",
                rpc = PlannedRpc(VpnIntent.START, target.protocol, target.groupId, target.configId),
            )
        }
    } else {
        val target = snap.active ?: snap.lastUsed
        when {
            snap.active == null -> PlannedToggle(
                request, "Turn Router VPN off?",
                "Router VPN is already off on $router (profile $profile).",
                "Turn off", rpc = null, alreadyInState = true,
            )
            target == null -> PlannedToggle(
                request, "Turn Router VPN off?",
                "No VPN client is running on $router.",
                "Turn off", rpc = null, alreadyInState = true,
            )
            else -> PlannedToggle(
                request, "Turn Router VPN off?",
                "This will stop the VPN client on $router (profile ${target.protocolLabel()} · ${target.displayProfile}).",
                "Turn off",
                rpc = PlannedRpc(VpnIntent.STOP, target.protocol, target.groupId, target.configId),
            )
        }
    }
}

fun snapshotStateRow(snap: RouterVpnSnapshot, updatedAtMs: Long): Map<String, Any?> = VpnToggleContract.stateRow(
    connected = snap.connected,
    enabled = snap.enabled,
    protocol = snap.active?.protocol?.wireName.orEmpty(),
    profile = (snap.active ?: snap.lastUsed)?.displayProfile.orEmpty(),
    router = snap.displayRouter,
    host = snap.host,
    status = snap.statusLabel,
    updatedAtMs = updatedAtMs,
)

internal fun presentPositiveInt(obj: JSONObject?, key: String): Int? {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return null
    val v = obj.optInt(key, 0)
    return v.takeIf { it != 0 }
}

internal fun presentString(obj: JSONObject?, key: String): String? {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return null
    return obj.optString(key).takeIf { it.isNotBlank() && it != "null" }
}
