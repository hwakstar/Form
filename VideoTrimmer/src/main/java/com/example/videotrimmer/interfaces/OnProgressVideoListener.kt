package com.sample.videotrimmerlib.interfaces

/**
 * @author Mukesh Yadav on 29/9/19.
 */
interface OnProgressVideoListener {
    fun updateProgress(time: Int, max: Int, scale: Float)
}