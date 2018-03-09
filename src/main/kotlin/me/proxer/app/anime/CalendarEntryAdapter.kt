package me.proxer.app.anime

import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutParams
import android.text.SpannableString
import android.text.SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.CalendarEntryAdapter.ViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.calculateAndFormatDifference
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.childrenSequence
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class CalendarEntryAdapter : BaseAdapter<CalendarEntry, ViewHolder>() {

    private companion object {
        private val HOUR_MINUTE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, CalendarEntry>> = PublishSubject.create()

    private var layoutManager: RecyclerView.LayoutManager? = null
    private var currentMinAiringInfoLines = 4
    private var currentMinStatusLines = 1

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.itemView.post {
            val parentViewGroup = holder.itemView.parent as? ViewGroup?

            if (parentViewGroup != null) {
                if (holder.airingInfo.lineCount > currentMinAiringInfoLines) {
                    currentMinAiringInfoLines = holder.airingInfo.lineCount

                    parentViewGroup.childrenSequence().forEach {
                        it.findViewById<TextView>(R.id.airingInfo).minLines = currentMinAiringInfoLines
                    }

                    layoutManager?.requestSimpleAnimationsInNextLayout()
                }

                if (holder.status.lineCount > currentMinStatusLines) {
                    currentMinStatusLines = holder.status.lineCount

                    parentViewGroup.childrenSequence().forEach {
                        it.findViewById<TextView>(R.id.status).minLines = currentMinStatusLines
                    }

                    layoutManager?.requestSimpleAnimationsInNextLayout()
                }
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.airingInfoDisposable?.dispose()
        holder.airingInfoDisposable = null

        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image by bindView<ImageView>(R.id.image)
        internal val title by bindView<TextView>(R.id.title)
        internal val episode by bindView<TextView>(R.id.episode)
        internal val ratingContainer by bindView<ViewGroup>(R.id.ratingContainer)
        internal val rating by bindView<RatingBar>(R.id.rating)
        internal val airingInfo by bindView<TextView>(R.id.airingInfo)
        internal val status by bindView<TextView>(R.id.status)

        internal var airingInfoDisposable: Disposable? = null

        init {
            val width = DeviceUtils.getScreenWidth(itemView.context) / when {
                DeviceUtils.isLandscape(itemView.resources) -> 4.5
                else -> 2.5
            }

            itemView.layoutParams = LayoutParams(width.toInt(), LayoutParams.WRAP_CONTENT)

            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: CalendarEntry) {
            // Optimization to update the currentMinStatusLines since we now that there will in all cases two lines.
            if (item.date.convertToDateTime().toLocalDate() == LocalDate.now() && currentMinStatusLines < 2) {
                currentMinStatusLines = 2
            }

            ViewCompat.setTransitionName(image, "calendar_${item.id}")

            title.text = item.name
            episode.text = episode.context.getString(R.string.fragment_calendar_episode, item.episode.toString())

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            val airingDate = HOUR_MINUTE_DATE_TIME_FORMATTER.format(item.date.convertToDateTime())
            val uploadDate = HOUR_MINUTE_DATE_TIME_FORMATTER.format(item.uploadDate.convertToDateTime())

            if (item.date == item.uploadDate) {
                airingInfo.text = airingInfo.context.getString(R.string.fragment_calendar_airing, airingDate)
            } else {
                airingInfo.text = airingInfo.context.getString(R.string.fragment_calendar_airing_upload,
                        airingDate, uploadDate)
            }

            airingInfo.minLines = currentMinAiringInfoLines
            status.minLines = currentMinStatusLines

            airingInfoDisposable?.dispose()
            airingInfoDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(AiringInfoUpdateConsumer(item))

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }

        internal inner class AiringInfoUpdateConsumer(private val item: CalendarEntry) : Consumer<Long> {

            override fun accept(t: Long?) {
                val now = LocalDateTime.now()

                if (item.uploadDate.convertToDateTime().isBefore(now)) {
                    if (item.date == item.uploadDate) {
                        status.text = status.context.getString(R.string.fragment_calendar_aired)
                    } else {
                        val uploadedText = status.context.getString(R.string.fragment_calendar_uploaded)

                        status.text = SpannableString(uploadedText).apply {
                            val span = ForegroundColorSpan(ContextCompat.getColor(status.context, R.color.md_green_500))

                            setSpan(span, 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    }
                } else {
                    if (item.date.convertToDateTime().isBefore(now)) {
                        status.text = status.context.getString(R.string.fragment_calendar_aired_remaining_time,
                                Date().calculateAndFormatDifference(item.uploadDate))
                    } else {
                        status.text = status.context.getString(R.string.fragment_calendar_remaining_time,
                                Date().calculateAndFormatDifference(item.date))
                    }
                }
            }
        }
    }
}
