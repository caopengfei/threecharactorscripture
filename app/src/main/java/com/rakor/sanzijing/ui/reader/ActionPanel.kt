package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.rakor.sanzijing.data.content.Chapter

/** Task 12 用完整操作面板替换此占位。
 * 须在 ReaderScreen 根节点内就地组合（勿用 Popup/Dialog），按键由根节点 onPreviewKeyEvent 统一捕获。
 */
@Composable
fun ActionPanel(
    state: ReaderUiState,
    chapters: List<Chapter>,
    vm: ReaderViewModel,
    panelFocus: FocusRequester,
    onDismiss: () -> Unit,
) {
    Box(Modifier)
}
