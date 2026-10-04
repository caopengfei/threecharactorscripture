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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.rakor.sanzijing.R
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.content.LessonUnit
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.common.PinyinPhrase
import com.rakor.sanzijing.ui.common.ToastHost
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.DividerBrown
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralTrack
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.InkBrownFaded
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
    var panelHasFocus by remember { mutableStateOf(false) }
    val timerScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { anchorFocus.requestFocus() }
    // 面板全关时显式收回焦点到锚点，避免依赖“焦点节点移除后隐式回落”的版本行为
    LaunchedEffect(state.showActionPanel) {
        if (state.showActionPanel) panelFocus.requestFocus() else anchorFocus.requestFocus()
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

    BackHandler(enabled = state.showActionPanel) { vm.closePanels() }
    BackHandler(enabled = !state.showActionPanel) { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                handleKey(e, vm, state.showActionPanel, panelHasFocus) { panelFocus.requestFocus() }
            }
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
                    ProgressRepository.STATUS_MASTERED -> StatusChip(true)
                    ProgressRepository.STATUS_READ -> StatusChip(false)
                }
                Spacer(Modifier.width(16.dp))
                if (state.isPlaying) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            tint = Coral,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("朗读中 · 跟着一起读", style = CaptionStyle.copy(color = Coral))
                    }
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
            AnnotationCard(unit = state.unit)
            Spacer(Modifier.height(10.dp))
            Text(
                "← → 前后句    ↑ ↓ 换章    OK 重读    长按OK 熟读    菜单键 更多",
                style = CaptionStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.showActionPanel) {
            Box(Modifier.onFocusChanged { panelHasFocus = it.hasFocus }) {
                ActionPanel(
                    state = state,
                    chapters = content.chapters,
                    vm = vm,
                    panelFocus = panelFocus,
                    onDismiss = { vm.closePanels() },
                )
            }
        }

        ToastHost(toast = state.toast, onConsumed = { vm.consumeToast() })
    }
}

/** 操作面板打开时，BACK/菜单键由本页在预览阶段处理——面板行（tv Card）持焦时会吞掉 BACK，
 *  不在预览阶段拦截的话第一次返回关不掉面板；焦点丢失的死态下方向键先召回面板焦点，
 *  其余按键仍交给面板内焦点组件。 */
private fun handleKey(
    e: KeyEvent,
    vm: ReaderViewModel,
    actionPanelOpen: Boolean,
    panelHasFocus: Boolean,
    refocusPanel: () -> Unit,
): Boolean {
    val key = e.key
    if (actionPanelOpen) {
        when (key) {
            Key.Back -> {
                if (e.type == KeyEventType.KeyUp) vm.closePanels()
                return true
            }
            Key.Menu -> {
                if (e.type == KeyEventType.KeyUp) vm.toggleActionPanel()
                return true
            }
            Key.DirectionRight, Key.DirectionLeft, Key.DirectionUp, Key.DirectionDown,
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
            -> {
                if (!panelHasFocus && e.type == KeyEventType.KeyDown) {
                    refocusPanel()
                    return true
                }
                return false
            }
            else -> return false
        }
    }
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

/** 释文常驻正文下方：注释 / 译文 / 典故（若有），无需按键唤出。 */
@Composable
private fun AnnotationCard(unit: LessonUnit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        AnnotationBlock("注释", unit.annotation, InkBrownFaded)

        Spacer(Modifier.height(8.dp))
        AnnotationBlock("译文", unit.translation, InkBrown)

        unit.storyTitle?.let { title ->
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(DividerBrown))
            Spacer(Modifier.height(8.dp))
            Text("典故 · $title", style = BodyStyle.copy(color = Coral, fontSize = 14.sp))
            Spacer(Modifier.height(2.dp))
            Text(
                unit.story.orEmpty(),
                style = BodyStyle.copy(color = InkBrownFaded, fontSize = 14.sp, lineHeight = 21.sp),
            )
        }
    }
}

@Composable
private fun AnnotationBlock(label: String, text: String, textColor: Color) {
    Row {
        Text(
            label,
            style = BodyStyle.copy(color = Coral, fontSize = 15.sp),
            modifier = Modifier.width(52.dp),
        )
        Text(text, style = BodyStyle.copy(color = textColor, fontSize = 15.sp, lineHeight = 24.sp))
    }
}

@Composable
private fun StatusChip(mastered: Boolean) {
    Row(
        Modifier
            .background(
                if (mastered) Coral else CoralTrack,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mastered) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("已熟读", style = CaptionStyle.copy(color = Color.White))
        } else {
            Text("已读", style = CaptionStyle.copy(color = Coral))
        }
    }
}
