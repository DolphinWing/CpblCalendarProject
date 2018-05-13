package dolphin.android.apps.CpblCalendar3

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * https://stackoverflow.com/a/13437997/2673859
 */

class WizardPager : ViewPager {
    companion object {
        private const val TAG = "WizardPager"
    }

    private var mSwipeEnabled = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    val isLastStep: Boolean
        get() = adapter == null || adapter!!.count - 1 == currentItem

    val isFirstStep: Boolean
        get() = currentItem == 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return this.mSwipeEnabled && super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return this.mSwipeEnabled && super.onInterceptTouchEvent(event)

    }

    fun setSwipeEnabled(enabled: Boolean) {
        this.mSwipeEnabled = enabled
    }

    fun nextPage() {
        var position = currentItem
        if (isLastStep) {
            Log.w(TAG, "last page")//stay in the same index
        } else {
            position++
            currentItem = position
        }
    }

    fun backPage() {
        var position = currentItem
        if (isFirstStep) {
            Log.w(TAG, "first step")
        } else {
            position--
            currentItem = position
        }
    }

    /**
     * Enhanced PagerAdapter that supports getting child fragments.
     */
    abstract class PagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        //https://stackoverflow.com/a/15261142/2673859
        private val childFragments = SparseArray<Fragment>()

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            childFragments.put(position, fragment)
            return fragment
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            childFragments.remove(position)
            super.destroyItem(container, position, `object`)
        }

        fun getChildFragment(position: Int): Fragment? {
            return childFragments.get(position)
        }
    }

    /**
     * Basic StepFragment for WizardPager. It can support back and forth navigation.
     */
    open class StepFragment : Fragment() {
        private var mListener: StepListener? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.activity_splash, container, false)
        }

        override fun onAttach(context: Context?) {
            super.onAttach(context)

            mListener = if (activity is StepListener) activity as StepListener? else null
        }

        override fun onDetach() {
            super.onDetach()

            mListener = null
        }

        interface StepListener {
            fun onNextStep()

            fun onPreviousStep()

            fun onAbort()

            fun onStepResult(requestCode: Int, bundle: Bundle?)
        }

        fun nextStep() {
            mListener?.onNextStep()
        }

        fun prevStep() {
            mListener?.onPreviousStep()
        }

        fun abort() {
            mListener?.onAbort()
        }

        fun setStepResult(requestCode: Int, bundle: Bundle?) {
            mListener?.onStepResult(requestCode, bundle)
        }
    }
}
