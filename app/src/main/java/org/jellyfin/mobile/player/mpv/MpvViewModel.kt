package org.jellyfin.mobile.player.mpv

import android.app.Application
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.MediaSource
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.launch
import org.jellyfin.mobile.player.deviceprofile.DeviceProfileBuilder
import org.jellyfin.mobile.player.mpv.BasePlayerViewModel
import org.jellyfin.mobile.player.source.JellyfinMediaSource
import org.jellyfin.mobile.player.source.MediaSourceResolver
import org.jellyfin.mobile.player.source.RemoteJellyfinMediaSource
import org.jellyfin.mobile.player.ui.PlayState
import org.jellyfin.mobile.utils.getVolumeLevelPercent
import org.jellyfin.mobile.utils.toMediaMetadata
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.api.operations.PlayStateApi
import org.jellyfin.sdk.api.operations.VideosApi
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.RepeatMode
import org.jellyfin.sdk.model.extensions.inWholeTicks
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.getValue
import kotlin.time.Duration

class MpvViewModel (application: Application) : AndroidViewModel(application), BasePlayerViewModel, KoinComponent {
    private val apiClient: ApiClient = get()
    private val videosApi: VideosApi = apiClient.videosApi
    private val mediaSourceResolver: MediaSourceResolver by inject()
    private val deviceProfileBuilder: DeviceProfileBuilder by inject()
    private val deviceProfile = deviceProfileBuilder.getDeviceProfile()
    private val playStateApi: PlayStateApi = apiClient.playStateApi
    private val audioManager: AudioManager by lazy { getApplication<Application>().getSystemService()!! }
    private var _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    override fun setupPlayer() {
        TODO("Not yet implemented")
    }

    override fun load(jellyfinMediaSource: JellyfinMediaSource, playWhenReady: Boolean) {
//        MPVLib.command(arrayOf("loadfile", jellyfinMediaSource.id, "replace"))


//        initialTracksSelected.set(false)

//        val startTime = jellyfinMediaSource.startTime
//        if (startTime > Duration.ZERO) player.seekTo(startTime.inWholeMilliseconds)

//        applyMediaSegments(jellyfinMediaSource)
//
//        player.playWhenReady = playWhenReady

//        mediaSession.setMetadata(jellyfinMediaSource.toMediaMetadata())

        if (jellyfinMediaSource is RemoteJellyfinMediaSource) {
            viewModelScope.launch {
                reportPlaybackStart(jellyfinMediaSource)
            }
        }
    }

    private suspend fun reportPlaybackStart(mediaSource: RemoteJellyfinMediaSource) {
        try {
            playStateApi.reportPlaybackStart(
                PlaybackStartInfo(
                    itemId = mediaSource.itemId,
                    playMethod = mediaSource.playMethod,
                    playSessionId = mediaSource.playSessionId,
                    audioStreamIndex = mediaSource.selectedAudioStream?.index,
                    subtitleStreamIndex = mediaSource.selectedSubtitleStream?.index,
                    isPaused = false,
                    isMuted = false,
                    canSeek = true,
                    positionTicks = mediaSource.startTime.inWholeTicks,
                    volumeLevel = audioManager.getVolumeLevelPercent(),
                    repeatMode = RepeatMode.REPEAT_NONE,
                    playbackOrder = PlaybackOrder.DEFAULT,
                ),
            )
        } catch (e: ApiClientException) {
            Timber.e(e, "Failed to report playback start")
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
