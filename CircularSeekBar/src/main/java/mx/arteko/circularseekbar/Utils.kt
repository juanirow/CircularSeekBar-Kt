package mx.arteko.circularseekbar

import android.content.res.Resources

/**
 * Created by Juancho - j.herandez@arteko.mx on 8/06/20.
 * Powered by Arteko
 */
object Utils {

    /**
     * This method convert dp unit to equivalent pixels, depending on device density
     * @param r get the density of the resource
     * @param dp A value in dp (density independent pixels) unit.
     * Which we need to convert into pixels
     *
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(r: Resources, dp: Float): Float {
        val metrics = r.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }
}