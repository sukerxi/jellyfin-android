package org.jellyfin.mobile.player.mpv

import android.app.Application
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface

import dev.jdtech.mpv.MPVLib
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_FLAG
import dev.jdtech.mpv.MPVLib.MPV_FORMAT_NONE
import java.util.function.BiConsumer

/**
 * @author dr
 */
class MpvCore private constructor(context: Application) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mpvLibEventObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {}
        override fun eventProperty(property: String, value: Long) {}
        override fun eventProperty(property: String, value: Double) {}
        override fun eventProperty(property: String, value: Boolean) {
            if(property=="paused-for-cache"){
                mainHandler.post {
                    currentEvent =if (value) MPV_EVENT_PAUSED_FOR_CACHE_START
                    else  MPV_EVENT_PAUSED_FOR_CACHE_END
                    for (consumer in propertyListeners) {
                        consumer.accept(property, value)
                    }
                    currentEvent= MPV_EVENT_NONE
                }
            }
        }
        override fun eventProperty(property: String, value: String) {}
        override fun event(eventId: Int) {
            if (eventsNeedListen.contains(eventId)) {
                mainHandler.post {
                    currentEvent=eventId
                    for (consumer in eventListeners) {
                        consumer.accept(eventId, "")
                    }
                    currentEvent= MPV_EVENT_NONE
                }
            }
        }
    }


    private val eventsNeedListen=arrayOf(
        MPV_EVENT_START_FILE,
        MPV_EVENT_FILE_LOADED,
        MPV_EVENT_END_FILE,
        MPV_EVENT_PLAYBACK_RESTART,
        MPV_EVENT_SEEK,
        MPV_EVENT_PAUSED_FOR_CACHE_START,
        MPV_EVENT_PAUSED_FOR_CACHE_END
    )


    companion object {
        private val propertyListeners: ArrayList<BiConsumer<String, Any>> = arrayListOf()
        private val eventListeners: ArrayList<BiConsumer<Int, Any>> = arrayListOf()
        var currentEvent: Int = MPV_EVENT_NONE
            private set
        const val MPV_EVENT_PAUSED_FOR_CACHE_START =1000
        const val MPV_EVENT_PAUSED_FOR_CACHE_END =1001
        const val MPV_EVENT_START_FILE =MPVLib.MPV_EVENT_START_FILE
        const val MPV_EVENT_FILE_LOADED =MPVLib.MPV_EVENT_FILE_LOADED
        const val MPV_EVENT_END_FILE = MPVLib.MPV_EVENT_END_FILE
        const val MPV_EVENT_PLAYBACK_RESTART = MPVLib.MPV_EVENT_PLAYBACK_RESTART
        const val MPV_EVENT_SEEK =MPVLib.MPV_EVENT_SEEK
        const val MPV_EVENT_NONE =MPVLib.MPV_EVENT_NONE

        private var INSTANCE: MpvCore? = null
        fun initialize(application: Application): MpvCore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MpvCore(application).also { INSTANCE = it }
            }
        }
        inline fun <reified T> getProperty(name: String): T? {
            return when (T::class) {
                String::class -> MPVLib.getPropertyString(name) as T?
                Int::class -> MPVLib.getPropertyInt(name) as T?
                Long::class -> MPVLib.getPropertyInt(name)?.toLong() as T?
                Float::class -> MPVLib.getPropertyDouble(name)?.toFloat() as T?
                Double::class -> MPVLib.getPropertyDouble(name) as T?
                Boolean::class -> MPVLib.getPropertyBoolean(name) as T?
                else -> throw IllegalArgumentException("Unsupported property type: ${T::class}")
            }
        }
        fun command(cmd: Array<String>) {
            MPVLib.command(cmd)
        }
        fun setOptions(name: String,value: String) {
            MPVLib.setOptionString(name, value)
        }
        fun setProperty(name: String, value: Any) {
            when (value) {
                is String -> MPVLib.setPropertyString(name, value)
                is Int -> MPVLib.setPropertyInt(name, value)
                is Double -> MPVLib.setPropertyDouble(name, value)
                is Boolean -> MPVLib.setPropertyBoolean(name, value)
                is Float -> MPVLib.setPropertyDouble(name, value.toDouble())
                is Long -> MPVLib.setPropertyInt(name, value.toInt())
                else -> throw IllegalArgumentException("Unsupported property type: ${value::class}")
            }
        }
        fun subscribe(propertyListener: BiConsumer<String,Any>,eventListener:BiConsumer<Int, Any>) {
            propertyListeners.add(propertyListener)
            eventListeners.add(eventListener)
        }
        fun unsubscribe(propertyListener: BiConsumer<String,Any>,eventListener:BiConsumer<Int, Any>) {
            propertyListeners.remove(propertyListener)
            eventListeners.remove(eventListener)
        }
        fun attachSurface(surface: Surface) {
            MPVLib.attachSurface(surface)
        }
        fun detachSurface() {
            MPVLib.detachSurface()
        }

    }



    init {
        MPVLib.create(context)
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("hwdec", "auto")
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

        MPVLib.init()

        MPVLib.setOptionString("save-position-on-quit", "no")
//        MPVLib.setOptionString("idle", "once")
        // MPVLib.setOptionString("force-window", "yes")
        observeProperties()
        MPVLib.addObserver(mpvLibEventObserver)
    }

    private fun observeProperties() {
        // This observes all properties needed by MPVView, MPVActivity or other classes
        data class Property(val name: String, val format: Int = MPV_FORMAT_NONE)
        val p = arrayOf(
            // Property("time-pos/full", MPV_FORMAT_INT64),
            // Property("duration/full", MPV_FORMAT_INT64),
            // Property("pause", MPV_FORMAT_FLAG),
            Property("paused-for-cache", MPV_FORMAT_FLAG),
            // Property("speed", MPV_FORMAT_STRING),
            // Property("track-list"),
            // Property("video-params/aspect", MPV_FORMAT_DOUBLE),
            // Property("video-params/rotate", MPV_FORMAT_DOUBLE),
            // Property("playlist-pos", MPV_FORMAT_INT64),
            // Property("playlist-count", MPV_FORMAT_INT64),
            // Property("current-tracks/video/image"),
            // Property("media-title", MPV_FORMAT_STRING),
            // Property("metadata"),
            // Property("loop-playlist"),
            // Property("loop-file"),
            // Property("shuffle", MPV_FORMAT_FLAG),
            // Property("hwdec-current"),
            // Property("mute", MPV_FORMAT_FLAG),
            // Property("current-tracks/audio/selected"),
        )
        for ((name, format) in p)
            MPVLib.observeProperty(name, format)
    }



}
