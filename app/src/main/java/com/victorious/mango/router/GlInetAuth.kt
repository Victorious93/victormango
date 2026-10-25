package com.victorious.mango.router

import java.security.MessageDigest

/** Login hash helpers for GL.iNet 4.x `/rpc` auth. Verified against the official doc example. */
object GlInetAuth {
    fun loginHash(username: String, cipherPassword: String, nonce: String): String =
        md5Hex("$username:$cipherPassword:$nonce")

    /** Unix crypt as used by `openssl passwd -<alg>`. alg 1 (MD5) implemented; 5/6 TODO. */
    fun cipherPassword(alg: Int, salt: String, password: String): String = when (alg) {
        1 -> md5Crypt(password, salt)
        5, 6 -> throw RouterException("TODO: SHA-crypt alg=$alg not yet implemented")
        else -> throw RouterException("Unknown alg $alg")
    }

    private fun md5Hex(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private const val ITOA = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun md5Crypt(password: String, saltIn: String): String {
        val salt = saltIn.take(8)
        val pw = password.toByteArray(); val sb = salt.toByteArray(); val magic = "\$1\$".toByteArray()
        fun md() = MessageDigest.getInstance("MD5")
        val alt = md().apply { update(pw); update(sb); update(pw) }.digest()
        val ctx = md().apply { update(pw); update(magic); update(sb) }
        var n = pw.size
        while (n > 0) { ctx.update(alt, 0, minOf(16, n)); n -= 16 }
        var i = pw.size
        while (i != 0) { if (i and 1 != 0) ctx.update(0) else ctx.update(pw[0]); i = i ushr 1 }
        var fin = ctx.digest()
        for (r in 0 until 1000) {
            val c = md()
            if (r and 1 != 0) c.update(pw) else c.update(fin)
            if (r % 3 != 0) c.update(sb)
            if (r % 7 != 0) c.update(pw)
            if (r and 1 != 0) c.update(fin) else c.update(pw)
            fin = c.digest()
        }
        val out = StringBuilder("\$1\$$salt\$")
        fun to64(v0: Int, len: Int) { var v = v0; repeat(len) { out.append(ITOA[v and 0x3f]); v = v ushr 6 } }
        fun b(x: Int) = fin[x].toInt() and 0xff
        to64((b(0) shl 16) or (b(6) shl 8) or b(12), 4)
        to64((b(1) shl 16) or (b(7) shl 8) or b(13), 4)
        to64((b(2) shl 16) or (b(8) shl 8) or b(14), 4)
        to64((b(3) shl 16) or (b(9) shl 8) or b(15), 4)
        to64((b(4) shl 16) or (b(10) shl 8) or b(5), 4)
        to64(b(11), 2)
        return out.toString()
    }
}
