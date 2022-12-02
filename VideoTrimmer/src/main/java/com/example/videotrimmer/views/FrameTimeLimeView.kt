package com.sample.videotrimmerlib.views;

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.sample.videotrimmerlib.videoutils.BackgroundExecutor
import com.sample.videotrimmerlib.videoutils.UiThreadExecutor

/**
 * @author Mukesh Yadav on 29/9/19.
 */
public class FrameTimeLimeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var videoUri: Uri? = null
    private val bitmapList = ArrayList<Bitmap?>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw)
            getBitmap(w, h)
    }

    private fun getBitmap(viewWidth: Int, viewHeight: Int) {
        val thumbSize = viewHeight
        val numberOfThumbnail = Math.ceil(viewWidth / viewHeight.toDouble()).toInt()
        bitmapList.clear()
        if (isInEditMode) {
            val thumb = ThumbnailUtils.extractThumbnail(
                BitmapFactory.decodeResource(resources, android.R.drawable.sym_def_app_icon)!!, viewHeight, viewHeight
            )
            for (i in 0 until numberOfThumbnail)
                bitmapList.add(thumb)
            return
        }
        BackgroundExecutor.cancelAll("", true)
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            override fun execute() {
                try {
                    val thumbnailList = ArrayList<Bitmap?>()
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, videoUri)
                    // Retrieve media data
                    val videoLengthInMs =
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() * 1000L
                    val interval = videoLengthInMs / numberOfThumbnail
                    for (i in 0 until numberOfThumbnail) {
                        var bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                            mediaMetadataRetriever.getScaledFrameAtTime(
                                i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, thumbSize, thumbSize
                            )
                        else mediaMetadataRetriever.getFrameAtTime(
                            i * interval,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        if (bitmap != null)
                            bitmap = ThumbnailUtils.extractThumbnail(bitmap, thumbSize, thumbSize)
                        thumbnailList.add(bitmap)
                    }
                    mediaMetadataRetriever.release()
                    returnBitmaps(thumbnailList)
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }

        })
    }

    private fun returnBitmaps(thumbnailList: ArrayList<Bitmap?>) {
        UiThreadExecutor.runTask("", Runnable {
            bitmapList.clear()
            bitmapList.addAll(thumbnailList)
            invalidate()
        }, 0L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        var x = 0
        val thumbSize = height
        for (bitmap in bitmapList) {
            if (bitmap != null)
                canvas.drawBitmap(bitmap, x.toFloat(), 0f, null)
            x += thumbSize
        }
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
    }
}
