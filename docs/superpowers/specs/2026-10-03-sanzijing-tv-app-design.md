# 三字经学习 App（Android TV 版）设计文档

日期：2026-10-03
状态：已与用户逐节确认（方案选型、页面交互、视觉风格、数据/错误/测试）

## 1. 概述

在 Android TV 上构建一款《三字经》亲子共学应用：家长用遥控器操作，带孩子逐句朗读、讲解释义典故，并追踪学习进度。应用完全离线，全部内容与音频打包在 APK 内。

**目标用户与场景**：亲子共学。家长主导遥控器操作，孩子跟随大字拼音跟读；界面可承载注释、译文、典故等讲解性内容。

**功能范围（已确认）**

- 朗读 + 跟读：正文带拼音大字展示，逐句预置音频朗读，支持连读
- 释义 + 典故：每个单元的注释、白话译文，可选典故小故事（纯文字）
- 学习进度追踪：已读/已熟读状态、每日学习时长、连续学习天数、章节进度

**明确不做**：测验/游戏、语音识别评测、多用户档案、账号与云同步、在线内容服务、手机/平板适配、竖屏布局。

## 2. 总体方案

**骨架（方案 A，已确认）**：目录式导航 + 沉浸读书页。首页提供「继续学习」大卡片、学习报告、设置入口和章节卡片；读书页全屏沉浸，一屏一句。

**视觉风格（方案 C，已确认）**：童趣暖阳 · 奶油圆角。奶油渐变底色、白色大圆角悬浮卡片、珊瑚橙主色。

## 3. 技术架构

| 项 | 选择 | 理由 |
|---|---|---|
| 语言 / UI | Kotlin + Compose for TV（`androidx.tv` Material3） | 用户指定；TV 焦点体系原生支持 |
| SDK | minSdk 24 / targetSdk 35 | 覆盖 Android 7+ 主流电视盒子 |
| 导航 | Navigation Compose，单 Activity | 页面少（首页/读书/报告/设置） |
| 进度存储 | Room | 每单元独立状态，需聚合统计查询 |
| 设置存储 | DataStore Preferences | 少量键值 |
| 依赖注入 | 手动 DI（AppContainer） | 规模小，避免 Hilt 构建复杂度 |
| 序列化 | kotlinx.serialization | 解析 assets 内容 JSON |
| 模块 | 单 `:app` 模块 | 小应用 |

构建工具链（AGP / Kotlin / Compose BOM）版本在实现计划中按撰写时最新稳定版确定。开发环境：Android Studio 位于 `D:\develop\soft\Android Studio`，Android SDK 位于 `D:\develop\androidsdk`（`local.properties` 指向后者）。

**数据流（UDF）**：每页一个 ViewModel，对外暴露 `StateFlow<UiState>`，用户事件以函数回调传入；ViewModel 经 Repository 访问内容与进度。内容在进程内只读（启动时解析一次并缓存），进度读写走 Room Flow，UI 自动响应。

## 4. 内容数据模型

**学习单元 = 六字一组**（两个三字短语，如「人之初，性本善」）。全文采用通行版《三字经》，预计约 190 个单元，划分为 6 章（预期规模，以实际整理为准）：

| 章节 | 预期句数 |
|---|---|
| 教育之要 | 28 |
| 蒙学伦理 | 36 |
| 经典导读 | 40 |
| 历史纲鉴 | 52 |
| 勤学典故 | 22 |
| 劝学结语 | 12 |

assets 布局：

```
assets/
  content/sanzijing.json
  audio/u001.mp3 … u190.mp3
```

JSON schema：

```json
{
  "version": 1,
  "chapters": [
    {
      "id": "ch01",
      "title": "教育之要",
      "units": [
        {
          "id": "u001",
          "phrases": [
            { "hanzi": "人之初", "pinyin": "rén zhī chū" },
            { "hanzi": "性本善", "pinyin": "xìng běn shàn" }
          ],
          "annotation": "之：的、助词。……",
          "translation": "人出生之初，禀性本来都是善良的……",
          "storyTitle": "孟母三迁",
          "story": "……（可选，无则省略两字段）"
        }
      ]
    }
  ]
}
```

