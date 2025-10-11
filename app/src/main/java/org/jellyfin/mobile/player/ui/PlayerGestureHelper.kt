package org.jellyfin.mobile.player.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.media.AudioManager
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.jellyfin.mobile.R
import org.jellyfin.mobile.app.AppPreferences
import org.jellyfin.mobile.databinding.FragmentPlayerBinding
import org.jellyfin.mobile.utils.Constants
import org.jellyfin.mobile.utils.brightness
import org.jellyfin.mobile.utils.dip
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Formatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class PlayerGestureHelper(
    private val fragment: PlayerFragment,
    private val playerBinding: FragmentPlayerBinding,
    private val playerLockScreenHelper: PlayerLockScreenHelper,
    private val player: Player?,
) : KoinComponent {
    private val appPreferences: AppPreferences by inject()
    private val audioManager: AudioManager by lazy { fragment.requireActivity().getSystemService()!! }
    private val playerView: PlayerView by playerBinding::playerView
    private val gestureIndicatorOverlayLayout: LinearLayout by playerBinding::gestureOverlayLayout
    private val gestureIndicatorOverlayImage: ImageView by playerBinding::gestureOverlayImage
    private val gestureIndicatorOverlayText: TextView by playerBinding::gestureOverlayText
    private val gestureIndicatorOverlayProgress: ProgressBar by playerBinding::gestureOverlayProgress
    private var isOnPressingSpeedUp = false
    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())
    private var startPosition: Long = -1L
    private var seekToPosition: Long = -1L
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var isVerticalGesture = false
    private var isHorizontalGesture = false
    private var hasExcluded = false
    private val touchSlop = ViewConfiguration.get(playerView.context).scaledTouchSlop

    init {
        if (appPreferences.exoPlayerRememberBrightness) {
            fragment.requireActivity().window.brightness = appPreferences.exoPlayerBrightness
        }
    }

    /**
     * Tracks whether video content should fill the screen, cutting off unwanted content on the sides.
     * Useful on wide-screen phones to remove black bars from some movies.
     */
    private var isZoomEnabled = false

    /**
     * Tracks a value during a swipe gesture (between multiple onScroll calls).
     * When the gesture starts it's reset to an initial value and gets increased or decreased
     * (depending on the direction) as the gesture progresses.
     */
    private var swipeGestureValueTracker = -1f

    /**
     * Runnable that hides [playerView] controller
     */
    private val hidePlayerViewControllerAction = Runnable {
        playerView.hideController()
    }

    /**
     * Runnable that hides [gestureIndicatorOverlayLayout]
     */
    private val hideGestureIndicatorOverlayAction = Runnable {
        gestureIndicatorOverlayLayout.isVisible = false
    }

    /**
     * Handles taps when controls are locked
     */
    private val unlockDetector = GestureDetector(
        playerView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                playerLockScreenHelper.peekUnlockButton()
                return true
            }
        },
    )

    /**
     * Handles double tap to seek and brightness/volume gestures
     */
    private val gestureDetector = GestureDetector(
        playerView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val viewWidth = playerView.measuredWidth
                val viewHeight = playerView.measuredHeight
                val viewCenterX = viewWidth / 2
                val viewCenterY = viewHeight / 2
                val isFastForward = e.x.toInt() > viewCenterX

                // Show ripple effect
                playerView.foreground?.apply {
                    val left = if (isFastForward) viewCenterX else 0
                    val right = if (isFastForward) viewWidth else viewCenterX
                    setBounds(left, viewCenterY - viewCenterX / 2, right, viewCenterY + viewCenterX / 2)
                    setHotspot(e.x, e.y)
                    state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
                    playerView.postDelayed(Constants.DOUBLE_TAP_RIPPLE_DURATION_MS) {
                        state = IntArray(0)
                    }
                }

                // Fast-forward/rewind
                with(fragment) { if (isFastForward) onFastForward() else onRewind() }

                // Cancel previous runnable to not hide controller while seeking
                playerView.removeCallbacks(hidePlayerViewControllerAction)

                // Ensure controller gets hidden after seeking
                playerView.postDelayed(hidePlayerViewControllerAction, Constants.DEFAULT_CONTROLS_TIMEOUT_MS.toLong())
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                playerView.apply {
                    if (!isControllerFullyVisible) showController() else hideController()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!appPreferences.exoPlayerAllowPressSpeedUp) {
                    return
                }

                with(fragment) {
                    isOnPressingSpeedUp = true
                    onPressSpeedUp(true)
                }
            }

        },

    )

    @SuppressLint("SetTextI18n")
    private fun handleSeekGesture(deltaX: Float) {
        val player = player ?: return
        val duration = player.duration
        if (duration <= 0) return

        // 全屏宽度对应整个视频时长
        val screenWidth = playerView.measuredWidth
        if (screenWidth <= 0) return

        // 滑动比例（deltaX 正 = 向右滑 = 快进）
        val ratio = deltaX / screenWidth
        val deltaMs = (ratio * duration).toLong()
        val currentPosition = player.currentPosition
        val newPosition = (currentPosition + deltaMs).coerceIn(0L, duration)

        seekToPosition = newPosition
        gestureIndicatorOverlayProgress.max=100
        gestureIndicatorOverlayProgress.progress = (newPosition * 100 / duration).toInt()
        gestureIndicatorOverlayText.text = "${Util.getStringForTime(formatBuilder, formatter, deltaMs)} / ${Util.getStringForTime(formatBuilder, formatter, newPosition)}"
        gestureIndicatorOverlayImage.isVisible=false
        gestureIndicatorOverlayText.isVisible=true
        gestureIndicatorOverlayLayout.isVisible = true

    }



    private fun handleHorizontalGestureEnd() {
        val player = player ?: return
        val duration = player.duration
        if (duration.toInt() == 0) return
        player.seekTo(seekToPosition)
    }

    private fun handleVerticalGesture(distanceY: Float){
        val viewCenterX = playerView.measuredWidth / 2
        val distanceFull = playerView.measuredHeight * Constants.FULL_SWIPE_RANGE_SCREEN_RATIO
        val ratioChange = distanceY / distanceFull

        if (gestureStartX > viewCenterX) {
            // Right: Volume
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (swipeGestureValueTracker == -1f) swipeGestureValueTracker = currentVolume.toFloat()

            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (swipeGestureValueTracker + ratioChange * maxVolume).coerceIn(0f, maxVolume.toFloat())
            val toSet = targetVolume.toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, toSet, 0)

            gestureIndicatorOverlayImage.setImageResource(R.drawable.ic_volume_white_24dp)
            gestureIndicatorOverlayProgress.max = maxVolume
            gestureIndicatorOverlayProgress.progress = toSet
        } else {
            // Left: Brightness
            val window = fragment.requireActivity().window
            val brightnessRange = BRIGHTNESS_OVERRIDE_OFF..BRIGHTNESS_OVERRIDE_FULL

            if (swipeGestureValueTracker == -1f) {
                val brightness = window.brightness
                swipeGestureValueTracker = when (brightness) {
                    in brightnessRange -> brightness
                    else -> {
                        Settings.System.getFloat(
                            fragment.requireActivity().contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                        ) / Constants.SCREEN_BRIGHTNESS_MAX
                    }
                }
            }

            val targetVolume = (swipeGestureValueTracker + ratioChange).coerceIn(brightnessRange)
            window.brightness = targetVolume
            if (appPreferences.exoPlayerRememberBrightness) {
                appPreferences.exoPlayerBrightness = targetVolume
            }

            gestureIndicatorOverlayImage.setImageResource(R.drawable.ic_brightness_white_24dp)
            gestureIndicatorOverlayProgress.max = Constants.PERCENT_MAX
            gestureIndicatorOverlayProgress.progress = (targetVolume * Constants.PERCENT_MAX).toInt()
        }
        gestureIndicatorOverlayImage.isVisible=true
        gestureIndicatorOverlayText.isVisible=false
        gestureIndicatorOverlayLayout.isVisible = true
    }

    /**
     * Handles scale/zoom gesture
     */
    private val zoomGestureDetector = ScaleGestureDetector(
        playerView.context,
        object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = fragment.isLandscape()

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                if (abs(scaleFactor - Constants.ZOOM_SCALE_BASE) > Constants.ZOOM_SCALE_THRESHOLD) {
                    isZoomEnabled = scaleFactor > 1
                    updateZoomMode(isZoomEnabled)
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
        },
    ).apply { isQuickScaleEnabled = false }

    init {
        @Suppress("ClickableViewAccessibility")
        playerView.setOnTouchListener { _, event ->
            if (playerView.useController) {
                when (event.pointerCount) {
                    1 -> {
                        gestureDetector.onTouchEvent(event)
                        onGesture(event)
                    }
                    2 -> zoomGestureDetector.onTouchEvent(event)
                }
            } else {
                unlockDetector.onTouchEvent(event)
            }
            if (event.action == MotionEvent.ACTION_UP) {
                if (isOnPressingSpeedUp) {
                    isOnPressingSpeedUp = false
                    with(fragment) {
                        onPressSpeedUp(false)
                    }
                }
                // Hide gesture indicator after timeout, if shown
                gestureIndicatorOverlayLayout.apply {
                    if (isVisible) {
                        removeCallbacks(hideGestureIndicatorOverlayAction)
                        postDelayed(
                            hideGestureIndicatorOverlayAction,
                            Constants.DEFAULT_CENTER_OVERLAY_TIMEOUT_MS.toLong(),
                        )
                    }
                }
                swipeGestureValueTracker = -1f
            }
            true
        }
    }

    private fun onGesture(event: MotionEvent): Boolean{
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = event.x
                gestureStartY = event.y
                isVerticalGesture = false
                isHorizontalGesture = false
                hasExcluded = false
                startPosition =player?.contentPosition?:-1

                // Check exclusion zone (top/bottom)
                val exclusionSize = playerView.resources.dip(Constants.SWIPE_GESTURE_EXCLUSION_SIZE_VERTICAL)
                if (gestureStartY < exclusionSize || gestureStartY > playerView.height - exclusionSize) {
                    hasExcluded = true
                    return false
                }

                if (!appPreferences.exoPlayerAllowSwipeGestures) {
                    return false
                }

                // Initialize tracker
                swipeGestureValueTracker = -1f
            }

            MotionEvent.ACTION_MOVE -> {
                if (hasExcluded || !appPreferences.exoPlayerAllowSwipeGestures) {
                    return false
                }

                val deltaX = event.x - gestureStartX
                val deltaY = gestureStartY-event.y
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                if (distance > touchSlop) {
                    // 判断方向（只在首次移动时决定）
                    if (!isVerticalGesture && !isHorizontalGesture) {
                        if (abs(deltaY) > abs(deltaX) * 2) {
                            isVerticalGesture = true
                        } else if (abs(deltaX) > abs(deltaY) * 2) {
                            isHorizontalGesture = true
                        }
                    }
                }

                if (isVerticalGesture) {
                    handleVerticalGesture(deltaY)
                    return true
                } else if (isHorizontalGesture) {
                    handleSeekGesture(deltaX) // 可选：显示进度条等
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isHorizontalGesture && !hasExcluded) {
                    handleHorizontalGestureEnd() // 执行跳转逻辑
                }

                // Reset
                gestureIndicatorOverlayLayout.isVisible = false
                swipeGestureValueTracker = -1f
                isVerticalGesture = false
                isHorizontalGesture = false
                hasExcluded = false
                startPosition = -1L
                seekToPosition = -1L
            }
        }
        return true
    }

    fun handleConfiguration(newConfig: Configuration) {
        updateZoomMode(fragment.isLandscape(newConfig) && isZoomEnabled)
    }

    private fun updateZoomMode(enabled: Boolean) {
        playerView.resizeMode = if (enabled) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
}
