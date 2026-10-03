# -*- coding: utf-8 -*-
"""edge-tts 批量生成逐句音频：assets/audio/uXXX.mp3（可整体替换真人音频）"""
import asyncio, json, os
import edge_tts

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")
OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "audio")
VOICE = "zh-CN-XiaoxiaoNeural"   # 高质量中文女声；可换 zh-CN-XiaoyiNeural 更活泼
RATE = "-15%"                    # 放慢 15%，方便孩子跟读
CONCURRENCY = 4

async def gen(sem, unit_id, text):
    async with sem:
        path = os.path.join(OUT, f"{unit_id}.mp3")
        if os.path.exists(path) and os.path.getsize(path) > 1_000:
            return
        await edge_tts.Communicate(text, VOICE, rate=RATE).save(path)
        print("done", unit_id)

async def main():
    os.makedirs(OUT, exist_ok=True)
    with open(CONTENT, encoding="utf-8") as f:
        data = json.load(f)
    sem = asyncio.Semaphore(CONCURRENCY)
    tasks = []
    for c in data["chapters"]:
        for u in c["units"]:
            text = u["phrases"][0]["hanzi"] + "，" + u["phrases"][1]["hanzi"]
            tasks.append(gen(sem, u["id"], text))
    results = await asyncio.gather(*tasks, return_exceptions=True)
    failures = [(i, r) for i, r in enumerate(results) if isinstance(r, Exception)]
    if failures:
        for i, r in failures:
            print("FAILED task", i, repr(r))
        raise SystemExit(f"{len(failures)} 个任务失败，请重跑本脚本（会跳过已完成文件）")
    files = [f for f in os.listdir(OUT) if f.endswith(".mp3")]
    small = [f for f in files if os.path.getsize(os.path.join(OUT, f)) <= 1_000]
    if small:
        raise SystemExit(f"异常小文件: {small}")
    print("total mp3:", len(files))
    assert len(files) >= 192, "音频数量不足"

asyncio.run(main())
