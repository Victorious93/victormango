package com.victorious.mango.vpn

import com.victorious.mango.fleet.VictorLaunchPins
import com.victorious.mango.ui.theme.fleet.FleetThemeContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnToggleContractTest {
    private val pin = "B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69"

    @Test fun publicContract() {
        assertEquals("com.victorious.mango", VpnToggleContract.PACKAGE)
        assertEquals("com.victorious.mango.VpnToggleActivity", VpnToggleContract.ACTIVITY)
        assertEquals("com.victorious.mango.action.VPN_ON", VpnToggleContract.ACTION_VPN_ON)
        assertEquals("com.victorious.mango.action.VPN_OFF", VpnToggleContract.ACTION_VPN_OFF)
        assertEquals("com.victorious.mango.action.VPN_TOGGLE", VpnToggleContract.ACTION_VPN_TOGGLE)
        assertEquals("vpn_on", VpnToggleContract.SHORTCUT_VPN_ON)
        assertEquals("vpn_off", VpnToggleContract.SHORTCUT_VPN_OFF)
        assertEquals("com.victorious.mango.permission.READ_VPN_STATE", VpnToggleContract.PERMISSION_READ_STATE)
        assertEquals("content://com.victorious.mango.vpnstate/current", VpnToggleContract.URI)
        assertEquals("com.victorious.mango.action.VPN_STATE_CHANGED", VpnToggleContract.ACTION_STATE_CHANGED)
        assertEquals(1, VpnToggleContract.SCHEMA_VERSION)
    }

    @Test fun requestMapping() {
        assertEquals(VpnToggleContract.Request.ON, VpnToggleContract.requestOf(VpnToggleContract.ACTION_VPN_ON))
        assertEquals(VpnToggleContract.Request.OFF, VpnToggleContract.requestOf(VpnToggleContract.ACTION_VPN_OFF))
        assertEquals(VpnToggleContract.Request.TOGGLE, VpnToggleContract.requestOf(VpnToggleContract.ACTION_VPN_TOGGLE))
        assertNull(VpnToggleContract.requestOf("android.intent.action.MAIN"))
        assertNull(VpnToggleContract.requestOf(null))
    }

    @Test fun pinsMatchFleetThemeAndKnownCertsHex() {
        assertEquals(setOf(pin), VictorLaunchPins.CERT_PINS)
        assertEquals(FleetThemeContract.PROVIDER_CERT_PINS, VictorLaunchPins.CERT_PINS)
        assertEquals(listOf("B99AAFE2C1418B5D64AA0D4BAC61FCF7A7BA814435E0EF89E8AA55BE564E4A69"), VictorLaunchPins.knownCertsHex())
        assertTrue(VictorLaunchPins.isTrusted("com.victorious.launch", listOf(pin)))
        assertTrue(VictorLaunchPins.isTrusted("com.victorious.launch", listOf(pin.replace(":", "").lowercase())))
        assertFalse(VictorLaunchPins.isTrusted("com.evil.launch", listOf(pin)))
        assertFalse(VictorLaunchPins.isTrusted("com.victorious.launch", emptyList()))
        assertFalse(VictorLaunchPins.isTrusted("com.victorious.launch", listOf(pin, "AA:BB")))
    }

    @Test fun stateRowIsReadShaped() {
        val row = VpnToggleContract.stateRow(true, true, "wireguard", "mullvad_ae_all", "GL-MT300N-V2", "192.168.8.1", "connected", 9L)
        assertEquals(1, row["schemaVersion"])
        assertEquals(1, row["connected"])
        assertEquals("wireguard", row["protocol"])
        assertEquals("mullvad_ae_all", row["profile"])
        assertEquals("GL-MT300N-V2", row["router"])
        assertEquals(9L, row["updatedAtMs"])
    }
}
