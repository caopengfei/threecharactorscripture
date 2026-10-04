# 语音生成流程与方式

> 2026-10-04 定稿。当前 `app/src/main/assets/audio/` 里的 192 条音频由本文档记录的方式生成。
> 换真人音频仍是同名覆盖即可（见 README「替换真人音频」），本文只管 TTS 生成链路。

## 背景与选型

- 初版用 `tools/gen_audio.py`（edge-tts，zh-CN-XiaoxiaoNeural）生成占位音频，**多音字错误多**（弟于长、句读、论语、受周禅、朝于斯、我教子等）。
- 现版改用 **airvoz.com** 网页版 TTS 生成全套 192 条：
  - 声音：**zh-CN-XiaoshuangNeural**（儿童声「晓双」，适合亲子跟读场景）
  - 参数：`speed=0.85`（放慢 15%，与原 gen_audio.py 的 RATE=-15% 一致）、`pitch=1`、`volume=1`
- 注意：该声音与 edge-tts **同为 Azure 神经语音，多音字错误相同**，所以配套了同音字替换对策（下文）。

## 接口方式（关键）

站点页面「生成」按钮背后就是：

```
POST https://airvoz.com/api/tts
Content-Type: application/json

{"input":"人之初，性本善","voice":"zh-CN-XiaoshuangNeural","language":"zh-CN","speed":0.85,"pitch":1,"volume":1}
```

- 响应：`audio/mpeg` 裸 MP3 流（无 ID3 头，MPEG 帧同步 0xFFEx，`AudioAssetsTest` 两种魔数都认）。
- **只能从站点页面上下文调用**：用 curl 直接 POST（即使带 UA/Referer/Origin/Cookie）会返回 200 但 body 为空——疑似 httpOnly 会话或浏览器指纹校验。因此批量生成走浏览器自动化（在打开的页面里发 fetch），手工在浏览器控制台逐条跑也行。
- 页面限制单次 3000 字符、免费不限次数；礼貌起见并发 ≤3、失败退避重试。

## 多音字/生僻字对策：同音字替换

只改**喂给 TTS 的文本**，app 内显示文本永远不变。完整替换表存档在 `tools/voice_script.txt` B 节，规律如下：

| 错误模式 | 替换 | 涉及单元 |
|---|---|---|
| 短语尾「子」被读轻声 zi（应 zǐ） | 子→**紫** | u015 为人紫、u046 身而紫、u064 乃增紫、u096 夏传紫、u189 人遗紫、u190 我较紫 |
| 「教」jiào 被读 jiāo | 教→叫/较 | u003/u004/u008/u009/u010/u190 |
| 弟 tì、长 zhǎng | 悌于掌 | u020；u051 长幼序→掌幼序 |
| 句读 dòu、论语 lún | 句逗、伦语/鲁伦 | u055、u058、u150 |
| 礼乐 yuè、受周禅 shàn、游说 shuì、朝于斯 zhāo、都金陵 dū | 越备、受周善、游税、招于斯、督金陵 | u077、u126、u103、u147、u115 |
| 数字/短语尾「传」chuán 被读 zhuàn | 传→**船** | u119 不再船、u122 二十船、u127 十八船；**u082「三传者」读 zhuàn 是对的，勿替换** |
| 纪年「载」zǎi | 载→宰 | u097/u099/u101/u122/u143 |
| 其余单点 | 择邻楚、爱误欲、此六触、至玄增、老何维、玉不浊、方绍时、首孝替、何维人、金满盈、师项陀、毕七岁、谢道蕴 | u005、u042、u040、u047、u012、u013、u015、u021、u182、u189、u148、u169、u173 |

- **不替换**的常用词：弟子、女子、男子、父子、子孙（TTS 能读对）。
- 未替换、生成后需人工抽查的生僻字：祚(zuò)、灏(hào)、晏(yàn)、廿(niàn)、菽(shū)、黍(shǔ)、稷(jì)、豕(shǐ)、匏(páo)、杼(zhù)、篡(cuàn)。

## 批量重生成流程

1. 打开 `https://airvoz.com/zh/text-to-speech/voice/Xiaoshuang`（用 ZCode browser-use 或普通浏览器）。**页面下拉框选什么无所谓**——接口参数里已指定 voice，打开页面只是为了获得合法会话上下文。
2. 在该页面上下文对 192 个单元循环调用（单元文本以 `app/src/main/assets/content/sanzijing.json` 为准，对照稿见 `tools/voice_script.txt` A 节）：

   ```js
   // 在 https://airvoz.com 的页面里执行
   async function tts(text) {
     const r = await fetch("/api/tts", {
       method: "POST",
       headers: { "content-type": "application/json" },
       body: JSON.stringify({ input: text, voice: "zh-CN-XiaoshuangNeural",
                              language: "zh-CN", speed: 0.85, pitch: 1, volume: 1 }),
     });
     if (!r.ok) throw new Error("HTTP " + r.status);
     return new Uint8Array(await r.arrayBuffer());
   }
   ```

   每条先落 `build/voice-staging/uXXX.mp3`，校验 MP3 魔数（ID3 或 0xFFEx）且 >1KB 再落定（防截断文件）。
3. 全部 192 条成功后整体覆盖 `app/src/main/assets/audio/`（不要边生成边覆盖 assets）。
4. 跑校验：`.\gradlew :app:testDebugUnitTest --tests "com.rakor.sanzijing.data.AudioAssetsTest"`（检查 192 条齐全、>1KB、MP3 魔数）。
5. 重装抽查：`.\gradlew :app:installDebug`，模拟器/真机试听（重点：上表替换单元 + 生僻字清单）。
6. 提交 git（音频与 `tools/voice_script.txt` 一起）。

## 单条重生成

按单元号删掉 `build/voice-staging/uXXX.mp3` → 用上面的 `tts()` 只生成该条 → 覆盖 assets 对应文件 → 跑校验 → 重装。

## 注意事项

- 免费第三方服务，勿加压：并发 ≤3，失败指数退避重试。
- `speed=0.85` 是跟读节奏的一部分，不要改。
- 若 `gen_content.py` 调整了单元文本/数量，需同步 `tools/voice_script.txt` 对照稿，并检查新增文本是否引入新的多音字替换需求。
- 暂存目录 `build/voice-staging/` 会被 gradle clean 清掉；持久的对照稿与替换表在 `tools/voice_script.txt`。
