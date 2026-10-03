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
    "u088": ["jīng zǐ tōng", "dú zhū shǐ"],    # 经子 zǐ
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
    for raw in open(RAW, encoding="utf-8"):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#chapter"):
            _, cid, title = line.split()
            chapters.append({"id": cid, "title": title, "units": []})
            continue
        uid, text = line.split("|")
        parts = text.split("，")
        assert len(parts) == 2 and all(len(p) == 3 for p in parts), f"bad line: {line}"
        assert uid not in seen_ids, f"duplicate id: {uid}"
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
    total = sum(len(c["units"]) for c in chapters)
    ann = json.load(open(ANN, encoding="utf-8"))
    missing = [c["units"][i]["id"] for c in chapters for i in range(len(c["units"])) if c["units"][i]["id"] not in ann]
    if missing:
        sys.exit(f"annotations.json 缺少 {len(missing)} 条: {missing[:10]} ...")
    for c in chapters:
        for u in c["units"]:
            a = ann[u["id"]]
            u["annotation"] = a["annotation"]
            u["translation"] = a["translation"]
            if a.get("storyTitle"):
                assert a.get("story"), f"{u['id']} storyTitle 无对应 story"
                u["storyTitle"] = a["storyTitle"]
                u["story"] = a["story"]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump({"version": 1, "chapters": chapters}, open(OUT, "w", encoding="utf-8"),
              ensure_ascii=False, indent=1)
    print(f"OK {len(chapters)} chapters, {total} units -> {os.path.abspath(OUT)}")

if __name__ == "__main__":
    main()
