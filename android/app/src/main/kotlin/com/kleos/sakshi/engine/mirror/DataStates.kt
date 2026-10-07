package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F17: flags are data, sentences are fixed wording for exactly those seven
 * flags — this is NOT the general SentenceBuilder (T2.14); INTERNAL_PARTIAL
 * is used elsewhere and has no F17 sentence, so it never appears here.
 * A number that cannot be computed is null, never zero.
 */
object DataStates {
    fun flags(
        notificationAccessGranted: Boolean,
        everHadListenerSession: Boolean,
        listenerCoverageFraction: Double,
        notSeenDays: Int,
        pausedDays: Int,
        nonPartialWindowCount: Int,
        validDayCount: Int,
        weekWindowMinutes: Double,
        baselineMeanWeeklyMinutes: Double?,
        provisional: Boolean,
    ): List<DataFlag> {
        val result = mutableListOf<DataFlag>()

        if (!notificationAccessGranted || !everHadListenerSession) {
            result += DataFlag.PING_OFF
        } else if (listenerCoverageFraction < Tuning.PING_COVERAGE_PARTIAL) {
            result += DataFlag.PARTIAL_PING
        }
        if (notSeenDays > 0) result += DataFlag.NOT_SEEN
        if (pausedDays > 0) result += DataFlag.PAUSED
        if (nonPartialWindowCount < 4 || validDayCount == 0) result += DataFlag.TOO_LITTLE_DATA
        if (baselineMeanWeeklyMinutes != null && weekWindowMinutes < Tuning.UNUSUAL_WEEK_WINDOW_MIN_RATIO * baselineMeanWeeklyMinutes) {
            result += DataFlag.UNUSUAL_WEEK
        }
        if (provisional) result += DataFlag.FIRST_LOOK

        return result
    }

    fun sentences(flags: List<DataFlag>, notSeenDays: Int, pausedDays: Int, lastPingDaysAgo: Int?): List<String> =
        flags.mapNotNull { flag ->
            when (flag) {
                DataFlag.PING_OFF -> "Ping awareness is off, so I can't tell a ping from a reach."
                DataFlag.PARTIAL_PING ->
                    "Ping awareness is partial; I last heard a ping $lastPingDaysAgo days ago. " +
                        "Stays I could not check are left out of the ping count."
                DataFlag.NOT_SEEN ->
                    "I did not see $notSeenDays days this week (the phone was off, or I was away longer " +
                        "than Android keeps data). They are left out."
                DataFlag.PAUSED -> "Collection was paused for $pausedDays days this week."
                DataFlag.TOO_LITTLE_DATA -> "Too little data this week to say much."
                DataFlag.UNUSUAL_WEEK -> "This looks like an unusual week, so I have left it out of the trends."
                DataFlag.FIRST_LOOK -> "These are your last few days as Android kept them."
                DataFlag.INTERNAL_PARTIAL -> null
            }
        }
}
