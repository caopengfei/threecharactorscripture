package com.rakor.sanzijing.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ProgressBarSimple
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardTitleStyle
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralDeep
import com.rakor.sanzijing.ui.theme.TrackBrown

@Composable
fun ReportScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val app = LocalContext.current.applicationContext as SanzijingApp
    val vm: ReportViewModel = viewModel(factory = ReportViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()

    Row(
        Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("学习报告", style = AppTitleStyle.copy(fontSize = 30.sp))
            Spacer(Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center) {
                val fraction = if (state.totalCount == 0) 0f else state.totalRead.toFloat() / state.totalCount
                Canvas(Modifier.size(190.dp)) {
                    val stroke = Stroke(width = 22f, cap = StrokeCap.Round)
                    drawArc(Color(0x22FF8A65), -90f, 360f, useCenter = false, style = stroke)
                    drawArc(Coral, -90f, 360f * fraction, useCenter = false, style = stroke)
                }
                Text("${(fraction * 100).toInt()}%", style = CardTitleStyle.copy(fontSize = 34.sp, color = CoralDeep))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                "已读 ${state.totalRead} 句 · 熟读 ${state.masteredCount} 句\n连续学习 ${state.streakDays} 天 · 累计 ${state.totalMinutes} 分钟 · 今日 ${state.minutesToday} 分钟",
                style = CardTitleStyle.copy(fontSize = 20.sp),
            )
            Spacer(Modifier.height(28.dp))
            if (state.totalRead == 0) {
                Text("还没有学习记录，回首页开始第一句吧！", style = CaptionStyle)
            }
            state.chapters.forEach { ch ->
                Text("${ch.title}（熟读 ${ch.mastered}）", style = CaptionStyle)
                Spacer(Modifier.height(6.dp))
                ProgressBarSimple(
                    fraction = if (ch.total == 0) 0f else ch.read.toFloat() / ch.total,
                    barColor = Coral,
                    trackColor = TrackBrown,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
