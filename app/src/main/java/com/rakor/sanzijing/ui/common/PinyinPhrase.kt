package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.ui.theme.HanziStyle
import com.rakor.sanzijing.ui.theme.PinyinStyle

/** 拼音字号 ≈ 汉字 0.28，与 21sp/76sp 默认一致。 */
private const val PinyinRatio = 0.28f

/** 字间距 ≈ 汉字 0.21（76sp 汉字下约 16sp，与原整行字距一致）。 */
private const val HanziGapRatio = 0.21f

/**
 * 汉字大字 + 上方拼音；读书页与面板共用。
 *
 * 按「字 + 注音」逐列排版：每个字的拼音夹在自己的字正上方居中，
 * 因而拼音与汉字严格对齐（整行拼字符串排版时，各音节宽度不同会整体错位）。
 */
@Composable
fun PinyinPhrase(
    hanzi: String,
    pinyin: String,
    showPinyin: Boolean,
    hanziSize: TextUnit = 76.sp,
    modifier: Modifier = Modifier,
) {
    val cells = pinyinCells(hanzi, pinyin)
    val hanziStyle = HanziStyle.copy(fontSize = hanziSize, letterSpacing = 0.sp)
    val pinyinStyle = PinyinStyle.copy(
        fontSize = hanziSize * PinyinRatio,
        letterSpacing = 0.sp,   // 字距交给列间距控制，避免音节被撑歪
    )
    val gap = with(LocalDensity.current) { (hanziSize * HanziGapRatio).toDp() }

    if (cells == null) {
        // 音节数与字数不符时的兜底：整句排版（ContentIntegrityTest 保证不会发生）
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            if (showPinyin) Text(pinyin, style = pinyinStyle, maxLines = 1, softWrap = false)
            Text(hanzi, style = hanziStyle)
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { cell ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showPinyin) {
                    Text(cell.pinyin, style = pinyinStyle, maxLines = 1, softWrap = false)
                }
                Text(cell.hanzi, style = hanziStyle, maxLines = 1, softWrap = false)
            }
        }
    }
}
