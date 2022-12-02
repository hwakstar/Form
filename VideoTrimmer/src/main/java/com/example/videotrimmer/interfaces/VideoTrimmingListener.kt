package com.sample.videotrimmerlib.interfaces

import android.net.Uri
import androidx.annotation.UiThread

/**
 * @author Mukesh Yadav on 29/9/19.
 */
public interface VideoTrimmingListener {
    @UiThread
    fun onVideoPrepared()

    @UiThread
    fun onTrimStarted()

    /**
     * @param uri the result, trimmed video, or null if failed
     */
    @UiThread
    fun onFinishedTrimming(uri: Uri?)

    /**
     * check {[android.media.MediaPlayer.OnErrorListener]}
     */
    @UiThread
    fun onErrorWhileViewingVideo(what: Int, extra: Int)
}