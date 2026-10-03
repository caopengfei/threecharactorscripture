package com.rakor.sanzijing.data

import com.rakor.sanzijing.TestFiles
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioAssetsTest {
    @Test
    fun `每个单元都有对应且有效的音频文件`() {
        val dir = TestFiles.moduleFile("src/main/assets/audio")
        val bad = mutableListOf<String>()
        (1..192).forEach { i ->
            val name = "u%03d.mp3".format(i)
            val f = File(dir, name)
            // 与 tools/gen_audio.py 共用 1000 字节阈值：过滤截断/占位文件
            if (!f.exists() || f.length() <= 1_000) {
                bad += "$name 缺失或过小（${f.length()}B）"
                return@forEach
            }
            // MP3 魔数：ID3 前缀，或 MPEG 帧同步 0xFFEx
            val head = f.inputStream().use { it.readNBytes(4) }
            val isMp3 = (head.size >= 3 && head.copyOfRange(0, 3).contentEquals("ID3".toByteArray())) ||
                (head.size >= 2 && head[0] == 0xFF.toByte() && (head[1].toInt() and 0xE0) == 0xE0)
            if (!isMp3) bad += "$name 非法MP3（头部 ${head.joinToString(" ") { "%02X".format(it) }}）"
        }
        assertTrue("缺失或非法的音频: $bad", bad.isEmpty())
    }
}
