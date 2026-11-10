package com.victorious.mango.fleet

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

object SigningCerts {
    /** SHA-256 of the package's current signers (not the rotation history). */
    @Suppress("DEPRECATION")
    fun sha256(pm: PackageManager, pkg: String): List<String> = runCatching {
        val sigs: Array<Signature> = if (Build.VERSION.SDK_INT >= 28) {
            val si = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else arrayOf(si.signingCertificateHistory.last())
        } else {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
        } ?: emptyArray()
        sigs.map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(":") { "%02X".format(it) } }
    }.getOrDefault(emptyList())
}
