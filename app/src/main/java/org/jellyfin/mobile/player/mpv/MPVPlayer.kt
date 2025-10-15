package org.jellyfin.mobile.player.mpv

import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.Util
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.jdtech.mpv.MPVLib
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_DOUBLE
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_FLAG
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_INT64
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_NONE
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_STRING

/**
 * @author dr
 */
class MPVPlayer(applicationLooper: Looper, context: Context) : SimpleBasePlayer(applicationLooper) {

    init {
        MPVLib.create(context)
        preInitOptions()
        MPVLib.init()
        postInitOptions()
        observeProperties()
    }
    fun preInitOptions(){
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("vo", "gpu") // output  gpu_next  gpu
        MPVLib.setOptionString("hwdec", "no")
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")

        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
        MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
        //
//        val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//        screenshotDir.mkdirs()
//        MPVLib.setOptionString("screenshot-directory", screenshotDir.path)
        // workaround for <https://github.com/mpv-player/mpv/issues/14651>
        MPVLib.setOptionString("vd-lavc-film-grain", "cpu")
        // Disable youtube-dl/yt-dlp integration (not needed for Jellyfin streams)
        MPVLib.setOptionString("ytdl", "no")
    }
    fun postInitOptions() {
        // we need to call write-watch-later manually
        MPVLib.setOptionString("save-position-on-quit", "no")
        // could mess up VO init before surfaceCreated() is called
        MPVLib.setOptionString("force-window", "no")
        // need to idle at least once for playFile() logic to work
        MPVLib.setOptionString("idle", "once")
    }

    override fun getState(): State {
        val builder = State.Builder()

        // 1. 获取 mpv 状态
//        val paused = MPVLib.getPropertyBoolean("pause")
//        val duration = MPVLib.getPropertyDouble("duration")
//        val position = MPVLib.getPropertyDouble("time-pos/full")
//        val buffering = MPVLib.getPropertyBoolean("paused-for-cache")

        // 2. 确定播放状态
//        var playerState: Int
//        playerState = if (buffering) {
//            STATE_BUFFERING
//        } else if (position != null && position >= duration) {
//            STATE_ENDED
//        } else {
//            STATE_READY
//        }

        val permanentAvailableCommands =
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


        builder
//            .setPlaybackState(playerState)
            .setAvailableCommands(permanentAvailableCommands)
        return  builder.build()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        val mediaItem = mediaItems[0]
        val localConfiguration = mediaItem.localConfiguration ?: return Futures.immediateFuture(null)
        val uri = localConfiguration.uri
        MPVLib.command(arrayOf("loadfile", uri.toString()))

        return Futures.immediateFuture(null)
        //        return createMPVFuture(MPVLib.MPV_EVENT_FILE_LOADED,
//            arrayOf("loadfile", uri.toString()))
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        MPVLib.setPropertyBoolean("pause", !playWhenReady)
        return Futures.immediateFuture(null)
//        return createMPVFuture("pause", !playWhenReady)

    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        MPVLib.setPropertyInt("time-pos/full", positionMs.toInt())
        return Futures.immediateFuture(null)
//        return createMPVFuture("time-pos/full", positionMs)
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
        //todo
        return Futures.immediateFuture(null)
    }



    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        val surfaceView = videoOutput as SurfaceView?
        val surfaceHolder = surfaceView?.holder ?: return super.handleSetVideoOutput(videoOutput)
        val surface = surfaceHolder.surface
        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Surface 创建完成，可以安全使用
                MPVLib.attachSurface(surface)
                // This forces mpv to render subs/osd/whatever into our surface even if it would ordinarily not
                MPVLib.setOptionString("force-window", "yes")
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                MPVLib.setPropertyString("android-surface-size", "${width}x$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Surface 即将销毁，必须停止使用
//                Log.w(TAG, "detaching surface")
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setPropertyString("force-window", "no")
                // Note that before calling detachSurface() we need to be sure that libmpv
                // is done using the surface.
                // FIXME: There could be a race condition here, because I don't think
                // setting a property will wait for VO deinit.
                MPVLib.command(arrayOf("stop"))
                MPVLib.detachSurface()
                MPVLib.destroy()
            }
        }

        surfaceView.holder.addCallback(callback)

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
            Property("time-pos", MPV_FORMAT_INT64),
            Property("duration/full", MPV_FORMAT_DOUBLE),
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
            Property("current-tracks/audio/selected")
        )
        for ((name, format) in p)
            MPVLib.observeProperty(name, format)
    }

}
