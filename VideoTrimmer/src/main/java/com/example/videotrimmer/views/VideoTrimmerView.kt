package com.sample.videotrimmerlib.views

import android.content.Context
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.VideoView
import com.sample.videotrimmerlib.BaseVideoTrimmerView
import com.sample.videotrimmerlib.R
import com.sample.videotrimmerlib.videoutils.TrimVideoUtils
import kotlinx.android.synthetic.main.video_trimmer.view.*

class VideoTrimmerView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) :
    BaseVideoTrimmerView(context, attrs, defStyleAttr) {

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val timeFormatter = java.util.Formatter()
        return if (hours > 0)
            timeFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        else
            timeFormatter.format("%02d:%02d", minutes, seconds).toString()
    }

    override fun initRootView() {
        LayoutInflater.from(context).inflate(R.layout.video_trimmer, this, true)
        saveBtn.setOnClickListener { initiateTrimming() }
    }

    override fun getFrameTimeLineView(): FrameTimeLimeView = timeLineView

    override fun getTimeInfoContainer(): View = timeTextContainer

    override fun getPlayView(): View = playIndicatorView

    override fun getVideoView(): VideoView = videoView

    override fun getVideoViewContainer(): View = videoViewContainer

    override fun getSeekBarView(): SeekBarView = rangeSeekBarView

    override fun onRangeUpdated(startTimeInMs: Int, endTimeInMs: Int) {
        playbackTimeTextView.text = "${stringForTime(Math.abs(endTimeInMs - startTimeInMs).coerceAtLeast(1000))}"
        trimTimeRangeTextView.text = "${stringForTime(startTimeInMs)} - ${stringForTime(endTimeInMs)}"
        onGotVideoFileSize(TrimVideoUtils.getFileSize(endTimeInMs - startTimeInMs, totalDuration, originSizeFile))
    }

    override fun onVideoPlaybackReachingTime(timeInMs: Int) {

    }

    override fun onGotVideoFileSize(videoFileSize: Long) {
        videoFileSizeTextView.text = Formatter.formatShortFileSize(context, videoFileSize)
    }
}
