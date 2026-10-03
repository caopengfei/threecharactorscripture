package com.rakor.sanzijing.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakor.sanzijing.SanzijingApp
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
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChapterUi(
    val id: String,
    val title: String,
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

    private val _state = MutableStateFlow(HomeUiState(totalCount = contentRepo.units.size))
    val state: StateFlow<HomeUiState> = _state

    private val collectorJob = externalScope.launch {
        combine(progressRepo.observeProgress(), progressRepo.observeDailyStats(), ::compute)
            .collect { _state.value = it }
    }

    /** 生产环境 externalScope 为应用级作用域；测试中以 TestScope 充当，随 VM 结束。 */
    override fun onCleared() {
        collectorJob.cancel()
        super.onCleared()
    }

    private fun compute(
        progress: List<UnitProgressEntity>,
        stats: List<DailyStatsEntity>,
    ): HomeUiState {
        val statusOf = progress.associate { it.unitId to it.status }
        val contId = progressRepo.continueUnitId(contentRepo.units.map { it.id }, progress)
        val contUnit = contentRepo.units.first { it.id == contId }
        val contChapter = contentRepo.chapters.first { ch -> ch.units.any { it.id == contId } }
        val todayStr = today().toString()
        return HomeUiState(
            ready = true,
            continueUnitId = contId,
            continueText = contUnit.fullText,
            continueChapterTitle = contChapter.title,
            readCount = progress.count { it.status > ProgressRepository.STATUS_NEW },
            totalCount = contentRepo.units.size,
            streakDays = progressRepo.studyStreak(stats, today()),
            minutesToday = (stats.firstOrNull { it.date == todayStr }?.secondsStudied ?: 0) / 60,
            chapters = contentRepo.chapters.map { ch ->
                ChapterUi(
                    id = ch.id,
                    title = ch.title,
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
