package com.victorious.mango.router

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GL.iNet 4.x `/rpc` client. Auth (challenge/login/call) follows the official SDK 4.0 docs.
 * WireGuard and OpenVPN *client* start/stop/get_status use the official `wg-client` and
 * `ovpn-client` modules. Other feature endpoints remain stubbed until verified on a device.
 */
class GlInetRpcClient(
    private val http: OkHttpClient = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build(),
) : RouterClient {
    private var session: Session? = null
    private var nextId = 1

    private suspend fun rpc(method: String, params: Any): JSONObject = withContext(Dispatchers.IO) {
        val host = session?.host ?: pendingHost ?: throw RouterException("No host")
        val body = JSONObject().put("jsonrpc", "2.0").put("id", nextId++).put("method", method).put("params", params)
        val req = Request.Builder().url("http://$host/rpc")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        http.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string() ?: throw RouterException("Empty response"))
            json.optJSONObject("error")?.let { throw RouterException(it.optString("message"), it.optInt("code")) }
            json
        }
    }
    private var pendingHost: String? = null

    override suspend fun login(host: String, username: String, password: String): Session {
        pendingHost = host
        // Steps 2-4 must finish within ~1000 ms of the challenge.
        val ch = rpc("challenge", JSONObject().put("username", username)).getJSONObject("result")
        val cipher = GlInetAuth.cipherPassword(ch.getInt("alg"), ch.getString("salt"), password)
        val hash = GlInetAuth.loginHash(username, cipher, ch.getString("nonce"))
        val res = rpc("login", JSONObject().put("username", username).put("hash", hash)).getJSONObject("result")
        return Session(host, username, res.getString("sid")).also { session = it }
    }

    override suspend fun logout() {
        session?.let { runCatching { rpc("logout", JSONObject().put("sid", it.sid)) } }
        session = null
    }

    override suspend fun call(module: String, function: String, params: Map<String, Any?>): String {
        return callResult(module, function, JSONObject(params)).toString()
    }

    private suspend fun callResult(module: String, function: String, params: JSONObject = JSONObject()): JSONObject {
        val s = session ?: throw RouterException("Not logged in")
        val raw = rpc("call", JSONArray().put(s.sid).put(module).put(function).put(params)).opt("result")
        val obj = when (raw) {
            is JSONObject -> raw
            JSONObject.NULL, null -> JSONObject()
            else -> JSONObject().put("value", raw)
        }
        if (obj.has("err_code") && obj.optInt("err_code") != 0) {
            throw RouterException(obj.optString("err_msg").ifBlank { "RPC error" }, obj.optInt("err_code"))
        }
        return obj
    }

    // ---- Stubs (TODO: map remaining modules, e.g. wifi, adguardhome, tethering, upgrade) ----
    override suspend fun systemStatus() = SystemStatus("GL-MT300N-V2", "GL-MT300N-V2", 0, 0.0, 0, 0, false)
    override suspend fun wifiInterfaces() = listOf(WifiInterface("wlan0", "GL-MT300N-V2-xxx", "2.4GHz", 6, true, "psk2"))
    override suspend fun setWifiEnabled(iface: String, enabled: Boolean) = Unit
    override suspend fun adGuardStatus() = AdGuardStatus(false, 0, 0)
    override suspend fun setAdGuardEnabled(enabled: Boolean) = Unit
    override suspend fun tetherStatus() = TetherStatus("none", false, null)
    override suspend fun firmwareInfo() = FirmwareInfo("4.x", null, false)

    override suspend fun routerIdentity(): String {
        val info = runCatching { callResult("system", "get_info") }.getOrNull() ?: return session?.host ?: "router"
        return parseRouterIdentity(info, session?.host ?: "router")
    }

    override suspend fun vpnSnapshot(): RouterVpnSnapshot {
        val host = session?.host ?: throw RouterException("Not logged in")
        val name = runCatching { routerIdentity() }.getOrDefault(host)
        val wg = clientReport(VpnProtocol.WIREGUARD)
        val ov = clientReport(VpnProtocol.OPENVPN)
        return RouterVpnSnapshot(host, name, wg, ov)
    }

    override suspend fun wireGuardStatus(): VpnStatus {
        val snap = vpnSnapshot()
        val r = snap.wireguard
        return VpnStatus(
            enabled = r?.isUp == true,
            connected = r?.link == VpnLink.CONNECTED,
            peer = r?.displayProfile,
            rxBytes = r?.rxBytes ?: 0,
            txBytes = r?.txBytes ?: 0,
            protocol = r?.protocol?.wireName,
        )
    }

    override suspend fun setWireGuardEnabled(enabled: Boolean) {
        val snap = vpnSnapshot()
        val plan = planToggle(if (enabled) com.victorious.mango.vpn.VpnToggleContract.Request.ON else com.victorious.mango.vpn.VpnToggleContract.Request.OFF, snap)
        val rpc = plan.rpc ?: return
        if (rpc.protocol != VpnProtocol.WIREGUARD) return
        applyVpn(rpc)
    }

    override suspend fun applyVpn(rpc: PlannedRpc) {
        val params = when (rpc.intent) {
            VpnIntent.START -> {
                val gid = rpc.groupId ?: throw RouterException("Missing group_id")
                val cid = rpc.configId ?: throw RouterException("Missing profile id")
                startParams(rpc.protocol, gid, cid)
            }
            VpnIntent.STOP -> stopParams(rpc.protocol, rpc.groupId, rpc.configId)
        }
        callResult(rpc.protocol.module, if (rpc.intent == VpnIntent.START) "start" else "stop", params)
    }

    private suspend fun clientReport(protocol: VpnProtocol): VpnClientReport? {
        val status = runCatching { parseVpnClientStatus(protocol, callResult(protocol.module, "get_status")) }.getOrNull()
        if (status?.hasStartIds == true) return status
        val listed = runCatching { firstProfileFromConfigList(protocol, callResult(protocol.module, "get_all_config_list")) }.getOrNull()
        return when {
            status == null -> listed
            listed == null -> status
            else -> status.copy(groupId = listed.groupId, configId = listed.configId, profileName = status.profileName ?: listed.profileName)
        }
    }
}
