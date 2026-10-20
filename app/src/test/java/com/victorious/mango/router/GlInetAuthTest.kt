package com.victorious.mango.router

import org.junit.Assert.assertEquals
import org.junit.Test

/** Test vectors from the official GL.iNet 4.x API docs example. */
class GlInetAuthTest {
    @Test fun md5CryptMatchesDocs() =
        assertEquals("\$1\$shGzEq91\$.TLXit4pO5gGOdru0MYHz.", GlInetAuth.cipherPassword(1, "shGzEq91", "goodlife"))

    @Test fun loginHashMatchesDocs() = assertEquals(
        "6e5fd33bbd77a67eb108cf0b8bbdb9be",
        GlInetAuth.loginHash("root", "\$1\$shGzEq91\$.TLXit4pO5gGOdru0MYHz.", "SGgyhFWf3lFrIpX2BFImBjE1gv2AKPC2"),
    )
}
