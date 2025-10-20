package org.jellyfin.mobile.player.mpv

import android.content.Context
import android.os.Build
import android.os.Handler
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
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.jdtech.mpv.MPVLib
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_DOUBLE
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_FLAG
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_INT64
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_NONE
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_STRING
import java.util.UUID

/**
 * @author dr
 */
class MPVPlayer(applicationLooper: Looper) : SimpleBasePlayer(applicationLooper) {
    private var initFlag: Boolean = false
    private var mpvEvent: Int = MPVLib.MPV_EVENT_NONE
    private val permanentAvailableCommands =
        Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_PREPARE,
                COMMAND_STOP,
                COMMAND_SET_SPEED_AND_PITCH,
//                    COMMAND_SET_SHUFFLE_MODE,
//                    COMMAND_SET_REPEAT_MODE,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_TIMELINE,
                COMMAND_GET_METADATA,
                COMMAND_SET_PLAYLIST_METADATA,
                COMMAND_SET_MEDIA_ITEM,
//                    COMMAND_CHANGE_MEDIA_ITEMS,
                COMMAND_GET_TRACKS,
                COMMAND_GET_AUDIO_ATTRIBUTES,
                COMMAND_SET_AUDIO_ATTRIBUTES,
                COMMAND_GET_VOLUME,
//                    COMMAND_SET_VOLUME,
                COMMAND_SET_VIDEO_SURFACE,
                COMMAND_GET_TEXT,
                COMMAND_RELEASE,
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                COMMAND_SET_TRACK_SELECTION_PARAMETERS,
            )
//                .addIf(
//                    COMMAND_SET_TRACK_SELECTION_PARAMETERS, trackSelector.isSetParametersSupported(),
//                )
//                .addIf(COMMAND_GET_DEVICE_VOLUME, builder.deviceVolumeControlEnabled)
//                .addIf(COMMAND_SET_DEVICE_VOLUME, builder.deviceVolumeControlEnabled)
//                .addIf(COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS, builder.deviceVolumeControlEnabled)
//                .addIf(COMMAND_ADJUST_DEVICE_VOLUME, builder.deviceVolumeControlEnabled)
//                .addIf(COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS, builder.deviceVolumeControlEnabled)
            .build()

    private val eventsNeedListen=arrayOf(
        MPVLib.MPV_EVENT_START_FILE,
        MPVLib.MPV_EVENT_FILE_LOADED,
        MPVLib.MPV_EVENT_END_FILE,
    )
    private fun initMpv(context: Context){
        val mainHandler = Handler(Looper.getMainLooper())
        MPVLib.create(context)
        preInitOptions()
        MPVLib.init()
        postInitOptions()
        observeProperties()

        val observer = object : MPVLib.EventObserver {
            override fun eventProperty(property: String) {}
            override fun eventProperty(property: String, value: Long) {}
            override fun eventProperty(property: String, value: Double) {}
            override fun eventProperty(property: String, value: Boolean) {
                if(property=="paused-for-cache"){
                    mainHandler.post {
                        invalidateState()
                    }
                }
            }
            override fun eventProperty(property: String, value: String) {}
            override fun event(eventId: Int) {
                if (eventsNeedListen.contains(eventId)) {
                    mainHandler.post {
                        this@MPVPlayer.mpvEvent=eventId
                        invalidateState()
                    }
                }
            }
        }
        MPVLib.addObserver(observer)
        initFlag=true
    }


    fun preInitOptions(){
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("vo", "gpu") // output  gpu_next  gpu libmpv
//        MPVLib.setOptionString("hwdec", "mediacodec,mediacodec-copy")
        MPVLib.setOptionString("hwdec", "no")
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPVLib.setOptionString("gpu-context", "android")  //auto
        MPVLib.setOptionString("opengl-es", "yes")

        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
        MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")

        MPVLib.setOptionString("vd-lavc-film-grain", "cpu")
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString("cache-pause-initial", "yes")
    }
    fun postInitOptions() {
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("idle", "once")
    }

    override fun getState(): State {

        var pState=STATE_READY
        var pNewlyRenderedFirstFrame=false
        if (MPVLib.MPV_EVENT_START_FILE==mpvEvent){
            pState=STATE_BUFFERING
        }else if (MPVLib.MPV_EVENT_FILE_LOADED==mpvEvent){
            pState=STATE_READY
            pNewlyRenderedFirstFrame=true
        }else if (MPVLib.MPV_EVENT_END_FILE==mpvEvent){
            pState=STATE_ENDED
        }
        if (!initFlag||pState!=STATE_READY){
            return State.Builder()
                .setAvailableCommands(permanentAvailableCommands)
                .build()
        }

        val isBuffering = MPVLib.getPropertyBoolean("paused-for-cache")?:false
        if (isBuffering) {
            pState=STATE_BUFFERING
        }

        val duration = MPVLib.getPropertyInt("duration/full")?:0

        val position = MPVLib.getPropertyInt("time-pos/full")?:0

        val pause = MPVLib.getPropertyBoolean("pause")?:true


        val durationUs = Util.msToUs(duration*1000.toLong())
        val positionUs = Util.msToUs(position*1000.toLong())
        val mediaItemData = MediaItemData.Builder(UUID.randomUUID())
            .setDefaultPositionUs(positionUs)
            .setDurationUs(durationUs)
            .setIsSeekable(true)
            .build()
        val listMediaItemData = arrayListOf(mediaItemData)

        val builder = State.Builder()
            .setPlaylist(listMediaItemData)
            .setAvailableCommands(permanentAvailableCommands)
            .setPlayWhenReady(!pause,PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(pState)
            .setPlaybackSuppressionReason(PLAYBACK_SUPPRESSION_REASON_NONE)
            .setContentPositionMs {
                (MPVLib.getPropertyInt("time-pos/full")?:0)*1000.toLong()
            }
            .setPlaybackParameters(PlaybackParameters((MPVLib.getPropertyDouble("speed")?:0).toFloat()))
            .setNewlyRenderedFirstFrame(pNewlyRenderedFirstFrame)
        return builder.build()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        val mediaItem = mediaItems[0]
        val localConfiguration = mediaItem.localConfiguration ?: return Futures.immediateFuture(null)
        val uri = localConfiguration.uri
//        MPVLib.command(arrayOf("loadfile", uri.toString()))
//        val file = File(context.filesDir, "sample-20s.mp4")
//        val file = File(context.filesDir, "sample.mp4")
//        val file = File(context.filesDir, "458700_Finance_District_3840x2160.mp4")
        MPVLib.command(arrayOf("loadfile","/data/user/0/org.jellyfin.mobile.debug/files/458700_Finance_District_3840x2160.mp4"))


        return Futures.immediateFuture(null)

    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        MPVLib.setPropertyBoolean("pause", !playWhenReady)
        return Futures.immediateFuture(null)

    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val absolutePos= positionMs/1000
        MPVLib.command(arrayOf("seek", (absolutePos).toString(), "absolute"))
        return Futures.immediateFuture(null)
    }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handlePrepare(): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleRelease(): ListenableFuture<*> {
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


    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        MPVLib.setPropertyDouble("speed", playbackParameters.speed.toDouble())
        return Futures.immediateFuture(null)
    }



    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        val surfaceView = videoOutput as SurfaceView? ?: return super.handleSetVideoOutput(videoOutput)
        initMpv(surfaceView.context)
        val surfaceHolder = surfaceView.holder
        surfaceHolder.removeCallback(callback)
        surfaceHolder.addCallback(callback)

        return Futures.immediateFuture(null)
    }

