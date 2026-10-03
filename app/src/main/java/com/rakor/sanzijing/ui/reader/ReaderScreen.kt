package com.rakor.sanzijing.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.common.PinyinPhrase
import com.rakor.sanzijing.ui.common.ToastHost
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.DividerBrown
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralTrack
import com.rakor.sanzijing.ui.theme.SectionLabelStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(app: SanzijingApp, startUnitId: String, onBack: () -> Unit) {
    val vm: ReaderViewModel = viewModel(factory = ReaderViewModel.factory(startUnitId, app))
    val state by vm.state.collectAsStateWithLifecycle()
    val content = app.container.contentRepository
    val anchorFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    val timerScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { anchorFocus.requestFocus() }
    // 面板全关时显式收回焦点到锚点，避免依赖“焦点节点移除后隐式回落”的版本行为
    LaunchedEffect(state.showAnnotation, state.showActionPanel) {
        if (!state.showAnnotation && !state.showActionPanel) anchorFocus.requestFocus()
        else if (state.showActionPanel) panelFocus.requestFocus()
    }

    // 学习时长：页面 RESUMED 期间每秒 +1（暂停/离开自动停止）
    LifecycleResumeEffect(Unit) {
        val job = timerScope.launch {
            while (true) {
                delay(1_000)
                vm.tickSecond()
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    BackHandler(enabled = state.showAnnotation || state.showActionPanel) { vm.closePanels() }
    BackHandler(enabled = !state.showAnnotation && !state.showActionPanel) { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e -> handleKey(e, vm, state.showActionPanel) }
            .focusRequester(anchorFocus)
            .focusable()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.chapterTitle} · ${state.chapterPos} / ${state.chapterSize}",
                    style = SectionLabelStyle,
                )
                Spacer(Modifier.weight(1f))
                when (state.currentStatus) {
                    ProgressRepository.STATUS_MASTERED -> StatusChip("✓ 已熟读", filled = true)
                    ProgressRepository.STATUS_READ -> StatusChip("已读", filled = false)
                }
                Spacer(Modifier.width(16.dp))
                if (state.isPlaying) {
                    Text("♪ 朗读中 · 跟着一起读", style = CaptionStyle.copy(color = Coral))
                }
            }
            // 内容契约：每单元恒为两个三字短语（ContentIntegrityTest 守卫）
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinyinPhrase(
                    hanzi = state.unit.phrases[0].hanzi,
                    pinyin = state.unit.phrases[0].pinyin,
                    showPinyin = state.settings.showPinyin,
                )
                Box(
                    Modifier
                        .padding(horizontal = 40.dp)
                        .width(1.dp)
                        .height(90.dp)
                        .background(DividerBrown)
                )
                PinyinPhrase(
                    hanzi = state.unit.phrases[1].hanzi,
                    pinyin = state.unit.phrases[1].pinyin,
                    showPinyin = state.settings.showPinyin,
                )
            }
            Text(
                "◀ ▶ 前后句    ▲ ▼ 换章    OK 释义典故    播放键 重读    长按OK 熟读    菜单键 更多",
                style = CaptionStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        if (state.showAnnotation) {
            AnnotationPanel(state = state, modifier = Modifier.align(Alignment.CenterEnd))
        }

        if (state.showActionPanel) {
            ActionPanel(
                state = state,
                chapters = content.chapters,
                vm = vm,
                panelFocus = panelFocus,
                onDismiss = { vm.closePanels() },
            )
        }

        ToastHost(toast = state.toast, onConsumed = { vm.consumeToast() })
    }
}

/** 操作面板打开时，仅菜单键由本页处理（关闭面板），其余按键交给面板内焦点组件。 */
private fun handleKey(e: KeyEvent, vm: ReaderViewModel, actionPanelOpen: Boolean): Boolean {
    val key = e.key
    if (actionPanelOpen && key != Key.Menu) return false
    when (e.type) {
        KeyEventType.KeyUp -> return when (key) {
            Key.Menu -> { vm.toggleActionPanel(); true }
            Key.MediaPlayPause, Key.MediaPlay -> { vm.replay(); true }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { vm.okUp(); true }
            else -> false
        }
        KeyEventType.KeyDown -> return when (key) {
            Key.DirectionRight -> { vm.next(); true }
            Key.DirectionLeft -> { vm.previous(); true }
            Key.DirectionUp -> { vm.jumpChapter(-1); true }
            Key.DirectionDown -> { vm.jumpChapter(1); true }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { vm.okDown(); true }
            else -> false
        }
    }
    return false
}

@Composable
private fun StatusChip(label: String, filled: Boolean) {
    Box(
        Modifier
            .background(
                if (filled) Coral else CoralTrack,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label, style = CaptionStyle.copy(color = if (filled) Color.White else Coral))
    }
}
