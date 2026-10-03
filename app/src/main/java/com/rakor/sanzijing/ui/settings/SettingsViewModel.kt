package com.rakor.sanzijing.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.prefs.ReaderSettings
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ReaderSettings = ReaderSettings(),
    val showClearDialog: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val progressRepo: ProgressRepository,
    private val externalScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    // 设置收集随 VM 取消（VM 级 externalScope，与 Home/Reader 约定一致）
    private val collectorJob = externalScope.launch {
        settingsRepo.settings.collect { s -> _state.update { it.copy(settings = s) } }
    }

    override fun onCleared() {
        collectorJob.cancel()
        super.onCleared()
    }

    fun toggleAutoPlay() { externalScope.launch { settingsRepo.setAutoPlay(!_state.value.settings.autoPlay) } }
    fun toggleContinuous() { externalScope.launch { settingsRepo.setContinuousPlay(!_state.value.settings.continuousPlay) } }
    fun togglePinyin() { externalScope.launch { settingsRepo.setShowPinyin(!_state.value.settings.showPinyin) } }

    fun requestClearDialog(show: Boolean) = _state.update { it.copy(showClearDialog = show) }

    fun confirmClear() {
        externalScope.launch { progressRepo.clearAll() }
        _state.update { it.copy(showClearDialog = false, message = "学习进度已清除") }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepo = app.container.settingsRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
