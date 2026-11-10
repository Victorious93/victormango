package com.victorious.mango.vpn

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.victorious.mango.fleet.SigningCerts
import com.victorious.mango.fleet.VictorLaunchPins

/**
 * Read-only router VPN state for VictorLaunch. Writes are rejected.
 * The manifest also requires [VpnToggleContract.PERMISSION_READ_STATE]
 * (`signature|knownSigner` pinned to the VictorLaunch debug cert). This class
 * re-checks the caller's package and cert so the pin still holds on API < 31,
 * where knownSigner is not enforced.
 */
class VpnStateProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { VpnStateCache.load(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        enforceRead()
        val ctx = context ?: throw IllegalStateException("no context")
        val row = VpnStateCache.load(ctx)
        val keys = projection?.toList() ?: row.keys.toList()
        val cursor = MatrixCursor(keys.toTypedArray())
        cursor.addRow(keys.map { key -> row[key] ?: "" })
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.${VpnToggleContract.AUTHORITY}.state"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = rejectWrite()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = rejectWrite()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = rejectWrite()

    private fun enforceRead() {
        val ctx = context ?: throw SecurityException("no context")
        val pkg = callingPackage
        if (pkg == ctx.packageName) return
        if (!VictorLaunchPins.isTrusted(pkg, SigningCerts.sha256(ctx.packageManager, pkg ?: ""))) {
            throw SecurityException("caller $pkg is not the pinned VictorLaunch")
        }
    }

    private fun rejectWrite(): Nothing = throw SecurityException("VPN state provider is read-only")
}
