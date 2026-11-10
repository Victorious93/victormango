package com.victorious.mango.vpn

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.victorious.mango.router.RouterVpnSnapshot
import com.victorious.mango.router.snapshotStateRow

/**
 * Last known router VPN state for the read-only provider. Updated after a confirmed toggle
 * (and whenever the app refreshes status). Query never hits the router itself.
 */
object VpnStateCache {
    @Volatile private var memory: Map<String, Any?> = emptyRow()

    fun current(): Map<String, Any?> = memory

    fun load(context: Context): Map<String, Any?> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(VpnToggleContract.COL_UPDATED)) {
            memory = emptyRow()
            return memory
        }
        memory = VpnToggleContract.stateRow(
            connected = prefs.getInt(VpnToggleContract.COL_CONNECTED, 0) == 1,
            enabled = prefs.getInt(VpnToggleContract.COL_ENABLED, 0) == 1,
            protocol = prefs.getString(VpnToggleContract.COL_PROTOCOL, "").orEmpty(),
            profile = prefs.getString(VpnToggleContract.COL_PROFILE, "").orEmpty(),
            router = prefs.getString(VpnToggleContract.COL_ROUTER, "").orEmpty(),
            host = prefs.getString(VpnToggleContract.COL_HOST, "").orEmpty(),
            status = prefs.getString(VpnToggleContract.COL_STATUS, "disconnected").orEmpty(),
            updatedAtMs = prefs.getLong(VpnToggleContract.COL_UPDATED, 0L),
        )
        return memory
    }

    fun update(context: Context, snapshot: RouterVpnSnapshot) {
        val row = snapshotStateRow(snapshot, System.currentTimeMillis())
        memory = row
        val editor = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        row.forEach { (k, v) ->
            when (v) {
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
                is String -> editor.putString(k, v)
            }
        }
        editor.apply()
        context.contentResolver.notifyChange(VpnToggleContract.URI.toUri(), null)
        val broadcast = Intent(VpnToggleContract.ACTION_STATE_CHANGED)
            .setPackage(VpnToggleContract.CALLER_PACKAGE)
        row.forEach { (k, v) ->
            when (v) {
                is Int -> broadcast.putExtra(k, v)
                is Long -> broadcast.putExtra(k, v)
                is String -> broadcast.putExtra(k, v)
            }
        }
        context.sendBroadcast(broadcast, VpnToggleContract.PERMISSION_READ_STATE)
    }

    private fun emptyRow() = VpnToggleContract.stateRow(
        connected = false,
        enabled = false,
        protocol = "",
        profile = "",
        router = "",
        host = "",
        status = "unknown",
        updatedAtMs = 0L,
    )

    private const val PREFS = "vpn_state"
}
