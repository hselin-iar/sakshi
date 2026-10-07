package com.kleos.sakshi.engine.windows

import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * F3: with "learn my study hours" on, the user's usual block is the median start and end of the windows Sakshi inferred from the
 * apps they use (not from study hours they gave). It is only ever offered, as the Mirror's suggested study block, never applied.
 * Minutes are counted from the study day's 04:00 so a block that crosses midnight has a sensible median.
 */
object StudyHoursLearner {
    // DOC 3 says "at least 14 inferred windows" and "differs by 60 minutes or more"; Tuning.kt has no name for either. Flagged local constants.
    const val MIN_INFERRED_WINDOWS = 14
    const val MIN_DIFFERENCE_MIN = 60
    private const val DAY_MIN = 1440
    private const val DAY_START_MIN = 4 * 60

    fun learn(windows: List<Window>, zone: ZoneId): StudyBlock? {
        val inferred = windows.filter { it.source == WindowSource.INFERRED && !it.partial }
        if (inferred.size < MIN_INFERRED_WINDOWS) return null
        val starts = inferred.map { fromDayStart(minuteOfDay(it.start.value, zone)) }
        val lengths = inferred.map { ((it.end.value - it.start.value) / 60_000L).toInt() }
        val start = median(starts)
        val end = start + median(lengths)
        return StudyBlock(toMinuteOfDay(start), toMinuteOfDay(end))
    }

    /** The learned block, if it is not already what the user saved (any saved block within an hour at both ends counts as the same). */
    fun suggest(learned: StudyBlock?, saved: List<StudyBlock>): StudyBlock? {
        if (learned == null) return null
        val same = saved.any { circular(it.startMinute, learned.startMinute) < MIN_DIFFERENCE_MIN && circular(it.endMinute, learned.endMinute) < MIN_DIFFERENCE_MIN }
        return if (same) null else learned
    }

    private fun minuteOfDay(ms: Long, zone: ZoneId): Int {
        val t = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime()
        return t.hour * 60 + t.minute
    }
    private fun fromDayStart(minute: Int) = ((minute - DAY_START_MIN) % DAY_MIN + DAY_MIN) % DAY_MIN
    private fun toMinuteOfDay(sinceDayStart: Int) = (sinceDayStart + DAY_START_MIN) % DAY_MIN
    private fun circular(a: Int, b: Int): Int { val d = abs(a - b) % DAY_MIN; return minOf(d, DAY_MIN - d) }
    private fun median(v: List<Int>): Int = v.sorted().let { it[it.size / 2] }
}
