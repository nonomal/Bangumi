package soko.ekibun.bangumi.ui.topic

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 图片浏览页 Adapter
 * @property items 图片url列表
 * @property onDismiss 关闭回调
 * @constructor
 */
class PhotoPagerAdapter(private val items: List<String>, private val onDismiss: ()->Unit): androidx.viewpager.widget.PagerAdapter(){

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = DragPhotoView(container.context)
        photoView.isEnabled = true
        GlideUtil.with(photoView)?.load(items[position])?.into(photoView.glideTarget)
        photoView.mExitListener = { onDismiss() }
        photoView.mTapListener = { onDismiss() }
        photoView.mLongClickListener = {
            val systemUiVisibility = container.systemUiVisibility
            val dialog = AlertDialog.Builder(container.context)
                    .setItems(arrayOf(container.context.getString(R.string.share)))
                    { _, _ ->
                        AppUtil.shareDrawable(container.context, photoView.drawable ?: return@setItems)
                    }.setOnDismissListener {
                        container.systemUiVisibility = systemUiVisibility
                    }.create()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            dialog.show()
        }
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return photoView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView((`object` as? View) ?: return)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}