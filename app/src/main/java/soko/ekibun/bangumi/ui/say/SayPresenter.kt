package soko.ekibun.bangumi.ui.say

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.DataCacheModel
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.topic.ReplyDialog
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil

class SayPresenter(private val context: SayActivity, say: Say) {
    val sayView = SayView(context)

    var say: Say
    val dataCacheModel by lazy { App.app.dataCacheModel }

    init {
        DataCacheModel.merge(say, dataCacheModel.get(say.cacheKey))
        this.say = say
        sayView.processSay(say, isCache = true)
        getSay()

        context.item_swipe.setOnRefreshListener {
            getSay()
        }
        sayView.adapter.loadMoreModule.setOnLoadMoreListener { if (loadMoreFail == true) getSay() }

        var draft: String? = null
        context.btn_reply.setOnClickListener {
            if (HttpUtil.formhash.isNotEmpty()) showReply(say, draft) { draft = it }
            else WebActivity.launchUrl(context, say.url, "")
        }
        sayView.adapter.setOnItemChildClickListener { _, _, position ->
            WebActivity.launchUrl(context, sayView.adapter.data[position].t.user.url, "")
        }
        sayView.adapter.setOnItemChildLongClickListener { _, _, position ->
            showReply(say, "@${sayView.adapter.data[position].t.user.username} ") { draft = it }
            true
        }
    }

    fun updateHistory() {
        HistoryModel.addHistory(say)
    }

    private var loadMoreFail: Boolean? = null

    fun getSay() {
        sayView.adapter.loadMoreModule.loadMoreComplete()
        loadMoreFail = null
        context.item_swipe.isRefreshing = true
        context.disposeContainer.subscribeOnUiThread(
            Say.getSaySax(say),
            { data ->
                when (data) {
                    is Say.SayReply -> {
                        sayView.processSay(say, true)
                        listOf(data)
                    }
                    is List<*> -> data.map { it as Say.SayReply }
                    is Say -> {
                        sayView.processSay(say)
                        dataCacheModel.set(say.cacheKey, say)
                        updateHistory()
                        null
                    }
                    else -> null
                }?.forEach { post ->
                    val index = sayView.adapter.data.indexOfFirst { it.t.index == post.index }
                    if (index < 0) {
                        val insertIndex = sayView.adapter.data.indexOfLast { it.t.index < post.index }
                        sayView.adapter.addData(insertIndex + 1, SayAdapter.SaySection(false, post))
                    } else sayView.adapter.setData(index, SayAdapter.SaySection(false, post))
                }
            }, {
                loadMoreFail = true
                sayView.adapter.loadMoreModule.loadMoreFail()
            }, {
                context.item_swipe.isRefreshing = false
            },
            key = SAY_CALL
        )
    }

    private fun showReply(say: Say, draft: String?, updateDraft: (String?) -> Unit) {
        val self = UserModel.current() ?: return
        ReplyDialog.showDialog(
            context.supportFragmentManager,
            hint = context.getString(R.string.parse_hint_reply_topic, say.user.nickname) ?: "",
            draft = draft
        ) { content, _, send ->
            if (content != null && send) {
                context.disposeContainer.subscribeOnUiThread(
                    Say.reply(say, content),
                    {
                        if (it) {
                            updateDraft(null)
                            say.replies = (say.replies ?: ArrayList()).let { replies ->
                                replies.plus(
                                    Say.SayReply(
                                        user = self,
                                        message = content,
                                        index = replies.size
                                    )
                                )
                            }
                            sayView.processSay(say)
                            (context.item_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                sayView.adapter.itemCount,
                                0
                            )
                            getSay()
                        } else Toast.makeText(context, R.string.hint_submit_error, Toast.LENGTH_LONG).show()
                    },
                    key = SAY_REPLY_CALL
                )
            } else updateDraft(content)
        }
    }

    companion object {
        const val SAY_CALL = "bangumi_say"
        const val SAY_REPLY_CALL = "bangumi_say_reply"
    }
}