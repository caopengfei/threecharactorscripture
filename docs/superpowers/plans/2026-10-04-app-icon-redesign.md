# 应用图标重设计「暖阳积木」实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按规格 `docs/superpowers/specs/2026-10-04-app-icon-redesign-design.md` 重写图标生成器，产出满版出血「暖阳积木」启动图标与同构 banner，打包安装到模拟器与小米盒子（**只装不打开**）。

**Architecture:** 单文件工具 `tools/gen_icons.py` 重写（PIL，4x 超采样 → LANCZOS 降采样出 5 档密度图标；banner 2x 渲染缩 640×360），产物覆盖 `mipmap-*/ic_launcher.png` 与 `drawable-nodpi/banner.png`；manifest 与代码零改动。

**Tech Stack:** Python + Pillow（`tools/requirements.txt` 钉版）、LXGW WenKai Medium v1.510（banner 文字）、Gradle 8.10.2 + AGP 8.7.3（JDK17 钉扎在 gradle.properties）、adb。

**关键约束（用户指令）：** 安装到小米盒子后**不得启动应用**——全程禁止 `monkey`、`am start` 指定本应用；盒子上只允许 install 与被动 screencap。

---

### Task 1: 重写 tools/gen_icons.py

**Files:**
- Modify（整文件替换）: `tools/gen_icons.py`

- [ ] **Step 1: 用以下完整内容替换 `tools/gen_icons.py`**

```python
# -*- coding: utf-8 -*-
"""TV banner (640x360) 与启动图标：「暖阳积木」

奶油渐变底 + 蜜橙/珊瑚/叶绿三块胶囊积木（中横短，抽象"三"字）+ 光芒太阳，
满版出血、圆角透明。设计规格：docs/superpowers/specs/2026-10-04-app-icon-redesign-design.md
banner 源字体：LXGW WenKai v1.510 — https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
FONT = os.path.join(HERE, "LXGWWenKai-Medium.ttf")

CREAM_TOP = (255, 248, 236)
CREAM_BOTTOM = (255, 230, 199)
HONEY = (212, 149, 74)         # 积木·上
CORAL = (255, 138, 101)        # 积木·中（= app 焦点色）
LEAF = (128, 178, 110)         # 积木·下
HONEY_LIGHT = (247, 178, 103)  # 太阳
INK = (90, 70, 52)
SUB = (138, 109, 78)
WHITE = (255, 255, 255)

# 启动图标（坐标按边长 S 归一化）：积木 (垂直中心 y, 颜色, 旋转角, 宽)；太阳 (cx, cy, r)
ICON_BLOCKS = ((0.365, HONEY, -2.5, 0.56), (0.515, CORAL, 2.0, 0.44), (0.665, LEAF, -1.5, 0.56))
ICON_SUN = (0.78, 0.21, 0.075)
ICON_BLOCK_H = 0.105
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SUPER = 4  # 超采样倍数（以最大密度 192 为基准）

# banner（@2x 1280x720 渲染后缩 640x360）：积木 (cy, 颜色, 旋转角, 宽)，中心 x=1065；太阳 (cx, cy, r, 线宽)
BANNER_BLOCKS = ((310, HONEY, -2.5, 300), (450, CORAL, 2.0, 230), (585, LEAF, -1.5, 300))
BANNER_SUN = (1140, 140, 52, 22)
BANNER_BLOCK_H = 58


def vgrad(size, top, bottom):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


def darken(color, d=30):
    return tuple(max(0, v - d) for v in color)


def draw_sun(d, cx, cy, r, lw):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=HONEY_LIGHT)
    for k in range(8):
        a = k * math.pi / 4
        d.line([(cx + math.cos(a) * r * 1.45, cy + math.sin(a) * r * 1.45),
                (cx + math.cos(a) * r * 1.85, cy + math.sin(a) * r * 1.85)], fill=HONEY_LIGHT, width=lw)


def draw_bar(img, cx, cy, w, h, color, rot):
    """胶囊积木：视觉中心 (cx, cy)，底部 14% 高深一号色底唇，绕层中心旋转 rot 度"""
    pad_w, pad_h = int(w * 1.4), int(h * 2.6)
    bar = Image.new("RGBA", (pad_w, pad_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(bar)
    x0, y0 = w * 0.2, h * 0.3
    d.rounded_rectangle((x0, y0, x0 + w, y0 + h), radius=h / 2, fill=darken(color) + (255,))
    d.rounded_rectangle((x0, y0 - h * 0.14, x0 + w, y0 + h * 0.8), radius=h / 2, fill=color + (255,))
    bar = bar.rotate(rot, resample=Image.BICUBIC, expand=False)
    img.alpha_composite(bar, (int(cx - w * 0.7), int(cy - h / 2)))


def make_launcher():
    s = 192 * SUPER
    img = vgrad((s, s), CREAM_TOP, CREAM_BOTTOM).convert("RGBA")
    sun_layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw_sun(ImageDraw.Draw(sun_layer), int(s * ICON_SUN[0]), int(s * ICON_SUN[1]),
             int(s * ICON_SUN[2]), int(s * 0.016))
    img = Image.alpha_composite(img, sun_layer)
    for cy, col, rot, wf in ICON_BLOCKS:
        draw_bar(img, s / 2, s * cy, s * wf, s * ICON_BLOCK_H, col, rot)
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(s * 0.22), fill=255)
    img = Image.composite(img, Image.new("RGBA", (s, s), (0, 0, 0, 0)), mask)
    for dpi, px in DENSITIES.items():
        out = os.path.join(RES, f"mipmap-{dpi}")
        os.makedirs(out, exist_ok=True)
        img.resize((px, px), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))


def make_banner():
    w, h = 1280, 720
    img = vgrad((w, h), CREAM_TOP, CREAM_BOTTOM).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.text((90, 300), "三字经", font=ImageFont.truetype(FONT, 260), fill=INK, anchor="lm")
    d.text((95, 545), "亲子共读 · 逐句跟学", font=ImageFont.truetype(FONT, 58), fill=SUB, anchor="lm")
    for cy, col, rot, bw in BANNER_BLOCKS:
        draw_bar(img, 1065, cy, bw, BANNER_BLOCK_H, col, rot)
    sun_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw_sun(ImageDraw.Draw(sun_layer), BANNER_SUN[0], BANNER_SUN[1], BANNER_SUN[2], BANNER_SUN[3])
    img = Image.alpha_composite(img, sun_layer)
    out = os.path.join(RES, "drawable-nodpi")
    os.makedirs(out, exist_ok=True)
    img.resize((640, 360), Image.LANCZOS).save(os.path.join(out, "banner.png"))


if __name__ == "__main__":
    if not os.path.exists(FONT):
        sys.exit("缺少源字体 tools/LXGWWenKai-Medium.ttf（LXGW WenKai v1.510），"
                 "下载: https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）")
    make_banner()
    make_launcher()
    print("icons OK")
```

