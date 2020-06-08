package mx.arteko.circularseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import mx.arteko.circularseekbar.Utils.convertDpToPixel
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.*


/**
 * Created by Juancho - j.herandez@arteko.mx on 8/06/20.
 * Powered by Arteko
 */
class CircularSeekBar @JvmOverloads constructor(context: Context,
                      attrs: AttributeSet? = null,
                      defStyleAttr: Int = 0):
    View(context, attrs, defStyleAttr) {

    // settable by the client through attributes and programmatically
    private var mOnCircularSeekBarChangeListener: OnCircularSeekBarChangeListener? = null
    private var mOnCenterClickedListener: OnCenterClickedListener? = null
    private var mEnabled = true
    private var mShowIndicator = true
    private var mMinValue = 0f
    private var mMaxValue = 100f

    @FloatRange(from = 0.0)
    private var mSpeedMultiplier = 1f
    private var mProgress = 0f
    private var mShowText = true

    @FloatRange(from = 0.0, to = 1.0)
    private var mRingWidthFactor = 0.5f

    private var mProgressText: String? = null
    private var mShowInnerCircle = true

    @ColorInt
    private var mRingColor: Int = Color.rgb(192, 255, 140) //LIGHT LIME

    @ColorInt
    private var mInnerCircleColor: Int = Color.WHITE

    @ColorInt
    private var mProgressTextColor: Int = Color.BLACK

    @FloatRange(from = 0.0)
    private var mProgressTextSize =
        convertDpToPixel(resources, 24f)

    // settable by the client programmatically
    private var mRingPaint: Paint
    private var mInnerCirclePaint: Paint
    private var mProgressTextPaint: Paint
    private var mProgressTextFormat: NumberFormat = DecimalFormat("###,###,###,##0.0")

    // private
    private val mViewBox = RectF()
    private val mDimAlpha = 80
    private var mGestureDetector: GestureDetector? = null
    private var mTouching = false

    @FloatRange(from = 0.0, to = 360.0)
    private var mTouchAngle = 0f
    private var mAngularVelocityTracker: AngularVelocityTracker? = null

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularSeekBar,
            0,
            0
        )
        try {
            mEnabled = a.getBoolean(R.styleable.CircularSeekBar_enabled, mEnabled)
            mShowIndicator = a.getBoolean(R.styleable.CircularSeekBar_showIndicator, mShowIndicator)
            mMinValue = a.getFloat(R.styleable.CircularSeekBar_minValue, mMinValue)
            mMaxValue = a.getFloat(R.styleable.CircularSeekBar_maxValue, mMaxValue)
            mSpeedMultiplier =
                a.getFloat(R.styleable.CircularSeekBar_speedMultiplier, mSpeedMultiplier)
            mProgress = a.getFloat(R.styleable.CircularSeekBar_progress, mProgress)
            mShowText = a.getBoolean(R.styleable.CircularSeekBar_showProgressText, mShowText)
            mRingWidthFactor = a.getFloat(R.styleable.CircularSeekBar_ringWidth, mRingWidthFactor)
            mProgressText = a.getString(R.styleable.CircularSeekBar_progressText)
            mShowInnerCircle =
                a.getBoolean(R.styleable.CircularSeekBar_showInnerCircle, mShowInnerCircle)
            mRingColor = a.getColor(R.styleable.CircularSeekBar_ringColor, mRingColor)
            mInnerCircleColor =
                a.getColor(R.styleable.CircularSeekBar_innerCircleColor, mInnerCircleColor)
            mProgressTextColor =
                a.getColor(R.styleable.CircularSeekBar_progressTextColor, mProgressTextColor)
            mProgressTextSize = convertDpToPixel(
                resources,
                a.getFloat(R.styleable.CircularSeekBar_progressTextSize, mProgressTextSize)
            )
        } finally {
            a.recycle()
        }
        mRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mRingPaint.style = Paint.Style.FILL
        mRingPaint.color = mRingColor
        mInnerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mInnerCirclePaint.style = Paint.Style.FILL
        mInnerCirclePaint.color = mInnerCircleColor
        mProgressTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mProgressTextPaint.style = Paint.Style.STROKE
        mProgressTextPaint.textAlign = Align.CENTER
        mProgressTextPaint.color = mProgressTextColor
        mProgressTextPaint.textSize = mProgressTextSize
        mGestureDetector = GestureDetector(getContext(), GestureListener())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initViewBox()
        val center = center()
        mAngularVelocityTracker = AngularVelocityTracker(center.x, center.y)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWholeCircle(canvas)
        if (mShowIndicator && mTouching) {
            drawProgressArc(canvas)
        }
        if (mShowInnerCircle) {
            drawInnerCircle(canvas)
        }
        if (mShowText) {
            if (mProgressText != null) {
                drawCustomText(canvas)
            }
            else {
                drawProgressText(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnabled) {
            if (mGestureDetector?.onTouchEvent(event) == true) {
                return true
            }

            val distance = distanceToCenter(event.x, event.y)
            val outerCircleRadius = getOuterCircleRadius()
            val innerCircleRadius = getInnerCircleRadius()

            if (distance >= innerCircleRadius && distance < outerCircleRadius) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mTouching = true
                        trackTouchStart(event)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        mTouching = true
                        trackTouchMove(event)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        mTouching = false
                        trackTouchStop()
                    }
                }
            }
            else {
                mTouching = false
                mAngularVelocityTracker?.clear()
            }
            invalidate()
            return true
        }
        else {
            return super.onTouchEvent(event)
        }
    }

    /*********************************************************
    ******************* Public Methods ***********************
    *********************************************************/

    fun setOnCircularSeekBarChangeListener(listener: OnCircularSeekBarChangeListener?) {
        mOnCircularSeekBarChangeListener = listener
    }

    fun setOnCenterClickedListener(listener: OnCenterClickedListener?)  {
        mOnCenterClickedListener = listener
    }

    fun setIndicator(enable: Boolean) {
        mShowIndicator = enable
        invalidate()
    }

    fun isIndicatorEnabled(): Boolean = mShowIndicator

    fun setMinValue(min: Float) {
        mMinValue = min
        setProgress(min(mMinValue, mProgress))
    }

    fun getMin(): Float = mMinValue

    fun setMaxValue(max: Float) {
        mMaxValue = max
        setProgress(max(mMaxValue, mProgress))
    }

    fun getMax(): Float = mMaxValue


    fun setSpeedMultiplier(speedMultiplied: Float) {
        mSpeedMultiplier = speedMultiplied
    }

    fun getSpeedMultiplied() = mSpeedMultiplier

    fun setProgress(progress: Float) {
        mProgress = progress
        mOnCircularSeekBarChangeListener?.onProgressChanged(this, mProgress, false)
        invalidate()
    }

    fun getProgress() = mProgress

    fun setViewEnabled(enabled: Boolean) {
        mEnabled = enabled
        invalidate()
    }

    fun isViewEnabled(): Boolean = mEnabled

    fun setProgressText(enabled: Boolean) {
        mShowText = enabled
        invalidate()
    }

    fun isProgressTextEnabled(): Boolean {
        return mShowText
    }

    /**
     * Set the thickness of the outer ring (touchable area), relative to the size of the whole view
     * @param factor
     */
    fun setRingWidthFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        mRingWidthFactor = factor
        invalidate()
    }

    fun getRingWidthFactor(): Float {
        return mRingWidthFactor
    }

    fun setProgressText(text: String?) {
        mProgressText = text
        invalidate()
    }

    fun getProgressText(): String? {
        return mProgressText
    }

    /**
     * Enable/disable inner circle display
     * @param enable
     */
    fun setInnerCircle(enable: Boolean) {
        mShowInnerCircle = enable
        invalidate()
    }

    fun isInnerCircleEnabled(): Boolean {
        return mShowInnerCircle
    }

    /**
     * Set color for the outer ring (touchable area)
     * @param color
     */
    fun setRingColor(@ColorInt color: Int) {
        mRingColor = color
        mRingPaint.color = mRingColor
        invalidate()
    }

    @ColorInt
    fun getRingColor(): Int {
        return mRingColor
    }

    fun setInnerCircleColor(@ColorInt color: Int) {
        mInnerCircleColor = color
        mInnerCirclePaint.color = mInnerCircleColor
        invalidate()
    }

    @ColorInt
    fun getInnerCircleColor(): Int {
        return mInnerCircleColor
    }

    fun setProgressTextColor(@ColorInt color: Int) {
        mProgressTextColor = color
        mProgressTextPaint.color = mProgressTextColor
        invalidate()
    }

    @ColorInt
    fun getProgressTextColor(): Int {
        return mProgressTextColor
    }

    fun setProgressTextSize(@FloatRange(from = 0.0) pixels: Float) {
        mProgressTextSize = pixels
        mProgressTextPaint.textSize = mProgressTextSize
        invalidate()
    }

    fun getProgressTextSize(): Float {
        return mProgressTextSize
    }

    fun setRingPaint(paint: Paint) {
        mRingPaint = paint
        invalidate()
    }

    /**
     * Set the Paint used to draw the inner circle
     * @param paint
     */
    fun setInnerCirclePaint(paint: Paint) {
        mInnerCirclePaint = paint
        invalidate()
    }

    /**
     * Set the Paint used to draw the progress text or fixed custom text
     * @param paint
     */
    fun setProgressTextPaint(paint: Paint) {
        mProgressTextPaint = paint
        invalidate()
    }

    /**
     * Set the format of the progress text. <br></br>
     *
     *  * "###,###,###,##0.0" will display: 1,234.5
     *  * "###,###,###,##0.00" will display: 1,234.56
     *
     * @param format
     */
    fun setProgressTextFormat(format: NumberFormat) {
        mProgressTextFormat = format
        invalidate()
    }

    fun getProgressTextFormat(): NumberFormat? {
        return mProgressTextFormat
    }

    /*********************************************************
    ******************* Touch Events ***********************
    *********************************************************/

    private fun trackTouchStart(event: MotionEvent) {
        mAngularVelocityTracker?.let {
            it.clear()
            updateProgress(event.x, event.y, it.getAngularVelocity())
        }
        mOnCircularSeekBarChangeListener?.onStartTrackingTouch(this)
    }

    private fun trackTouchMove(event: MotionEvent) {
        mAngularVelocityTracker?.let {
            it.addMovement(event)
            updateProgress(event.x, event.y, it.getAngularVelocity())
        }
        mOnCircularSeekBarChangeListener?.onProgressChanged(this, mProgress, true)
    }

    private fun trackTouchStop() {
        mAngularVelocityTracker?.clear()
        mOnCircularSeekBarChangeListener?.onStopTrackingTouch(this)
    }

    /*********************************************************
    ******************* Draw Views ***********************
    *********************************************************/

    private fun drawWholeCircle(canvas: Canvas) {
        mRingPaint.alpha = mDimAlpha
        canvas.drawCircle(width / 2f, height / 2f, getOuterCircleRadius(), mRingPaint)
    }

    private fun drawProgressArc(canvas: Canvas) {
        mRingPaint.alpha = 255
        canvas.drawArc(mViewBox,
            mTouchAngle - 105,
            30f,
            true,
            mRingPaint)
    }

    private fun drawInnerCircle(canvas: Canvas) {
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            getInnerCircleRadius(),
            mInnerCirclePaint
        )
    }

    private fun drawCustomText(canvas: Canvas) {
        canvas.drawText(mProgressText ?: "",
        width / 2f,
        height / 2f + mProgressTextPaint.descent(),
        mProgressTextPaint)
    }

    private fun drawProgressText(canvas: Canvas) {
        mAngularVelocityTracker?.let {
            canvas.drawText(mProgressTextFormat.format(mProgress),
                width / 2f,
                height / 2f + mProgressTextPaint.descent(),
                mProgressTextPaint
            )
        }
    }

    /*********************************************************
    ******************* Private Methods ***********************
    *********************************************************/

    private fun diameter() = min(width, height)

    private fun center() = PointF(width / 2f, height / 2f)

    private fun getOuterCircleRadius(): Float = diameter() / 2f

    private fun getInnerCircleRadius() = getOuterCircleRadius() * (1 - mRingWidthFactor)


    private fun distanceToCenter(x: Float, y: Float): Float {
        val c = center()
        return sqrt(
            ( x - c.x ).toDouble().pow(2.0) + ( y - c.y ).toDouble().pow(2.0)
        ).toFloat()
    }

    private fun initViewBox() {
        val halfWidth: Float = (width / 2.0).toFloat()
        val halfHeight: Float = (height / 2.0).toFloat()
        val halfDiameter: Float = (diameter() / 2.0).toFloat()
        mViewBox.set(
            halfWidth - halfDiameter,
            halfHeight - halfDiameter,
            halfWidth + halfDiameter,
            halfHeight + halfDiameter
        )
    }

    private fun updateProgress(x: Float, y: Float, speed: Float) {
        mTouchAngle = getAngle(x, y)
        var newVal = mProgress + mMaxValue / 100 * speed * mSpeedMultiplier
        newVal = min(newVal, mMaxValue)
        newVal = max(newVal, mMinValue)
        mProgress = newVal
    }

    @FloatRange(from = 0.0, to = 360.0)
    private fun getAngle(x: Float, y: Float): Float {
        val c: PointF = center()
        return (-Math.toDegrees(
            atan2(
                c.x - x.toDouble(),
                c.y - y.toDouble()
            )
        )).toFloat()
    }
    
    /*********************************************************
    ******************* GestureListener ***********************
    *********************************************************/

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            // get the distance from the touch to the center of the view
            val distance: Float = distanceToCenter(event.x, event.y)
            val r: Float = getOuterCircleRadius()

            // touch gestures only work when touches are made exactly on the bar/arc
            if (mOnCenterClickedListener != null
                && distance <= r - r * mRingWidthFactor
            ) {
                mOnCenterClickedListener?.onCenterClicked(this@CircularSeekBar, mProgress)
            }
            return false
        }
    }

    /*********************************************************
    ******************* Listeners ***********************
    *********************************************************/

    /**
     * Listen for touch-events on the ring area
     */
    interface OnCircularSeekBarChangeListener {
        fun onProgressChanged(
            seekBar: CircularSeekBar?,
            progress: Float,
            fromUser: Boolean
        )

        fun onStartTrackingTouch(seekBar: CircularSeekBar?)
        fun onStopTrackingTouch(seekBar: CircularSeekBar?)
    }

    /**
     * Listen for singletap-events on the inner circle area
     */
    interface OnCenterClickedListener {
        fun onCenterClicked(seekBar: CircularSeekBar?, progress: Float)
    }
}