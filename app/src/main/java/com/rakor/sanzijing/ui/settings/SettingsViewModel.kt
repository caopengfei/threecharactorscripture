package com.rakor.sanzijing.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.prefs.ReaderSettings
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CancellationException
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

    /** 设置写入失败只提示重试，不崩溃（与 ReaderViewModel.tracked 同约定，CancellationException 照常重抛）。 */
    private inline fun tracked(tag: String, crossinline block: suspend () -> Unit) =
        externalScope.launch {
            try {
                block()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "$tag failed", e)
                _state.update { it.copy(message = "操作失败，请重试") }
            }
        }

    fun toggleAutoPlay() = tracked("setAutoPlay") { settingsRepo.setAutoPlay(!_state.value.settings.autoPlay) }
    fun toggleContinuous() = tracked("setContinuousPlay") { settingsRepo.setContinuousPlay(!_state.value.settings.continuousPlay) }
    fun togglePinyin() = tracked("setShowPinyin") { settingsRepo.setShowPinyin(!_state.value.settings.showPinyin) }

    fun requestClearDialog(show: Boolean) = _state.update { it.copy(showClearDialog = show) }

    /** 对话框先乐观关闭；成功提示只在 clearAll 真正成功后给出。 */
    fun confirmClear() {
        _state.update { it.copy(showClearDialog = false) }
        externalScope.launch {
            try {
                progressRepo.clearAll()
                _state.update { it.copy(message = "学习进度已清除") }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "clearAll failed", e)
                _state.update { it.copy(message = "清除失败，请重试") }
            }
        }
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
