package com.rakor.sanzijing.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ToastHost
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralDeep
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.ScrimBrown

@Composable
fun SettingsScreen(app: SanzijingApp, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }

    LaunchedEffect(state.showClearDialog) {
        if (state.showClearDialog) cancelFocus.requestFocus()   // 破坏性操作默认焦点在取消上
        else firstFocus.requestFocus()                           // 初始进入/对话框关闭后收回焦点到首行
    }

    // 返回键优先关对话框；对话框未开时才退出设置页（勿合回单 handler，否则导航层会弹走整页）
    BackHandler(enabled = state.showClearDialog) { vm.requestClearDialog(false) }
    BackHandler(enabled = !state.showClearDialog) { onBack() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 44.dp)) {
            Text("设置", style = AppTitleStyle.copy(fontSize = 30.sp))
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.width(720.dp)) {
                // 对话框打开时禁用背景行（禁用 Card 不可聚焦），D-pad 焦点被锁在对话框内
                val rowsEnabled = !state.showClearDialog
                SettingRow(
                    "自动朗读当前句",
                    if (state.settings.autoPlay) "开" else "关",
                    enabled = rowsEnabled,
                    modifier = Modifier.focusRequester(firstFocus),
                ) { vm.toggleAutoPlay() }
                SettingRow("连读（播完自动下一句）", if (state.settings.continuousPlay) "开" else "关", enabled = rowsEnabled) { vm.toggleContinuous() }
                SettingRow("拼音显示", if (state.settings.showPinyin) "开" else "关", enabled = rowsEnabled) { vm.togglePinyin() }
                SettingRow("清除学习进度", "确认 ›", enabled = rowsEnabled) { vm.requestClearDialog(true) }
            }
            Spacer(Modifier.height(28.dp))
            Text("版本 1.0 · 内容基于通行版《三字经》", style = CaptionStyle)
        }

        if (state.showClearDialog) {
            ClearProgressDialog(
                cancelFocus = cancelFocus,
                onConfirm = { vm.confirmClear() },
                onDismiss = { vm.requestClearDialog(false) },
            )
        }
        ToastHost(toast = state.message, onConsumed = { vm.consumeMessage() })
    }
}

/** tv-material 1.0.1 的 Card 无 enabled 参数：禁用时退化为同视觉的静态 Box（不可聚焦），锁定焦点在对话框内。 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val rowModifier = modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = BodyStyle)
            Text(value, style = BodyStyle.copy(color = Coral))
        }
    }
    if (enabled) {
        Card(
            onClick = onClick,
            modifier = rowModifier,
            colors = CardDefaults.colors(
                containerColor = CardWhite,
                contentColor = InkBrown,
                focusedContainerColor = CardFocused,
                focusedContentColor = InkBrown,
            ),
        ) { content() }
    } else {
        Box(rowModifier.background(CardWhite, RoundedCornerShape(8.dp))) { content() }
    }
}

/** 就地组合的确认对话框（勿用 Popup/Dialog；遮罩不可聚焦——D-pad 焦点陷阱），默认焦点在取消。 */
@Composable
private fun ClearProgressDialog(
    cancelFocus: FocusRequester,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(ScrimBrown),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(520.dp)
                .background(CardWhite, RoundedCornerShape(28.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("清除学习进度？", style = BodyStyle.copy(color = CoralDeep, fontSize = 22.sp))
            Text("将删除全部已读/熟读记录与学习时长统计，且无法恢复。", style = BodyStyle)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(cancelFocus),
                    colors = CardDefaults.colors(
                        containerColor = CardWhite,
                        focusedContainerColor = CardFocused,
                    ),
                ) {
                    Text("取消", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = BodyStyle)
                }
                Card(
                    onClick = onConfirm,
                    colors = CardDefaults.colors(
                        containerColor = Coral,
                        contentColor = Color.White,
                        focusedContainerColor = CoralDeep,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text("确认清除", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = BodyStyle.copy(color = Color.White))
                }
            }
        }
    }
}
