package com.victorious.mango.ui.theme.fleet

import com.victorious.mango.ui.theme.NeonScheme
import com.victorious.mango.ui.theme.schemeFor
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetThemeContractTest {
    private val pin = "B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69"
    private val specJson = """
        {"schemaVersion":1,"mode":"NEON_FLEET","iconStyle":"SYSTEM","iconPackPackage":"","updatedAtMs":42,
         "palette":{"background":"#FF0B0C12","surface":"#FF161821","primary":"#FF00E5FF","secondary":"#FFE840FF",
         "onBackground":"#FFF5F7FF","muted":"#FF9AA3B5","danger":"#FFFF6B8A","stale":"#FFFFC14D","tile":"#CC1A1D2B"}}
    """.trimIndent()

    @Test fun constantsMatchSpec() {
        assertEquals("content://com.victorious.launch.theme/current", FleetThemeContract.URI)
        assertEquals("com.victorious.launch.action.FLEET_THEME_CHANGED", FleetThemeContract.ACTION_CHANGED)
        assertEquals("com.victorious.launch", FleetThemeContract.PROVIDER_PACKAGE)
        assertEquals(1, FleetThemeContract.SCHEMA_VERSION)
    }

    @Test fun parsesSpecJson() {
        val r = FleetThemeContract.parseJson(specJson) as FleetThemeResult.Ok
        assertEquals("NEON_FLEET", r.palette.mode)
        assertEquals(0xFF00E5FFL, r.palette.primary)
        assertEquals(0xFFFF6B8AL, r.palette.danger)
        assertEquals(42L, r.palette.updatedAtMs)
    }

    @Test fun columnsWinOverJsonAndUnknownColumnsIgnored() {
        val r = FleetThemeContract.parseRow(mapOf("schemaVersion" to 1L, "primary" to "#112233", "futureCol" to "x", "json" to specJson))
        assertEquals(0xFF112233L, (r as FleetThemeResult.Ok).palette.primary)
        assertEquals(0xFF0B0C12L, r.palette.background)
    }

    @Test fun rejectsOtherSchemaVersions() {
        for (v in listOf(0, 2, 99)) {
            val r = FleetThemeContract.parseJson(specJson.replace("\"schemaVersion\":1", "\"schemaVersion\":$v"))
            assertEquals(FleetThemeResult.UnsupportedVersion(v), r)
            assertTrue(fleetStateFor(Result.success(r)) is FleetThemeState.Unavailable)
        }
        assertTrue(FleetThemeContract.parseRow(mapOf("schemaVersion" to 2L, "json" to specJson)) is FleetThemeResult.UnsupportedVersion)
        assertTrue(FleetThemeContract.parseJson("{}") is FleetThemeResult.Invalid)
    }

    @Test fun badHexIsInvalid() {
        assertNull(FleetThemeContract.parseHex("00E5FF"))
        assertNull(FleetThemeContract.parseHex("#12345"))
        assertNull(FleetThemeContract.parseHex("#GGGGGG"))
        assertEquals(0xCC1A1D2BL, FleetThemeContract.parseHex("#CC1A1D2B"))
        val r = FleetThemeContract.parseJson(specJson.replace("#FF00E5FF", "cyan"))
        assertTrue(r is FleetThemeResult.Invalid)
    }

    @Test fun providerAuthNeedsPackageAndPinnedCert() {
        assertTrue(FleetThemeContract.isTrustedProvider("com.victorious.launch", listOf(pin)))
        assertTrue(FleetThemeContract.isTrustedProvider("com.victorious.launch", listOf(pin.replace(":", "").lowercase())))
        assertFalse(FleetThemeContract.isTrustedProvider("com.evil.launch", listOf(pin)))
        assertFalse(FleetThemeContract.isTrustedProvider("com.victorious.launch", listOf("1B:06:00")))
        assertFalse(FleetThemeContract.isTrustedProvider("com.victorious.launch", emptyList()))
        assertFalse(FleetThemeContract.isTrustedProvider("com.victorious.launch", listOf(pin, "AA:BB")))
        assertFalse(FleetThemeContract.isTrustedProvider(null, listOf(pin)))
    }

    @Test fun securityExceptionFallsBackToNeon() {
        val s = fleetStateFor(Result.failure(SecurityException("not pinned")))
        assertTrue((s as FleetThemeState.Unavailable).reason.contains("trusted"))
        assertEquals(NeonScheme, schemeFor(s))
        assertEquals(NeonScheme, schemeFor(fleetStateFor(Result.success(null))))
        assertEquals(NeonScheme, schemeFor(fleetStateFor(Result.failure(IllegalStateException()))))
        assertEquals(NeonScheme, schemeFor(null as FleetThemeState?))
    }

    @Test fun appliedPaletteDrivesScheme() {
        val s = fleetStateFor(Result.success(FleetThemeContract.parseJson(specJson)))
        val scheme = schemeFor(s)
        assertEquals(Color(0xFF00E5FF.toInt()), scheme.primary)
        assertEquals(Color(0xFF0B0C12.toInt()), scheme.background)
        assertEquals(Color(0xFFFF6B8A.toInt()), scheme.error)
    }
}
