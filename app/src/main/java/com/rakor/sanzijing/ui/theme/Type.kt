package com.rakor.sanzijing.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.R

// 字体策略（课本式混排）：
// - 汉字大字/标题用文楷 Medium（真字重，笔画扎实，与手写教学形一致）
// - 拼音用文楷 Regular（手写风格与楷体正文协调）
// - 注释/译文等小字正文用系统黑体（FontFamily.Default）：小字号远距离可读性优于楷体细笔画
val WenKai = FontFamily(Font(R.font.wenkai_subset))
val WenKaiMedium = FontFamily(Font(R.font.wenkai_medium_subset, weight = FontWeight.Medium))

val AppTitleStyle = TextStyle(fontFamily = WenKaiMedium, fontWeight = FontWeight.Medium, fontSize = 40.sp, letterSpacing = 8.sp, color = InkBrown)
val SectionLabelStyle = TextStyle(fontFamily = WenKaiMedium, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 4.sp, color = CoralDeep)
val HanziStyle = TextStyle(fontFamily = WenKaiMedium, fontWeight = FontWeight.Medium, fontSize = 64.sp, letterSpacing = 14.sp, color = InkBrown)
val PinyinStyle = TextStyle(fontFamily = WenKai, fontSize = 18.sp, letterSpacing = 5.sp, color = Honey)
val CardTitleStyle = TextStyle(fontFamily = WenKaiMedium, fontWeight = FontWeight.Medium, fontSize = 22.sp, color = InkBrown)
val BodyStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 17.sp, lineHeight = 30.sp, color = InkBrown)
val CaptionStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, color = InkBrownFaded)
