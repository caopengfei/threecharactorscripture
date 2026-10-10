package com.rakor.sanzijing.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.content.ContentFile
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressEntity
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChapterUi(
    val id: String,
    val title: String,
    val firstUnitId: String,
    val total: Int,
    val read: Int,
    val inProgress: Boolean,
)

data class HomeUiState(
    val ready: Boolean = false,
    val continueUnitId: String = "u001",
    val continueText: String = "",
    val continueChapterTitle: String = "",
    val readCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 0,
    val minutesToday: Long = 0,
    val chapters: List<ChapterUi> = emptyList(),
)

class HomeViewModel(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    externalScope: CoroutineScope,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    // 初始值不触碰 contentRepo：JSON 解析只发生在 IO 线程（contentFlow），主线程构造路径零解析
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    /** 首次触碰 content 即触发 lazy JSON 解析，flowOn 保证落在 IO 线程；ready 前 UI 留白。 */
    private val contentFlow = flow { emit(contentRepo.content) }.flowOn(Dispatchers.IO)

    private val collectorJob = externalScope.launch {
        combine(
            contentFlow,
            progressRepo.observeProgress(),
            progressRepo.observeDailyStats(),
            ::compute,
        ).collect { _state.value = it }
    }

    /** collector 随 VM 取消；externalScope 为 VM 级作用域（与 ReaderViewModel 约定一致）。 */
    override fun onCleared() {
        collectorJob.cancel()
        super.onCleared()
    }

    private fun compute(
        content: ContentFile,
        progress: List<UnitProgressEntity>,
        stats: List<DailyStatsEntity>,
    ): HomeUiState {
        val statusOf = progress.associate { it.unitId to it.status }
        val units = content.chapters.flatMap { it.units }
        val contId = progressRepo.continueUnitId(units.map { it.id }, progress)
        val contUnit = units.first { it.id == contId }
        val contChapter = content.chapters.first { ch -> ch.units.any { it.id == contId } }
        val todayStr = today().toString()
        return HomeUiState(
            ready = true,
            continueUnitId = contId,
            continueText = contUnit.fullText,
            continueChapterTitle = contChapter.title,
            readCount = progress.count { it.status > ProgressRepository.STATUS_NEW },
            totalCount = units.size,
            streakDays = progressRepo.studyStreak(stats, today()),
            minutesToday = ((stats.firstOrNull { it.date == todayStr }?.secondsStudied ?: 0) + 59) / 60,
            chapters = content.chapters.map { ch ->
                ChapterUi(
                    id = ch.id,
                    title = ch.title,
                    firstUnitId = ch.units.first().id,
                    total = ch.units.size,
                    read = ch.units.count { (statusOf[it.id] ?: 0) > ProgressRepository.STATUS_NEW },
                    inProgress = ch.units.any { it.id == contId },
                )
            },
        )
    }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                HomeViewModel(
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
