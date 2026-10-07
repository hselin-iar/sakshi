package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.Instant
import kotlin.math.ceil

/**
 * P4: Held / Pinged / Reached. label() is shared with WindowLabeler, which
 * sets each Window's own shape field; detect() aggregates into the "mix"
 * Pattern.
 */
object WindowShape : PatternDetector {
    override val kind = PatternKind.SHAPE

    /** Held if stats beat the user's own median (or there are zero stays); else Pinged/Reached by stone share. */
    fun label(
        staysPerHour: Double,
        stretchMin: Double,
        originsOfStays: List<Origin>,
        userMedianStaysPerHour: Double,
        userMedianStretchMin: Double,
    ): Shape? {
        if (originsOfStays.isEmpty() || (staysPerHour <= userMedianStaysPerHour && stretchMin >= userMedianStretchMin)) {
            return Shape.HELD
        }
        val known = originsOfStays.filter { it == Origin.STONE || it == Origin.SELF_STARTED }
        if (known.size < 2) return null
        val stoneShare = known.count { it == Origin.STONE }.toDouble() / known.size
        return if (stoneShare >= 0.60) Shape.PINGED else Shape.REACHED
    }

    override fun detect(ctx: PatternContext): List<Pattern> {
        val windows = qualifyingWindows(ctx)
        if (windows.isEmpty()) return emptyList()

        val userMedianStaysPerHour = median(windows.map { staysPerHourOf(it) })
        val userMedianStretchMin = median(windows.map { longestStretchOf(it) })

        val labeled = windows.map { wd ->
            wd to label(staysPerHourOf(wd), longestStretchOf(wd), wd.stays.map { it.origin }, userMedianStaysPerHour, userMedianStretchMin)
        }
        val labeledCount = labeled.count { it.second != null }
        if (labeledCount < Tuning.SHAPE_MIN_WINDOWS) return emptyList()

        val held = labeled.count { it.second == Shape.HELD }
        val pinged = labeled.count { it.second == Shape.PINGED }
        val reached = labeled.count { it.second == Shape.REACHED }
        val reachedWindows = labeled.filter { it.second == Shape.REACHED }.map { it.first }
        val reachedStartsAfter = reachedStartsAfterOf(reachedWindows, ctx.zone)

        return listOf(
            Pattern(
                kind = PatternKind.SHAPE,
                key = "mix",
                strength = reached.toDouble() / labeledCount,
                evidenceWindows = labeledCount,
                evidenceDays = windows.map { it.window.day }.distinct().size,
                firstSeen = ctx.asOf,
                lastSeen = ctx.asOf,
                args = mapOf(
                    "held" to held.toString(),
                    "pinged" to pinged.toString(),
                    "reached" to reached.toString(),
                    "reachedStartsAfter" to (reachedStartsAfter ?: "null"),
                ),
            ),
        )
    }

    // The earliest local clock time T such that >= 70% of Reached windows started at or after T:
    // the 30th percentile of their start times (so 70% are at/after it), rounded to 30 minutes.
    // [JUDGMENT CALL: no golden example gives a concrete number for this.]
    private fun reachedStartsAfterOf(reachedWindows: List<WindowWithDetail>, zone: java.time.ZoneId): String? {
        if (reachedWindows.size < 3) return null
        val minutesOfDay = reachedWindows.map { wd ->
            val t = Instant.ofEpochMilli(wd.window.start.value).atZone(zone).toLocalTime()
            t.hour * 60 + t.minute
        }.sorted()
        val idx = (0.30 * minutesOfDay.size).let { ceil(it).toInt() }.coerceIn(0, minutesOfDay.size - 1)
        val rounded = ((minutesOfDay[idx] + 15) / 30) * 30
        val h = (rounded / 60) % 24
        val m = rounded % 60
        return "%02d:%02d".format(h, m)
    }
}

internal fun staysPerHourOf(wd: WindowWithDetail): Double {
    val hours = (wd.window.end.value - wd.window.start.value) / 3_600_000.0
    return wd.stays.size / hours
}

internal fun longestStretchOf(wd: WindowWithDetail): Double = wd.stretches.maxOfOrNull { it.minutes } ?: 0.0
