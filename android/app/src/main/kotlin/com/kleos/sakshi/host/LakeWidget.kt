package com.kleos.sakshi.host

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.kleos.sakshi.MainActivity
import com.kleos.sakshi.R
import com.kleos.sakshi.engine.model.LakeRow
import com.kleos.sakshi.engine.model.LakeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Lake on the home screen. It draws the stored `lake_state` row and computes nothing: the engine chose the state and
 * the phrase. Last completed window only; no timer, no counters, no animation (DOC 3 F7).
 */
class LakeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Room is read off the main thread; goAsync keeps the receiver alive until the redraw is done.
        val pending = goAsync()
        Thread {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    /** What the widget shows, decided from the stored row alone. */
    data class Face(val drawableRes: Int, val phrase: String, val asOfText: String?)

    companion object {
        const val EXTRA_ROUTE = "route"
        const val ROUTE_MIRROR = "mirror"
        const val DEMO_SUFFIX = " (demo)"

        /** LEARNING and NO_DATA use the still drawable. A missing row draws NO_DATA. */
        fun faceFor(row: LakeRow?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): Face {
            val state = row?.state ?: LakeState.NO_DATA
            val drawable = when (state) {
                LakeState.CHOPPY -> R.drawable.lake_choppy
                LakeState.RIPPLED -> R.drawable.lake_rippled
                LakeState.STILL, LakeState.LEARNING, LakeState.NO_DATA -> R.drawable.lake_still
            }
            // Before the first run there is no stored row: the engine's own "nothing to show yet" wording, never a blank.
            val phrase = row?.phrase ?: com.kleos.sakshi.engine.mirror.SentenceBuilder.lakePhrase(LakeState.NO_DATA, gentle = false)
            return Face(drawable, phrase, row?.asOf?.let { "as of " + asOfClock(it.value, zone, locale) })
        }

        /** "9:42 pm" in the phone's own zone. */
        fun asOfClock(epochMs: Long, zone: ZoneId, locale: Locale): String =
            DateTimeFormatter.ofPattern("h:mm a", locale).format(Instant.ofEpochMilli(epochMs).atZone(zone)).lowercase(locale)

        fun views(context: Context, face: Face): RemoteViews = RemoteViews(context.packageName, R.layout.lake_widget).apply {
            setImageViewResource(R.id.lake_image, face.drawableRes)
            setTextViewText(R.id.lake_phrase, face.phrase)
            setContentDescription(R.id.lake_image, face.phrase)
            if (face.asOfText == null) setViewVisibility(R.id.lake_as_of, View.GONE) else {
                setViewVisibility(R.id.lake_as_of, View.VISIBLE)
                setTextViewText(R.id.lake_as_of, face.asOfText)
            }
            setOnClickPendingIntent(R.id.lake_root, openMirror(context))
        }

        /** The tap opens the Mirror. MainActivity turns the extra into the initial route, so no call into Flutter is needed. */
        private fun openMirror(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ROUTE, ROUTE_MIRROR).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        /**
         * Reads the stored row and redraws every Lake on the home screen. Never throws: a widget failure must not take down
         * a worker run, so it leaves a short code in ingest_state.lastError instead.
         */
        fun refresh(context: Context) {
            try {
                val container = AppContainer.from(context)
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, LakeWidget::class.java))
                if (ids.isEmpty()) return
                val face = faceFor(container.state.lake())
                // A demo can never be mistaken for real data, even from across the room.
                val views = views(context, if (container.demoActive) face.copy(phrase = face.phrase + DEMO_SUFFIX) else face)
                manager.updateAppWidget(ids, views)
            } catch (e: Exception) {
                try {
                    val state = AppContainer.from(context).state
                    state.saveIngest(state.ingest().copy(lastError = "WIDGET_" + (e::class.simpleName ?: "ERROR")))
                } catch (_: Exception) {
                    // nothing left to report to
                }
            }
        }
    }
}
