# -*- coding: utf-8 -*-
"""LXGW 文楷子集：只保留内容与 UI 用字，输出 Regular + Medium 两个字重

源字体：LXGW WenKai v1.510 — https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）
输出：res/font/wenkai_subset.ttf（Regular，拼音/小字）+ res/font/wenkai_medium_subset.ttf（Medium，大字/标题）
"""
import json, os, sys
from fontTools import subset

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")
FONTS = [
    # (源字体文件, 输出文件)
    ("LXGWWenKai-Regular.ttf", "wenkai_subset.ttf"),
    ("LXGWWenKai-Medium.ttf", "wenkai_medium_subset.ttf"),
]

UI_TEXT = ("三字经继续学习章节教育之要蒙学伦理经典导读历史纲鉴勤学典故劝学结语"
           "学习报告设置已读熟连续天今日分钟总进度个句正在朗读跟着一起重开开关显示拼音"
           "清除确认取消版本内容基于通行版下一上更多操作跳转标记面板全部没有记录还没有"
           "开始学习吧退出释义典故注释译文小故事篇本章中第天启"
           "操作失败请重试")
EXTRA = "0123456789.%/：，。！？、（）·—…♪▶◀✓☰←→↑↓" \
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"  # A-Z a-z 兜底：低端电视盒 fallback 不可预测

def collect_chars():
    chars = set(UI_TEXT + EXTRA)
    with open(CONTENT, encoding="utf-8") as f:
        d = json.load(f)
    for c in d["chapters"]:
        chars.update(c["title"])
        for u in c["units"]:
            for p in u["phrases"]:
                chars.update(p["hanzi"])
                chars.update(p["pinyin"])   # 拼音声调字母 ā á ǎ à ǖ 等随内容一并收录
            chars.update(u["annotation"])
            chars.update(u["translation"])
            if u.get("storyTitle"):
                chars.update(u["storyTitle"])
            if u.get("story"):
                chars.update(u["story"])
    return "".join(sorted(chars))

def subset_one(src, out, needed):
    options = subset.Options()
    options.notdef_outline = True
    font = subset.load_font(src, options)
    s = subset.Subsetter(options)
    s.populate(text=needed)
    s.subset(font)
    subset.save_font(font, out, options)

    # 自检：重新加载输出字体，逐字确认 cmap 覆盖；缺字宁可构建失败，不可静默回退系统字体
    from fontTools.ttLib import TTFont
    cmap = TTFont(out).getBestCmap()
    missing = "".join(c for c in needed if ord(c) not in cmap)
    if missing:
        print("MISSING in %s (%d chars): %s" % (os.path.basename(out), len(missing), missing))
        sys.exit(1)
    print("OK ->", os.path.abspath(out), os.path.getsize(out) // 1024, "KB,",
          len(needed), "chars all covered")

def main():
    needed = collect_chars()
    out_dir = os.path.join(HERE, "..", "app", "src", "main", "res", "font")
    os.makedirs(out_dir, exist_ok=True)
    for src_name, out_name in FONTS:
        src = os.path.join(HERE, src_name)
        if not os.path.exists(src):
            sys.exit("缺少源字体 tools/%s（v1.510），下载: https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）" % src_name)
        subset_one(src, os.path.join(out_dir, out_name), needed)

if __name__ == "__main__":
    main()
