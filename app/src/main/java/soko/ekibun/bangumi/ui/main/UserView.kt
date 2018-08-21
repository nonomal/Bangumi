package soko.ekibun.bangumi.ui.main

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.model.UserModel

class UserView(private val context: MainActivity, onUserFigureClickListener: View.OnClickListener){
    private val headerView by lazy{context.nav_view.getHeaderView(0)}

    init{
        headerView.user_figure.setOnClickListener(onUserFigureClickListener)
    }

    fun setUser(user: UserInfo?){
        context.runOnUiThread {
            if(context.isDestroyed) return@runOnUiThread
            Glide.with(headerView)
                    .load(user?.avatar?.large)
                    .apply(RequestOptions.placeholderOf(R.drawable.akkarin))
                    .apply(RequestOptions.circleCropTransform())
                    .into(headerView.user_figure)
            val token = UserModel(context).getToken()
            headerView.user_id.text = if(user?.id == null) "" else "@${user.id}"
            headerView.user_name.text = user?.nickname
                    ?:token?.user_id?.toString()?:context.getString(R.string.login_hint)
        }
    }

}