# -*- coding: utf-8 -*-
"""LXGW 文楷子集：只保留内容与 UI 用字，输出 res/font/wenkai_subset.ttf"""
import json, os
from fontTools import subset

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "LXGWWenKai-Regular.ttf")
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "font", "wenkai_subset.ttf")
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")

UI_TEXT = ("三字经继续学习章节教育之要蒙学伦理经典导读历史纲鉴勤学典故劝学结语"
           "学习报告设置已读熟连续天今日分钟总进度个句正在朗读跟着一起重开开关显示拼音"
           "清除确认取消版本内容基于通行版下一上更多操作跳转标记面板全部没有记录还没有"
           "开始学习吧退出释义典故注释译文小故事篇本章中第天启"
           "操作失败请重试")
EXTRA = "0123456789.%/：，。！？、（）·—…♪▶◀✓☰←→↑↓"

def collect_chars():
    chars = set(UI_TEXT + EXTRA)
    d = json.load(open(CONTENT, encoding="utf-8"))
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

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    options = subset.Options()
    options.notdef_outline = True
    font = subset.load_font(SRC, options)
    s = subset.Subsetter(options)
    s.populate(text=collect_chars())
    s.subset(font)
    subset.save_font(font, OUT, options)
    print("OK ->", os.path.abspath(OUT), os.path.getsize(OUT) // 1024, "KB")

if __name__ == "__main__":
    main()
