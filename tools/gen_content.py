# -*- coding: utf-8 -*-
"""sanzijing_raw.txt + annotations.json -> ../app/src/main/assets/content/sanzijing.json"""
import json, os, sys
from pypinyin import lazy_pinyin, Style

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "sanzijing_raw.txt")
ANN = os.path.join(HERE, "annotations.json")
OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")

# pypinyin 默认读音在《三字经》语境下错误的多音字，按单元整体覆盖（拼音以空格分隔）
PINYIN_OVERRIDES = {
    "u005": ["xī mèng mǔ", "zé lín chǔ"],      # 处 chǔ
    "u006": ["zǐ bù xué", "duàn jī zhù"],      # 子＝孟子 zǐ
    "u008": ["jiào wǔ zǐ", "míng jù yáng"],    # 五子（五个儿子）zǐ
    "u011": ["zǐ bù xué", "fēi suǒ yí"],       # 子 zǐ
    "u012": ["yòu bù xué", "lǎo hé wéi"],      # 为（作为）wéi
    "u013": ["yù bù zhuó", "bù chéng qì"],     # 琢 zhuó（雕琢）
    "u015": ["wéi rén zǐ", "fāng shǎo shí"],   # 为人子：子 zǐ
    "u020": ["dì yú zhǎng", "yí xiān zhī"],    # 弟通悌 tì，长 zhǎng
    "u032": ["cǐ sì fāng", "yìng hū zhōng"],   # 应（对应）yìng
    "u042": ["ài wù yù", "qī qíng jù"],        # 恶（憎恶）wù
    "u046": ["shēn ér zǐ", "zǐ ér sūn"],       # 子（儿子）zǐ
    "u047": ["zì zǐ sūn", "zhì xuán zēng"],    # 曾（玄曾）zēng
    "u056": ["wéi xué zhě", "bì yǒu chū"],     # 为学（做学问）wéi
    "u060": ["mèng zǐ zhě", "qī piān zhǐ"],    # 孟子 zǐ
    "u062": ["zuò zhōng yōng", "zǐ sī bǐ"],    # 子思 zǐ
    "u064": ["zuò dà xué", "nǎi zēng zǐ"],     # 曾子 zēng zǐ
    "u075": ["zhù liù guān", "cún zhì tǐ"],    # 著 zhù
    "u082": ["sān zhuàn zhě", "yǒu gōng yáng"],# 传（传注）zhuàn
    "u084": ["jīng jì míng", "fāng dú zǐ"],    # 读子（诸子）zǐ
    "u086": ["wǔ zǐ zhě", "yǒu xún yáng"],     # 五子 zǐ
    "u087": ["wén zhōng zǐ", "jí lǎo zhuāng"], # 文中子 zǐ（王通）
    "u088": ["jīng zǐ tōng", "dú zhū shǐ"],    # 经子 zǐ
    "u096": ["xià chuán zǐ", "jiā tiān xià"],  # 夏传子 zǐ（儿子）
    "u097": ["sì bǎi zǎi", "qiān xià shè"],    # 载（年）zǎi
    "u099": ["liù bǎi zǎi", "zhì zhòu wáng"],  # 载 zǎi
    "u101": ["bā bǎi zǎi", "zuì cháng jiǔ"],   # 载 zǎi
    "u103": ["chěng gān gē", "shàng yóu shuì"],# 说 shuì
    "u110": ["guāng wǔ xīng", "wéi dōng hàn"], # 为（是为）wéi
    "u115": ["wéi nán cháo", "dū jīn líng"],   # 为 wéi，都（都城）dū
    "u122": ["èr shí chuán", "sān bǎi zǎi"],   # 载 zǎi
    "u126": ["yán sòng xīng", "shòu zhōu shàn"],# 禅 shàn
    "u143": ["zǎi zhì luàn", "zhī xīng shuāi"],# 载（记载）zǎi
    "u147": ["zhāo yú sī", "xī yú sī"],        # 朝（早晨）zhāo
    "u150": ["zhào zhōng lìng", "dú lǔ lún"],  # 鲁论（《鲁论语》）lún
    "u169": ["bì qī suì", "néng fù qí"],       # 泌 bì（李泌）
    "u175": ["ěr nán zǐ", "dāng zì jǐng"],     # 男子 zǐ
    "u189": ["rén wèi zǐ", "jīn mǎn yíng"],    # 遗 wèi（赠给）
    "u190": ["wǒ jiāo zǐ", "wéi yī jīng"],     # 教（教导，动词）jiāo
}