约束：`id` 全文唯一且按序连续（`u001` 起）；`phrases` 恒为 2 项；每单元必有 `annotation` 与 `translation`；`storyTitle`/`story` 成对出现、可省略。

## 5. 音频方案

- **每单元一个 MP3**，命名与单元 id 一致（`u001.mp3`），置于 `assets/audio/`
- **开发期占位音频**由脚本批量生成：优先 edge-tts 高质量中文女声（需联网）；不可达时以 Windows 内置中文语音兜底
- **可替换**：后续拿到真人录制音频，按同名文件覆盖 assets 目录即可，App 零改动；内容完整性测试保证命名匹配
- **播放器**：系统 `MediaPlayer` 封装（`data/audio/AudioPlayer`），播放 assets 短音频，零额外依赖；播放完成回调驱动"记已读 + 连读推进"；若未来需要无缝连播再评估 Media3

## 6. 页面与交互设计

### 6.1 首页

两级焦点行，打开 App 默认焦点在「继续学习」：

- 顶部标题「三字经」+ 学习概况（总进度、连续学习天数）
- 第一行：**继续学习**大卡片（下次要读的句子 + 章节名 + 总进度条）、学习报告、设置
- 第二行：6 个章节卡片（章节名、句数、章节进度，进行中的章节高亮标记）

不做独立章节目录页与释义浏览页（首页与读书页已覆盖）。

### 6.2 读书页（核心页面）

**直接键位映射**（非焦点导航型页面），一屏一句，六字分两行大字展示，汉字上方标注拼音：

- 进入单元**自动朗读**（设置可关）
- 底部**键位提示条常驻**，保证遥控操作可发现性
- 顶部：章节名 + 单元序号（如 9/40）+ 已熟读标记 + 播放状态
- 翻到章首/章尾时跨章连续（长按方向键可快速连翻）

**释义典故面板**：按 OK 从右侧滑出（约 42% 宽），显示注释、译文、典故；正文缩小左移；再按 OK 或返回关闭。家长讲解用，不打断跟读沉浸。

**操作面板**：菜单键呼出，含连读开关、标记熟读、跳转章节。

**连读模式**：当前句播完后自动跳下一句继续播，可实现"放着让孩子跟读"；开关持久化。

### 6.3 学习报告页

总进度环（已读/全部）、章节进度条列表、已熟读数、连续学习天数、累计学习时长、今日学习时长。

### 6.4 设置页

自动朗读开关（默认开）、连读开关（默认关）、拼音显示开关（默认开）、清除学习进度（二次确认对话框）、版本与内容来源说明。

### 6.5 遥控器键位映射

| 页面 | 按键 |
|---|---|
| 读书页 | ◀ ▶ 前后句（长按连续）；▲ ▼ 跳上/下一章开头；OK 开/关释义面板；播放/暂停键 重读当前句；长按 OK 标记/取消熟读；菜单键 操作面板；返回 退出（面板开时先关面板） |
| 首页/报告/设置 | 方向键移动焦点；OK 进入/切换；返回 回上级 |
| 全局 | 焦点高亮可见（卡片放大 + 珊瑚描边 + 阴影）；无触摸依赖；长按方向键支持连续滚动 |

### 6.6 视觉风格：童趣暖阳

- 底色：奶油渐变 `#FFF8EC → #FFE6C7`，右上/左下大圆形柔光装饰
- 卡片：白色，大圆角（16–24dp），柔和投影；焦点态 = 珊瑚橙描边 + 放大 + 阴影增强
- 主色：珊瑚橙 `#FF8A65`（焦点、强调、进行中标记）；拼音色：蜜橙 `#D4954A`
- 正文：楷体系字体；汉字特大号，拼音中号置于上方
- 朗读状态：珊瑚橙胶囊「♪ 正在朗读 · 跟着一起读」
- 电视浅色 UI 在暗光环境偏亮的 tradeoff 已由用户知情选择；焦点描边与阴影保证遥控可视性

