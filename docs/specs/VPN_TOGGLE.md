# Router VPN toggles

schemaVersion: 1

VictorMango does not run a VPN on the phone. It starts and stops the **WireGuard or OpenVPN client on a GL.iNet 4.x router** through the official `/rpc` API. VictorLaunch may offer these as **"Router VPN"** on / off actions.

Nothing in this contract toggles silently. The exported activity always opens VictorMango's own confirm dialog, which names the router and the VPN profile, and only then calls the router API.

## Package and entry points

| Item | Value |
| --- | --- |
| Package | `com.victorious.mango` |
| Activity | `com.victorious.mango.VpnToggleActivity` (exported, no permission) |
| Action: on | `com.victorious.mango.action.VPN_ON` |
| Action: off | `com.victorious.mango.action.VPN_OFF` |
| Action: toggle | `com.victorious.mango.action.VPN_TOGGLE` |
| Shortcut id: on | `vpn_on` (label: "Router VPN on") |
| Shortcut id: off | `vpn_off` (label: "Router VPN off") |

Static shortcuts live in `res/xml/shortcuts.xml` and are registered on `MainActivity`. Each shortcut starts `VpnToggleActivity` with `VPN_ON` or `VPN_OFF`.

`VPN_TOGGLE` is for the launcher: if a client is connected or connecting, the confirm dialog offers "off"; otherwise it offers "on".

### Confirm behavior

1. The activity loads the saved router login (set in the Status screen). If none is saved, it shows a dialog explaining that and does **not** call the API.
2. It logs in and reads official `get_status` (and `get_all_config_list` if needed) so the dialog can name the router (`system.get_info` hostname/model, else the host) and the profile (`name`, else domain).
3. The user must tap **Turn on** / **Turn off**. Cancel or dismiss does nothing.
4. Only after confirm does VictorMango call `start` or `stop`. If the client is already in the requested state, confirm is a no-op (no RPC).

## Official GL.iNet 4.x RPC

Checked against the archived official SDK 4.0 API guide
<https://web.archive.org/web/20240121142533/https://dev.gl-inet.com/router-4.x-api/>
and the official GL-audited method examples for `wg-client` and `ovpn-client`.

Auth is the documented `challenge` → crypt → `login` → `call [sid, module, function, params]` flow already used by this app. Router identity uses `system.get_info`.

| Module | Function | Params | Notes |
| --- | --- | --- | --- |
| `wg-client` | `get_status` | `{}` | `status` 0 / 1 / 2 = not connected / connected / connecting; includes `group_id`, `peer_id`, `name` |
| `wg-client` | `start` | `{group_id, peer_id}` | |
| `wg-client` | `stop` | `{}` | |
| `ovpn-client` | `get_status` | `{}` | Same status codes; includes `group_id`, `client_id`, `name` |
| `ovpn-client` | `start` | `{group_id, client_id}` | |
| `ovpn-client` | `stop` | `{group_id, client_id}` | |

If `get_status` has no last-used ids, the app falls back to the first profile in official `get_all_config_list`. If both a WireGuard and an OpenVPN client are configured, a running client wins; otherwise the last-used (or first listed) WireGuard profile is preferred.

Some 4.8.x builds have been reported to drop `wg-client.get_status`. This implementation still targets the official 4.x module methods above.

## Read-only state query (optional for the launcher)

The toggle activity itself needs **no** special permission, because it always confirms. VictorLaunch may also *read* state:

| Item | Value |
| --- | --- |
| Permission | `com.victorious.mango.permission.READ_VPN_STATE` |
| Protection | `signature\|knownSigner` |
| Provider | `content://com.victorious.mango.vpnstate/current` (exported, read-only) |
| Broadcast | `com.victorious.mango.action.VPN_STATE_CHANGED` (sent with the same permission; package-targeted at `com.victorious.launch`) |

### Pins

VictorLaunch package: `com.victorious.launch`.

Debug cert SHA-256 (same pin as FleetThemeClient / `VictorLaunchPins`):

`B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69`

`android:knownCerts` uses the no-colon form of that digest in `res/values/arrays.xml` (`victorlaunch_known_certs`). **There is no VictorLaunch release cert yet.** When one is published, add it to:

1. `VictorLaunchPins.CERT_PINS`
2. `victorlaunch_known_certs` in `res/values/arrays.xml`

The provider also re-checks the caller's package and cert so the pin still holds on API 26–30, where `knownSigner` is not enforced.

Columns / extras (`schemaVersion` = 1): `connected`, `enabled` (0/1), `protocol` (`wireguard` / `openvpn` / empty), `profile`, `router`, `host`, `status` (`connected` / `connecting` / `disconnected` / `unknown`), `updatedAtMs`. The provider returns a cached snapshot only; it never starts or stops the VPN, and insert/update/delete throw.

VictorLaunch should declare:

```xml
<uses-permission android:name="com.victorious.mango.permission.READ_VPN_STATE" />
```
