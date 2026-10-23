package com.rakor.sanzijing.ui

import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.ui.common.PinyinCell
import com.rakor.sanzijing.ui.common.pinyinCells
import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinyinCellsTest {
    private val repo = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }

    @Test
    fun `逐字配对 - 三字短语音节一一对应`() {
        assertEquals(
            listOf(
                PinyinCell("人", "rén"),
                PinyinCell("之", "zhī"),
                PinyinCell("初", "chū"),
            ),
            pinyinCells("人之初", "rén zhī chū"),
        )
    }

    @Test
    fun `逐字配对 - 多余空白不影响`() {
        assertEquals(
            listOf(PinyinCell("性", "xìng"), PinyinCell("本", "běn"), PinyinCell("善", "shàn")),
            pinyinCells(" 性本善 ", "  xìng   běn shàn "),
        )
    }

    @Test
    fun `逐字配对 - 音节数不符时返回 null`() {
        assertNull(pinyinCells("人之初", "rén zhī"))
        assertNull(pinyinCells("人之初", "rén zhī chū yě"))
        assertNull(pinyinCells("", ""))
    }

    @Test
    fun `逐字配对 - 全部内容单元都能逐字对齐`() {
        repo.units.forEach { u ->
            u.phrases.forEach { p ->
                val cells = pinyinCells(p.hanzi, p.pinyin)
                requireNotNull(cells) { "${u.id} 无法逐字对齐: ${p.hanzi} / ${p.pinyin}" }
                assertEquals(p.hanzi, cells.joinToString("") { it.hanzi }, u.id)
                assertEquals(p.pinyin.trim().split(" "), cells.map { it.pinyin }, u.id)
            }
        }
    }
}
