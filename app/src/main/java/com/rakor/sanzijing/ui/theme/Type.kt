package com.rakor.sanzijing.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.R

val WenKai = FontFamily(Font(R.font.wenkai_subset))

val AppTitleStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 8.sp, color = InkBrown)
val SectionLabelStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 4.sp, color = CoralDeep)
val HanziStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 64.sp, letterSpacing = 14.sp, color = InkBrown)
val PinyinStyle = TextStyle(fontFamily = WenKai, fontSize = 18.sp, letterSpacing = 5.sp, color = Honey)
val CardTitleStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBrown)
val BodyStyle = TextStyle(fontFamily = WenKai, fontSize = 17.sp, lineHeight = 30.sp, color = InkBrown)
val CaptionStyle = TextStyle(fontFamily = WenKai, fontSize = 14.sp, color = InkBrownFaded)
