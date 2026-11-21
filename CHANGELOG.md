# 变更记录 (CHANGELOG)

> 本仓库 = 可构建的 Mindustry 分支，用于产出可安装的 APK。
> 纯源码（仅供 AI 阅读）在 [mindustry-src](https://github.com/jinitaimei436/mindustry-src)。

## v1.0 — 2026-11-21

首个可安装构建（debug，未混淆，包名 `io.anuke.mindustry`）。
对应 APK：见本版本 Release 附件 `mindustry-global-logistics-debug.apk`。

### 新增功能（相对官方 `19a0f40d`）

| 文件 | 说明 |
|---|---|
| `core/src/mindustry/game/strategy/` | 全新包：全球物流图求解器、事件驱动模拟引擎、区块产线编译器、战略派兵/AI 预留接口 |
| `CampaignRules.java` | 新增 `globalLogistics` 开关，默认 `true` |
| `SectorInfo.java` | 新增 `strategyProfile` 字段；退出战场时编译产线画像 |
| `Universe.java` | 秒级 tick 接入 `GlobalStrategyManager`；开启物流后关闭原版批次结算 |
| `tests/src/test/java/GlobalLogisticsTests.java` | JUnit 5 单元测试（**尚未在 CI 执行**） |
| `.github/workflows/build-apk.yml` | 云端构建 Debug APK 的工作流 |

### 构建修复（原始交付包无法编译）

1. `GlobalStrategyManager.ensureProfile()`：lambda 捕获了会被重新赋值的局部变量 → 改用 `final` 局部变量
2. `build-apk.yml`：`android-actions/setup-android@v3` 在新版 runner 上失败（`Failed to find package 'tools'`）→ 改用 runner 预装 SDK + 直接调用 `sdkmanager`
3. debug 构建关闭 R8：AGP 8.2 的 `merged_java_res/base.jar` 缺失 bug

### 已知限制

- debug 未混淆包，体积约 64 MB，启动略慢
- 关闭 R8 后缺少 default interface 脱糖，**API < 24（Android 7 以下）可能崩溃**；Android 7+ 正常
- `globalLogistics` 没有 UI 开关，只有代码默认值；实际效果**未经真机验证**
- `tests/` 的单元测试从未运行通过，CI 里的 `Tests` workflow 被取消过

---

## 版本规则

- 版本号形如 `v1.0`、`v1.1`，每次上游 AI 交付并成功装包后递增
- 每次打包在 git 打同名 tag，并把 APK 附到同名 Release（不走 30 天过期的 Actions artifact）
- 文件改动一律记录在上表，避免新旧版本混淆
