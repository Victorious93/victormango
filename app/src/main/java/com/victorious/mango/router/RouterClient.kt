package com.victorious.mango.router

/**
 * Abstraction over a GL.iNet / OpenWrt router.
 *
 * Primary transport: GL.iNet SDK 4.x JSON-RPC at `http://<host>/rpc`
 * (https://dev.gl-inet.com/router-4.x-api/). Flow:
 *  1. `challenge {username}` -> `{alg, salt, nonce}` (nonce valid ~1000 ms)
 *  2. cipher = unix crypt(password, "$alg$salt$")  (same value as /etc/shadow)
 *  3. hash = md5("username:cipher:nonce")
 *  4. `login {username, hash}` -> `{sid}` (sid idles out after 5 minutes)
 *  5. `call [sid, module, function, params]`
 *
 * TODO(ssh): add an SshRouterClient fallback (ubus/uci over SSH, e.g. via sshj) for
 * routers on 3.x firmware or features not exposed by /rpc. See
 * https://openwrt.org/docs/techref/ubus and https://openwrt.org/docs/guide-user/base-system/uci
 */
interface RouterClient {
    suspend fun login(host: String, username: String = "root", password: String): Session
    suspend fun logout()
    suspend fun systemStatus(): SystemStatus
    suspend fun wifiInterfaces(): List<WifiInterface>
    suspend fun setWifiEnabled(iface: String, enabled: Boolean)
    suspend fun wireGuardStatus(): VpnStatus
    suspend fun setWireGuardEnabled(enabled: Boolean)
    suspend fun adGuardStatus(): AdGuardStatus
    suspend fun setAdGuardEnabled(enabled: Boolean)
    suspend fun tetherStatus(): TetherStatus
    suspend fun firmwareInfo(): FirmwareInfo

    /** Raw escape hatch: `call [sid, module, function, params]`. Returns the JSON `result`. */
    suspend fun call(module: String, function: String, params: Map<String, Any?> = emptyMap()): String
}

enum class Transport { HTTP_RPC, SSH /* TODO */ }

data class Session(val host: String, val username: String, val sid: String, val transport: Transport = Transport.HTTP_RPC)
data class SystemStatus(val model: String, val hostname: String, val uptimeSec: Long, val cpuLoad: Double, val memUsedPct: Int, val clients: Int, val wanUp: Boolean)
data class WifiInterface(val name: String, val ssid: String, val band: String, val channel: Int, val enabled: Boolean, val encryption: String)
data class VpnStatus(val enabled: Boolean, val connected: Boolean, val peer: String?, val rxBytes: Long, val txBytes: Long)
data class AdGuardStatus(val enabled: Boolean, val dnsQueries: Long, val blocked: Long)
data class TetherStatus(val mode: String, val active: Boolean, val device: String?)
data class FirmwareInfo(val current: String, val latest: String?, val updateAvailable: Boolean)

class RouterException(message: String, val code: Int? = null) : Exception(message)
