package com.sample.videotrimmerlib.interfaces

import com.sample.videotrimmerlib.views.SeekBarView

/**
 * @author Mukesh Yadav on 29/9/19.
 */
interface OnRangeSeekBarListener {
    fun onCreate(rangeSeekBarView: SeekBarView, index: Int, value: Float)

    fun onSeek(rangeSeekBarView: SeekBarView, index: Int, value: Float)

    fun onSeekStart(rangeSeekBarView: SeekBarView, index: Int, value: Float)

    fun onSeekStop(rangeSeekBarView: SeekBarView, index: Int, value: Float)
}