- [ ] **Step 2: 运行生成器**

Run: `cd D:/develop/src/rakor/threecharactorscripture && python tools/gen_icons.py`
Expected: 输出 `icons OK`，无异常栈

---

### Task 2: 产物视觉自检

- [ ] **Step 1: 查看最大密度图标与 48px 图标、banner**

用 Read 工具查看：
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`（192px）
- `app/src/main/res/mipmap-mdpi/ic_launcher.png`（48px）
- `app/src/main/res/drawable-nodpi/banner.png`（640×360）

Expected: 与已定稿草案（.superpowers/brainstorm/manual-1791081667/content/icon-b1.png 与 banner-draft.png）观感一致：奶油渐变底、蜜橙/珊瑚/叶绿三积木（中横短）、右上光芒太阳、满版圆角透明；banner 左字右图无遮挡。48px 下积木清晰可辨。若有偏差，回到 Task 1 修参数后重跑。

- [ ] **Step 2: 确认变更文件清单**

Run: `git status --porcelain -- app/src/main/res tools/gen_icons.py`
Expected: 恰好 6 个产物变更：5×`mipmap-*/ic_launcher.png` + `drawable-nodpi/banner.png`，加 `tools/gen_icons.py`（其余已修改文件如 audio/mp3 不在列）

---

### Task 3: 打包 assembleDebug

- [ ] **Step 1: 构建**

Run: `cd D:/develop/src/rakor/threecharactorscripture && ./gradlew assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: 若构建因代理失败（dl.google.com 不可达）**

检查代理是否存活：`curl -s --connect-timeout 2 -x http://127.0.0.1:9999 https://dl.google.com -o /dev/null -w "%{http_code}\n"`
- 代理活但构建仍失败：保留 gradle.properties 代理行重试一次
- 代理死：临时注释 `gradle.properties` 中 `systemProp.http.proxyHost/https.proxyHost` 等 4 行（dl.google.com 常可直连），构建完成后**必须恢复**并 `git diff gradle.properties` 确认无残留

---

### Task 4: 安装两台设备（不打开）

**Files:** 无代码变更；产物 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: 确认设备在线**

