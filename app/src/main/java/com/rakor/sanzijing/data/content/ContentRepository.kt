package com.rakor.sanzijing.data.content

import kotlinx.serialization.json.Json
import java.io.InputStream

/** 内容只读仓库：启动后解析一次 assets JSON，进程内缓存。open 参数便于测试注入。 */
class ContentRepository(private val open: (String) -> InputStream) {

    private val json = Json { ignoreUnknownKeys = true }

    val content: ContentFile by lazy {
        open("content/sanzijing.json").use { stream ->
            json.decodeFromString<ContentFile>(stream.readBytes().decodeToString())
        }
    }

    val chapters: List<Chapter> get() = content.chapters
    val units: List<LessonUnit> get() = content.chapters.flatMap { it.units }

    fun unitIndexOf(unitId: String): Int = units.indexOfFirst { it.id == unitId }

    fun unitAt(index: Int): LessonUnit = units[index]

    fun chapterIndexOf(unitId: String): Int = chapters.indexOfFirst { ch -> ch.units.any { it.id == unitId } }

    fun chapterAt(index: Int): Chapter = chapters[index]

    fun firstUnitIdOfChapter(chapterIndex: Int): String = chapters[chapterIndex].units.first().id
}