//    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
//        return super.handleSetVolume(volume)
//    }

    override fun handleStop(): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }
    private fun observeProperties() {
        // This observes all properties needed by MPVView, MPVActivity or other classes
        data class Property(val name: String, val format: Int = MPV_FORMAT_NONE)
        val p = arrayOf(
            Property("time-pos/full", MPV_FORMAT_INT64),
            Property("duration/full", MPV_FORMAT_INT64),
            Property("pause", MPV_FORMAT_FLAG),
            Property("paused-for-cache", MPV_FORMAT_FLAG),
            Property("speed", MPV_FORMAT_STRING),
            Property("track-list"),
            Property("video-params/aspect", MPV_FORMAT_DOUBLE),
            Property("video-params/rotate", MPV_FORMAT_DOUBLE),
            Property("playlist-pos", MPV_FORMAT_INT64),
            Property("playlist-count", MPV_FORMAT_INT64),
            Property("current-tracks/video/image"),
            Property("media-title", MPV_FORMAT_STRING),
            Property("metadata"),
            Property("loop-playlist"),
            Property("loop-file"),
            Property("shuffle", MPV_FORMAT_FLAG),
            Property("hwdec-current"),
            Property("mute", MPV_FORMAT_FLAG),
            Property("current-tracks/audio/selected"),
        )
        for ((name, format) in p)
            MPVLib.observeProperty(name, format)
    }



    @UnstableApi
    fun setAnalyticsCollector(analyticsCollector: AnalyticsCollector?) {
        Assertions.checkNotNull<AnalyticsCollector?>(analyticsCollector)
        analyticsCollector!!.setPlayer(this, applicationLooper)
    }

    val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            MPVLib.attachSurface(holder.surface)
            // This forces mpv to render subs/osd/whatever into our surface even if it would ordinarily not
//                MPVLib.setOptionString("force-window", "yes")
        }
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            MPVLib.attachSurface(holder.surface)
            MPVLib
                .setPropertyString(
                    "android-surface-size",
                    "${width}x${height}",
                )
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            MPVLib.setPropertyString("vo", "null")
            MPVLib.setPropertyString("force-window", "no")
            MPVLib.command(arrayOf("stop"))
            MPVLib.detachSurface()
        }
    }
}
