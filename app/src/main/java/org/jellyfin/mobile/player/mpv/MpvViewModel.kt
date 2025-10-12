package org.jellyfin.mobile.player.mpv

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.source.MediaSource
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.launch
import org.jellyfin.mobile.player.deviceprofile.DeviceProfileBuilder
import org.jellyfin.mobile.player.mpv.BasePlayerViewModel
import org.jellyfin.mobile.player.source.JellyfinMediaSource
import org.jellyfin.mobile.player.source.MediaSourceResolver
import org.jellyfin.mobile.player.source.RemoteJellyfinMediaSource
import org.jellyfin.mobile.player.ui.PlayState
import org.jellyfin.mobile.utils.toMediaMetadata
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.api.operations.VideosApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.getValue
import kotlin.time.Duration

class MpvViewModel : ViewModel(), BasePlayerViewModel, KoinComponent {
    private val apiClient: ApiClient = get()
    private val videosApi: VideosApi = apiClient.videosApi
    private val mediaSourceResolver: MediaSourceResolver by inject()
    private val deviceProfileBuilder: DeviceProfileBuilder by inject()
    private val deviceProfile = deviceProfileBuilder.getDeviceProfile()


    private var _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    override fun setupPlayer() {
        TODO("Not yet implemented")
    }

    override fun load(jellyfinMediaSource: JellyfinMediaSource,exoMediaSource: MediaSource, playWhenReady: Boolean) {
        MPVLib.command(arrayOf("loadfile", jellyfinMediaSource.id, "replace"))

        player.setMediaSource(exoMediaSource)
        player.prepare()

        initialTracksSelected.set(false)

        val startTime = jellyfinMediaSource.startTime
        if (startTime > Duration.ZERO) player.seekTo(startTime.inWholeMilliseconds)

        applyMediaSegments(jellyfinMediaSource)

        player.playWhenReady = playWhenReady

        mediaSession.setMetadata(jellyfinMediaSource.toMediaMetadata())

        if (jellyfinMediaSource is RemoteJellyfinMediaSource) {
            viewModelScope.launch {
                player.reportPlaybackStart(jellyfinMediaSource)
            }
        }
    }



    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun seekTo(pos: Long) {
        TODO("Not yet implemented")
    }

    override fun rewind() {
        TODO("Not yet implemented")
    }

    override fun fastForward() {
        TODO("Not yet implemented")
    }

    override fun skipToPrevious() {
        TODO("Not yet implemented")
    }

    override fun skipToNext() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }
}
