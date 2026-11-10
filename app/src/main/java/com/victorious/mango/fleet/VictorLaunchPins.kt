package com.victorious.mango.fleet

/**
 * Shared VictorLaunch package + signing-cert pins.
 *
 * The fleet does not share a signing key, so a signature permission alone is not enough.
 * [CERT_PINS] is the source of truth for Kotlin checks; keep `res/values/arrays.xml`
 * (`victorlaunch_known_certs`, hex with no colons) in sync for `signature|knownSigner`.
 */
object VictorLaunchPins {
    const val PACKAGE = "com.victorious.launch"

    /**
     * SHA-256 of the VictorLaunch debug signing cert (FleetPins / FLEET_THEME.md).
     * There is no release cert yet. When one is published, add it to [CERT_PINS] and to
     * `victorlaunch_known_certs` in `res/values/arrays.xml`.
     */
    const val DEBUG_CERT_SHA256 =
        "B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69"

    val CERT_PINS: Set<String> = setOf(
        DEBUG_CERT_SHA256,
        // Add the VictorLaunch release cert SHA-256 here once one is published.
    )

    /** Uppercase colon form; accepts "b99aaf…" or "B9:9A:…". */
    fun normalizeCert(digest: String): String {
        val hex = digest.filter { it.isLetterOrDigit() }.uppercase()
        return hex.chunked(2).joinToString(":")
    }

    /** Hex without colons, as required by `android:knownCerts`. */
    fun knownCertsHex(): List<String> = CERT_PINS.map { normalizeCert(it).replace(":", "") }

    /** Trusted only if it is VictorLaunch and every current signer is pinned. */
    fun isTrusted(packageName: String?, signerDigests: List<String>): Boolean {
        if (packageName != PACKAGE || signerDigests.isEmpty()) return false
        val pins = CERT_PINS.map(::normalizeCert).toSet()
        return signerDigests.all { normalizeCert(it) in pins }
    }
}
