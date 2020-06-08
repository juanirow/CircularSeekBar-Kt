package mx.arteko.circularseekbar

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan2


/**
 * Created by Juancho - j.herandez@arteko.mx on 8/06/20.
 * Powered by Arteko
 */
class AngularVelocityTracker(private val centreX: Float, private val centreY: Float) {

    private var mInitialTime: Long = 0
    private var mFinalTime: Long = 0
    private var mInitialX = 0f
    private var mInitialY = 0f
    private var mFinalX = 0f
    private var mFinalY = 0f

    fun addMovement(event: MotionEvent) {
        mInitialX = mFinalX
        mInitialY = mFinalY
        mInitialTime = mFinalTime
        mFinalX = event.x
        mFinalY = event.y
        mFinalTime = event.eventTime
    }

    fun getAngularVelocity(): Float {
        var retVal = 0f
        if (mInitialTime != mFinalTime) {
            val timeLapse = mInitialTime - mFinalTime
            val initialAngle = calcAngle(mInitialX, mInitialY)
            val finalAngle = calcAngle(mFinalX, mFinalY)
            if (abs(finalAngle - initialAngle) < 20) {
                // Avoid strange results from quirks in angle calculation (goes from 0.1 to 359)
                retVal = (finalAngle - initialAngle) / timeLapse
            }
        }
        return retVal
    }

    fun clear() {
        mInitialX = 0f
        mInitialY = 0f
        mInitialTime = 0
        mFinalX = 0f
        mFinalY = 0f
        mFinalTime = 0
    }

    private fun calcAngle(x: Float, y: Float): Float {
        return Math.toDegrees(
            atan2(
                centreX - x.toDouble(),
                centreY - y.toDouble()
            )
        ).toFloat()
    }
}