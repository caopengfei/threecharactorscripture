# 三字经 · Android TV 亲子共学

Compose for TV 构建的全离线《三字经》学习应用：逐句大字拼音、朗读跟读、释义典故、学习进度追踪。

## 开发环境

- Android Studio（本机: `D:\develop\soft\Android Studio`），SDK: `D:\develop\androidsdk`
- JDK 17 钉扎于 `D:/develop/jdk/bellsoft-17.0.15`（`gradle.properties` 的 `org.gradle.java.home`）；AS 自带 JBR(Java 25) 无法运行 Gradle 8.10 守护进程
- 命令行构建需代理（已配置于 `gradle.properties`）

## 常用命令

```bash
./gradlew assembleDebug          # 打 debug 包
./gradlew testDebugUnitTest      # 全部单元测试
./gradlew connectedDebugAndroidTest  # 模拟器冒烟测试
```

## 替换真人音频

音频按单元编号存放于 `app/src/main/assets/audio/u001.mp3 … u192.mp3`。
用真人录制音频按**同名文件**覆盖该目录后重新打包即可，无需改任何代码。
重新生成占位音频: `cd tools && python gen_audio.py`。

## 第三方字体

课本式混排：汉字大字/标题用 [LXGW WenKai（霞鹜文楷）](https://github.com/lxgw/LxgwWenKai) v1.510 **Medium** 子集（`wenkai_medium_subset.ttf`），拼音用同源 **Regular** 子集（`wenkai_subset.ttf`）；注释/译文等小字正文用系统默认黑体（小字号远距离可读性更好，且与语文课本"课文楷体、注释黑体"的排式一致）。两个子集均遵循 SIL Open Font License 1.1 授权重新分发，由 `tools/subset_font.py` 一次生成。

## 修改内容（注释/译文/典故/章节）

0. 首次使用工具脚本：`pip install -r tools/requirements.txt`；`subset_font.py`/`gen_icons.py` 需先按脚本内说明下载 LXGW 文楷源字体放入 `tools/`
1. 编辑 `tools/sanzijing_raw.txt`（文本）与 `tools/annotations.json`（注释/译文/典故）
2. `cd tools && python gen_content.py` 重新生成 `app/src/main/assets/content/sanzijing.json`
3. 若新增了生僻字/新拼音字符，重跑 `python subset_font.py` 更新字体子集

## 目录速览

- `docs/superpowers/specs/` 设计规格；`docs/superpowers/plans/` 实现计划
- `tools/` 内容与资产生成脚本（不参与打包）
