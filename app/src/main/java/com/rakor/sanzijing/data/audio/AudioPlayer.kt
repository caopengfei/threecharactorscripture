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
    fun stop()
    fun release()
}

class MediaPlayerAudioPlayer(private val assets: AssetManager) : AudioPlayer {
    private var player: MediaPlayer? = null

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

    override fun stop() {
        player?.release()   // release 在任意状态合法
        player = null
    }

    override fun release() { stop() }
}
