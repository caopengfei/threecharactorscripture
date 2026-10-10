# 启动优化（主线程 JSON 解析异步化 + R8）

日期：2026-10-10
状态：已验证（模拟器实测通过，单测/构建全绿）
参考：`D:\develop\src\rakor\tangshi\docs\superpowers\specs\2026-10-10-startup-optimization.md`（唐诗项目同日方案，16.4s → 0.85s）

## 背景与问题

TV 盒子/低端设备（minSdk 23，dex2oat 可能失败被迫解释执行）上启动白屏、首帧慢。
与唐诗项目同根因：**首帧必经路径同步解析内容 JSON，且 release 未做 R8 压缩**——
解释执行下 kotlin 类加载与 JSON 解析开销被急剧放大，dex 越大首启越慢。

本项目内容库：`assets/content/sanzijing.json`，144KB / 6 章 / 192 句。

## 诊断结论

1. **`HomeViewModel` 构造路径同步触碰 JSON（可修）**
   `_state = MutableStateFlow(HomeUiState(totalCount = contentRepo.units.size))`
   在主线程首次触碰 `ContentRepository.content` 的 `lazy`，同步执行 144KB
   `kotlinx.serialization` 解析；`compute()` 也在主线程多次调 `contentRepo.units`。
2. **无预热**：`AppContainer` 全是 `by lazy`，首个触碰内容的主线程要等解析完成
   （`lazy` 是 synchronized——预热未做时主线程会阻塞在锁上直到解析结束）。
3. **`ContentRepository.units` 每次 getter 都 `flatMap` 重建列表**，`compute` 热路径一次调 3 处。
4. **release 包未压缩**：`isMinifyEnabled = false`，且未配置正式签名。

## 方案与改动

| 文件 | 改动 |
|---|---|
| `SanzijingApp.kt` | `AppContainer` 新增 `appScope` 与 `warmUp()`：`appScope.launch { contentRepository.content }`，IO 线程预热 JSON 解析；`SanzijingApp.onCreate` 调用 |
| `ui/home/HomeViewModel.kt` | 异步化：初始 `HomeUiState()` 不触碰 `contentRepo`；新增 `contentFlow = flow { emit(contentRepo.content) }.flowOn(Dispatchers.IO)` 作为 `combine` 第三路（原两路为 progress/dailyStats）；`compute` 改收 `ContentFile`，内部一次 `flatMap` 复用——主线程构造/收集路径零 JSON 解析 |
| `ui/home/HomeScreen.kt` | 加载中留白：`ready` 前统计行显示"加载中…"（不闪 `0 / 0`）、继续学习卡显示"内容加载中…"；`ready` 语义天然保证 content 已就绪（combine 需三路各至少一个值） |
| `data/content/ContentRepository.kt` | `units` 由 `get() = chapters.flatMap {...}` 改为 `by lazy` 缓存，消除热路径重复重建 |
| `app/build.gradle.kts` | release `isMinifyEnabled = true`（R8）；正式 release 签名 `D:/develop/key/rakor.jks`（alias=rakor），密码经 `RELEASE_STORE_PASSWORD`/`RELEASE_KEY_PASSWORD` gradle property 读取，存于本机 `~/.gradle/gradle.properties`（不入库）。**注**：最初按唐诗方案复用 debug 签名，后按正式分发要求切换为独立签名——签名已变，装过旧包的设备须卸载重装 |
| `app/proguard-rules.pro` | kotlinx.serialization keep 规则（R8 full mode 下 `Json` 反射查找 `Companion.serializer()` 需要） |

未改：`ReportViewModel`/`ReaderViewModel` 构造仍触碰 `contentRepo`，但它们不在首帧路径，
且首页 `ready=true` 即意味着 JSON 已解析完毕，到达它们之前内容必已就绪。

## 实测结果（模拟器，release 包）

| 场景 | Displayed / TotalTime |
|---|---|
| 冷启动第 1 次（安装后） | 930ms |
| 冷启动第 2 次 | 453ms |
| 冷启动第 3 次 | 478ms |

- 三次冷启动均无 `FATAL EXCEPTION`。
- APK 体积：debug 15.9MB → release **6.3MB**（R8）。
- release 包签名：`rakor.jks`（证书 SHA-256 `15ddeeaaa2b3…`），与 debug 签名（`f7e0b97f…`）
  不同——覆盖安装旧包会报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，须先卸载重装；
  新签名包模拟器实测冷启动 481ms、无崩溃。
- 截图验证：首页统计 `已读 7 / 192 句`（异步 total 正确）、继续学习卡、6 章节进度、
  焦点正常；阅读页拼音/注释/译文/典故/音频播放全部正常——R8 下序列化、Room、
  DataStore、MediaPlayer 运行时均无问题。
- 构建与测试：`gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  全绿（38 个单测通过，含异步化后的 `HomeViewModelTest`）。
- dexdump 确认 release `classes.dex` 保留 `ContentFile$$serializer`。

## 备注

- 优化前基线未实测（问题由代码审查按唐诗方案同模式确认；改动以正确性 + 体积 +
  冷启动绝对值验证）。若需对比，可 `git stash` 后重装 debug 包取 `Displayed`。
- 后续可选：kapt → KSP（Room）、`ContentRepository` 单测覆盖 warmUp 幂等性。
