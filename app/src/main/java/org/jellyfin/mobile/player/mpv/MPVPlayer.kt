package org.jellyfin.mobile.player.mpv

import android.content.Context
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.BasePlayer
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Size
import com.google.common.collect.ImmutableList
import dev.jdtech.mpv.MPVLib
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author dr
 */
class MPVPlayer(context: Context) : BasePlayer(), Player {


    // --- 播放器状态 ---
    var playWhenReady: Boolean = false
        private set
    var playbackState: Int = STATE_IDLE
        private set
    var playbackSuppressionReason: Int = PLAYBACK_SUPPRESSION_REASON_NONE
        private set
    var isPlaying: Boolean = false
        private set
    private var mediaItems: ImmutableList<MediaItem> = ImmutableList.of()
    var currentMediaItemIndex: Int = C.INDEX_UNSET
        private set
    private var timeline: Timeline = Timeline.EMPTY
    private val listeners = CopyOnWriteArrayList<Player.Listener>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // --- 视频渲染 ---
    private var surface: Surface? = null
    private var surfaceView: SurfaceView? = null
    private var textureView: TextureView? = null

    // --- 音量与速度 ---
    private var volume: Float = 1.0f
    private var speed: Float = 1.0f

    init {
        // 监听 MPV 事件
        MPVLib.addObserver(object : MPVLib.EventObserver {
            override fun event(eventID: Int) {
                handleMpvEvent(eventID)
            }

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
                TODO("Not yet implemented")
            }

            override fun eventProperty(property: String, value: String) {
                TODO("Not yet implemented")
            }
        })

