package com.rakor.sanzijing.ui.report

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

data class ChapterReport(val title: String, val read: Int, val total: Int, val mastered: Int)

data class ReportUiState(
    val ready: Boolean = false,
    val totalRead: Int = 0,
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val streakDays: Int = 0,
    val totalMinutes: Long = 0,
    val minutesToday: Long = 0,
    val chapters: List<ChapterReport> = emptyList(),
)

class ReportViewModel(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    externalScope: CoroutineScope,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState(totalCount = contentRepo.units.size))
    val state: StateFlow<ReportUiState> = _state

    // 与 HomeViewModel 约定一致：collector 随 VM 取消；externalScope 为 VM 级作用域
    private val collectorJob = externalScope.launch {
        combine(progressRepo.observeProgress(), progressRepo.observeDailyStats(), ::compute)
            .collect { _state.value = it }
    }

    override fun onCleared() {
        collectorJob.cancel()
        super.onCleared()
    }

    private fun compute(progress: List<UnitProgressEntity>, stats: List<DailyStatsEntity>): ReportUiState {
        val statusOf = progress.associate { it.unitId to it.status }
        val todayStr = today().toString()
        return ReportUiState(
            ready = true,
            totalRead = progress.count { it.status > ProgressRepository.STATUS_NEW },
            totalCount = contentRepo.units.size,
            masteredCount = progress.count { it.status == ProgressRepository.STATUS_MASTERED },
            streakDays = progressRepo.studyStreak(stats, today()),
            totalMinutes = stats.sumOf { it.secondsStudied } / 60,
            minutesToday = ((stats.firstOrNull { it.date == todayStr }?.secondsStudied ?: 0) + 59) / 60,
            chapters = contentRepo.chapters.map { ch ->
                ChapterReport(
                    title = ch.title,
                    read = ch.units.count { (statusOf[it.id] ?: 0) > ProgressRepository.STATUS_NEW },
                    total = ch.units.size,
                    mastered = ch.units.count { statusOf[it.id] == ProgressRepository.STATUS_MASTERED },
                )
            },
        )
    }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                ReportViewModel(
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
