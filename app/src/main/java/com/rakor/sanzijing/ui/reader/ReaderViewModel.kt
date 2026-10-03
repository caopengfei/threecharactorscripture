package com.rakor.sanzijing.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.audio.AudioPlayer
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.content.LessonUnit
import com.rakor.sanzijing.data.prefs.ReaderSettings
import com.rakor.sanzijing.data.prefs.SettingsRepository
import android.util.Log
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val ready: Boolean = false,
    val pendingWork: Int = 0,
    val unit: LessonUnit,
    val unitIndex: Int = 0,
    val totalCount: Int = 0,
    val chapterTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterPos: Int = 0,
    val chapterSize: Int = 0,
    val showAnnotation: Boolean = false,
    val showActionPanel: Boolean = false,
    val isPlaying: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
    val statusOf: Map<String, Int> = emptyMap(),
    val toast: String? = null,
) {
    val currentStatus: Int get() = statusOf[unit.id] ?: ProgressRepository.STATUS_NEW
}

class ReaderViewModel(
    startUnitId: String,
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    private val settingsRepo: SettingsRepository,
    private val audioPlayer: AudioPlayer,
    private val externalScope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val totalCount = contentRepo.units.size
    private var okDownAt: Long? = null
    private var collectorJobs: List<Job> = emptyList()

    private val _state = MutableStateFlow(stateFor(startIndex(startUnitId), ReaderUiState(unit = contentRepo.unitAt(0))))
    val state: StateFlow<ReaderUiState> = _state

    init {
        tracked {
            val settings = settingsRepo.settings.first()
            val progress = progressRepo.observeProgress().first()
            _state.update {
                // ready 前 UI 不会发指令；防御性保护已发生的导航
                val base = if (it.unitIndex != startIndex(startUnitId)) it else stateFor(startIndex(startUnitId), it)
                base.copy(settings = settings, statusOf = progress.associate { e -> e.unitId to e.status }, ready = true)
            }
            if (settings.autoPlay) playCurrent()
        }
        collectorJobs = listOf(
            externalScope.launch {
                settingsRepo.settings.collect { s -> _state.update { it.copy(settings = s) } }
            },
            externalScope.launch {
                progressRepo.observeProgress().collect { p ->
                    _state.update { it.copy(statusOf = p.associate { e -> e.unitId to e.status }) }
                }
            },
        )
    }

    /** 统计在途可完成协程（测试 settle 用）；永不结束的收集不在此列。 */
    private fun tracked(block: suspend CoroutineScope.() -> Unit) =
        externalScope.launch {
            _state.update { it.copy(pendingWork = it.pendingWork + 1) }
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                // 儿童应用：Room/DataStore 偶发 IO 抖动不崩溃，toast 提示后可重试
                Log.w("ReaderViewModel", "tracked 操作失败", e)
            _state.update { it.copy(toast = "操作失败，请重试") }
            } finally {
                _state.update { s -> s.copy(pendingWork = s.pendingWork - 1) }
            }
        }

    private fun startIndex(unitId: String): Int = contentRepo.unitIndexOf(unitId).coerceIn(0, totalCount - 1)

    private fun stateFor(index: Int, prev: ReaderUiState): ReaderUiState {
        val unit = contentRepo.unitAt(index)
        val chapterIdx = contentRepo.chapterIndexOf(unit.id)
        val chapter = contentRepo.chapterAt(chapterIdx)
        return prev.copy(
            unit = unit,
            unitIndex = index,
            totalCount = totalCount,
            chapterTitle = chapter.title,
            chapterIndex = chapterIdx,
            chapterPos = chapter.units.indexOfFirst { it.id == unit.id } + 1,
            chapterSize = chapter.units.size,
        )
    }

    fun next() = moveTo(_state.value.unitIndex + 1)
    fun previous() = moveTo(_state.value.unitIndex - 1)

    fun jumpChapter(delta: Int) = jumpToChapter(contentRepo.chapterIndexOf(_state.value.unit.id) + delta)

    fun jumpToChapter(index: Int) {
        val target = index.coerceIn(0, contentRepo.chapters.size - 1)
        moveTo(contentRepo.unitIndexOf(contentRepo.firstUnitIdOfChapter(target)))
    }

    fun replay() { playCurrent() }

    fun setContinuous(enabled: Boolean) {
        _state.update { it.copy(settings = it.settings.copy(continuousPlay = enabled)) }
        tracked { settingsRepo.setContinuousPlay(enabled) }
    }

    fun toggleActionPanel() = _state.update {
        it.copy(showActionPanel = !it.showActionPanel, showAnnotation = false)
    }

    /** OK 按下：记录时刻（忽略重复 down）。 */
    fun okDown() { if (okDownAt == null) okDownAt = now() }

    /** OK 抬起：>=600ms 长按标记熟读，短按切换释义面板。 */
    fun okUp() {
        val downAt = okDownAt ?: return
        okDownAt = null
        val elapsed = now() - downAt
        if (elapsed > 10_000) return   // 遥控器丢抬起事件保护——超过 10 秒的按下视为残留状态，忽略本次
        if (elapsed >= 600) {
            tracked { progressRepo.toggleMastered(_state.value.unit.id) }
        } else {
            _state.update { it.copy(showAnnotation = !it.showAnnotation, showActionPanel = false) }
        }
    }

    fun tickSecond() {
        tracked { progressRepo.addStudySeconds(1) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun closePanels() = _state.update { it.copy(showAnnotation = false, showActionPanel = false) }

    fun toggleMasteredNow() {
        tracked { progressRepo.toggleMastered(_state.value.unit.id) }
    }

    override fun onCleared() {
        collectorJobs.forEach { it.cancel() }   // externalScope 生命周期长于 VM，收集器须随 VM 取消
        audioPlayer.stop()
        super.onCleared()
    }

    private fun moveTo(index: Int, forcePlay: Boolean = false) {
        val clamped = index.coerceIn(0, totalCount - 1)
        if (clamped == _state.value.unitIndex && !forcePlay) return
        _state.update { stateFor(clamped, it) }
        if (forcePlay || _state.value.settings.autoPlay) playCurrent()
    }

    private fun playCurrent() {
        val unit = _state.value.unit
        _state.update { it.copy(isPlaying = true) }   // 先置 true：同步 onError 的 false 才能生效
        audioPlayer.play(
            assetPath = "audio/${unit.id}.mp3",
            onComplete = { tracked { onPlaybackCompleted(unit.id) } },
            onError = {
                _state.update {
                    // 过期回调守卫：单元已切换时不再翻转新单元的 isPlaying/误报 toast
                    if (it.unit.id == unit.id) it.copy(isPlaying = false, toast = "音频缺失：${unit.fullText}") else it
                }
            },
        )
    }

    private suspend fun onPlaybackCompleted(unitId: String) {
        progressRepo.onPlaybackCompleted(unitId)   // 旧音频确实播完，进度照记
        _state.update { if (it.unit.id == unitId) it.copy(isPlaying = false) else it }   // 过期回调不翻转新单元状态
        val st = _state.value
        if (st.unit.id == unitId && st.settings.continuousPlay && st.unitIndex < totalCount - 1) {
            moveTo(st.unitIndex + 1, forcePlay = true)
        }
    }

    companion object {
        fun factory(startUnitId: String, app: SanzijingApp) = viewModelFactory {
            initializer {
                ReaderViewModel(
                    startUnitId = startUnitId,
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    settingsRepo = app.container.settingsRepository,
                    audioPlayer = app.container.audioPlayer,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
