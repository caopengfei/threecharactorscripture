package com.rakor.sanzijing.data

import com.rakor.sanzijing.TestFiles
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioAssetsTest {
    @Test
    fun `每个单元都有对应且有效的音频文件`() {
        val dir = TestFiles.moduleFile("src/main/assets/audio")
        (1..192).forEach { i ->
            val f = File(dir, "u%03d.mp3".format(i))
            assertTrue("${f.name} 缺失或过小（${f.length()}B）", f.exists() && f.length() > 1_000)
        }
    }
}
