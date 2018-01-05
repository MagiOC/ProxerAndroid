package me.proxer.app.ui.view.bbcode.prototype

import android.app.Activity
import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import me.proxer.app.GlideRequests
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBTree.Companion.GLIDE_ARGUMENT
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.Utils
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object ImagePrototype : BBPrototype {

    private const val DELIMITER = "size="
    private const val WIDTH_ARGUMENT = "width"

    private val INVALID_IMAGE = HttpUrl.parse("https://cdn.proxer.me/keinbild.jpg")
            ?: throw IllegalArgumentException("Could not parse url")

    override val startRegex = Regex(" *img( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, DELIMITER)?.toIntOrNull()

        return BBTree(this, parent, args = mapOf(WIDTH_ARGUMENT to width))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.flatMap { it.makeViews(context) }

        val glide = args[GLIDE_ARGUMENT] as GlideRequests?
        val width = args[WIDTH_ARGUMENT] as Int?

        val url = Utils.safelyParseAndFixUrl((childViews.firstOrNull() as? TextView)?.text.toString())
                ?: INVALID_IMAGE

        return listOf(AppCompatImageView(context).also { it: ImageView ->
            ViewCompat.setTransitionName(it, "bb_image_$url")

            glide?.load(url.toString())
                    ?.override(width ?: SIZE_ORIGINAL, SIZE_ORIGINAL)
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.format(when (DeviceUtils.shouldShowHighQualityImages(context)) {
                        true -> DecodeFormat.PREFER_ARGB_8888
                        false -> DecodeFormat.PREFER_RGB_565
                    })
                    ?.into(it)

            if (context is Activity) {
                it.setOnClickListener { _ ->
                    if (it.drawable != null) {
                        ImageDetailActivity.navigateTo(context, url, it)
                    }
                }
            }
        })
    }
}