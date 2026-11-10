package com.victorious.mango.router

import com.victorious.mango.vpn.VpnToggleContract
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnApiTest {
    private val wgOfficial = """{"group_id":2312, "peer_id":5712, "name":"mullvad_ae_all", "proxy":false, "log":"", "status":1, "ipv4":"10.70.33.3", "ipv6":"fc00:bbbb:bb01::11", "rx_bytes":50, "tx_bytes":45, "port":41348, "domain":"ae-dxb-001.mullvad.net"}"""
    private val ovOfficial = """{"group_id":3212, "client_id":1254, "name":"mullvad_ae_all", "log":"", "status":1, "tx_bytes":56, "rx_bytes":50, "ipv4":"10.70.33.3", "ipv6":"fc00:bbbb:bb01::11", "port":1194, "domain":"ae-dxb-001.mullvad.net"}"""

    @Test fun parsesOfficialWireGuardStatus() {
        val r = parseVpnClientStatus(VpnProtocol.WIREGUARD, wgOfficial)
        assertEquals(VpnLink.CONNECTED, r.link)
        assertEquals(2312, r.groupId)
        assertEquals(5712, r.configId)
        assertEquals("mullvad_ae_all", r.profileName)
        assertEquals(50L, r.rxBytes)
        assertEquals(45L, r.txBytes)
        assertTrue(r.isUp)
        assertTrue(r.hasStartIds)
    }

    @Test fun parsesOfficialOpenVpnStatus() {
        val r = parseVpnClientStatus(VpnProtocol.OPENVPN, ovOfficial)
        assertEquals(VpnLink.CONNECTED, r.link)
        assertEquals(3212, r.groupId)
        assertEquals(1254, r.configId)
        assertEquals("OpenVPN · mullvad_ae_all", "${r.protocolLabel()} · ${r.displayProfile}")
    }

    @Test fun statusCodes() {
        assertEquals(VpnLink.DISCONNECTED, parseVpnLink(0))
        assertEquals(VpnLink.CONNECTED, parseVpnLink(1))
        assertEquals(VpnLink.CONNECTING, parseVpnLink(2))
        assertEquals(VpnLink.DISCONNECTED, parseVpnLink(null))
        assertEquals(VpnLink.DISCONNECTED, parseVpnLink(99))
        val down = parseVpnClientStatus(VpnProtocol.WIREGUARD, JSONObject().put("status", 0).put("group_id", 2312).put("peer_id", 5712).put("name", "home"))
        assertFalse(down.isUp)
        assertTrue(down.hasStartIds)
        assertEquals("home", down.displayProfile)
    }

    @Test fun zeroIdsAreNotUsable() {
        val r = parseVpnClientStatus(VpnProtocol.WIREGUARD, JSONObject().put("status", 0).put("group_id", 0).put("peer_id", 0))
        assertFalse(r.hasStartIds)
        assertEquals("No profile selected", r.displayProfile)
    }

    @Test fun officialStartAndStopParams() {
        val wgStart = startParams(VpnProtocol.WIREGUARD, 3212, 1254)
        assertEquals(3212, wgStart.getInt("group_id"))
        assertEquals(1254, wgStart.getInt("peer_id"))
        assertFalse(wgStart.has("client_id"))
        val ovStart = startParams(VpnProtocol.OPENVPN, 3212, 1254)
        assertEquals(3212, ovStart.getInt("group_id"))
        assertEquals(1254, ovStart.getInt("client_id"))
        assertFalse(ovStart.has("peer_id"))
        assertEquals(0, stopParams(VpnProtocol.WIREGUARD, 1, 2).length())
        val ovStop = stopParams(VpnProtocol.OPENVPN, 3212, 1254)
        assertEquals(3212, ovStop.getInt("group_id"))
        assertEquals(1254, ovStop.getInt("client_id"))
    }

    @Test fun firstProfileFromOfficialConfigList() {
        val wg = firstProfileFromConfigList(
            VpnProtocol.WIREGUARD,
            JSONObject("""{"config_list":[{"group_id":2312,"group_name":"mullvad","peers":[{"peer_id":1254,"name":"mullvad_au4"}]}]}"""),
        )
        assertEquals(2312, wg!!.groupId)
        assertEquals(1254, wg.configId)
        assertEquals("mullvad_au4", wg.profileName)
        val ov = firstProfileFromConfigList(
            VpnProtocol.OPENVPN,
            JSONObject("""{"config_list":[{"group_id":2312,"group_name":"mullvad","clients":[{"client_id":3212,"name":"AirVPN"}]}]}"""),
        )
        assertEquals(2312, ov!!.groupId)
        assertEquals(3212, ov.configId)
        assertEquals("AirVPN", ov.profileName)
        assertNull(firstProfileFromConfigList(VpnProtocol.WIREGUARD, JSONObject("""{"config_list":[]}""")))
    }

    @Test fun identityPrefersHostname() {
        val info = JSONObject("""{"model":"xe300","board_info":{"hostname":"GL-MT300N-V2","model":"GL Technologies, Inc. MT300N-V2"}}""")
        assertEquals("GL-MT300N-V2", parseRouterIdentity(info, "192.168.8.1"))
        assertEquals("xe300", parseRouterIdentity(JSONObject("""{"model":"xe300"}"""), "192.168.8.1"))
        assertEquals("192.168.8.1", parseRouterIdentity(JSONObject(), "192.168.8.1"))
    }

    @Test fun planOnOffToggleAndNeverSilent() {
        val wgUp = parseVpnClientStatus(VpnProtocol.WIREGUARD, wgOfficial)
        val up = RouterVpnSnapshot("192.168.8.1", "GL-MT300N-V2", wireguard = wgUp)
        val down = RouterVpnSnapshot("192.168.8.1", "GL-MT300N-V2", wireguard = wgUp.copy(link = VpnLink.DISCONNECTED))
        val empty = RouterVpnSnapshot("192.168.8.1", "GL-MT300N-V2")

        val start = planToggle(VpnToggleContract.Request.ON, down)
        assertEquals(VpnIntent.START, start.rpc!!.intent)
        assertEquals(VpnProtocol.WIREGUARD, start.rpc!!.protocol)
        assertEquals(2312, start.rpc!!.groupId)
        assertEquals(5712, start.rpc!!.configId)
        assertTrue(start.message.contains("GL-MT300N-V2"))
        assertTrue(start.message.contains("mullvad_ae_all"))
        assertTrue(start.title.contains("Router VPN"))

        val stop = planToggle(VpnToggleContract.Request.OFF, up)
        assertEquals(VpnIntent.STOP, stop.rpc!!.intent)
        assertTrue(stop.message.contains("GL-MT300N-V2"))
        assertTrue(stop.message.contains("mullvad_ae_all"))

        val toggleOff = planToggle(VpnToggleContract.Request.TOGGLE, up)
        assertEquals(VpnIntent.STOP, toggleOff.rpc!!.intent)
        val toggleOn = planToggle(VpnToggleContract.Request.TOGGLE, down)
        assertEquals(VpnIntent.START, toggleOn.rpc!!.intent)

        val alreadyOn = planToggle(VpnToggleContract.Request.ON, up)
        assertTrue(alreadyOn.alreadyInState)
        assertNull(alreadyOn.rpc)
        val alreadyOff = planToggle(VpnToggleContract.Request.OFF, down)
        assertTrue(alreadyOff.alreadyInState)
        assertNull(alreadyOff.rpc)

        val blocked = planToggle(VpnToggleContract.Request.ON, empty)
        assertNull(blocked.rpc)
        assertTrue(blocked.blockedReason!!.contains("GL-MT300N-V2"))
        assertTrue(blocked.message.contains("GL-MT300N-V2"))
    }

    @Test fun prefersRunningOpenVpnOverIdleWireGuard() {
        val wgIdle = parseVpnClientStatus(VpnProtocol.WIREGUARD, wgOfficial).copy(link = VpnLink.DISCONNECTED)
        val ovUp = parseVpnClientStatus(VpnProtocol.OPENVPN, ovOfficial)
        val snap = RouterVpnSnapshot("192.168.8.1", "Mango", wireguard = wgIdle, openvpn = ovUp)
        assertEquals(VpnProtocol.OPENVPN, snap.active!!.protocol)
        val stop = planToggle(VpnToggleContract.Request.OFF, snap)
        assertEquals(VpnProtocol.OPENVPN, stop.rpc!!.protocol)
        assertEquals(3212, stop.rpc!!.groupId)
        assertEquals(1254, stop.rpc!!.configId)
    }

    @Test fun snapshotRowForProvider() {
        val wgUp = parseVpnClientStatus(VpnProtocol.WIREGUARD, wgOfficial)
        val row = snapshotStateRow(RouterVpnSnapshot("192.168.8.1", "GL-MT300N-V2", wireguard = wgUp), 7L)
        assertEquals(1, row["connected"])
        assertEquals("wireguard", row["protocol"])
        assertEquals("mullvad_ae_all", row["profile"])
        assertEquals("connected", row["status"])
        assertEquals(7L, row["updatedAtMs"])
    }
}
