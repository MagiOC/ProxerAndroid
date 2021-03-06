package me.proxer.app.news.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * @author Ruben Gees
 */
class NewsWidgetDarkService : RemoteViewsService() {

    companion object {
        const val ARGUMENT_NEWS = "news"
        const val ARGUMENT_NEWS_WRAPPER = "news_wrapper" /* Hack for making it possible to share
                                                            between processes. */
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val newsWrapper = intent.getBundleExtra(ARGUMENT_NEWS_WRAPPER)
        val news = newsWrapper.getParcelableArray(ARGUMENT_NEWS).map { it as SimpleNews }

        return NewsWidgetViewsFactory(applicationContext, true, news)
    }
}
