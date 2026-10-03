package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.ui.theme.HanziStyle
import com.rakor.sanzijing.ui.theme.PinyinStyle

/** 汉字大字 + 上方拼音；读书页与面板共用。 */
@Composable
fun PinyinPhrase(
    hanzi: String,
    pinyin: String,
    showPinyin: Boolean,
    hanziSize: TextUnit = 64.sp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showPinyin) {
            Text(pinyin, style = PinyinStyle.copy(fontSize = hanziSize * 0.28f))
        }
        Text(hanzi, style = HanziStyle.copy(fontSize = hanziSize))
    }
}
