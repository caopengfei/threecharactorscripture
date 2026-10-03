package com.rakor.sanzijing.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.audio.AudioPlayer
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FakeAudioPlayer : AudioPlayer {
    val played = mutableListOf<String>()
    var onCompletion: (() -> Unit)? = null
    var failPaths: Set<String> = emptySet()
    var stopped = 0
    override fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        if (assetPath in failPaths) { onError(IOException("missing $assetPath")); return }
        played.add(assetPath)
        onCompletion = onComplete
    }
    override fun stop() { stopped++ }
    override fun release() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var content: ContentRepository
    private lateinit var player: FakeAudioPlayer
    private lateinit var scope: TestScope
    private lateinit var vm: ReaderViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        content = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }
        player = FakeAudioPlayer()
        var tick = 1_000_000L
        val progress = ProgressRepository(db, clock = { ++tick })
        val dir = File(System.getProperty("java.io.tmpdir"), "szj-${System.nanoTime()}").apply { mkdirs() }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { File(dir, "settings.preferences_pb") }
        val settings = SettingsRepository(store)
        scope = TestScope(StandardTestDispatcher())
        vm = ReaderViewModel(
            startUnitId = "u001",
            contentRepo = content,
            progressRepo = progress,
            settingsRepo = settings,
            audioPlayer = player,
            externalScope = scope,
        )
        awaitReady()
    }

    @After
    fun tearDown() { db.close() }

    /** init 的加载在 IO 线程（Room/DataStore）+ 测试调度器上混合进行，轮询直到 ready。 */
    private fun awaitReady() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.ready) return
            Thread.sleep(20)
        }
        error("ViewModel not ready")
    }

    private fun settle() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.pendingWork == 0) return
            Thread.sleep(20)
        }
    }

    @Test
    fun `初始进入指定单元并自动朗读`() {
        assertEquals("u001", vm.state.value.unit.id)
        assertEquals(listOf("audio/u001.mp3"), player.played)
        assertEquals("教育之要", vm.state.value.chapterTitle)
        assertEquals(1, vm.state.value.chapterPos)
        assertEquals(14, vm.state.value.chapterSize)
        assertEquals(192, vm.state.value.totalCount)
    }

    @Test
    fun `前后翻句跨章连续，首尾不越界`() {
        vm.previous()
        assertEquals("u001", vm.state.value.unit.id)   // 首句 ◀ 不动
        repeat(15) { vm.next() }                        // 跨入第二章
        assertEquals("u016", vm.state.value.unit.id)
        assertEquals("蒙学伦理", vm.state.value.chapterTitle)
        repeat(300) { vm.next() }
        assertEquals("u192", vm.state.value.unit.id)   // 尾句 ▶ 不动
        vm.next()
        assertEquals("u192", vm.state.value.unit.id)
    }

    @Test
    fun `上下键跳章`() {
        vm.jumpChapter(1)
        assertEquals("u015", vm.state.value.unit.id)   // 第二章开头
        vm.jumpChapter(-1)
        assertEquals("u001", vm.state.value.unit.id)
        vm.jumpChapter(-1)
        assertEquals("u001", vm.state.value.unit.id)   // 已在第一章，不动
    }

    @Test
    fun `播放完成记已读，连读开启自动下一句`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.onCompletion!!.invoke()
        settle()
        val p = db.unitProgressDao().get("u001")!!
        assertEquals(ProgressRepository.STATUS_READ, p.status)
        assertEquals(1, p.readCount)
        assertEquals("u002", vm.state.value.unit.id)
        assertEquals(listOf("audio/u001.mp3", "audio/u002.mp3"), player.played)
    }

    @Test
    fun `短按OK开关释义面板，长按OK标记熟读`() = runTest {
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertTrue(vm.state.value.showAnnotation)
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertFalse(vm.state.value.showAnnotation)

        vm.okDown(); Thread.sleep(700); vm.okUp()      // 长按
        assertFalse(vm.state.value.showAnnotation)
        settle()
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u001")!!.status)
        vm.okDown(); Thread.sleep(700); vm.okUp()
        settle()
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u001")!!.status)
    }

    @Test
    fun `菜单键开关操作面板`() {
        vm.toggleActionPanel()
        assertTrue(vm.state.value.showActionPanel)
        vm.toggleActionPanel()
        assertFalse(vm.state.value.showActionPanel)
    }

    @Test
    fun `重读当前句`() {
        player.played.clear()
        vm.replay()
        assertEquals("audio/u001.mp3", player.played.single())
    }

    @Test
    fun `音频缺失时提示且不崩溃`() {
        player.failPaths = setOf("audio/u001.mp3")
        vm.replay()
        assertTrue(vm.state.value.toast!!.contains("音频缺失"))
        assertFalse(vm.state.value.isPlaying)
        vm.consumeToast()
        assertEquals(null, vm.state.value.toast)
    }

    @Test
    fun `学习时长按秒累计`() = runTest {
        repeat(3) { vm.tickSecond() }
        settle()
        val all = db.dailyStatsDao().observeAllBlocking()
        assertTrue(all.sumOf { it.secondsStudied } >= 3)
    }
}
