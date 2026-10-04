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
