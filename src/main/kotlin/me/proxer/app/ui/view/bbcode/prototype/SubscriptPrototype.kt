package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.style.SubscriptSpan
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SubscriptPrototype : TextMutatorPrototype {

    override val startRegex = Regex(" *sub( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *sub *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        setSpan(SubscriptSpan(), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
    }
}
