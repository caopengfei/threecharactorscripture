package com.rakor.sanzijing.data

import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentIntegrityTest {
    private val repo = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }

    @Test
    fun `结构完整 - 6章 192单元`() {
        assertEquals(6, repo.chapters.size)
        assertEquals(192, repo.units.size)
    }

    @Test
    fun `单元id唯一且连续`() {
        val ids = repo.units.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals((1..192).map { "u%03d".format(it) }, ids)
    }

    @Test
    fun `每单元两个短语，各3字，拼音音节数与字数一致`() {
        repo.units.forEach { u ->
            assertEquals(2, u.phrases.size)
            u.phrases.forEach { p ->
                assertEquals(3, p.hanzi.length, "${u.id} hanzi=${p.hanzi}")
                assertEquals(p.hanzi.length, p.pinyin.split(" ").size, "${u.id} pinyin=${p.pinyin}")
            }
        }
    }

    @Test
    fun `注释译文齐全`() {
        repo.units.forEach { u ->
            assertTrue(u.annotation.isNotBlank(), "${u.id} annotation 空")
            assertTrue(u.translation.isNotBlank(), "${u.id} translation 空")
        }
    }

    @Test
    fun `典故字段成对出现`() {
        repo.units.forEach { u ->
            assertEquals(u.storyTitle != null, u.story != null, "${u.id} story 字段不成对")
            if (u.story != null) assertTrue(u.story!!.length >= 40, "${u.id} story 过短")
        }
    }

    @Test
    fun `典故覆盖率不低于百分之六十`() {
        val withStory = repo.units.count { it.story != null }
        assertTrue(withStory >= 115, "有典故的单元 $withStory < 115")
    }

    @Test
    fun `章节标题非空且id连续`() {
        repo.chapters.forEachIndexed { i, ch ->
            assertTrue(ch.title.isNotBlank())
            assertEquals("ch%02d".format(i + 1), ch.id)
            assertTrue(ch.units.isNotEmpty())
        }
    }
}
