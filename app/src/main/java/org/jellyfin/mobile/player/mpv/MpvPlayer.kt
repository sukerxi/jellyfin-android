package org.jellyfin.mobile.player.mpv

import android.app.Application
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.source.MediaSource
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.jellyfin.mobile.player.source.JellyfinMediaSource

import java.util.UUID
import java.util.function.BiConsumer

/**
 * @author dr
 */
class MpvPlayer (application: Application, looper: Looper) : SimpleBasePlayer(looper) {
    private var startingFlag= false
    private var playerState: Int = STATE_IDLE
    private var mediaSource: JellyfinMediaSource? =null
    private val propertyListener= BiConsumer<String, Any> { _, _ ->
        invalidateState()
    }
    private val eventListener= BiConsumer<Int, Any> { eventId, _ ->
        if (eventId== MpvCore.MPV_EVENT_END_FILE&&startingFlag){
            return@BiConsumer
        }else if(eventId== MpvCore.MPV_EVENT_START_FILE){
            startingFlag=false
        }
        invalidateState()
    }
    init {
        MpvCore.initialize(application)
        MpvCore.subscribe(propertyListener,eventListener)
    }

    val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            MpvCore.setOptions("vo", "gpu_next,gpu")
            MpvCore.setOptions("force-window", "yes")
            MpvCore.attachSurface(holder.surface)
        }
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            MpvCore.setProperty("android-surface-size", "${width}x${height}")
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            MpvCore.detachSurface()
            MpvCore.setOptions("vo", "null")
            MpvCore.setOptions("force-window", "no")
        }
    }
    private val permanentAvailableCommands =
        Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,

                COMMAND_STOP,
                COMMAND_SET_SPEED_AND_PITCH,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_TIMELINE,
                COMMAND_GET_METADATA,
                COMMAND_SET_PLAYLIST_METADATA,
                COMMAND_SET_MEDIA_ITEM,
                COMMAND_GET_TRACKS,
                COMMAND_GET_AUDIO_ATTRIBUTES,
                COMMAND_SET_AUDIO_ATTRIBUTES,
                COMMAND_GET_VOLUME,
                COMMAND_SET_VOLUME,
                COMMAND_SET_VIDEO_SURFACE,
                COMMAND_GET_TEXT,
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                COMMAND_SET_TRACK_SELECTION_PARAMETERS,
                COMMAND_RELEASE,
                // COMMAND_SET_SHUFFLE_MODE,
                // COMMAND_SET_REPEAT_MODE,
                // COMMAND_CHANGE_MEDIA_ITEMS,
                // COMMAND_PREPARE,  mpv 可以使用start option  ，在加载后自动跳转到指定位置，无需准备这一步
            )
            .build()

    override fun getState(): State {


        val duration = MpvCore.getProperty<Int>("duration/full")?:0
        val position = MpvCore.getProperty<Int>("time-pos/full")?:0
        val pause = MpvCore.getProperty<Boolean>("pause")?:true

        val durationUs = Util.msToUs(duration*1000.toLong())
        val positionUs = Util.msToUs(position*1000.toLong())
        val mediaItemData = MediaItemData.Builder(UUID.randomUUID())
            .setDefaultPositionUs(positionUs)
            .setDurationUs(durationUs)
            .setIsSeekable(true)
            .build()
        val listMediaItemData = arrayListOf(mediaItemData)

        var localNewlyRenderedFirstFrame=false
        if (MpvCore.MPV_EVENT_START_FILE== MpvCore.currentEvent){
            playerState=STATE_BUFFERING
        }else if (MpvCore.MPV_EVENT_FILE_LOADED==MpvCore.currentEvent){
            playerState=STATE_READY
            localNewlyRenderedFirstFrame=true
        }else if (MpvCore.MPV_EVENT_SEEK==MpvCore.currentEvent){
            playerState=STATE_BUFFERING
        }else if (MpvCore.MPV_EVENT_PLAYBACK_RESTART==MpvCore.currentEvent){
            playerState=STATE_READY
        } else if (MpvCore.MPV_EVENT_END_FILE==MpvCore.currentEvent){
            playerState=STATE_ENDED
        } else if (MpvCore.MPV_EVENT_PAUSED_FOR_CACHE_START==MpvCore.currentEvent){
            playerState=STATE_BUFFERING
        }else if (MpvCore.MPV_EVENT_PAUSED_FOR_CACHE_END==MpvCore.currentEvent){
            playerState=STATE_READY
        }


        val builder = State.Builder()
            .setPlaylist(listMediaItemData)
            .setAvailableCommands(permanentAvailableCommands)
            .setPlayWhenReady(!pause,PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playerState)
            .setNewlyRenderedFirstFrame(localNewlyRenderedFirstFrame)
            .setPlaybackSuppressionReason(PLAYBACK_SUPPRESSION_REASON_NONE)
            .setContentPositionMs {
                (MpvCore.getProperty<Long>("time-pos/full")?:0)*1000
            }
            .setPlaybackParameters(PlaybackParameters((MpvCore.getProperty<Float>("speed"))?:0f))



        return builder.build()
    }

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        val surfaceView = videoOutput as SurfaceView
        val holder = surfaceView.holder
        holder.addCallback(surfaceHolderCallback)
        return Futures.immediateFuture(null)
    }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        val surfaceView = videoOutput as SurfaceView
        val holder = surfaceView.holder
        holder.removeCallback(surfaceHolderCallback)
        return Futures.immediateFuture(null)
    }




    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        MpvCore.setProperty("pause", !playWhenReady)
        return Futures.immediateFuture(null)

    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val absolutePos= positionMs/1000
        MpvCore.command(arrayOf("seek", (absolutePos).toString(), "absolute"))
        return Futures.immediateFuture(null)
    }



    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        MpvCore.setProperty("speed", playbackParameters.speed.toDouble())
        return Futures.immediateFuture(null)
    }

    override fun handleStop(): ListenableFuture<*> {
        MpvCore.command(arrayOf("stop"))
        return Futures.immediateFuture(null)
    }

    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }


    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        val mediaItem = mediaItems[0]
        val localConfiguration = mediaItem.localConfiguration ?: return Futures.immediateFuture(null)
        val uri = localConfiguration.uri
        startingFlag=true
        MpvCore.command(arrayOf("loadfile", uri.toString()))
//        val file = File(context.filesDir, "sample-20s.mp4")
//        val file = File(context.filesDir, "sample.mp4") duangxiao
//        val file = File(context.filesDir, "458700_Finance_District_3840x2160.mp4") office
//        MPVLib.command(arrayOf("loadfile","/data/user/0/org.jellyfin.mobile.debug/files/sample.mp4"))
        return Futures.immediateFuture(null)
    }


    override fun handleRelease(): ListenableFuture<*> {
        MpvCore.unsubscribe(propertyListener,eventListener)
        MpvCore.command(arrayOf("stop"))
        return Futures.immediateFuture(null)
    }

    override fun handleSetAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    fun setMediaSource(mediaSource: JellyfinMediaSource?,
                       mainMediaSource: MediaSource,
                       externalSubtitleMediaSources: Array<MediaSource>){
        // Util.
        // MpvCore.command(arrayOf("loadfile", uri.toString()))
    }

    @UnstableApi
    fun setAnalyticsCollector(analyticsCollector: AnalyticsCollector?) {
        Assertions.checkNotNull<AnalyticsCollector?>(analyticsCollector)
        analyticsCollector!!.setPlayer(this, applicationLooper)
    }

    fun setProperty(name: String, value: Any ){
        MpvCore.setProperty(name, value)
    }

}
