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
    var stopCount = 0
    var pauseCount = 0
    var resumeCount = 0
    var onCompletion: (() -> Unit)? = null
    var failPaths: Set<String> = emptySet()

    /** true 时模拟 MediaPlayer 的 PlaybackCompleted 态：此时 pause 非法（真实实现返回 false）。 */
    var ended = false

    /** true 时模拟播放器已释放（如退后台 stop 过）：此时 resume 返回 false。 */
    var released = false
    private var paused = false

    override fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        if (assetPath in failPaths) { onError(IOException("missing $assetPath")); return }
        played.add(assetPath)
        onCompletion = onComplete
        paused = false
        ended = false
        released = false
    }

    override fun pause(): Boolean {
        if (released || ended) return false
        if (paused) return true
        pauseCount++
        paused = true
        return true
    }

    override fun resume(): Boolean {
        if (released || ended || !paused) return false
        resumeCount++
        paused = false
        return true
    }

    override fun stop() { stopCount++; paused = false; released = true }
    override fun release() { stop() }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var content: ContentRepository
    private lateinit var player: FakeAudioPlayer
    private lateinit var scope: TestScope
    private lateinit var settingsRepo: SettingsRepository
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
        settingsRepo = SettingsRepository(store)
        scope = TestScope(StandardTestDispatcher())
        vm = ReaderViewModel(
            startUnitId = "u001",
            contentRepo = content,
            progressRepo = progress,
            settingsRepo = settingsRepo,
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
        error("settle timeout: pendingWork never reached 0")
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
    fun `连读读完最后一句循环回第一句`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        repeat(191) { vm.next() }                       // 直达尾句
        assertEquals("u192", vm.state.value.unit.id)
        player.played.clear()
        player.onCompletion!!.invoke()                  // 最后一句播完
        settle()
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u192")!!.status)
        assertEquals("u001", vm.state.value.unit.id)
        assertEquals(listOf("audio/u001.mp3"), player.played)
    }

    @Test
    fun `退到后台停止朗读且连读不再自动翻句`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.onCompletion!!.invoke()                  // 第一句播完 → 连读进入第二句
        settle()
        assertEquals("u002", vm.state.value.unit.id)

        val stopsBefore = player.stopCount
        vm.onLeaveForeground()                          // 遥控器 HOME：退到后台
        assertFalse(vm.state.value.isPlaying)
        assertEquals(stopsBefore + 1, player.stopCount)

        player.played.clear()
        player.onCompletion!!.invoke()                  // 后台到达的过期播完回调
        settle()
        assertEquals("u002", vm.state.value.unit.id)    // 停在这一句，不往后翻
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `回前台续读离开时正在朗读的那句`() {
        settle()
        player.played.clear()
        vm.onLeaveForeground()
        vm.onEnterForeground()
        assertEquals(listOf("audio/u001.mp3"), player.played)
        assertTrue(vm.state.value.isPlaying)
    }

    @Test
    fun `离开时未在朗读则回前台不自动朗读`() {
        player.failPaths = setOf("audio/u001.mp3")
        vm.replay()                                     // 播失败 → isPlaying=false
        assertFalse(vm.state.value.isPlaying)
        player.played.clear()
        vm.onLeaveForeground()
        vm.onEnterForeground()
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `短按OK重读当前句，长按OK标记熟读`() = runTest {
        settle()
        player.played.clear()
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertEquals(listOf("audio/u001.mp3"), player.played)   // 短按重读

        vm.okDown(); Thread.sleep(700); vm.okUp()      // 长按
        settle()
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u001")!!.status)
        vm.okDown(); Thread.sleep(700); vm.okUp()
        settle()
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u001")!!.status)
    }

    @Test
    fun `连读开启短按OK暂停，再按OK继续`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.played.clear()

        vm.okDown(); Thread.sleep(50); vm.okUp()        // 第一下：暂停
        assertTrue(vm.state.value.paused)
        assertFalse(vm.state.value.isPlaying)
        assertEquals(1, player.pauseCount)
        assertTrue(player.played.isEmpty())             // 暂停不重读

        vm.okDown(); Thread.sleep(50); vm.okUp()        // 第二下：继续
        assertFalse(vm.state.value.paused)
        assertTrue(vm.state.value.isPlaying)
        assertEquals(1, player.resumeCount)
        assertTrue(player.played.isEmpty())             // 从暂停处继续，不从头
    }

    @Test
    fun `连读开启长按OK只暂停不标记熟读`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(700); vm.okUp()       // 长按
        settle()
        assertTrue(vm.state.value.paused)               // 连读下长按也只暂停
        val p = db.unitProgressDao().get("u001")
        assertTrue(p == null || p.status != ProgressRepository.STATUS_MASTERED)
    }

    @Test
    fun `连读关闭短按OK仍是重读`() = runTest {
        settle()
        assertFalse(vm.state.value.settings.continuousPlay)
        player.played.clear()
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertEquals(listOf("audio/u001.mp3"), player.played)
        assertFalse(vm.state.value.paused)
    }

    @Test
    fun `连读开启暂停后到达的播完回调不推进连读链`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 暂停
        assertTrue(vm.state.value.paused)
        player.onCompletion!!.invoke()                  // 交错的播完回调
        settle()
        assertEquals("u001", vm.state.value.unit.id)    // 停在暂停那一句
        assertTrue(vm.state.value.paused)
    }

    @Test
    fun `连读开启且音频刚播完时按OK不误置暂停态`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.ended = true                             // PlaybackCompleted 态：pause 非法
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertFalse(vm.state.value.paused)
        assertEquals(0, player.pauseCount)
    }

    @Test
    fun `连读开启暂停态播放器已释放时按OK从头重读`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 暂停
        assertTrue(vm.state.value.paused)
        player.released = true                          // 播放器被释放（如退过后台）
        player.played.clear()

        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertEquals(listOf("audio/u001.mp3"), player.played)   // resume 失败 → 从头重读
        assertTrue(vm.state.value.isPlaying)
        assertFalse(vm.state.value.paused)
    }

    @Test
    fun `连读开启暂停后退后台，回前台不自动朗读且暂停态清除`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 暂停
        assertTrue(vm.state.value.paused)

        vm.onLeaveForeground()
        assertFalse(vm.state.value.paused)              // 播放器已释放，不再显示暂停
        player.played.clear()
        vm.onEnterForeground()
        assertTrue(player.played.isEmpty())             // 自己按停的，回前台不自动朗读
    }

    @Test
    fun `连读暂停后关闭连读会结束暂停会话`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 暂停
        assertTrue(vm.state.value.paused)

        val stopsBefore = player.stopCount
        vm.setContinuous(false)                         // 菜单里关掉连读
        assertFalse(vm.state.value.paused)              // 暂停随连读关闭一起结束
        assertEquals(stopsBefore + 1, player.stopCount) // 暂停中的播放器被释放

        player.played.clear()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 连读关：OK 又变回重读
        assertEquals(listOf("audio/u001.mp3"), player.played)
    }

    @Test
    fun `连读开启尾部窗口按OK重读本句且不静默推进`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.played.clear()

        player.onCompletion!!.invoke()                  // 音频播完，但进度写入尚未跑完（尾部窗口）
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 窗口内按 OK

        settle()
        assertEquals("u001", vm.state.value.unit.id)            // 不静默推进到下一句
        assertEquals(listOf("audio/u001.mp3"), player.played)   // 本句重读
        assertTrue(vm.state.value.isPlaying)
        assertFalse(vm.state.value.paused)
    }

    @Test
    fun `连读暂停后翻句会结束暂停会话并释放播放器`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        vm.okDown(); Thread.sleep(50); vm.okUp()        // 暂停
        assertTrue(vm.state.value.paused)

        settingsRepo.setAutoPlay(false)                 // 关自动朗读：翻句不会重新播放
        settle()
        assertFalse(vm.state.value.settings.autoPlay)
        player.played.clear()
        val stopsBefore = player.stopCount
        vm.next()

        assertFalse(vm.state.value.paused)              // 换句不再停在暂停态
        assertEquals(stopsBefore + 1, player.stopCount) // 暂停中的播放器被显式释放
        assertTrue(player.played.isEmpty())             // 未自动重新播放
    }

    @Test
    fun `连读开启快速连按OK只切换暂停与继续`() = runTest {
        settle()
        vm.setContinuous(true)
        settle()
        player.played.clear()

        repeat(4) { vm.okDown(); Thread.sleep(30); vm.okUp() }   // 暂停→继续→暂停→继续
        assertFalse(vm.state.value.paused)
        assertTrue(vm.state.value.isPlaying)
        assertEquals(2, player.pauseCount)
        assertEquals(2, player.resumeCount)
        assertTrue(player.played.isEmpty())             // 全程不重读
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
        assertEquals(3L, all.sumOf { it.secondsStudied })
    }

    @Test
    fun `toggleMasteredNow 直接标记熟读`() = kotlinx.coroutines.test.runTest {
        vm.toggleMasteredNow()
        settle()
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u001")!!.status)
    }

    @Test
    fun `jumpToChapter 直达指定章`() = kotlinx.coroutines.test.runTest {
        vm.jumpToChapter(4)
        assertEquals("u148", vm.state.value.unit.id)   // 第五章开头
        assertEquals("勤学典故", vm.state.value.chapterTitle)
        vm.jumpToChapter(-1)                            // 越界钳制到第一章
        assertEquals("u001", vm.state.value.unit.id)
    }
}
