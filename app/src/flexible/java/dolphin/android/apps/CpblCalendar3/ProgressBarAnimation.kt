package dolphin.android.apps.CpblCalendar3

import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import android.widget.ProgressBar

class ProgressBarAnimation(private val progressBar: ProgressBar, private val from: Int,
                           private val to: Int) : Animation(), Animation.AnimationListener {
    init {
        setAnimationListener(this)
        interpolator = LinearInterpolator()
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        super.applyTransformation(interpolatedTime, t)
        progressBar.progress = (from + (to - from) * interpolatedTime).toInt()
    }

    override fun onAnimationRepeat(animation: Animation?) {
    }

    override fun onAnimationEnd(animation: Animation?) {
        onAnimationEndAction()
    }

    override fun onAnimationStart(animation: Animation?) {
        onAnimationStartAction()
    }

    private var onAnimationStartAction: () -> Unit = { }

    fun withStartAction(body: () -> Unit): ProgressBarAnimation {
        onAnimationStartAction = body
        return this
    }

    private var onAnimationEndAction: () -> Unit = { }

    fun withEndAction(body: () -> Unit): ProgressBarAnimation {
        onAnimationEndAction = body
        return this
    }

    fun setRunTime(duration: Long): ProgressBarAnimation {
        setDuration(duration)
        return this
    }

    fun animate() {
        progressBar.startAnimation(this)
    }

}