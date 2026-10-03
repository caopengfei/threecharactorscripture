package com.rakor.sanzijing.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ProgressBarSimple
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardTitleStyle
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.SectionLabelStyle
import com.rakor.sanzijing.ui.theme.TrackBrown

@Composable
fun HomeScreen(
    app: SanzijingApp,
    onContinue: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val continueFocus = remember { FocusRequester() }

    LaunchedEffect(state.ready) { if (state.ready) continueFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("三字经", style = AppTitleStyle)
            Spacer(Modifier.width(24.dp))
            Text(
                "已读 ${state.readCount} / ${state.totalCount} 句 · 连续学习 ${state.streakDays} 天 · 今日 ${state.minutesToday} 分钟",
                style = CaptionStyle,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(
                onClick = { onContinue(state.continueUnitId) },
                modifier = Modifier.weight(2.2f).height(150.dp).focusRequester(continueFocus),
                colors = homeCardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("▶ 继续学习", style = SectionLabelStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("「${state.continueText}」 · ${state.continueChapterTitle}", style = CardTitleStyle)
                    Spacer(Modifier.height(12.dp))
                    val fraction = if (state.totalCount == 0) 0f else state.readCount.toFloat() / state.totalCount
                    ProgressBarSimple(fraction, Coral, TrackBrown)
                    Spacer(Modifier.height(6.dp))
                    Text("总进度 ${(fraction * 100).toInt()}%", style = CaptionStyle)
                }
            }
            Card(
                onClick = onOpenReport,
                modifier = Modifier.weight(1f).height(150.dp),
                colors = homeCardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("📈 学习报告", style = CardTitleStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("进度 · 熟读 · 连续天数", style = CaptionStyle)
                }
            }
            Card(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).height(150.dp),
                colors = homeCardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("⚙ 设置", style = CardTitleStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("朗读 · 连读 · 拼音", style = CaptionStyle)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("章节", style = SectionLabelStyle)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            state.chapters.forEach { ch ->
                Card(
                    onClick = { onContinue(ch.firstUnitId) },
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = homeCardColors(),
                ) {
                    Column(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(ch.title, style = CardTitleStyle.copy(fontSize = 18.sp))
                        Spacer(Modifier.height(6.dp))
                        Text("${ch.read} / ${ch.total} 句", style = CaptionStyle)
                        if (ch.inProgress) Text("学习中", style = SectionLabelStyle.copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun homeCardColors() = CardDefaults.colors(
    containerColor = CardWhite,
    contentColor = InkBrown,
    focusedContainerColor = CardFocused,
    focusedContentColor = InkBrown,
)
