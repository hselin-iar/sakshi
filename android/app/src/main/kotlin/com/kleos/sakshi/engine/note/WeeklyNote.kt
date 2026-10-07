package com.kleos.sakshi.engine.note

import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NoteDecision
import com.kleos.sakshi.engine.model.WeekStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class NoteContext(
    val asOf: EpochMs,
    val enabled: Boolean,
    val canPost: Boolean,
    val lastCompletedWeek: WeekStart?,
    val validDaysInThatWeek: Int,
    val lastNoteWeek: WeekStart?,
    val mirrorViewedWeek: WeekStart?,
    val inWindowNow: Boolean,
    val zone: ZoneId,
)

/**
 * F9: the quiet note is posted at most once per week, only for a Mirror that is ready and not yet read, from Monday 08:00 to
 * Wednesday 23:59 of the following week, and never during a study window. Past Wednesday that week's note is skipped for good.
 * The reason is an internal code for logs and tests, not a sentence for the user.
 */
object WeeklyNote {
    const val MIN_VALID_DAYS = 3   // same threshold as a Mirror week (F6 step 1)

    fun decide(c: NoteContext): NoteDecision {
        val week = c.lastCompletedWeek
        return when {
            !c.enabled -> no("DISABLED")
            !c.canPost -> no("NO_PERMISSION")
            week == null -> no("NO_WEEK")
            c.validDaysInThatWeek < MIN_VALID_DAYS -> no("TOO_FEW_DAYS")
            c.lastNoteWeek == week -> no("ALREADY_NOTED")
            c.mirrorViewedWeek == week -> no("ALREADY_VIEWED")
            else -> {
                val local = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(c.asOf.value), c.zone)
                val monday = LocalDate.ofEpochDay(Weeks.next(week).studyDay.epochDay)
                val opens = LocalDateTime.of(monday, LocalTime.of(8, 0))
                val closes = LocalDateTime.of(monday.with(DayOfWeek.WEDNESDAY).plusDays(1), LocalTime.MIDNIGHT)   // Thursday 00:00, exclusive
                when {
                    local < opens -> no("TOO_EARLY")
                    local >= closes -> no("TOO_LATE")
                    c.inWindowNow -> no("IN_WINDOW")
                    else -> NoteDecision(post = true, reason = null)
                }
            }
        }
    }

    private fun no(reason: String) = NoteDecision(post = false, reason = reason)
}
