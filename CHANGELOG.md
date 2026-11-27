# 变更记录 (CHANGELOG)

> 本仓库 = 可构建的 Mindustry 分支，用于产出可安装的 APK。
> 纯源码（仅供 AI 阅读）在 [mindustry-src](https://github.com/jinitaimei436/mindustry-src)。

## v3.9 — 2026-11-27（跨区块部队空运）

### 新增

| 模块 | 说明 |
|---|---|
| 跨区块空运 | `SectorTroopAirliftManager`(1103) / `TroopAirliftMath`(1186) / `TroopAirliftAssets`(163) |
| 空运方块 | `world/blocks/campaign/TroopLaunchPad`(1002)：区块间单位投送 |
| 落地机制 | 落地后恢复单位编队；新增「落地部署」「轨道创伤」状态效果 |
| 滩头登陆 | 区块进入逻辑支持**无核心**登陆 |
| 扩展接口 | `CrossSectorTroopExtension` |
| UI | `PlanetDialog`(+1401/−236)、`HudFragment`(+55) |
| 测试 | `TroopAirliftTests`(401) |

### 同时合入（v3.2，此前未打包）

| 模块 | 说明 |
|---|---|
| 持久化优化 | 物流/电网管理器改为**增量脏状态持久化**，存档/退出时显式 flush（替代周期性全量序列化） |
| 战略层 | `PlanetNetworkSolver`(+105/−43)、`StrategySimulationEngine`(+42/−9) |

### 装包侧修复

`HudFragment.java:634/674`：`Table.pad(float)` 不存在（`pad` 是 `Cell` 的方法），改为 `Table.margin(float)`，共 2 处。

### 未合并内容

- `public/assets/aistudio/*.jpg`（AI Studio 截图残留）
- `AI交接文档_v3.2.md`（交接文档，不属于构建内容）

### 装包侧复核

| 检查项 | 结果 |
|---|---|
| `Comparator.comparing*` / `List.of` / `Map.of` 实际调用 | ✅ 无（新代码注释里还主动声明了遵守该约束） |
| `addLast(item.id)`（short 未强转） | ✅ 无 |
| `SurgeSectorPowerHub` 的 `mindustry.type` import | ✅ 保留 |
## v3.1 — 2026-11-22（全球物流强制化 + 跨区块扩展接口）

### 变更

| 模块 | 说明 |
|---|---|
| 全球物流强制化 | `CampaignRules` 调整，物流不再依赖开关；`Universe` 移除了旧的批次结算逻辑（−57 行） |
| 跨区块扩展接口 | 新增 `CrossSectorExtension` / `CrossSectorConveyorExtension` / `CrossSectorPowerExtension` |
| 战略层 | `GlobalStrategyManager`、`SectorProfileCompiler`、`PlanetNetworkSolver`、`SectorStrategyProfile` 增强 |
| 区块物流 | `SectorConveyorManager` / `SectorConveyorMath` 解析与配对逻辑增强 |
| 测试 | 新增 `RealTimeSettlementTests` |

### 装包侧复核

| 检查项 | 结果 |
|---|---|
| `Comparator.comparing*` 等 Java 8 接口静态方法 | ✅ 无 |
| `addLast(item.id)`（short 未强转） | ✅ 无 |
| `SurgeSectorPowerHub` 的 `mindustry.type` import | ⚠️ 上游 src 仍缺失，构建仓库保留已修版本（该文件本版未改动，不受影响） |
## v3.0 — 2026-11-22（存档持久化 + UI/连线修复）

### 新增 / 修复

| 内容 | 说明 |
|---|---|
| 存档持久化 | 新增 `SaveWriteEvent` / `SaveLoadEvent` 监听，区块电网与物流状态随存档读写 |
| 手动保存 | `Universe` 中实现手动触发保存 |
| 重载一致性 | `PowerGraph` 增加电力枢纽校验，读档后确保电网拓扑一致 |
| UI 修复 | `PlanetDialog` 布局与电力连线逻辑调整（+198/−146） |
| 物流管理器 | `SectorConveyorManager` 端口/配对逻辑重构（+54/−39） |
| 电网管理器 | `SectorPowerGridManager` 读写与校验增强（+79/−13） |

### 装包侧检查

- 版本号 `forkVersion = v3.0`，`VERSION` 同步为 `3.0`
- 复查既有两类构建坑，均未复发：
  - 无 `Comparator.comparing*` / `List.of` / `Map.of` 等 Java 8 接口静态方法
  - 无 `addLast(item.id)`（`short` 未转 `int`）
## v2.8.3 — 2026-11-22（修复：点「配对」必闪退 / 跨区块电网潜在闪退）

### 症状（用户实测）

打开「资源视野」→ 点「配对」→ 必定闪退。logcat：

```
java.lang.NoClassDefFoundError: Failed resolution of: Ljava/util/Comparator$-CC;
  at SectorConveyorMath$PlanetConveyorTopology.getSectorPorts(SectorConveyorMath.java:378)
  at SectorConveyorManager.getAvailableAdjacentTargets(SectorConveyorManager.java:213)
  at PlanetDialog.showPortPairingDialog(PlanetDialog.java:1932)
```

### 根因

代码使用了 **Java 8 接口静态方法** `Comparator.comparingInt(...)`：

```java
out.sort(Comparator.comparingInt(p -> p.portNumber));
```

本项目 `minSdk = 21`（< 24）且未启用 core library desugaring，构建时 R8 把调用重写为对合成类
`java.util.Comparator$-CC` 的调用，但该类未被打进 APK → 运行到该行即 `NoClassDefFoundError`。

### 修复

改为显式比较器 lambda，不再依赖接口静态方法：

| 文件:行 | 修改 |
|---|---|
| `SectorConveyorMath.java:378` | `Comparator.comparingInt(p -> p.portNumber)` → `(a, b) -> Integer.compare(a.portNumber, b.portNumber)` |
| `SectorPowerGridMath.java:604` | `Comparator.comparingInt(n -> n.sectorId)` → `(a, b) -> Integer.compare(a.sectorId, b.sectorId)` |

> 第二处尚未被玩家触发（跨区块电网的多节点排序路径），属同类潜在地雷，一并修掉。
> 全仓库已确认无其他 `Comparator.comparing*` / `List.of` / `Map.of` 等接口静态方法用法。
## v2.8.2 — 2026-11-22（修正：区块传送带方块贴图方向）

### 问题

v2.8.1 导入区块传送带方块贴图时，把 `input` / `output` 与代码里的 `receiver` / `sender` 映射反了：
发射端（sender）错用了接收端的美术，接收端（receiver）错用了发射端的美术，UI 图标（`-full`）也随之装反。

### 修复

8 张贴图全部对调：

| 原提供文件 | v2.8.1（错误） | v2.8.2（正确） |
|---|---|---|
| `*-sector-input.png` | `*-sector-conveyor-receiver.png` | **`*-sector-conveyor-sender.png`** |
| `*-sector-output.png` | `*-sector-conveyor-sender.png` | **`*-sector-conveyor-receiver.png`** |

方块内部名、尺寸、代码均未改动，仅替换贴图文件内容。
## v2.8.1 — 2026-11-22（区块传送带物流 + 外观更新）

### 新增功能

| 模块 | 内容 |
|---|---|
| 区块传送带物流 | `SectorConveyorManager` / `SectorConveyorMath`：跨区块物品传输 |
| 4 个新方块 | `basic-sector-conveyor-sender/receiver`（3×3）、`advanced-sector-conveyor-sender/receiver`（4×4） |
| 平衡调整 | 移速修饰、EMP 效果、超速投影器数值微调 |
| UI | 星球界面新增区块物流相关面板 |

### 美术资源

- **替换**：`surge-sector-power-hub*` 全部 38 张（新版 vanilla 风格，128×128）
- **新增**：区块传送带方块 8 张

> 提供的贴图按 `input/output` 命名，代码中的方块名是 `receiver/sender`，导入时做了对应映射：
> `sector-input → sector-conveyor-receiver`（物品流入本区块）、`sector-output → sector-conveyor-sender`（流出到跨区块网络）。
> `basic` 为 96×96（3×3），`advanced` 为 128×128（4×4），与代码 `size` 一致。
## v2.5 — 2026-11-22（跨区块电网 + 单位编队 + 电力枢纽方块）

### 新增功能

| 模块 | 内容 |
|---|---|
| `game/power/` | 跨区块电网：`SectorPowerGridManager` / `SectorPowerGridMath`，多区块实时电力与共享蓄电池互通 |
| `world/blocks/power/SurgeSectorPowerHub` | 4×4「巨浪区块电力枢纽」方块，每区块限 1 座，半径 20 格并入跨区块电网（纯传输无蓄电） |
| 动画控制器 | 启动 / 运行 / 关闭三态状态机 + 帧序列自动扫描（`HubAnimationController`） |
| `ai/formation/` | 单位编队系统：`FormationManager` / `FormationMath` / `TacticalFormation` |
| `net/FormationPacket` | 编队同步网络包 |
| UI | 星球界面「电力视野」面板、单位指挥面板「编队」区块、方块状态条 |

### 美术资源（本次导入）

`core/assets-raw/sprites/blocks/power/` 下新增 38 张 128×128 PNG：

- `surge-sector-power-hub.png`（基础外观）+ `-full.png`（图标）
- `-idle.png`（待机）、`-glow.png`（加色发光层）
- `-start0..11.png`（启动 12 帧）
- `-run0..11.png`（运行循环 12 帧）
- `-stop0..9.png`（关闭 10 帧）

> 命名严格匹配 `SurgeSectorPowerHub.load()` 的自动扫描规则：`<块名>-<前缀><连续编号>`，起始 0、不跳号。

### 装包侧处理

- 合并 `1a08d281`（2.5 主功能）与 `1c4338bd`（动画控制器）两个上游提交
- 未合并上游删除的 `.github/`、`VERSION`（构建仓库自带）
## v2.0.1 — 2026-11-22（V2 多区块合作系统）

### 新增（上游 AI 交付）

- `core/src/mindustry/game/coop/` 全新包：多区块合作协调、区块占用、远端区块桥接、战役数据分片传输、权限
- `net/CampaignPacket`：战役相关网络包
- `NetServer` / `NetClient` / `Net` / `NetConnection` / `Control` / `GameState` / `WorldReloader`：多人战役同步改造
- `PlanetDialog` / `HostDialog` / `PlayerListFragment` / `ResearchDialog` 等 UI 适配
- `tests/.../MultiSectorCoopTests.java`：新增单元测试

### 合并时的处理（装包侧）

- **未采用上游自带的 `.github/workflows/build-apk.yml`**：它缺少 `tools:pack` 步骤，会重新制造「缺图集闪退」
- **未合并** `public/assets/aistudio/*.txt`（AI Studio 导出残留）与 src 仓库的 `README.md`
- `JsonIO.java` 的 6 处 `java.util.Map.Entry` 已沿用之前的修复（V2 代码里已正确）

### 两个闪退问题的最终处置（已固化进仓库，不再依赖 CI 补丁）

| 问题 | 修复位置 |
|---|---|
| `AbstractMethodError: arc.Application.addListener`（R8 未脱糖） | `android/build.gradle`：AGP `8.2.2 → 8.13.2`，保持 `minifyEnabled = true` |
| `FileNotFoundException: sprites/sprites.aatls`（图集没进包） | `android/build.gradle`：新增 `mergeDebugAssets/mergeReleaseAssets dependsOn ':tools:pack'` |

> 也就是说：现在任何人 clone 本仓库直接执行 `./gradlew android:assembleDebug`，都能得到可正常启动的 APK，不再需要 CI 里的临时补丁。
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