        // 监听播放暂停状态
        MPVLib.observeProperty("pause", MPVLib.Format.NODE)
        // 监听播放位置
        MPVLib.observeProperty("time-pos", MPVLib.Format.NODE)
        // 监听文件加载完成
        MPVLib.observeProperty("eof-reached", MPVLib.Format.NODE)
    }

    // --- Player 接口方法 ---
    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        TODO("Not yet implemented")
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long, seekCommand: Int, isRepeatingCurrentItem: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getApplicationLooper(): Looper {
        TODO("Not yet implemented")
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        TODO("Not yet implemented")
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun getPlayerError(): PlaybackException? {
        TODO("Not yet implemented")
    }

    override fun setRepeatMode(repeatMode: Int) {
        TODO("Not yet implemented")
    }

    override fun getRepeatMode(): Int {
        TODO("Not yet implemented")
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getShuffleModeEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isLoading(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSeekBackIncrement(): Long {
        TODO("Not yet implemented")
    }

    override fun getSeekForwardIncrement(): Long {
        TODO("Not yet implemented")
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun getCurrentTracks(): Tracks {
        TODO("Not yet implemented")
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        TODO("Not yet implemented")
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        TODO("Not yet implemented")
    }

    override fun getMediaMetadata(): MediaMetadata {
        TODO("Not yet implemented")
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        TODO("Not yet implemented")
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        TODO("Not yet implemented")
    }

    override fun getCurrentPeriodIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun getTotalBufferedDuration(): Long {
        TODO("Not yet implemented")
    }

    override fun isPlayingAd(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCurrentAdGroupIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        TODO("Not yet implemented")
    }

    override fun getContentPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun getContentBufferedPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun getAudioAttributes(): AudioAttributes {
        TODO("Not yet implemented")
    }

    override fun clearVideoSurface() {
        TODO("Not yet implemented")
    }

    override fun clearVideoSurface(surface: Surface?) {
        TODO("Not yet implemented")
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        TODO("Not yet implemented")
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        TODO("Not yet implemented")
    }

    override fun getVideoSize(): VideoSize {
        TODO("Not yet implemented")
    }

    override fun getSurfaceSize(): Size {
        TODO("Not yet implemented")
    }

    override fun getCurrentCues(): CueGroup {
        TODO("Not yet implemented")
    }

    override fun getDeviceInfo(): DeviceInfo {
        TODO("Not yet implemented")
    }

    override fun getDeviceVolume(): Int {
        TODO("Not yet implemented")
    }

    override fun isDeviceMuted(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDeviceVolume(volume: Int) {
        TODO("Not yet implemented")
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        TODO("Not yet implemented")
    }

    override fun increaseDeviceVolume() {
        TODO("Not yet implemented")
    }

    override fun increaseDeviceVolume(flags: Int) {
        TODO("Not yet implemented")
    }

    override fun decreaseDeviceVolume() {
        TODO("Not yet implemented")
    }

    override fun decreaseDeviceVolume(flags: Int) {
        TODO("Not yet implemented")
    }

    override fun setDeviceMuted(muted: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        TODO("Not yet implemented")
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        this.mediaItems = ImmutableList.copyOf(mediaItems)
        if (mediaItems.isNotEmpty()) {
            currentMediaItemIndex = if (resetPosition) 0 else maxOf(0, minOf(currentMediaItemIndex, mediaItems.size - 1))
            timeline = SingleTimeline(getDuration()) // 简化，实际应为多片段Timeline
        } else {
            currentMediaItemIndex = C.INDEX_UNSET
            timeline = Timeline.EMPTY
        }
        // 通知监听器
        for (listener in listeners) {
            listener.onMediaItemsChanged(this.mediaItems, NO_RESET_POSITION)
        }
        if (currentMediaItemIndex != C.INDEX_UNSET) {
            loadCurrentMediaItem()
        }
    }



    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        setMediaItems(mediaItems, false)
        if (startIndex >= 0 && startIndex < mediaItems.size) {
            currentMediaItemIndex = startIndex
            if (startPositionMs != C.TIME_UNSET) {
                MPVLib.setPropertyDouble("time-pos", startPositionMs.toDouble() / 1000.0) // MPV 使用秒
            }
        }
        loadCurrentMediaItem()
    }

    private fun loadCurrentMediaItem() {
        if (currentMediaItemIndex != C.INDEX_UNSET && currentMediaItemIndex < mediaItems.size) {
            val mediaItem = mediaItems[currentMediaItemIndex]
            val uri = mediaItem.localConfiguration?.uri?.toString()
            uri?.let { MPVLib.command(arrayOf("loadfile", it)) }
        }
    }

    override fun getCurrentTimeline(): Timeline = timeline

    override fun getCurrentMediaItemIndex(): Int = currentMediaItemIndex



    override fun getCurrentPosition(): Long {
        val pos = MPVLib.getPropertyDouble("time-pos")
        return if (pos != null) (pos * 1000).toLong() else 0 // 秒转毫秒
    }

    override fun getBufferedPosition(): Long = getCurrentPosition() // MPV 通常不直接暴露缓冲进度

    override fun getDuration(): Long {
        if (currentMediaItemIndex != C.INDEX_UNSET && currentMediaItemIndex < mediaItems.size) {
            val dur = MPVLib.getPropertyDouble("duration")
            return if (dur != null) (dur * 1000).toLong() else C.TIME_UNSET // 秒转毫秒
        }
        return C.TIME_UNSET
    }


    private fun playbackStateIsBufferingOrEnded(state: Int): Boolean {
        return state == STATE_BUFFERING || state == STATE_ENDED
    }

    override fun getPlaybackState(): Int = playbackState

    override fun getPlaybackSuppressionReason(): Int = playbackSuppressionReason

    override fun getPlayWhenReady(): Boolean = playWhenReady

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (this.playWhenReady != playWhenReady) {
            this.playWhenReady = playWhenReady
            if (playWhenReady) {
                MPVLib.setPropertyBoolean("pause", false)
            } else {
                MPVLib.setPropertyBoolean("pause", true)
            }
            // 通知监听器
            for (listener in listeners) {
                listener.onPlayWhenReadyChanged(playWhenReady, PLAYBACK_SUPPRESSION_REASON_NONE)
                listener.onIsPlayingChanged(isPlaying())
            }
        }
    }

    override fun prepare() {
        // 在 Media3 中，prepare 通常意味着加载媒体，这里加载当前媒体项
        if (currentMediaItemIndex != C.INDEX_UNSET) {
            loadCurrentMediaItem()
        }
    }


    override fun stop() {
        MPVLib.command(arrayOf("stop"))
        playbackState = STATE_IDLE
        for (listener in listeners) {
            listener.onPlaybackStateChanged(STATE_IDLE)
        }
    }

    override fun release() {
        stop()
        MPVLib.destroy()
        surface?.let { it.release() }
        surface = null
    }

    // --- 音量与速度控制 ---
    override fun setVolume(volume: Float) {
        this.volume = volume
        MPVLib.setPropertyDouble("volume", (volume * 100).toDouble()) // MPV 音量范围 0-100
    }

    override fun getVolume(): Float {
        val vol = MPVLib.getPropertyDouble("volume")
        return if (vol != null) (vol / 100.0).toFloat() else 1.0f
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        this.speed = playbackParameters.speed
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun getPlaybackParameters(): PlaybackParameters = PlaybackParameters(speed)

    // --- 视频输出设置 ---
    override fun setVideoSurface(surface: Surface?) {
        this.surface?.let { oldSurface ->
            MPVLib.detachSurface()
            oldSurface.release()
        }
        this.surface = surface
        surface?.let { MPVLib.attachSurface(it) }
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        this.surfaceView = surfaceView
        setVideoSurface(surfaceView?.holder?.surface)
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        this.textureView = textureView
        // TextureView 需要特殊处理，这里简化处理为 Surface
        // 实际需要使用 TextureView 的 SurfaceTexture
        val surface = textureView?.surfaceTexture?.let { Surface(it) }
        setVideoSurface(surface)
    }

    // --- 监听器管理 ---
    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
    }

    // --- Commands (简化实现) ---
    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_SEEK_TO_DEFAULT_POSITION,
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SET_VOLUME, COMMAND_SET_SPEED_AND_PITCH
            )
            .build()
    }


    // --- 处理 MPV 事件 ---
    private fun handleMpvEvent(eventID: Int) {
        when (eventID) {
            MPVLib.Event.IDLE -> {
                mainHandler.post {
                    playbackState = STATE_IDLE
                    isPlaying = false
                    for (listener in listeners) {
                        listener.onPlaybackStateChanged(STATE_IDLE)
                        listener.onIsPlayingChanged(false)
                    }
                }
            }
            MPVLib.Event.START_FILE -> {
                mainHandler.post {
                    playbackState = STATE_BUFFERING
                    for (listener in listeners) {
                        listener.onPlaybackStateChanged(STATE_BUFFERING)
                    }
                }
            }
            MPVLib.Event.FILE_LOADED -> {
                mainHandler.post {
                    playbackState = STATE_READY
                    for (listener in listeners) {
                        listener.onPlaybackStateChanged(STATE_READY)
                    }
                }
            }
            MPVLib.Event.END_FILE -> {
                mainHandler.post {
                    playbackState = STATE_ENDED
                    isPlaying = false
                    for (listener in listeners) {
                        listener.onPlaybackStateChanged(STATE_ENDED)
                        listener.onIsPlayingChanged(false)
                    }
                }
            }
        }
    }

    // --- 简化 Timeline 实现 ---
    private class SingleTimeline(private val duration: Long) : Timeline() {
        override fun getWindowCount(): Int = 1

        override fun getWindow(
            windowIndex: Int,
            destination: Window,
            defaultPositionProjectionUs: Long
        ): Window {
            Assertions.checkIndex(windowIndex, 0, 1)
            destination.set(
                0, null, null, duration, 0, 0, 0, C.TIME_UNSET, C.TIME_UNSET,
                false, false, null
            )
            return destination
        }

        override fun getPeriodCount(): Int = 1

        override fun getPeriod(periodIndex: Int, destination: Period, setIds: Boolean): Period {
            Assertions.checkIndex(periodIndex, 0, 1)
            destination.set(0, 0, 0, duration, 0)
            return destination
        }

        override fun getNextWindowIndex(
            windowIndex: Int,
            repeatMode: Int,
            shuffleModeEnabled: Boolean
        ): Int {
            return if (windowIndex < getWindowCount() - 1) windowIndex + 1 else C.INDEX_UNSET
        }

        override fun getPreviousWindowIndex(
            windowIndex: Int,
            repeatMode: Int,
            shuffleModeEnabled: Boolean
        ): Int {
            return if (windowIndex > 0) windowIndex - 1 else C.INDEX_UNSET
        }

        override fun getDefaultPositionUs(windowIndex: Int): Int = 0
    }

    companion object {
        private const val NO_RESET_POSITION = -1
    }
}
