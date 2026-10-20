# 三字经 · Android TV 亲子共学

面向客厅大屏的《三字经》学习应用：**全离线、纯遥控器操作**，逐句大字拼音 + 朗读跟读 + 注释译文典故 + 学习进度追踪。

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)
![Compose for TV](https://img.shields.io/badge/Compose%20for%20TV-1.1.0-4285F4?logo=android&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0-C0392B)
![License](https://img.shields.io/badge/license-MIT-2EA44F)

## 简介

用 Jetpack Compose for TV 实现的 Android TV 应用，把《三字经》拆成 **6 章 / 192 个单元 / 384 个三字短语**，一句一屏：汉字大字 + 拼音、内置朗读音频、注释与译文、可选典故。适合家长陪孩子在电视前一句一句跟读，进度、熟读标记与学习时长都会自动记录。

所有内容与音频都打包在 APK 内，**无需网络、无广告、无账号**。

## 功能特性

- **逐句精读**：一屏一句（两个三字短语），课本式「楷体大字 + 拼音」混排，远距离可读
- **朗读与跟读**：内置 192 条逐句音频，支持重读、暂停 / 继续、播完自动下一句的连读模式（读完循环）
- **释义常驻**：注释、译文、典故（有则显示）显示在正文下方，无需按键唤出
- **进度与熟读**：已读 / 熟读两级标记，首页显示总进度、连续学习天数、今日时长
- **学习报告**：环形总进度 + 已读 / 熟读句数 + 累计与今日分钟数 + 每章分项进度
- **章节导航**：主页六章卡片直达，阅读页 ↑↓ 换章、菜单键面板可跳章
- **贴心细节**：全程屏幕常亮；退到后台立即停止朗读，回前台续读离开时那一句
- **低配兼容**：支持 API 23 起的老电视盒子（`java.time` 走脱糖），release 开启 R8 压缩以缩短首启时间

## 遥控器操作

| 按键 | 阅读页行为 |
| --- | --- |
| ← / → | 上一句 / 下一句 |
| ↑ / ↓ | 上一章 / 下一章 |
| OK 短按（连读关闭） | 重读当前句 |
| OK 长按 ≥600ms（连读关闭） | 标记本句熟读 / 取消熟读 |
| OK 短按（连读开启） | 暂停 / 继续 |
| 播放暂停媒体键 | 重读当前句 |
| 菜单键 | 呼出操作面板：连读开关、本句熟读、跳转章节 |
| 返回键 | 关闭面板；面板已关则返回首页 |

## 内容数据

`app/src/main/assets/content/sanzijing.json`（由工具脚本生成，勿手改）：

| 章节 | 标题 | 单元数 |
| --- | --- | --- |
| ch01 | 教育之要 | 14 |
| ch02 | 蒙学伦理 | 39 |
| ch03 | 经典导读 | 34 |
| ch04 | 历史纲鉴 | 60 |
| ch05 | 勤学典故 | 33 |
| ch06 | 劝学结语 | 12 |

每个单元固定包含两个三字短语（`ContentIntegrityTest` 守卫该契约），字段为 `hanzi`、`pinyin`、`annotation`、`translation` 及可选的 `storyTitle` / `story`。

## 技术栈

- **UI**：Jetpack Compose + `androidx.tv:tv-material` 1.1.0（Compose BOM 2026.02.01）、Navigation Compose
- **数据**：Room 2.7.2（学习进度与每日统计）、DataStore Preferences（阅读设置）、kotlinx.serialization（内容 JSON）
- **音频**：`MediaPlayer` 播 `assets/audio/uXXX.mp3`（`noCompress += "mp3"`，`openFd` 要求未压缩）
- **构建**：AGP 8.8.2、Kotlin 2.1.20、Gradle 8.10.2、JDK 17、`compileSdk` / `targetSdk` 35、`minSdk` 23
- **测试**：JUnit4 + Robolectric + coroutines-test（单元测试），Compose UI Test（设备冒烟测试）

## 快速开始

### 环境要求

- JDK 17（Gradle 8.10.2 尚不支持更高版本的 JBR，若用 Android Studio 自带 JBR，请显式指定 JDK 17）
- Android SDK Platform 35
- 设备或模拟器需支持 Leanback（Android TV）

`local.properties` 需写入本地 SDK 路径：

```properties
sdk.dir=/path/to/Android/Sdk
```

> `gradle.properties` 中的 `org.gradle.java.home` 与 HTTP 代理项为作者本机配置，请按自己的环境修改或删除，否则构建会因路径 / 代理不可用而失败。

### 构建与运行

```bash
./gradlew assembleDebug              # 打 debug 包
./gradlew installDebug               # 安装到已连接的设备/模拟器
./gradlew testDebugUnitTest          # 全部单元测试
./gradlew connectedDebugAndroidTest  # 设备冒烟测试
```

构建 release 包需要本机签名：在 `~/.gradle/gradle.properties` 提供 `RELEASE_STORE_PASSWORD`、`RELEASE_KEY_PASSWORD`，并在 `app/build.gradle.kts` 中把 `storeFile` 指向自己的 keystore。缺少这些配置时请只打 debug 包。

## 项目结构

```
app/src/main/java/com/rakor/sanzijing/
├── MainActivity.kt            # 入口（屏幕常亮 + 主题 + 导航）
├── data/
│   ├── content/               # 内容模型与只读仓库（assets JSON 解析）
│   ├── audio/                 # AudioPlayer 抽象与 MediaPlayer 实现
│   ├── db/                    # Room 实体、DAO、数据库
│   ├── prefs/                 # DataStore 阅读设置
│   └── progress/              # 进度、学习时长、连续天数、熟读
└── ui/
    ├── home/                  # 首页：继续学习、章节卡
    ├── reader/                # 阅读页：键位处理、操作面板
    ├── report/                # 学习报告
    ├── settings/              # 设置（自动朗读 / 连读 / 拼音 / 清除进度）
    ├── common/                # 拼音短语排版、进度条、Toast
    └── theme/                 # 童趣暖阳配色与课本式字体
docs/                          # 设计规格、实现计划、语音生成流程
tools/                         # 内容与资产生成脚本（不参与打包）
```

## 内容维护

修改注释 / 译文 / 典故 / 章节：

```bash
pip install -r tools/requirements.txt
# 1. 编辑 tools/sanzijing_raw.txt（原文）与 tools/annotations.json（注释/译文/典故）
# 2. 重新生成内容 JSON
cd tools && python gen_content.py
# 3. 若新增了生僻字或新拼音字符，重跑字体子集生成
python subset_font.py
```

`gen_content.py` 内置了《三字经》语境下的多音字拼音覆盖表（弟/长/传/载/说/为等），新增内容后请检查是否需要补充。

### 替换或重生成音频

音频按单元编号存放于 `app/src/main/assets/audio/u001.mp3 … u192.mp3`。**用真人录音同名覆盖该目录后重新打包即可，无需改任何代码。**

当前内置音频由 airvoz.com 的 Xiaoshuang 儿童声合成（含多音字同音字替换对策），批量 / 单条重生成流程见 [docs/voice-generation.md](docs/voice-generation.md)；`tools/gen_audio.py`（edge-tts）仍可作为占位音频的备用方案。音频完整性由 `AudioAssetsTest` 保障（192 条齐全、>1KB、MP3 魔数正确）。

## 测试

- `app/src/test/`：Robolectric + coroutines-test 单元测试，覆盖内容完整性、音频资源、进度 / 设置仓库与四个 ViewModel
- `app/src/androidTest/`：应用启动冒烟测试

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.AudioAssetsTest"
```

## 第三方资源与授权

- **字体**：课本式混排——汉字大字与标题用 [LXGW WenKai（霞鹜文楷）](https://github.com/lxgw/LxgwWenKai) v1.510 Medium 子集，拼音用同源 Regular 子集；两者均以 SIL Open Font License 1.1 重新分发，由 `tools/subset_font.py` 生成。小字正文使用系统默认黑体（远距离小字号可读性更好，也贴合语文课本「课文楷体、注释黑体」的排式）
- **文本**：《三字经》原文及通行注释属公版内容；注释与译文为本项目整理
- **音频**：由 airvoz.com 的 Azure 神经语音（zh-CN-XiaoshuangNeural，`speed=0.85`）合成，仅供本地学习使用
- **应用代码**：采用 [MIT 许可](LICENSE) © 2026 rakor，可自由使用、修改与再分发，请保留版权声明

## 相关文档

- [docs/superpowers/specs/](docs/superpowers/specs/) —— 设计规格（含页面与交互、数据设计、验收标准）
- [docs/superpowers/plans/](docs/superpowers/plans/) —— 实现计划
- [docs/voice-generation.md](docs/voice-generation.md) —— 语音生成流程与多音字对策
