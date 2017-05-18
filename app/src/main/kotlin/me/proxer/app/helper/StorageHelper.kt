package me.proxer.app.helper

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Hawk.put
import com.orhanobut.hawk.Parser
import me.proxer.app.application.MainApplication.Companion.globalContext
import me.proxer.app.application.MainApplication.Companion.moshi
import me.proxer.app.entity.LocalUser
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashMap

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    private const val FIRST_START = "first_start"
    private const val USER = "user"
    private const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
    private const val LAST_NEWS_DATE = "last_news_date"
    private const val CHAT_INTERVAL = "chat_interval"
    private const val CONFERENCE_LIST_REACHED_END = "conference_list_reached_end"
    private const val CONFERENCE_REACHED_END = "conference_reached_end"

    private const val DEFAULT_CHAT_INTERVAL = 10_000L
    private const val MAX_CHAT_INTERVAL = 850_000L

    var isFirstStart: Boolean
        get() {
            ensureInit()

            return Hawk.get(FIRST_START, true)
        }
        set(value) {
            ensureInit()

            Hawk.put(FIRST_START, value)
        }

    var user: LocalUser?
        get() {
            ensureInit()

            return Hawk.get(USER)
        }
        set(value) {
            ensureInit()

            when (value) {
                null -> Hawk.delete(USER)
                else -> Hawk.put(USER, value)
            }
        }

    var isTwoFactorAuthenticationEnabled: Boolean
        get() {
            ensureInit()

            return Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        }
        set(value) {
            ensureInit()

            Hawk.put(TWO_FACTOR_AUTHENTICATION, value)
        }

    var lastNewsDate: Date
        get() {
            ensureInit()

            return Date(Hawk.get(LAST_NEWS_DATE, 0L))
        }
        set(value) {
            ensureInit()

            Hawk.put(LAST_NEWS_DATE, value.time)
        }

    val chatInterval: Long
        get() {
            ensureInit()

            return Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
        }

    var hasConferenceListReachedEnd: Boolean
        get() {
            ensureInit()

            return Hawk.get(CONFERENCE_LIST_REACHED_END, false)
        }
        set(value) {
            ensureInit()

            Hawk.put(CONFERENCE_LIST_REACHED_END, value)
        }

    fun hasConferenceReachedEnd(id: String): Boolean {
        return Hawk.get<Map<String, Boolean>>(CONFERENCE_REACHED_END, HashMap()).getOrElse(id, { false })
    }

    fun setConferenceReachedEnd(id: String) {
        Hawk.get<Map<String, Boolean>>(CONFERENCE_REACHED_END, HashMap()).apply {
            put(id, true)
        }.let {
            Hawk.put<Map<String, Boolean>>(CONFERENCE_REACHED_END, it)
        }
    }

    fun resetConferenceReachedEnd() {
        Hawk.delete(CONFERENCE_REACHED_END)
    }

    fun incrementChatInterval() {
        ensureInit()

        Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
            if (it < MAX_CHAT_INTERVAL) {
                Hawk.put(CHAT_INTERVAL, (it * 1.5f).toLong())
            }
        }
    }

    fun resetChatInterval() {
        ensureInit()

        Hawk.put(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }

    private fun ensureInit() {
        if (!Hawk.isBuilt()) {
            Hawk.init(globalContext).setParser(object : Parser {
                override fun <T : Any?> fromJson(content: String?, type: Type) = moshi.adapter<T>(type).fromJson(content)
                override fun toJson(body: Any) = when (body) {
                    is Map<*, *> -> moshi.adapter(Map::class.java).toJson(body)
                    else -> moshi.adapter(body.javaClass).toJson(body)
                }
            }).build()
        }
    }
}
