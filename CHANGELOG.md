# 变更记录 (CHANGELOG)

> 本仓库 = 可构建的 Mindustry 分支，用于产出可安装的 APK。
> 纯源码（仅供 AI 阅读）在 [mindustry-src](https://github.com/jinitaimei436/mindustry-src)。

## v1.6.2 — 2026-11-22（修复：进入游戏 1 秒后闪退）

### 症状

v1.6.1 启动正常，但进入加载界面约 1 秒后崩溃：

```
arc.util.ArcRuntimeException: Error reading pack file: sprites/sprites.aatls
  at arc.graphics.g2d.TextureAtlas$TextureAtlasData.<init>(TextureAtlas.java:365)
  at mindustry.ClientLauncher.update(ClientLauncher.java:236)
Caused by: java.io.FileNotFoundException: sprites/sprites.aatls
```

### 根因（构建顺序 bug）

`android/build.gradle` 只声明了 `assembleDebug.dependsOn ':tools:pack'`，**没有约束 `mergeDebugAssets`**。
实际执行顺序变成：

```
02:56:45  > Task :android:mergeDebugAssets     ← 先把 assets 打进 APK（此时还没有图集）
02:57:23  > Task :tools:pack                   ← 之后才生成 sprites.aatlas / .aatls / .png
```

结果 APK 里只有 13 张零散 PNG，没有 `sprites.aatls`，进主菜单读图集即崩。

### 修复

- CI 中**先单独执行 `./gradlew tools:pack`**，再执行 `android:assembleDebug`，确保图集先落盘
- CI 校验步骤增加两项硬性检查，任一缺失即构建失败：
  - APK 内必须存在 `assets/sprites/sprites.aatl*`
  - dex 内必须存在 `Application$-CC`（脱糖伴随类）

### 说明

- 代码与 v1.6 / v1.6.1 完全一致，仍然只改构建流程
- 这类问题在本地开发时不出现（本地已有生成好的图集），只在干净 CI 环境暴露
## v1.6.1 — 2026-11-22（紧急修复：启动闪退）

### 问题

v1.5 / v1.6 安装后**一打开就闪退**：

```
java.lang.AbstractMethodError: abstract method "void arc.Application.addListener(arc.ApplicationListener)"
  at arc.backend.android.AndroidApplication.init(AndroidApplication.java:112)
  at mindustry.android.AndroidLauncher.onCreate(AndroidLauncher.java:58)
```

### 根因

为绕开 AGP 8.2 的 `merged_java_res/base.jar` bug，之前的 CI 在构建前把 `minifyEnabled` 关掉了。
但 **R8 同时负责 default interface method 的脱糖**（`android/build.gradle` 里本来就有注释警告这一点），
关掉后 `arc.Application` 的默认方法退化为抽象方法，启动即 `AbstractMethodError`。
这是**装包配置错误，不是 1.6 代码的问题**。

### 修复

- **升级 AGP 8.2.2 → 8.13.2**（真正的原因：8.2.2 的 R8 读不到 `merged_java_res/base.jar`，后续版本已修）
- 恢复 R8 开启（`minifyEnabled = true`、`shrinkResources = true`），不再 sed 覆盖 `android/build.gradle`
  并额外提供 Java 资源文件作为兜底
- CI 增加校验：检查 APK 的 dex 中是否有 `Application$-CC` 脱糖伴随类，缺失即视为构建失败

### 说明

- 代码与 v1.6 完全一致，仅构建配置变化
- 本版产物**开启 R8**，体积应明显小于 v1.6 的 61 MB
## v1.6 — 2026-11-22

战斗节奏平衡系统 + 战略层增强。改动 14 个文件（12 改 + 2 新增），约 +1120 行。

### 新增

| 文件 | 说明 |
|---|---|
| `core/src/mindustry/content/CombatPaceBalancer.java` | 新增：全局战斗节奏修饰器，按难度缩放伤害、波次间隔、建造速度 |
| `tests/src/test/java/mindustry/content/CombatPaceBalancerTests.java` | 新增：对应单元测试（354 行） |

### 修改

| 文件 | 说明 |
|---|---|
| `game/Rules.java` (+25) | 支持战斗节奏修饰的幂等应用 |
| `io/JsonIO.java` (+153) | 序列化能力增强（供平衡配置读写） |
| `core/ContentLoader.java` (+1) | 注册相关类型 |
| `core/Version.java` (+2) | 版本信息更新 |
| `maps/Map.java` (+4) | 小改 |
| `game/CampaignRules.java` (+3) | 新增字段 |
| `game/strategy/GlobalStrategyManager.java` (+159/−17) | 战略调度增强 |
| `game/strategy/SectorProfileCompiler.java` (+59/−18) | 产线编译增强 |
| `game/strategy/core/PlanetNetworkSolver.java` (+176/−56) | 物流求解器增强（多跳/环链处理） |
| `game/strategy/core/StrategySimulationEngine.java` (+31/−12) | 模拟引擎增强 |
| `game/strategy/core/SectorStrategyProfile.java` (+13) | 区块画像字段扩展 |
| `game/strategy/core/LogisticsRoute.java` (+5/−1) | 路由字段扩展 |

### 说明

- `editor/DrawOperation.java`、`editor/OperationStack.java`、`graphics/Voronoi.java` 在 AI Studio 同步时只有权限位变动、内容无差异，已忽略。
- 1.6 代码由上游 AI 在 AI Studio 生成，经 `mindustry-src` 仓库中转合并进本仓库。

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
