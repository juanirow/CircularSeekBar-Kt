package mx.arteko.circularseekbar

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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Created by Juancho - j.herandez@arteko.mx on 8/06/20.
 * Powered by Arteko
 */
class CircularSeekBar(context: Context,
                      attrs: AttributeSet? = null,
                      defStyleAttr: Int = 0):
    View(context, attrs, defStyleAttr) {

    // settable by the client through attributes and programmatically
    private val mOnCircularSeekBarChangeListener: OnCircularSeekBarChangeListener? = null
    private val mOnCenterClickedListener: OnCenterClickedListener? = null
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
    private val mProgressTextFormat: NumberFormat = DecimalFormat("###,###,###,##0.0")

    // private
    private val mViewBox = RectF()
    private val mDimAlpha = 80
    private var mGestureDetector: GestureDetector? = null
    private val mTouching = false

    @FloatRange(from = 0.0, to = 360.0)
    private val mTouchAngle = 0f
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

    /*********************************************************
    ******************* Draw Views ***********************
    *********************************************************/

    private fun drawWholeCircle(canvas: Canvas) {
        mRingPaint.alpha = mDimAlpha
        canvas.drawCircle(width / 2f, height / 2f, getOuterCircleRadius(), mRingPaint)
    }
    
    /*********************************************************
    ******************* Private Methods ***********************
    *********************************************************/

    private fun diameter() = min(width, height)

    private fun center() = PointF(width / 2f, height / 2f)

    private fun getOuterCircleRadius(): Float = diameter() / 2f

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
                mOnCenterClickedListener.onCenterClicked(this@CircularSeekBar, mProgress)
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