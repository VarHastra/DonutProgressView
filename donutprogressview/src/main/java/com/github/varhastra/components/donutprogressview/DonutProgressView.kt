package com.github.varhastra.components.donutprogressview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.github.varhastra.components.donutprogressview.interpolators.FastOutSlowInInterpolator
import kotlin.math.min

class DonutProgressView : View {

    private var viewWidth = 0f
    private var viewHeight = 0f
    private val defaultSize = 24.dp()
    private var scaleFactor = 1f
    private lateinit var boundsRect: RectF

    val minProgress = 0
    val maxProgress = 100
    var progress = 0
        set(value) {
            if (value < minProgress || value > maxProgress) {
                throw IllegalArgumentException("Invalid progress value: $value. Must be in [$minProgress, $maxProgress].")
            }
            oldProgress = field
            field = value
            invalidate()
        }
    private var oldProgress = 0

    var startAngle = -90f
        set(value) {
            field = value
            invalidate()
        }

    var progressColor: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            updateProgressPaint()
            invalidate()
        }
    var trackColor: Int = 0x1FFFFFFF.toInt()
        set(value) {
            field = value
            updateTrackPaint()
            invalidate()
        }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.FILL
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.FILL
    }

    private lateinit var baseBitmap: Bitmap
    private lateinit var baseCanvas: Canvas

    private lateinit var bufferBitmap: Bitmap
    private lateinit var bufferCanvas: Canvas

    private lateinit var maskBitmap: Bitmap
    private lateinit var maskCanvas: Canvas
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
    }
    private val paintDstOut = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    var animationDurationMillis = 150L
    private var animationValue = 1f


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        attrs?.apply {
            val styleAttrs = context.obtainStyledAttributes(attrs, R.styleable.DonutProgressView, 0, 0)
            progress = styleAttrs.getInt(R.styleable.DonutProgressView_progress, 0)
            progressColor = styleAttrs.getColor(R.styleable.DonutProgressView_progressColor, progressColor)
            trackColor = styleAttrs.getColor(R.styleable.DonutProgressView_trackColor, trackColor)
            startAngle = styleAttrs.getFloat(R.styleable.DonutProgressView_startAngle, startAngle)
            animationDurationMillis = styleAttrs.getInt(R.styleable.DonutProgressView_animationDuration, animationDurationMillis.toInt()).toLong()

            styleAttrs.recycle()
        }

        val twoDp = 2.dp().toFloat()
        boundsRect = RectF(
                twoDp,
                twoDp,
                defaultSize - twoDp,
                defaultSize - twoDp
        )
    }

    private fun updateTrackPaint() {
        trackPaint.color = trackColor
    }

    private fun updateProgressPaint() {
        progressPaint.color = progressColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        scaleFactor = min(w, h) / defaultSize.toFloat()
        val xCenter = w / 2f
        val yCenter = h / 2f

        baseBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        baseCanvas = Canvas(baseBitmap)
        baseCanvas.scale(scaleFactor, scaleFactor, xCenter, yCenter)

        maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        maskCanvas = Canvas(maskBitmap).apply {
            scale(scaleFactor, scaleFactor, xCenter, yCenter)
            drawCircle(w / 2f, h / 2f, 8.dp().toFloat(), maskPaint)
        }

        bufferBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bufferCanvas = Canvas(bufferBitmap)

        boundsRect.apply {
            val tenDp = 10.dp()
            left = xCenter - tenDp
            top = yCenter - tenDp
            right = xCenter + tenDp
            bottom = yCenter + tenDp
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val delta = progress - oldProgress
        val sweepAngle = (oldProgress + (delta * animationValue)) * 360f / maxProgress

        baseCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        baseCanvas.drawArc(boundsRect, startAngle, sweepAngle, true, progressPaint)
        baseCanvas.drawArc(boundsRect, startAngle + sweepAngle, 360f - sweepAngle, true, trackPaint)

        bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        bufferCanvas.drawBitmap(baseBitmap, 0f, 0f, null)
        bufferCanvas.drawBitmap(maskBitmap, 0f, 0f, paintDstOut)

        canvas.drawBitmap(bufferBitmap, 0f, 0f, null)
    }

    fun setProgress(progress: Int, animate: Boolean) {
        this.progress = progress

        if (!animate) {
            return
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDurationMillis
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                animationValue = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState() ?: Bundle()

        return SavedState(
                superState,
                progress,
                oldProgress,
                startAngle,
                progressColor,
                trackColor,
                animationDurationMillis
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        this.progress = state.progress
        this.oldProgress = state.oldProgress
        this.startAngle = state.startAngle
        this.progressColor = state.progressColor
        this.trackColor = state.trackColor
        this.animationDurationMillis = state.animationDurationMillis
    }


    class SavedState : BaseSavedState {
        var progress = 0
            private set
        var oldProgress = 0
            private set
        var startAngle = -90f
            private set
        var progressColor: Int = 0xFFFFFFFF.toInt()
            private set
        var trackColor: Int = 0x1FFFFFFF.toInt()
            private set
        var animationDurationMillis: Long = 150L
            private set

        constructor(
                superState: Parcelable,
                progress: Int,
                oldProgress: Int,
                startAngle: Float,
                progressColor: Int,
                trackColor: Int,
                animationDurationMillis: Long) : super(superState) {
            this.progress = progress
            this.oldProgress = oldProgress
            this.startAngle = startAngle
            this.progressColor = progressColor
            this.trackColor = trackColor
            this.animationDurationMillis = animationDurationMillis
        }

        private constructor(parcel: Parcel) : super(parcel) {
            progress = parcel.readInt()
            oldProgress = parcel.readInt()
            startAngle = parcel.readFloat()
            progressColor = parcel.readInt()
            trackColor = parcel.readInt()
            animationDurationMillis = parcel.readLong()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeInt(progress)
            out.writeInt(oldProgress)
            out.writeFloat(startAngle)
            out.writeInt(progressColor)
            out.writeInt(trackColor)
            out.writeLong(animationDurationMillis)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel) = SavedState(parcel)

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }


    private inline fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private inline fun Float.dp() = (this * resources.displayMetrics.density).toInt()

    private inline fun Int.sp() = (this * resources.displayMetrics.scaledDensity).toInt()

    private inline fun Float.sp() = (this * resources.displayMetrics.scaledDensity).toInt()
}