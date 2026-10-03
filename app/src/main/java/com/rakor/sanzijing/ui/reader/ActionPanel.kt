package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.rakor.sanzijing.data.content.Chapter
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.ScrimBrown
import com.rakor.sanzijing.ui.theme.SectionLabelStyle

/** 菜单键呼出的操作面板：连读、熟读、跳章。
 *  打开时焦点落在第一行（panelFocus），方向键在面板行间移动；
 *  勿改 Popup/Dialog，勿将遮罩设为可聚焦（会构成 D-pad 焦点陷阱）。
 */
@Composable
fun ActionPanel(
    state: ReaderUiState,
    chapters: List<Chapter>,
    vm: ReaderViewModel,
    panelFocus: FocusRequester,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ScrimBrown),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(600.dp)
                .background(CardWhite, RoundedCornerShape(28.dp))
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("操作", style = SectionLabelStyle.copy(fontSize = 24.sp))
            PanelRow("连读模式（播完自动下一句）", if (state.settings.continuousPlay) "开" else "关", modifier = Modifier.focusRequester(panelFocus)) {
                vm.setContinuous(!state.settings.continuousPlay)
            }
            PanelRow(
                "本句熟读 / 取消",
                when (state.currentStatus) {
                    ProgressRepository.STATUS_MASTERED -> "已熟读"
                    ProgressRepository.STATUS_READ -> "已读"
                    else -> "未学"
                },
            ) { vm.toggleMasteredNow() }
            Text("跳转章节", style = BodyStyle.copy(color = InkBrownFaded))
            chapters.chunked(3).forEachIndexed { r, rowChapters ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowChapters.forEachIndexed { c, ch ->
                        val index = r * 3 + c
                        val isCurrent = index == state.chapterIndex
                        Card(
                            onClick = {
                                vm.jumpToChapter(index)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.colors(
                                containerColor = if (isCurrent) CardFocused else CardWhite,
                                focusedContainerColor = CardFocused,
                            ),
                        ) {
                            Text(
                                ch.title + if (isCurrent) " ✦" else "",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                                style = BodyStyle.copy(fontSize = 15.sp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            PanelRow("关闭面板", "返回键 / OK") { onDismiss() }
        }
    }
}

@Composable
private fun PanelRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.colors(
            containerColor = CardWhite,
            contentColor = InkBrown,
            focusedContainerColor = CardFocused,
            focusedContentColor = InkBrown,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = BodyStyle)
            Text(value, style = BodyStyle.copy(color = Coral))
        }
    }
}
