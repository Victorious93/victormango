package com.victorious.mango.vpn

import com.victorious.mango.fleet.VictorLaunchPins

/**
 * Public contract VictorLaunch uses to offer "Router VPN" on / off.
 * Toggles always go through [com.victorious.mango.VpnToggleActivity]'s confirm dialog;
 * the state query is read-only and pinned to VictorLaunch's signing cert.
 */
object VpnToggleContract {
    const val PACKAGE = "com.victorious.mango"
    const val ACTIVITY = "com.victorious.mango.VpnToggleActivity"

    const val ACTION_VPN_ON = "com.victorious.mango.action.VPN_ON"
    const val ACTION_VPN_OFF = "com.victorious.mango.action.VPN_OFF"
    const val ACTION_VPN_TOGGLE = "com.victorious.mango.action.VPN_TOGGLE"

    const val SHORTCUT_VPN_ON = "vpn_on"
    const val SHORTCUT_VPN_OFF = "vpn_off"

    /** Read-only VPN state. Declared `signature|knownSigner` and pinned to VictorLaunch. */
    const val PERMISSION_READ_STATE = "com.victorious.mango.permission.READ_VPN_STATE"
    const val AUTHORITY = "com.victorious.mango.vpnstate"
    const val URI = "content://$AUTHORITY/current"
    const val ACTION_STATE_CHANGED = "com.victorious.mango.action.VPN_STATE_CHANGED"
    const val SCHEMA_VERSION = 1

    const val COL_SCHEMA = "schemaVersion"
    const val COL_CONNECTED = "connected"
    const val COL_ENABLED = "enabled"
    const val COL_PROTOCOL = "protocol"
    const val COL_PROFILE = "profile"
    const val COL_ROUTER = "router"
    const val COL_HOST = "host"
    const val COL_STATUS = "status"
    const val COL_UPDATED = "updatedAtMs"

    val CALLER_PACKAGE = VictorLaunchPins.PACKAGE
    val CALLER_CERT_PINS: Set<String> = VictorLaunchPins.CERT_PINS

    enum class Request { ON, OFF, TOGGLE }

    fun requestOf(action: String?): Request? = when (action) {
        ACTION_VPN_ON -> Request.ON
        ACTION_VPN_OFF -> Request.OFF
        ACTION_VPN_TOGGLE -> Request.TOGGLE
        else -> null
    }

    /** Columns for the exported provider / broadcast extras. Unknown columns must be ignored by clients. */
    fun stateRow(
        connected: Boolean,
        enabled: Boolean,
        protocol: String,
        profile: String,
        router: String,
        host: String,
        status: String,
        updatedAtMs: Long,
    ): Map<String, Any?> = mapOf(
        COL_SCHEMA to SCHEMA_VERSION,
        COL_CONNECTED to if (connected) 1 else 0,
        COL_ENABLED to if (enabled) 1 else 0,
        COL_PROTOCOL to protocol,
        COL_PROFILE to profile,
        COL_ROUTER to router,
        COL_HOST to host,
        COL_STATUS to status,
        COL_UPDATED to updatedAtMs,
    )
}
