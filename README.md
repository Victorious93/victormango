<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="140" alt="VictorMango logo"/>

# VictorMango

**Control your GL.iNet Mango travel router's VPN from your phone.**

<p>
  <a href="https://github.com/Victorious93/victormango/actions/workflows/android.yml"><img src="https://img.shields.io/github/actions/workflow/status/Victorious93/victormango/android.yml?branch=main&style=flat-square&labelColor=0B0F1A&logo=githubactions&logoColor=00E5FF&label=CI" alt="CI"/></a>
  <img src="https://img.shields.io/badge/platform-Android-00E5FF?style=flat-square&labelColor=0B0F1A" alt="Platform"/>
  <a href="https://victorious-framework.pages.dev"><img src="https://img.shields.io/badge/Victorious-Framework-FF2BD6?style=flat-square&labelColor=0B0F1A" alt="Victorious Framework"/></a>
</p>

<p><a href="https://github.com/Victorious93/victormango/releases">Releases</a> · <a href="https://victorious-framework.pages.dev">Victorious Framework</a></p>

</div>

---

## Features

| Area | Status |
|:--|:--|
| GL.iNet 4.x `/rpc` login (challenge → md5-crypt hash → session id) | Implemented, unit-tested against the documented vectors |
| Status screen: model, hostname, uptime, CPU load, memory, WAN, clients | Implemented |
| Router VPN: start / stop / status of the router's WireGuard and OpenVPN clients | Implemented, confirm-gated |
| Static launcher shortcuts "Router VPN on / off" | Implemented |
| Optional VictorLaunch fleet theme (read-only, pin-verified) | Implemented |
| Wi-Fi toggle, AdGuard Home toggle, tethering, firmware upgrade | Planned (screens exist; the router calls are still stubs) |
| VPN profile import (`.conf` / QR) | Planned |
| SSH transport fallback (ubus / uci) | Planned |
| SHA-crypt (`alg` 5/6) login hashing | Planned (only md5-crypt works today) |

VictorMango does **not** run a VPN on the phone. It switches the VPN client on the **router** through GL.iNet's official API. Nothing toggles silently: the exported `VpnToggleActivity` always shows a confirm dialog naming the router and the VPN profile first. The full contract is in [docs/specs/VPN_TOGGLE.md](docs/specs/VPN_TOGGLE.md).

## Install

No release has been published yet. Debug APKs are built by CI: open the latest [Android CI run](https://github.com/Victorious93/victormango/actions/workflows/android.yml) and download the `victormango-debug-apk` artifact. Package id: `com.victorious.mango`, minSdk 26.

## Build from source

Requires JDK 17 and the Android SDK (35).

```bash
git clone https://github.com/Victorious93/victormango.git
cd victormango
./gradlew testDebugUnitTest assembleDebug
```

`app/debug.keystore` is a fixed, public debug key so debug builds keep a stable signature. Release keys are never committed.

## Usage

1. Open **Status** and enter the router host, username and password (a GL.iNet 4.x router, such as the Mango GL-MT300N-V2).
2. Use the **VPN** screen or the launcher shortcuts to turn the router's VPN client on or off, then confirm in the dialog.

## Permissions

`INTERNET` and `ACCESS_NETWORK_STATE` only. No root is needed.

## Reference docs

- [GL.iNet SDK 4.x API](https://dev.gl-inet.com/router-4.x-api/) (the live site has shown "Website Closed"; [archived copy](https://web.archive.org/web/20240121142533/https://dev.gl-inet.com/router-4.x-api/))
- [GL.iNet docs](https://docs.gl-inet.com/router/en/4/) · [security advisories](https://github.com/gl-inet/CVE-issues) · [OpenWrt ubus](https://openwrt.org/docs/techref/ubus) · [LuCI JSON-RPC](https://github.com/openwrt/luci/wiki/JsonRpcHowTo) · [UCI](https://openwrt.org/docs/guide-user/base-system/uci)

---

## Victorious fleet

VictorMango is part of the [Victorious Framework](https://victorious-framework.pages.dev), a family of Android and PC tools by Don Victor. **Every app works standalone; fleet integrations are optional** and never required.

Related: [VictorLaunch](https://github.com/Victorious93/victorlaunch) (fleet launcher and theme provider), [droidcommand-AI](https://github.com/Victorious93/droidcommand-AI) (fleet controller), [fleet-manifest](https://github.com/Victorious93/fleet-manifest) (fleet status feed). The full roster lives in [victorious-fleet](https://github.com/Victorious93/victorious-fleet), and live status is in [fleet-manifest](https://github.com/Victorious93/fleet-manifest).

## Contributing

Issues and pull requests are welcome. Keep changes focused, match the existing code style, never commit secrets or release keys, and keep any fleet integration optional so the app still works on its own. Open a PR against the default branch and wait for CI to pass.

## License

This repository does not include a LICENSE file yet, so no license is granted beyond what the author states elsewhere. Please open an issue before reusing the code.