## 7. 进度数据设计

Room（v1）两张表 + DataStore：

```
unit_progress: unitId TEXT PK, status INTEGER(0未学/1已读/2已熟读),
               readCount INTEGER, lastReadAt INTEGER, markedAt INTEGER NULL
daily_stats:   date TEXT PK(yyyy-MM-dd), secondsStudied INTEGER, unitsRead INTEGER
```

计分规则：

- 播放某单元音频完成 → `readCount+1`，`status` 至少置 1，`daily_stats.unitsRead+1`
- 长按 OK → `status=2` 并记 `markedAt`；再次长按回退为 1
- 学习时长：读书页 RESUMED 且非面板态时按秒累计入当日 `daily_stats`
- **继续学习 = 全文顺序第一个「未学」单元**（跳章学习不丢位置；全部学完则回到最后互动单元）
- 连续学习天数：按 `daily_stats` 日期连续性计算

DataStore 键：`auto_play`（默认 true）、`continuous_play`（默认 false）、`show_pinyin`（默认 true）。

## 8. 错误处理

| 场景 | 处理 |
|---|---|
| 音频文件缺失 | 跳过播放，顶部轻提示「音频缺失」，不崩溃（内容完整性测试保证发布包不出现） |
| 内容 JSON 解析失败（包损坏） | 全屏错误页「内容数据异常，请重新安装」 |
| 音频解码/播放异常 | 捕获，toast 提示，流程继续 |
| 数据库不可用 | 进度功能降级并提示，阅读仍可用 |

## 9. 测试策略

按优先级：

1. **内容完整性测试**：JSON 可解析；单元 id 唯一连续；每单元拼音音节数 = 汉字数（去标点）；注释译文齐全、典故成对；**每个单元在 assets/audio/ 有同名 mp3**
2. ProgressRepository 单元测试（Room in-memory）：状态标记、继续学习计算、统计聚合
3. ReaderViewModel 单元测试：首末句翻页边界、连读推进、自动播放状态
4. Compose UI 测试（少量）：首页焦点进入读书页、OK 开关释义面板
5. 手动验收：Android TV 模拟器（1080p XML 遥控 profile）+ 键位走查

## 10. 工程结构

```
app/src/main/java/com/rakor/sanzijing/
  MainActivity.kt
  SanzijingApp.kt            # Application + AppContainer（手动 DI）
  data/
    content/                 # 内容模型、JSON 解析、ContentRepository（内存只读）
    db/                      # AppDatabase、实体、DAO
    prefs/                   # SettingsRepository（DataStore）
    audio/                   # AudioPlayer（MediaPlayer 封装）
    progress/                # ProgressRepository
  ui/
    theme/                   # 颜色/字体/主题（童趣暖阳）
    navigation/
    home/  reader/  report/  settings/   # 各页 Screen + ViewModel
tools/
  gen_content.py             # 全文→JSON（pypinyin 拼音）
  gen_audio.*                # edge-tts 批量生成 uXXX.mp3
```

applicationId：`com.rakor.sanzijing`；应用显示名：三字经；manifest 声明 `LEANBACK_LAUNCHER`、`android:banner`、`usesCleartextTraffic=false`、无触摸必需声明。

## 11. 实现阶段的内容准备

1. 通行版《三字经》全文整理为 6 章 × 六字单元；pypinyin 生成拼音
2. 注释、白话译文全量撰写；典故故事按单元选用（孟母三迁、黄香温席、孔融让梨、悬梁刺股、囊萤映雪、韦编三绝等），初稿交用户审阅
3. edge-tts 生成占位音频（约 190 个文件）

## 12. 验收标准

- TV 模拟器可安装启动，遥控器完成全流程：首页 → 读书 → 翻句 → 释义 → 熟读标记 → 报告 → 设置
- 每单元音频可播放；连读模式可完整过一章并正确累计进度
- 内容完整性测试全部通过
- 替换 assets 音频文件后无需改代码即生效
- 全离线：飞行模式下全部功能可用
