# -*- coding: utf-8 -*-
"""TV banner (640x360) 与启动图标：奶油渐变底 + 珊瑚印章 + 文楷字"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
FONT = os.path.join(HERE, "LXGWWenKai-Regular.ttf")

CREAM_TOP = (255, 248, 236)
CREAM_BOTTOM = (255, 230, 199)
CORAL = (255, 138, 101)
INK = (90, 70, 52)
SUB = (138, 109, 78)
WHITE = (255, 255, 255)

def gradient(size, top, bottom):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img

def make_banner():
    img = gradient((640, 360), CREAM_TOP, CREAM_BOTTOM)
    d = ImageDraw.Draw(img)
    d.text((250, 150), "三字经", font=ImageFont.truetype(FONT, 120), fill=INK, anchor="mm")
    d.text((250, 255), "亲子共读 · 逐句跟学", font=ImageFont.truetype(FONT, 36), fill=SUB, anchor="mm")
    d.rounded_rectangle(((470, 90), (590, 270)), radius=20, fill=CORAL)
    d.text((530, 180), "学", font=ImageFont.truetype(FONT, 96), fill=WHITE, anchor="mm")
    img.save(os.path.join(RES, "drawable-nodpi", "banner.png"))

def make_launcher():
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dpi, s in sizes.items():
        img = gradient((s, s), CREAM_TOP, CREAM_BOTTOM)
        d = ImageDraw.Draw(img)
        pad = max(2, s // 10)
        d.rounded_rectangle(((pad, pad), (s - pad, s - pad)), radius=s // 6, fill=CORAL)
        d.text((s / 2, s / 2 + s * 0.02), "三", font=ImageFont.truetype(FONT, s // 2), fill=WHITE, anchor="mm")
        os.makedirs(os.path.join(RES, f"mipmap-{dpi}"), exist_ok=True)
        img.save(os.path.join(RES, f"mipmap-{dpi}", "ic_launcher.png"))

make_banner()
make_launcher()
print("icons OK")
