# Changelog

Everything this fork has done since diverging from [Vector](https://github.com/JingMatrix/Vector) on 2026-09-27. Upstream's own history lives in the upstream repository; this file records only what the fork changed on top of it.

## Unreleased - 2026-09-27

### Added

- An independent API protection toggle: `DaemonState.isApiProtectionEnabled` (default **off**) now decides whether `PROP_RT_API_PROTECTION` is advertised to modules, no longer coupled to dex obfuscation. A manager switch can wire into the same flag later.
- Renovate for automated dependency updates.

### Changed

- Rebranded Vector as **Vekta**: the manager (every locale), the module's `module.prop`, the packaged zips, the release titles, the CLI banner, and the log archives all carry the new name. The module credits `JingMatrix, Lxyao` as authors, and the repository itself was renamed to `Lxiaoyao077/Vekta`.
- Version line moved to release-candidate naming. The `v2.2-rc` tag drives names like `Vekta-v2.2-rc-3130-Release.zip`, where the trailing number stays the commit count.

### Fixed

- Version names no longer leak CI's own canary tags (a fork shipped `Vector-vcanary-3115-3119` because the version lookup accepted any tag and the only tags a fork has are the canaries CI creates). The lookup reads `v`-prefixed tags only, and the workflow parses packaged names from the end, so a version name containing a dash survives.
- The build is fixed for the current toolchain landscape by way of upstream PR JingMatrix/Vector#984: the libxposed git submodules (dead, since the libxposed organization was banned from GitHub) are replaced by the `io.github.libxposed` Maven artifacts.

### Removed

- **New XSharedPreferences**, ahead of the upstream 2.3.0 removal. Gone are the daemon-provisioned prefs safe-zone, the `ContextImpl` `checkMode`/`getPreferencesDir` hooks, and the `getPrefsPath` IPC method. `XSharedPreferences` reads the classic world-readable `/data/data` path again; modules still on the bridge must move to the libxposed service's remote preferences. The rest of the legacy module - loading, hook dispatch, resource hooking - is untouched.
- The x86 and x86_64 ABIs. Artifacts carry `arm64-v8a` and `armeabi-v7a` only, and the module installer aborts cleanly with "Unsupported platform" on x86 devices.

### Build

- Gradle upgraded 9.7.0 to 9.8.0; AGP stays at 9.3.1.
- Both CI workflows pinned to `ubuntu-26.04`, ahead of the `ubuntu-latest` label migrating to Ubuntu 26.04 beginning 2026-10-19 (actions/runner-images#14748).
