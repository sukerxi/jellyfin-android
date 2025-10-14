package org.jellyfin.mobile.player.mpv

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.jdtech.mpv.MPVLib
import java.util.concurrent.atomic.AtomicReference

/**
 * @author dr
 */
class MPVPlayer(applicationLooper: Looper, context: Context) : SimpleBasePlayer(applicationLooper) {
    private var currentPlayWhenReadyChangeReason: @Player.PlayWhenReadyChangeReason Int =
        PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST

    init {
        MPVLib.create(context)
        preInitOptions()
        MPVLib.init()
        postInitOptions()
    }
    fun preInitOptions(){
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("vo", "gpu_next") // output    gpu
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
        val paused = MPVLib.getPropertyDouble("pause")
        val duration = MPVLib.getPropertyDouble("duration")
        val position = MPVLib.getPropertyDouble("time-pos")
        val buffering = MPVLib.getPropertyBoolean("cache-buffering")

        // 2. 确定播放状态
        var playerState: Int
        if (duration == null) {
            playerState = STATE_IDLE;
        } else if (buffering) {
            playerState = STATE_BUFFERING;
        } else if (position != null && position >= duration) {
            playerState = STATE_ENDED;
        } else {
            playerState = STATE_READY;
        }
        builder
            .setPlaybackState(playerState)
//            .setPlaybackSuppressionReason(playbackSuppressionReason)
//            .setPlayerError(playerError)
//            .setRepeatMode(repeatMode)
//            .setShuffleModeEnabled(shuffleModeEnabled)
//            .setIsLoading(isLoading)
//            .setSeekBackIncrementMs(seekBackIncrement)
//            .setSeekForwardIncrementMs(seekForwardIncrement)
//            .setMaxSeekToPreviousPositionMs(maxSeekToPreviousPosition)
//            .setPlaybackParameters(playbackParameters)
//            .setTrackSelectionParameters(trackSelectionParameters)
//            .setAudioAttributes(audioAttributes)
//            .setVolume(volume)
//            .setVideoSize(videoSize)
//            .setCurrentCues(currentCues)
//            .setDeviceInfo(deviceInfo)
//            .setDeviceVolume(deviceVolume)
//            .setIsDeviceMuted(isDeviceMuted)
//            .setSurfaceSize(surfaceSize)
//            .setContentPositionMs(currentPosition)


//        builder
//            .setNewlyRenderedFirstFrame
//            .setTimedMetadata(timedMetadata)
        return  builder.build()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {

        return Futures.immediateFuture(null)
//        return super.handleAddMediaItems(index, mediaItems)
    }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
//        return super.handleDecreaseDeviceVolume(flags)
        return Futures.immediateFuture(null)
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
//        return super.handleIncreaseDeviceVolume(flags)
        return Futures.immediateFuture(null)
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
//        return super.handleMoveMediaItems(fromIndex, toIndex, newIndex)
        return Futures.immediateFuture(null)
    }

    override fun handlePrepare(): ListenableFuture<*> {
//        return super.handlePrepare()
        return Futures.immediateFuture(null)
    }

    override fun handleRelease(): ListenableFuture<*> {
//        return super.handleRelease()
        MPVLib.destroy()
        return Futures.immediateFuture(null)
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        return Futures.immediateFuture(null)
    }

    override fun handleReplaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<*> {
//        return super.handleReplaceMediaItems(fromIndex, toIndex, mediaItems)
        return Futures.immediateFuture(null)
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        MPVLib.setPropertyDouble("time-pos", progress!!)
        return Futures.immediateFuture(null)
    }

    override fun handleSetAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): ListenableFuture<*> {
//        return super.handleSetAudioAttributes(audioAttributes, handleAudioFocus)
        return Futures.immediateFuture(null)
    }

    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
//        return super.handleSetDeviceMuted(muted, flags)
        return Futures.immediateFuture(null)
    }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
//        return super.handleSetDeviceVolume(deviceVolume, flags)
        return Futures.immediateFuture(null)
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        return super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        //todo
        return Futures.immediateFuture(null)
    }

    override fun handleSetPlaylistMetadata(playlistMetadata: MediaMetadata): ListenableFuture<*> {
        return super.handleSetPlaylistMetadata(playlistMetadata)
    }
    private val pendingPauseFuture = AtomicReference<SettableFuture<Void>?>(null)
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        val targetPause = !playWhenReady



        // 可选：取消之前的未完成操作（防堆积）
        pendingPauseFuture.getAndSet(null)?.set(null) // 或 setException

        val future = SettableFuture.create<Void>()
        pendingPauseFuture.set(future)
        val observer = object : MPVLib.EventObserver {
            override fun eventProperty(property: String) {
                TODO("Not yet implemented")
            }

            override fun eventProperty(property: String, value: Long) {
                TODO("Not yet implemented")
            }

            override fun eventProperty(property: String, value: Double) {
                TODO("Not yet implemented")
            }

            override fun eventProperty(property: String, value: Boolean) {
                if (property.equals("pause")) {
                    if (value == targetPause) {
                        // 确认值已生效
                        MPVLib.removeObserver(this)
                        pendingPauseFuture.set(null)
                        future.set(null)
                    }
                }
            }

            override fun eventProperty(property: String, value: String) {
                TODO("Not yet implemented")
            }

            override fun event(eventId: Int) {
                TODO("Not yet implemented")
            }
        }
        MPVLib.addObserver(observer)

        // 执行设置（异步生效）
        MPVLib.setPropertyBoolean("pause", targetPause)
        return future


    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        return super.handleSetRepeatMode(repeatMode)
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        return super.handleSetShuffleModeEnabled(shuffleModeEnabled)
    }

    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
        return super.handleSetTrackSelectionParameters(trackSelectionParameters)
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
                height: Int
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
                MPVLib.detachSurface()
            }
        }

        surfaceView.holder.addCallback(callback)

        return Futures.immediateFuture(null)
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        return super.handleSetVolume(volume)
    }

    override fun handleStop(): ListenableFuture<*> {
        return super.handleStop()
    }


}
