# 三字经 · Android TV 亲子共学

Compose for TV 构建的全离线《三字经》学习应用：逐句大字拼音、朗读跟读、释义典故、学习进度追踪。

## 开发环境

- Android Studio（本机: `D:\develop\soft\Android Studio`），SDK: `D:\develop\androidsdk`
- JDK 使用 Android Studio 自带 JBR（`gradle.properties` 已配置）
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

## 修改内容（注释/译文/典故/章节）

1. 编辑 `tools/sanzijing_raw.txt`（文本）与 `tools/annotations.json`（注释/译文/典故）
2. `cd tools && python gen_content.py` 重新生成 `app/src/main/assets/content/sanzijing.json`
3. 若新增了生僻字/新拼音字符，重跑 `python subset_font.py` 更新字体子集

## 目录速览

- `docs/superpowers/specs/` 设计规格；`docs/superpowers/plans/` 实现计划
- `tools/` 内容与资产生成脚本（不参与打包）
