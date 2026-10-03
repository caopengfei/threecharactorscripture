package com.rakor.sanzijing.data.audio

import android.content.res.AssetManager
import android.media.MediaPlayer

/** 音频播放抽象。assetPath 形如 "audio/u001.mp3"。 */
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
            val afd = assets.openFd(assetPath)   // 文件缺失/被压缩时抛 IOException
            val mp = MediaPlayer()
            player = mp
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { onComplete() }
            mp.setOnErrorListener { _, _, _ ->
                onError(java.io.IOException("playback failed: $assetPath"))
                true
            }
            mp.prepare()   // 短音频用同步 prepare 即可
            mp.start()
        } catch (e: Exception) {
            player?.release()
            player = null
            onError(e)
        }
    }

    override fun stop() {
        player?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        player = null
    }

    override fun release() { stop() }
}
