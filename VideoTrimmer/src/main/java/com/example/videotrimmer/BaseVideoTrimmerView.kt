@file:Suppress("LeakingThis")

package com.sample.videotrimmerlib

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.annotation.UiThread
import com.sample.videotrimmerlib.interfaces.OnProgressVideoListener
import com.sample.videotrimmerlib.interfaces.OnRangeSeekBarListener
import com.sample.videotrimmerlib.interfaces.VideoTrimmingListener
import com.sample.videotrimmerlib.videoutils.BackgroundExecutor
import com.sample.videotrimmerlib.videoutils.TrimVideoUtils
import com.sample.videotrimmerlib.videoutils.UiThreadExecutor
import com.sample.videotrimmerlib.views.FrameTimeLimeView
import com.sample.videotrimmerlib.views.SeekBarView
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

abstract class BaseVideoTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    protected var totalDuration: Long = 0L
    private val seekBarView: SeekBarView
    private val videoViewContainer: View
    private val timeInfoContainer: View
    private val videoView: VideoView
    private val playView: View
    private val timeLineView: FrameTimeLimeView
    private var src: Uri? = null
    private lateinit var dstFile: File
    protected var maxDurationInMillisecond: Int = 0
    protected var minDurationInMillisecond: Int = 0
    private var listeners = ArrayList<OnProgressVideoListener>()
    private var videoTrimmingListener: VideoTrimmingListener? = null
    private var duration = 0
    private var timeVideo = 0
    private var startPosition = 0
    private var endPosition = 0
    protected var originSizeFile: Long = 0
    private var resetSeekBar = true
    private val messageHandler = MessageHandler(this)

    init {
        initRootView()
        seekBarView = getSeekBarView()
        videoViewContainer = getVideoViewContainer()
        videoView = getVideoView()
        playView = getPlayView()
        timeInfoContainer = getTimeInfoContainer()
        timeLineView = getFrameTimeLineView()
        setUpListeners()
        setUpMargins()
    }

    abstract fun initRootView()

    abstract fun getFrameTimeLineView(): FrameTimeLimeView

    abstract fun getTimeInfoContainer(): View

    abstract fun getPlayView(): View

    abstract fun getVideoView(): VideoView

    abstract fun getVideoViewContainer(): View

    abstract fun getSeekBarView(): SeekBarView

    abstract fun onRangeUpdated(startTimeInMs: Int, endTimeInMs: Int)

    /**occurs during playback, to tell that you've reached a specific time in the video*/
    abstract fun onVideoPlaybackReachingTime(timeInMs: Int)

    abstract fun onGotVideoFileSize(videoFileSize: Long)

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        listeners.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                this@BaseVideoTrimmerView.updateVideoProgress(time)
            }
        })
        val gestureDetector = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            }
        )
        videoView.setOnErrorListener { _, what, extra ->
            if (videoTrimmingListener != null)
                videoTrimmingListener!!.onErrorWhileViewingVideo(what, extra)
            false
        }

        videoView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        seekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeek(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeekStop(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })
        videoView.setOnPreparedListener { this.onVideoPrepared(it) }
        videoView.setOnCompletionListener { onVideoCompleted() }
    }

    private fun setUpMargins() {
        val marge = seekBarView.thumbWidth
        val lp: MarginLayoutParams = timeLineView.layoutParams as MarginLayoutParams
        lp.setMargins(marge, lp.topMargin, marge, lp.bottomMargin)
        timeLineView.layoutParams = lp
    }

    @Suppress("unused")
    @UiThread
    fun initiateTrimming() {
        pauseVideo()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, src)
        val metadataKeyDuration =
            java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        if (timeVideo < MIN_TIME_FRAME) {
            if (metadataKeyDuration - endPosition > MIN_TIME_FRAME - timeVideo) {
                endPosition += MIN_TIME_FRAME - timeVideo
            } else if (startPosition > MIN_TIME_FRAME - timeVideo) {
                startPosition -= MIN_TIME_FRAME - timeVideo
            }
        }
        //notify that video trimming started
        if (videoTrimmingListener != null)
            videoTrimmingListener!!.onTrimStarted()
        BackgroundExecutor.execute(
            object : BackgroundExecutor.Task(null, 0L, null) {
                override fun execute() {
                    try {
                        TrimVideoUtils.startTrim(
                            context,
                            src!!,
                            dstFile,
                            null,
                            startPosition.toLong(),
                            endPosition.toLong(),
                            duration.toLong(),
                            videoTrimmingListener!!
                        )
                    } catch (e: Throwable) {
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                    }
                }
            }
        )
    }

    private fun onClickVideoPlayPause() {
        if (videoView.isPlaying) {
            messageHandler.removeMessages(SHOW_PROGRESS)
            pauseVideo()
        } else {
            playView.visibility = View.GONE
            if (resetSeekBar) {
                resetSeekBar = false
                videoView.seekTo(startPosition)
            }
            messageHandler.sendEmptyMessage(SHOW_PROGRESS)
            videoView.start()
        }
    }

    @UiThread
    private fun onVideoPrepared(mp: MediaPlayer) {
        // Adjust the size of the video
        // so it fits on the screen
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = videoViewContainer.width
        val screenHeight = videoViewContainer.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = videoView.layoutParams
        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        videoView.layoutParams = lp
        playView.visibility = View.VISIBLE
        duration = videoView.duration
        setSeekBarPosition()
        onRangeUpdated(startPosition, endPosition)
        onVideoPlaybackReachingTime(0)
        if (videoTrimmingListener != null)
            videoTrimmingListener!!.onVideoPrepared()
    }

    private fun setSeekBarPosition() {
        if (duration >= maxDurationInMillisecond) {
            startPosition = duration / 2 - maxDurationInMillisecond / 2
            endPosition = duration / 2 + maxDurationInMillisecond / 2
            seekBarView.setThumbValue(0, startPosition * 100f / duration)
            seekBarView.setThumbValue(1, endPosition * 100f / duration)
        } else {
            startPosition = 0
            endPosition = duration
        }
        setProgressBarPosition(startPosition)
        videoView.seekTo(startPosition)
        timeVideo = duration
        seekBarView.initMaxWidth()
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            SeekBarView.ThumbType.LEFT.index -> {
                startPosition = (duration * value / 100L).toInt()
                videoView.seekTo(startPosition)
            }
            SeekBarView.ThumbType.RIGHT.index -> {
                endPosition = (duration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(startPosition)

        onRangeUpdated(startPosition, endPosition)
        timeVideo = endPosition - startPosition
    }

    private fun onStopSeekThumbs() {
        messageHandler.removeMessages(SHOW_PROGRESS)
        pauseVideo()
    }

    private fun onVideoCompleted() {
        videoView.seekTo(startPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (duration == 0) return
        val position = videoView.currentPosition
        if (all)
            for (item in listeners)
                item.updateProgress(position, duration, position * 100f / duration)
        else
            listeners[1].updateProgress(position, duration, position * 100f / duration)
    }

    private fun updateVideoProgress(time: Int) {
        if (time >= endPosition) {
            messageHandler.removeMessages(SHOW_PROGRESS)
            pauseVideo()
            resetSeekBar = true
            return
        }
        setProgressBarPosition(time)
        onVideoPlaybackReachingTime(time)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun pauseVideo() {
        videoView.pause()
        playView.visibility = View.VISIBLE
    }

    private fun setProgressBarPosition(position: Int) {
        if (duration > 0) {
        }
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean) {
        timeInfoContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onVideoListener interface for events
     */
    fun setOnVideoListener(onVideoListener: VideoTrimmingListener) {
        this.videoTrimmingListener = onVideoListener
    }

    fun setDestinationFile(dst: File?) {
        dstFile = if (dst != null) dst else {
            val folder = Environment.getExternalStorageDirectory()
            File(folder.path + File.separator)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //Cancel all current operations
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     */
    fun setMaxDurationInMs(maxDurationInMs: Int) {
        this.maxDurationInMillisecond = maxDurationInMs
    }

    /**
     * Set the minimum duration of the trimmed video
     */
    fun setMinDurationInMs(minDuration: Int) {
        this.minDurationInMillisecond = minDuration
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri) {
        src = videoURI
        if (totalDuration == 0L) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, src)
            totalDuration =
                java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        }
        if (originSizeFile == 0L) {
            val cursor = context.contentResolver.query(videoURI, null, null, null, null)
            if (cursor != null) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                originSizeFile = cursor.getLong(sizeIndex)
                cursor.close()
            }
        }
        videoView.setVideoURI(src)
        videoView.requestFocus()
        timeLineView.setVideoUri(src!!)
    }

    private class MessageHandler internal constructor(view: BaseVideoTrimmerView) : Handler() {
        private val mView: WeakReference<BaseVideoTrimmerView> = WeakReference(view)

        override fun handleMessage(msg: Message?) {
            val view = mView.get()
            if (view?.videoView == null)
                return
            view.notifyProgressUpdate(true)
            if (view.videoView.isPlaying) {
                sendEmptyMessageDelayed(0, 10)
            }
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}
