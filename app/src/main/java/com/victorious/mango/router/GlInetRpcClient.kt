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
 * GL.iNet 4.x `/rpc` client. Auth (challenge/login/call) is implemented per the official docs;
 * feature endpoints are STUBBED until verified on a real MT300N-V2 (module/function names
 * should be confirmed via the router web UI's network inspector).
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
        val s = session ?: throw RouterException("Not logged in")
        return rpc("call", JSONArray().put(s.sid).put(module).put(function).put(JSONObject(params))).opt("result").toString()
    }

    // ---- Stubs (TODO: map to real modules, e.g. system/get_status, wg-client, adguardhome, upgrade) ----
    override suspend fun systemStatus() = SystemStatus("GL-MT300N-V2", "GL-MT300N-V2", 0, 0.0, 0, 0, false)
    override suspend fun wifiInterfaces() = listOf(WifiInterface("wlan0", "GL-MT300N-V2-xxx", "2.4GHz", 6, true, "psk2"))
    override suspend fun setWifiEnabled(iface: String, enabled: Boolean) = Unit
    override suspend fun wireGuardStatus() = VpnStatus(false, false, null, 0, 0)
    override suspend fun setWireGuardEnabled(enabled: Boolean) = Unit
    override suspend fun adGuardStatus() = AdGuardStatus(false, 0, 0)
    override suspend fun setAdGuardEnabled(enabled: Boolean) = Unit
    override suspend fun tetherStatus() = TetherStatus("none", false, null)
    override suspend fun firmwareInfo() = FirmwareInfo("4.x", null, false)
}
