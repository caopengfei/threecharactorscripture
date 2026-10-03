package com.rakor.sanzijing.data.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentFile(val version: Int, val chapters: List<Chapter>)

@Serializable
data class Chapter(val id: String, val title: String, val units: List<LessonUnit>)

@Serializable
data class LessonUnit(
    val id: String,
    val phrases: List<Phrase>,
    val annotation: String,
    val translation: String,
    val storyTitle: String? = null,
    val story: String? = null,
) {
    val fullText: String get() = phrases.joinToString("，") { it.hanzi }
}

@Serializable
data class Phrase(val hanzi: String, val pinyin: String)
