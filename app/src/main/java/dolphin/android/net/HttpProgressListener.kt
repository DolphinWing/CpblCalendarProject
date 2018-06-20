package dolphin.android.net

/**
 * Created by dolphin on 2015/03/07.
 */
interface HttpProgressListener {
    fun onStart(contentSize: Long)
    fun onUpdate(bytes: Int, totalBytes: Long)
    fun onComplete(totalBytes: Long)
}