def phrase_pinyin(unit_id, hanzi, idx):
    if unit_id in PINYIN_OVERRIDES:
        return PINYIN_OVERRIDES[unit_id][idx]
    return " ".join(lazy_pinyin(hanzi, style=Style.TONE))

def main():
    chapters = []
    seen_ids = set()
    for lineno, raw in enumerate(open(RAW, encoding="utf-8"), start=1):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#chapter"):
            parts = line.split(maxsplit=2)
            if len(parts) != 3:
                sys.exit(f"line {lineno}: 章节行格式错误: {line}")
            _, cid, title = parts
            chapters.append({"id": cid, "title": title, "units": []})
            continue
        if "|" not in line:
            sys.exit(f"line {lineno}: 单元行缺少 '|': {line}")
        uid, text = line.split("|", 1)
        parts = text.split("，")
        if not (len(parts) == 2 and all(len(p) == 3 for p in parts)):
            sys.exit(f"line {lineno}: 单元行格式错误（须为 三字，三字）: {line}")
        if uid in seen_ids:
            sys.exit(f"line {lineno}: 重复的单元 id: {uid}")
        seen_ids.add(uid)
        chapters[-1]["units"].append({
            "id": uid,
            "phrases": [
                {"hanzi": parts[0], "pinyin": phrase_pinyin(uid, parts[0], 0)},
                {"hanzi": parts[1], "pinyin": phrase_pinyin(uid, parts[1], 1)},
            ],
            "annotation": "",
            "translation": "",
        })

    # 覆盖表自检 1：键必须存在于 RAW
    unknown = sorted(k for k in PINYIN_OVERRIDES if k not in seen_ids)
    if unknown:
        sys.exit(f"PINYIN_OVERRIDES 含 RAW 中不存在的单元 id: {unknown}")
    # 覆盖表自检 2：每个拼音的音节数必须与对应短语的汉字数一致
    for c in chapters:
        for u in c["units"]:
            ov = PINYIN_OVERRIDES.get(u["id"])
            if not ov:
                continue
            if len(ov) != len(u["phrases"]):
                sys.exit(f"{u['id']}: 覆盖表应提供 {len(u['phrases'])} 个短语的拼音，实际 {len(ov)} 个")
            for idx, phrase in enumerate(u["phrases"]):
                n_syl = len(ov[idx].split())
                if n_syl != len(phrase["hanzi"]):
                    sys.exit(f"{u['id']} 第 {idx + 1} 短语「{phrase['hanzi']}」共 {len(phrase['hanzi'])} 字，"
                             f"覆盖拼音「{ov[idx]}」有 {n_syl} 个音节，数量不一致")

    total = sum(len(c["units"]) for c in chapters)
    ann = json.load(open(ANN, encoding="utf-8"))
    missing = [u["id"] for c in chapters for u in c["units"] if u["id"] not in ann]
    if missing:
        sys.exit(f"annotations.json 缺少 {len(missing)} 条: {missing[:10]} ...")
    orphans = sorted(k for k in ann if k not in seen_ids)
    if orphans:
        sys.exit(f"annotations.json 含 RAW 中不存在的孤儿键: {orphans}")
    for c in chapters:
        for u in c["units"]:
            a = ann[u["id"]]
            u["annotation"] = a["annotation"]
            u["translation"] = a["translation"]
            if a.get("storyTitle") and not a.get("story"):
                sys.exit(f"{u['id']}: storyTitle 无对应 story")
            if a.get("story") and not a.get("storyTitle"):
                sys.exit(f"{u['id']}: story 无对应 storyTitle")
            if a.get("storyTitle"):
                u["storyTitle"] = a["storyTitle"]
                u["story"] = a["story"]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"version": 1, "chapters": chapters}, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"OK {len(chapters)} chapters, {total} units -> {os.path.abspath(OUT)}")

if __name__ == "__main__":
    main()
