Vekta 2.2 is the first stable release of this fork. It is built on the Vector 2.2 line - libxposed API 102, hot reload, one module one configuration - and carries everything this fork changed on top of it.

## English

- **Rebranded as Vekta.** The manager, the module and every artifact carry the new name; the repository is `Lxiaoyao077/Vekta`. Module authors: JingMatrix, Lxyao.
- **New XSharedPreferences removed**, ahead of the upstream 2.3.0 schedule. `XSharedPreferences` reads the classic world-readable path again; modules still on the bridge must move to the libxposed service's remote preferences.
- **x86 and x86_64 dropped.** Artifacts ship arm64-v8a and armeabi-v7a only.
- **Version naming driven by `v` tags.** This release is `v2.2`; artifacts are named `Vekta-v2.2-<versionCode>-Release.zip`.
- **API protection toggle** (`PROP_RT_API_PROTECTION`), independent of dex obfuscation and off by default.
- **Manager**: quick actions on Home (GitHub, soft reboot), and the activity feed now merges this fork's commits with upstream's.
- **Toolchain**: Gradle 9.8, CI on Ubuntu 26.04.
- A 2.1-era warning still applies: if you installed the manager as a separate app, uninstall it before updating - an old manager cannot talk to a new daemon. A parasitic manager needs nothing.

## 中文

- **品牌更名为 Vekta**：管理器、模块与所有产物均使用新名称；仓库为 `Lxiaoyao077/Vekta`。模块作者：JingMatrix、Lxyao。
- **移除 New XSharedPreferences**，早于上游 2.3.0 的计划。`XSharedPreferences` 回归经典 world-readable 路径；仍在使用该桥接的模块请迁移至 libxposed 服务的远程偏好。
- **移除 x86 与 x86_64**。产物仅包含 arm64-v8a 与 armeabi-v7a。
- **版本名由 `v` 标签驱动**。本版本为 `v2.2`；产物命名为 `Vekta-v2.2-<版本号>-Release.zip`。
- **API 调用保护开关**（`PROP_RT_API_PROTECTION`），独立于 dex 混淆，默认关闭。
- **管理器**：主页新增快捷操作（GitHub、软重启）；动态时间线合并本仓库与上游的提交。
- **工具链**：Gradle 9.8，CI 迁移至 Ubuntu 26.04。
- 旧提醒仍然有效：如果你曾把管理器安装为独立应用，更新前请先卸载——旧管理器无法与新版 daemon 通信。寄生模式管理器无需任何操作。