Run: `"D:/develop/androidsdk/platform-tools/adb.exe" devices`
Expected: 列表含 `192.168.31.149:5555`（小米盒子）与 `emulator-5554`，均 `device`。盒子掉线则先跳过盒子并在结尾如实报告

- [ ] **Step 2: 安装（覆盖保留数据），两台各一条**

Run:
```bash
"D:/develop/androidsdk/platform-tools/adb.exe" -s 192.168.31.149:5555 install -r app/build/outputs/apk/debug/app-debug.apk
"D:/develop/androidsdk/platform-tools/adb.exe" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: 各自输出 `Success`

- [ ] **Step 3: 禁止启动（硬约束）**

本任务全程不得执行 `monkey`、`am start`、`input keyevent KEYCODE_HOME` 以外任何会拉起 `com.rakor.sanzijing` 的命令。验证安装状态用：
```bash
"D:/develop/androidsdk/platform-tools/adb.exe" -s 192.168.31.149:5555 shell pm list packages com.rakor.sanzijing
```
Expected: `package:com.rakor.sanzijing`

---

### Task 5: adb 截图验收

- [ ] **Step 1: 模拟器——TV 桌面 banner 行**

```bash
ADB="D:/develop/androidsdk/platform-tools/adb.exe"
mkdir -p build/icon-verify
"$ADB" -s emulator-5554 shell input keyevent KEYCODE_HOME
"$ADB" -s emulator-5554 shell input keyevent KEYCODE_DPAD_DOWN   # 重复 2~6 次，每按一次截一张
"$ADB" -s emulator-5554 shell screencap -p /sdcard/icon_check.png
MSYS_NO_PATHCONV=1 "$ADB" -s emulator-5554 pull /sdcard/icon_check.png build/icon-verify/emulator-home.png
```
用 Read 查看 `build/icon-verify/emulator-home.png`：应用行中应显示「三字经」banner（左文右积木）。未找到 banner 行则继续 DPAD_DOWN/DPAD_RIGHT 走查，最多 8 张。

- [ ] **Step 2: 模拟器——设置里的启动图标**

```bash
"$ADB" -s emulator-5554 shell am start -a android.settings.APPLICATION_SETTINGS
"$ADB" -s emulator-5554 shell screencap -p /sdcard/icon_check.png
MSYS_NO_PATHCONV=1 "$ADB" -s emulator-5554 pull /sdcard/icon_check.png build/icon-verify/emulator-settings.png
```
用 Read 查看：应用列表里「三字经」条目图标应为暖阳积木新图标。（设置页允许 `am start`——限制只针对本应用。）

- [ ] **Step 3: 盒子——被动截屏（不导航、不打开）**

```bash
"$ADB" -s 192.168.31.149:5555 shell screencap -p /sdcard/icon_check.png
MSYS_NO_PATHCONV=1 "$ADB" -s 192.168.31.149:5555 pull /sdcard/icon_check.png build/icon-verify/box-screen.png
```
用 Read 查看：若当前停在桌面且应用行可见，核对 banner；否则不强求（用户 constraint：不得操作盒子 UI），以模拟器验收 + 安装 Success 为准，并在总结中说明。

---

### Task 6: 提交

- [ ] **Step 1: 只加图标相关文件并提交**（工作区另有大量未提交改动——audio mp3 等——严禁带入）

```bash
cd D:/develop/src/rakor/threecharactorscripture
git add tools/gen_icons.py app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png app/src/main/res/drawable-nodpi/banner.png
git commit -m "feat: 图标重设计——暖阳积木（满版出血启动图标 + 同构 banner）"
git status --porcelain | head -5   # 确认其余改动仍未提交
```
Expected: 提交含 7 个文件；audio 等其余改动保持未提交状态

---

## 自审记录

- **规格覆盖**：图标规格→Task 1（ICON_* 常量与规格数值一致）；banner 规格→Task 1（BANNER_* 与规格 @2x 坐标一致）；不引入自适应图标→无相关任务（non-goal）；README 同步→核查过 README 仅有通用工具提法，无需改动；验收标准 1-4→Task 2/3/4/5。
- **占位符扫描**：无 TBD/TODO；所有代码步骤含完整代码。
- **类型一致性**：`draw_bar(img, cx, cy, w, h, color, rot)`、`draw_sun(d, cx, cy, r, lw)` 两处调用签名一致。
- **像素保真**：Task 1 代码从用户逐张选定的渲染脚本（.superpowers/brainstorm/manual-1791081667/render_options.py + render_b.py）原样移植，几何参数未做"顺手优化"。
