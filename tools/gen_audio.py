# -*- coding: utf-8 -*-
"""edge-tts 批量生成逐句音频：assets/audio/uXXX.mp3（可整体替换真人音频）

需联网（走代理 export http_proxy/https_proxy）；依赖见 tools/requirements.txt。
"""
import asyncio, json, os
import edge_tts

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")
OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "audio")
VOICE = "zh-CN-XiaoxiaoNeural"   # 高质量中文女声；可换 zh-CN-XiaoyiNeural 更活泼
RATE = "-15%"                    # 放慢 15%，方便孩子跟读
CONCURRENCY = 4
MIN_BYTES = 1_000                # 与 AudioAssetsTest 共用 1000 字节阈值：过滤截断/占位文件

async def gen(sem, unit_id, text):
    async with sem:
        path = os.path.join(OUT, f"{unit_id}.mp3")
        if os.path.exists(path) and os.path.getsize(path) > MIN_BYTES:
            return
        # 先写 .part 再原子替换：save() 是流式落盘，直接写目标路径的话，
        # 中途断流会留下 >MIN_BYTES 的截断文件并被跳过逻辑永久采纳
        part = path + ".part"
        await edge_tts.Communicate(text, VOICE, rate=RATE).save(part)
        os.replace(part, path)
        print("done", unit_id)

async def main():
    os.makedirs(OUT, exist_ok=True)
    with open(CONTENT, encoding="utf-8") as f:
        data = json.load(f)
    jobs = [(u["id"], u["phrases"][0]["hanzi"] + "，" + u["phrases"][1]["hanzi"])
            for c in data["chapters"] for u in c["units"]]
    sem = asyncio.Semaphore(CONCURRENCY)
    results = await asyncio.gather(*(gen(sem, uid, text) for uid, text in jobs),
                                   return_exceptions=True)
    failures = [f"{uid}: {r!r}" for (uid, _), r in zip(jobs, results)
                if isinstance(r, Exception)]
    if failures:
        for msg in failures:
            print("FAILED", msg)
        raise SystemExit(f"{len(failures)} 个单元失败，请重跑本脚本（已完成的单元会跳过）")
    expected = {uid for uid, _ in jobs}
    actual = {f[:-4] for f in os.listdir(OUT) if f.endswith(".mp3")}
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    if missing or extra:
        raise SystemExit(f"音频集合不匹配  缺失: {missing}  多余: {extra}")
    small = sorted(f for f in os.listdir(OUT) if f.endswith(".mp3")
                   and os.path.getsize(os.path.join(OUT, f)) <= MIN_BYTES)
    if small:
        raise SystemExit(f"异常小文件: {small}")
    print("total mp3:", len(actual))

if __name__ == "__main__":
    asyncio.run(main())
