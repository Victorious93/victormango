# VictorMango

Android companion app for the GL.iNet **Mango (GL-MT300N-V2)** and other GL.iNet OpenWrt travel routers.
Part of the Victorious fleet. Package id: `com.victorious.mango`.

## Reference docs
- GL.iNet SDK 4.x API (official, `/rpc` JSON-RPC): https://dev.gl-inet.com/router-4.x-api/
  (the live site currently shows "Website Closed"; archived copy: https://web.archive.org/web/20240121142533/https://dev.gl-inet.com/router-4.x-api/)
- GL.iNet docs: https://docs.gl-inet.com/router/en/4/
- GL.iNet security advisories (login bypass fixed in 4.5.0): https://github.com/gl-inet/CVE-issues
- OpenWrt ubus: https://openwrt.org/docs/techref/ubus
- OpenWrt LuCI JSON-RPC: https://github.com/openwrt/luci/wiki/JsonRpcHowTo
- OpenWrt UCI: https://openwrt.org/docs/guide-user/base-system/uci

## Build
`./gradlew assembleDebug` (JDK 17, Android SDK 35). CI builds the debug APK on every PR and uploads it as the `victormango-debug-apk` artifact.
`app/debug.keystore` is a fixed debug key (`android`/`androiddebugkey`/`android`). Release keys are never committed.

## Structure
- `router/RouterClient.kt` – transport-agnostic interface (SSH transport: TODO)
- `router/GlInetRpcClient.kt` – GL.iNet 4.x `/rpc` client: challenge → login → sid → call (auth real, features stubbed)
- `router/GlInetAuth.kt` – md5-crypt + login hash, unit-tested against the doc vectors
- `ui/screens` – Status, Wi-Fi, VPN/WireGuard, AdGuard, Tether, Firmware
