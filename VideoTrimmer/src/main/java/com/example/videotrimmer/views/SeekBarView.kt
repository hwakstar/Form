package com.sample.videotrimmerlib.views;

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.sample.videotrimmerlib.interfaces.OnRangeSeekBarListener
import kotlin.math.absoluteValue

/**
 * @author Mukesh Yadav on 29/9/19.
 */
open class SeekBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    enum class ThumbType(val index: Int) {
        LEFT(0), RIGHT(1)
    }

    private val TAG: String = "SeekBarView"
    private val thumbTouchExtraMultiplier = initThumbTouchExtraMultiplier()
    private val thumbs = arrayOf(Thumb(ThumbType.LEFT.index), Thumb(ThumbType.RIGHT.index))
    private var listeners = HashSet<OnRangeSeekBarListener>()
    private var maxWidth: Float = 0.toFloat()
    private var minWidth: Float = 0.toFloat()
    val thumbWidth = initThumbWidth(context)
    private var viewWidth: Int = 0
    private var pixelRangeMin: Float = 0.toFloat()
    private var pixelRangeMax: Float = 0.toFloat()
    private val scaleRangeMax: Float = 100f
    private var firstRun: Boolean = true
    private val shadowPaint = Paint()
    private val strokePaint = Paint()
    private val edgePaint = Paint()
    private var currentThumb = ThumbType.LEFT.index


    init {
        isFocusable = true
        isFocusableInTouchMode = true
        shadowPaint.isAntiAlias = true
        shadowPaint.color = initShadowColor()
        strokePaint.isAntiAlias = true
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)
        strokePaint.color = 0xffffffff.toInt()
        edgePaint.isAntiAlias = true
        edgePaint.color = 0xffffffff.toInt()
    }

    @ColorInt
    open fun initShadowColor(): Int = 0xB1000000.toInt()

    open fun initThumbTouchExtraMultiplier() = 1.0f

    open fun initThumbWidth(context: Context) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            27f,
            context.resources.displayMetrics
        ).toInt().coerceAtLeast(1)

    fun initMaxWidth() {
        maxWidth = thumbs[ThumbType.RIGHT.index].pos - thumbs[ThumbType.LEFT.index].pos
        minWidth = maxWidth/2
        onSeekStop(this, ThumbType.LEFT.index, thumbs[ThumbType.LEFT.index].value)
        onSeekStop(this, ThumbType.RIGHT.index, thumbs[ThumbType.RIGHT.index].value)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = measuredWidth
        pixelRangeMin = 0f
        pixelRangeMax = (viewWidth - thumbWidth).toFloat()
        Log.d(TAG, "pixelRangeMin and pixelRangeMax: $pixelRangeMin $pixelRangeMax")
        if (firstRun) {
            for ((index, thumb) in thumbs.withIndex()) {
                thumb.value = scaleRangeMax * index
                thumb.pos = pixelRangeMax * index
                Log.d(TAG, "First create: thumb$index-> $thumb")
            }
            onCreate(this, currentThumb, getThumbValue(currentThumb))
            firstRun = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thumbs.isEmpty()) return
        // draw shadows outside of selected range
        for (thumb in thumbs) {
            if (thumb.index == ThumbType.LEFT.index) {
                val x = thumb.pos + paddingLeft
                if (x > pixelRangeMin)
                    canvas.drawRect(thumbWidth.toFloat()-2, 0f, (x + thumbWidth)-2, height.toFloat(), shadowPaint)
            } else {
                val x = thumb.pos + paddingRight
                if (x < pixelRangeMax)
                    canvas.drawRect(x+2, 0f, (viewWidth - thumbWidth).toFloat()+2, height.toFloat(), shadowPaint)
            }
        }
        //draw stroke around selected range
        canvas.drawRect(
            (thumbs[ThumbType.LEFT.index].pos + paddingLeft + thumbWidth)-2,
            0f,
            thumbs[ThumbType.RIGHT.index].pos - paddingRight+2,
            height.toFloat(),
            strokePaint
        )
        //draw edges
        val circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics)
        canvas.drawCircle(
            (thumbs[ThumbType.LEFT.index].pos + paddingLeft + thumbWidth),
            height.toFloat() / 2f,
            circleRadius,
            edgePaint
        )
        canvas.drawCircle(
            thumbs[ThumbType.RIGHT.index].pos - paddingRight,
            height.toFloat() / 2f,
            circleRadius,
            edgePaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val thumb1: Thumb
        val thumb2: Thumb
        val xCoordinate = event.x
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentThumb = getClosestThumb(xCoordinate)
                if (currentThumb == -1) return false
                thumb1 = thumbs[currentThumb]
                thumb1.lastTouchX = xCoordinate
                Log.d(TAG, "ACTION_DOWN: thumb1-> $thumb1")
                onSeekStart(this, currentThumb, thumb1.value)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (currentThumb == -1)
                    return false
                thumb1 = thumbs[currentThumb]
                onSeekStop(this, currentThumb, thumb1.value)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                thumb1 = thumbs[currentThumb]
                thumb2 =
                    thumbs[if (currentThumb == ThumbType.LEFT.index) ThumbType.RIGHT.index else ThumbType.LEFT.index]
                val dx = xCoordinate - thumb1.lastTouchX
                val newX = thumb1.pos + dx
                Log.d(TAG, "dx: $dx")
                Log.d(TAG, "newX: $newX")
                Log.d(TAG, "Thumb1: ${thumb1.toString()}")
                Log.d(TAG, "Thumb2: ${thumb2.toString()}")
                Log.d(TAG, "Thumb2-thumb1: ${thumb2.value - thumb1.value}")

                Log.d(TAG, "Condition false")
                when {
                    currentThumb == 0 -> when {
                        newX + thumbWidth >= thumb2.pos -> {
                            thumb1.pos = thumb2.pos - thumbWidth
                        }
                        newX <= pixelRangeMin -> {
                            thumb1.pos = pixelRangeMin
                        }
                        else -> {
                            //Check if thumb is not out of max width
                            checkPositionThumb(thumb1, thumb2, dx, true)
                            // Move the object
                            thumb1.pos = thumb1.pos + dx
                            // Remember this touch position for the next move event
                            thumb1.lastTouchX = xCoordinate
                        }
                    }
                    newX <= thumb2.pos + thumbWidth -> {
                        thumb1.pos = thumb2.pos + thumbWidth
                    }
                    newX >= pixelRangeMax -> {
                        thumb1.pos = pixelRangeMax
                    }
                    else -> {
                        //Check if thumb is not out of max width
                        checkPositionThumb(thumb2, thumb1, dx, false)
                        // Move the object
                        thumb1.pos = thumb1.pos + dx
                        // Remember this touch position for the next move event
                        thumb1.lastTouchX = xCoordinate
                    }
                }
                setThumbPos(currentThumb, thumb1.pos)
                // Invalidate to request a redraw
                invalidate()
                return true
            }
        }
        return false
    }

    private fun checkPositionThumb(thumbLeft: Thumb, thumbRight: Thumb, dx: Float, isLeftMove: Boolean) {
        if (isLeftMove && dx < 0) {
            if (thumbRight.pos - (thumbLeft.pos + dx) > maxWidth) {
                thumbRight.pos = thumbLeft.pos + dx + maxWidth
                setThumbPos(ThumbType.RIGHT.index, thumbRight.pos)
            }
        } else if (!isLeftMove && dx > 0) {
            if (thumbRight.pos + dx - thumbLeft.pos > maxWidth) {
                thumbLeft.pos = thumbRight.pos + dx - maxWidth
                setThumbPos(ThumbType.LEFT.index, thumbLeft.pos)
            }
        }
    }

    private fun calculateThumbValue(index: Int) {
        if (index < thumbs.size && !thumbs.isEmpty()) {
            val th = thumbs[index]
            th.value = pixelToScale(index, th.pos)
            onSeek(this, index, th.value)
        }
    }

    private fun pixelToScale(index: Int, pixelValue: Float): Float {
        val scale = pixelValue * 100 / pixelRangeMax
        return if (index == 0) {
            val pxThumb = scale * thumbWidth / 100
            scale + pxThumb * 100 / pixelRangeMax
        } else {
            val pxThumb = (100 - scale) * thumbWidth / 100
            scale - pxThumb * 100 / pixelRangeMax
        }
    }

    private fun scaleToPixel(index: Int, scaleValue: Float): Float {
        val px = scaleValue * pixelRangeMax / 100
        return if (index == 0) {
            val pxThumb = scaleValue * thumbWidth / 100
            px - pxThumb
        } else {
            val pxThumb = (100 - scaleValue) * thumbWidth / 100
            px + pxThumb
        }
    }

    private fun calculateThumbPos(index: Int) {
        if (index < thumbs.size && !thumbs.isEmpty()) {
            val th = thumbs[index]
            th.pos = scaleToPixel(index, th.value)
        }
    }

    private fun getThumbValue(index: Int): Float {
        return thumbs[index].value
    }

    fun setThumbValue(index: Int, value: Float) {
        thumbs[index].value = value
        calculateThumbPos(index)
        // Tell the view we want a complete redraw
        invalidate()
    }

    private fun setThumbPos(index: Int, pos: Float) {
        thumbs[index].pos = pos
        calculateThumbValue(index)
        // Tell the view we want a complete redraw
        invalidate()
    }

    private fun getClosestThumb(xCoordinate: Float): Int {
        if (thumbs.isEmpty())
            return -1
        var closest = -1
        var minDistanceFound = Float.MAX_VALUE
        val x = xCoordinate - thumbWidth
        for (thumb in thumbs) {
            val thumbPos = if (thumb.index == ThumbType.LEFT.index) thumb.pos else thumb.pos - thumbWidth
            val xMin = thumbPos - thumbWidth * thumbTouchExtraMultiplier
            val xMax = thumbPos + thumbWidth * thumbTouchExtraMultiplier
            if (x in xMin..xMax) {
                val distance = (thumbPos - x).absoluteValue
                if (distance < minDistanceFound) {
                    closest = thumb.index
                    minDistanceFound = distance
                }
            }
        }
        Log.d(TAG, "closest: $closest")
        return closest
    }

    fun addOnRangeSeekBarListener(listener: OnRangeSeekBarListener) {
        listeners.add(listener)
    }

    private fun onCreate(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
        listeners.forEach { item -> item.onCreate(rangeSeekBarView, index, value) }
    }

    private fun onSeek(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
        listeners.forEach { item -> item.onSeek(rangeSeekBarView, index, value) }
    }

    private fun onSeekStart(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
        listeners.forEach { item -> item.onSeekStart(rangeSeekBarView, index, value) }
    }

    private fun onSeekStop(rangeSeekBarView: SeekBarView, index: Int, value: Float) {
        listeners.forEach { item -> item.onSeekStop(rangeSeekBarView, index, value) }
    }

    class Thumb(val index: Int = 0) {
        var value: Float = 0f
        var pos: Float = 0f
        var lastTouchX: Float = 0f

        override fun toString(): String {
            return "value: " + value + "pos: " + pos + "lastTouchX: " + lastTouchX
        }
    }

}
