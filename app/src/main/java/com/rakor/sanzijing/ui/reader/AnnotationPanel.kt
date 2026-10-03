package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.DividerBrown
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.SectionLabelStyle

/** 右侧滑出的释义典故面板（纯展示、无焦点，读书页按键不受影响）。 */
@Composable
fun AnnotationPanel(state: ReaderUiState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .width(460.dp)
            .padding(vertical = 24.dp, horizontal = 24.dp)
            .background(CardWhite, RoundedCornerShape(28.dp))
            .padding(32.dp)
    ) {
        // 滚动偏移按句重置：切句后从顶部开始，避免上一句的滚动位置残留
        val scroll = remember(state.unit.id) { ScrollState(0) }
        Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
            Text("释义 · ${state.unit.fullText}", style = SectionLabelStyle.copy(fontSize = 22.sp))
            Spacer(Modifier.height(18.dp))
            Text("注释", style = BodyStyle.copy(color = Coral, fontSize = 15.sp))
            Spacer(Modifier.height(6.dp))
            Text(state.unit.annotation, style = BodyStyle)
            Spacer(Modifier.height(18.dp))
            Text("译文", style = BodyStyle.copy(color = Coral, fontSize = 15.sp))
            Spacer(Modifier.height(6.dp))
            Text(state.unit.translation, style = BodyStyle)
            state.unit.storyTitle?.let { title ->
                Spacer(Modifier.height(22.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(DividerBrown))
                Spacer(Modifier.height(18.dp))
                Text("典故 · $title", style = BodyStyle.copy(color = Coral, fontSize = 16.sp))
                Spacer(Modifier.height(6.dp))
                Text(state.unit.story.orEmpty(), style = BodyStyle.copy(color = InkBrownFaded))
            }
            Spacer(Modifier.height(30.dp))
            Text("按 OK 或返回键关闭", style = BodyStyle.copy(color = InkBrownFaded, fontSize = 13.sp))
        }
    }
}
