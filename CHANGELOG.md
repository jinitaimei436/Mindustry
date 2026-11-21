# 变更记录 (CHANGELOG)

> 本仓库 = 可构建的 Mindustry 分支，用于产出可安装的 APK。
> 纯源码（仅供 AI 阅读）在 [mindustry-src](https://github.com/jinitaimei436/mindustry-src)。

## v1.5 — 2026-11-21

首个可安装构建（debug，未混淆，包名 `io.anuke.mindustry`）。
对应 APK：Release 附件 `mindustry-v1.5-debug.apk`。

> 版本号说明：本版最初标记为 v1.0，已按要求统一更正为 **v1.5**（跳过 v1.1–v1.4）。旧标签与旧 Release 均已删除，APK 二进制内容未变。

### 新增功能（相对官方 `19a0f40d`）

| 文件 | 说明 |
|---|---|
| `core/src/mindustry/game/strategy/` | 全新包：全球物流图求解器、事件驱动模拟引擎、区块产线编译器、战略派兵/AI 预留接口（9 个类） |
| `CampaignRules.java` | 新增 `globalLogistics` 开关，默认 `true` |
| `SectorInfo.java` | 新增 `strategyProfile` 字段；退出战场时编译产线画像 |
| `Universe.java` | 秒级 tick 接入 `GlobalStrategyManager`；开启物流后关闭原版批次结算 |
| `tests/src/test/java/GlobalLogisticsTests.java` | JUnit 5 单元测试 —— **已在 CI 通过**（tests:test，2m30s） |
| `.github/workflows/build-apk.yml` | 云端构建 Debug APK 的工作流 |

### 构建修复（原始交付包无法编译）

1. `GlobalStrategyManager.ensureProfile()`：lambda 捕获了会被重新赋值的局部变量 → 改用 `final` 局部变量
2. `build-apk.yml`：`android-actions/setup-android@v3` 在新版 runner 上失败（`Failed to find package 'tools'`）→ 改用 runner 预装 SDK + 直接调用 `sdkmanager`
3. debug 构建关闭 R8：AGP 8.2 的 `merged_java_res/base.jar` 缺失 bug

### 仓库治理

- 关闭上游的 `Deployment` 工作流：它由 `v*` 标签触发，需要 Anuken 的私有 token，在 fork 中必然失败（曾导致 v1.0 标签触发一次红色邮件）

### 已知限制

- debug 未混淆包，体积约 61 MB，启动略慢
- 关闭 R8 后缺少 default interface 脱糖，**API < 24（Android 7 以下）可能崩溃**；Android 7+ 正常
- `globalLogistics` 没有 UI 开关，只有代码默认值；实际效果**未经真机验证**
- `tests/` 单元测试已在 CI 通过（含 GlobalLogisticsTests）

---

## 版本规则

- 版本号形如 `v1.5`、`v1.6`，每次上游 AI 交付并成功装包后递增
- 每次打包在 git 打同名 tag，并把 APK 附到同名 Release（不走 30 天过期的 Actions artifact）
- 文件改动一律记录在上表，避免新旧版本混淆
