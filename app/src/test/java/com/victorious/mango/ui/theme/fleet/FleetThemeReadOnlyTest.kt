package com.victorious.mango.ui.theme.fleet

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/** Guard: the client must never write VictorLaunch's provider. */
class FleetThemeReadOnlyTest {
    @Test fun clientNeverCallsProviderWriteMethods() {
        val src = listOf("src/main/java", "app/src/main/java").map(::File).first { it.exists() }
        val code = src.walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        for (m in listOf(".insert(", ".update(", ".delete(", ".bulkInsert(", ".applyBatch(", "acquireContentProviderClient")) {
            assertFalse("found $m", code.contains("contentResolver$m") || code.contains("resolver$m") || (m.startsWith("acq") && code.contains(m)))
        }
    }
}
