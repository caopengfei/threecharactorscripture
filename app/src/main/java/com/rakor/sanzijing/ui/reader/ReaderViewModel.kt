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
    val showActionPanel: Boolean = false,
    val isPlaying: Boolean = false,
    /** 连读模式下的暂停态。不变式：paused 为 true 时连读必定开启（关连读会结束暂停会话）。 */
    val paused: Boolean = false,
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
    /** 每次 playCurrent 自增的播放序号：播完回调若发现在途已开始新播放（同一句重读）即作废。 */
    private var playbackSeq = 0L
    private var collectorJobs: List<Job> = emptyList()
    private var inForeground = true
    private var resumeOnForeground = false

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
                settingsRepo.settings.collect { s ->
                    _state.update { it.copy(settings = s) }
                    if (!s.continuousPlay) endPauseIfAny()   // 设置页关掉连读也要结束暂停会话
                }
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
            paused = false,
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
        if (!enabled) endPauseIfAny()
    }

    /** 暂停是连读模式下的状态：连读关掉即结束暂停会话并释放播放器，避免"已暂停/按OK继续"与"OK 重读"打架。 */
    private fun endPauseIfAny() {
        if (_state.value.paused) {
            _state.update { it.copy(paused = false) }
            audioPlayer.stop()
        }
    }

    fun toggleActionPanel() = _state.update {
        it.copy(showActionPanel = !it.showActionPanel)
    }

    /** OK 按下：记录时刻（忽略重复 down）。 */
    fun okDown() { if (okDownAt == null) okDownAt = now() }

    /** OK 抬起：连读开启时做暂停 ⇄ 继续（长按不再标熟读，熟读走菜单面板的"本句熟读"）；
     *  连读关闭时维持原语义——>=600ms 长按标记熟读，短按重读当前句（小米盒子遥控器无播放键）。 */
    fun okUp() {
        val downAt = okDownAt ?: return
        okDownAt = null
        val elapsed = now() - downAt
        if (elapsed > 10_000) return   // 遥控器丢抬起事件保护——超过 10 秒的按下视为残留状态，忽略本次
        when {
            _state.value.settings.continuousPlay -> togglePlayback()
            elapsed >= 600 -> tracked { progressRepo.toggleMastered(_state.value.unit.id) }
            else -> replay()
        }
    }

    /** 连读开启时 OK 的动作：暂停 ⇄ 继续；空闲（未在播、无可续的暂停）则朗读当前句。 */
    private fun togglePlayback() {
        val st = _state.value
        when {
            st.paused -> {
                if (audioPlayer.resume()) {
                    _state.update { it.copy(isPlaying = true, paused = false) }
                } else {
                    playCurrent()   // 暂停的播放器已释放（如退过后台）：从头重读本句
                }
            }
            st.isPlaying -> {
                // pause 失败说明音频刚好播完（PlaybackCompleted 态不可 pause），交给播完回调收尾，不误置暂停态
                if (audioPlayer.pause()) _state.update { it.copy(isPlaying = false, paused = true) }
            }
            else -> playCurrent()
        }
    }

    fun tickSecond() {
        tracked { progressRepo.addStudySeconds(1) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun closePanels() = _state.update { it.copy(showActionPanel = false) }

    fun toggleMasteredNow() {
        tracked { progressRepo.toggleMastered(_state.value.unit.id) }
    }

    /** 退到后台（遥控器 HOME、系统界面盖住本页）：立即停朗读并中断连读链。
     *  音频是 application 级单例，不主动停的话后台会一直播下去，连读还会一路自动翻句记进度。 */
    fun onLeaveForeground() {
        inForeground = false
        val st = _state.value
        resumeOnForeground = st.isPlaying
        if (st.isPlaying || st.paused) {
            // 暂停态也要释放：暂停中的 MediaPlayer 仍占资源，且回前台不自动续读（用户是主动停的）
            _state.update { it.copy(isPlaying = false, paused = false) }
            audioPlayer.stop()
        }
    }

    /** 回到前台：仅当离开时确在朗读才续读那一句（连读由播完回调继续往后走）。 */
    fun onEnterForeground() {
        inForeground = true
        if (!resumeOnForeground) return
        resumeOnForeground = false
        playCurrent()
    }

    override fun onCleared() {
        collectorJobs.forEach { it.cancel() }   // externalScope 生命周期长于 VM，收集器须随 VM 取消
        audioPlayer.stop()
        super.onCleared()
    }

    private fun moveTo(index: Int, forcePlay: Boolean = false) {
        val clamped = index.coerceIn(0, totalCount - 1)
        if (clamped == _state.value.unitIndex && !forcePlay) return
        endPauseIfAny()   // 换句即结束暂停会话：stateFor 会清 paused，但暂停中的播放器必须显式释放
        _state.update { stateFor(clamped, it) }
        if (forcePlay || _state.value.settings.autoPlay) playCurrent()
    }

    private fun playCurrent() {
        val unit = _state.value.unit
        val seq = ++playbackSeq
        _state.update { it.copy(isPlaying = true, paused = false) }   // 先置 true：同步 onError 的 false 才能生效
        audioPlayer.play(
            assetPath = "audio/${unit.id}.mp3",
            onComplete = {
                // 音频已确实播完，立刻同步翻掉播放态：若等下面的进度写入跑完再翻，这段窗口里 UI 仍显示"朗读中"，
                // 此时按 OK 会落空、连读还会静默推进到下一句。
                _state.update { if (it.unit.id == unit.id) it.copy(isPlaying = false) else it }
                tracked { onPlaybackCompleted(unit.id, seq) }
            },
            onError = {
                _state.update {
                    // 过期回调守卫：单元已切换时不再翻转新单元的 isPlaying/误报 toast
                    if (it.unit.id == unit.id) it.copy(isPlaying = false, toast = "音频缺失：${unit.fullText}") else it
                }
            },
        )
    }

    private suspend fun onPlaybackCompleted(unitId: String, seq: Long) {
        progressRepo.onPlaybackCompleted(unitId)   // 旧音频确实播完，进度照记
        if (seq != playbackSeq) return             // 期间已开始新的播放（如尾部窗口按 OK 重读），本次回调作废
        _state.update { if (it.unit.id == unitId) it.copy(isPlaying = false) else it }   // 过期回调不翻转新单元状态
        val st = _state.value
        // paused 时用户刚按了暂停，此刻到达的播完回调（交错的遥控器操作）不再推进连读链
        if (inForeground && !st.paused && st.unit.id == unitId && st.settings.continuousPlay) {
            // 连读循环：最后一句播完回到第一句；手动翻句（next/previous）仍首尾钳制不越界
            val next = if (st.unitIndex >= totalCount - 1) 0 else st.unitIndex + 1
            moveTo(next, forcePlay = true)
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
