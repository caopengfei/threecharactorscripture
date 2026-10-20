package com.rakor.sanzijing.data.audio

import android.content.res.AssetManager
import android.media.MediaPlayer
import java.io.IOException

/**
 * 音频播放抽象。assetPath 形如 "audio/u001.mp3"。
 *
 * onComplete/onError 可能在 MediaPlayer 事件线程或调用线程触发（非保证主线程），
 * 调用方需自行切换线程或使用线程安全的回调处理。
 */
interface AudioPlayer {
    fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit)
    /** 暂停当前播放；返回 false 表示此刻无可暂停的播放（未开始或刚刚播完）。 */
    fun pause(): Boolean
    /** 从暂停处继续；返回 false 表示无法继续（播放器已释放/已播完），调用方应从头重读。 */
    fun resume(): Boolean
    fun stop()
    fun release()
}

class MediaPlayerAudioPlayer(private val assets: AssetManager) : AudioPlayer {
    private var player: MediaPlayer? = null
    private var paused = false

    override fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        stop()
        try {
            assets.openFd(assetPath).use { afd ->   // 文件缺失/被压缩时抛 IOException
                val mp = MediaPlayer()
                player = mp
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mp.setOnCompletionListener { onComplete() }
                mp.setOnErrorListener { _, what, extra ->
                    onError(IOException("playback failed: $assetPath (what=$what extra=$extra)"))
                    true
                }
                mp.prepare()   // 短音频用同步 prepare 即可
                mp.start()
            }
        } catch (e: Exception) {
            player?.release()
            player = null
            onError(e)
        }
    }

    /** 播完（PlaybackCompleted）或已释放时 pause 非法——捕获后返回 false，交给播完回调处理。 */
    override fun pause(): Boolean {
        val mp = player ?: return false
        if (paused) return true
        return try {
            mp.pause()
            paused = true
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    override fun resume(): Boolean {
        val mp = player ?: return false
        if (!paused) return false
        return try {
            mp.start()
            paused = false
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    override fun stop() {
        player?.release()   // release 在任意状态合法
        player = null
        paused = false
    }

    override fun release() { stop() }
}
