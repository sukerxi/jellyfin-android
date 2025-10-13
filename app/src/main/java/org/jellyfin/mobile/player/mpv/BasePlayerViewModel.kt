package org.jellyfin.mobile.player.mpv

import androidx.media3.exoplayer.source.MediaSource
import org.jellyfin.mobile.player.source.JellyfinMediaSource
import org.jellyfin.mobile.player.source.RemoteJellyfinMediaSource
import org.jellyfin.mobile.player.ui.PlayState

interface BasePlayerViewModel {

//    fun getStateAndPause(): PlayState?
//    suspend fun stopTranscoding(mediaSource: RemoteJellyfinMediaSource)

    fun load(jellyfinMediaSource: JellyfinMediaSource, playWhenReady: Boolean)

    fun setupPlayer()

    fun pause()
    fun play()

    fun seekTo(pos: Long)

    fun rewind()

    fun fastForward()

    fun skipToPrevious()
    fun skipToNext()

    fun stop()

}
