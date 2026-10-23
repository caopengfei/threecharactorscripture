package com.rakor.sanzijing.ui.common

/** 一个「字 + 它的注音」单元。 */
data class PinyinCell(val hanzi: String, val pinyin: String)

private val Whitespace = Regex("\\s+")

/** 按码点切字，避免代理对（生僻字扩展区）被劈开。 */
private fun codePointChars(s: String): List<String> {
    if (s.isEmpty()) return emptyList()
    val out = ArrayList<String>(s.length)
    var i = 0
    while (i < s.length) {
        val cp = s.codePointAt(i)
        val n = Character.charCount(cp)
        out += s.substring(i, i + n)
        i += n
    }
    return out
}

/**
 * 汉字逐字与拼音音节配对，供逐字注音排版使用。
 * 音节数必须与字数一致（ContentIntegrityTest 守卫）；不一致时返回 null，
 * 调用方退回整句排版，避免注音错位到别的字上。
 */
fun pinyinCells(hanzi: String, pinyin: String): List<PinyinCell>? {
    val chars = codePointChars(hanzi.trim())
    val syllables = pinyin.trim().split(Whitespace).filter { it.isNotEmpty() }
    if (chars.isEmpty() || chars.size != syllables.size) return null
    return chars.zip(syllables) { h, p -> PinyinCell(h, p) }
}
