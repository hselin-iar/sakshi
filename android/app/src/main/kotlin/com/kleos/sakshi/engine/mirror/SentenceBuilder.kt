package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.metrics.Word
import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekStart
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Every sentence the product says about the user's data, and nowhere else (LC-8). Flutter and the host only show what they
 * are given. The voice: an observation with its evidence count, one plain action when there is one, no judgement and no
 * lecture. CopyRulesTest holds every template here to the forbidden-word list and the "!" ban.
 *
 * App names never appear: an app is only ever "one app" or "that app", the subject of a count.
 */
object SentenceBuilder {

    // ---------- small formatters ----------
    private fun whole(x: Double): Int = x.roundToInt()
    private fun minutes(x: Double): String = whole(x).let { if (it == 1) "1 minute" else "$it minutes" }
    private fun percent(share: Double): String = "${(share * 100).roundToInt()}%"
    private fun decimal(x: Double): String = "%.1f".format(Locale.ROOT, x)
    private fun clock(hour: Int): String = "%02d:00".format(Locale.ROOT, ((hour % 24) + 24) % 24)
    private fun times(perHour: Double): String = whole(perHour).let { if (it == 1) "about once an hour" else "about $it times an hour" }
    private fun count(n: Int, one: String, many: String): String = if (n == 1) "1 $one" else "$n $many"

    /** Every pattern line ends with its evidence count. */
    private fun withEvidence(sentence: String, windows: Int, days: Int): String =
        "$sentence (based on ${count(windows, "window", "windows")} over ${count(days, "day", "days")})."

    // ---------- the Mirror's headline and parts (F6) ----------
    fun headline(stretchMin: Double, returnMin: Double, word: Word): String {
        val relation = when (word) {
            Word.STEADIER -> "steadier than"
            Word.STEADY -> "close to"
            Word.WAVERING -> "less steady than"
        }
        return "You held ${whole(stretchMin)}-minute stretches and ${whole(returnMin)}-minute returns; $relation your starting normal."
    }

    fun provisionalHeadline(stretchMin: Double, staysPerHour: Double): String =
        "Your last few days: you held about ${minutes(stretchMin)} at a stretch. Something pulled you away ${times(staysPerHour)}."

    fun learningHeadline(): String = "I am still learning your starting normal."

    fun learningLine(validDays: Int, needed: Int): String =
        "I have seen ${count(validDays, "day", "days")} of the $needed I need to learn your starting normal."

    fun tooLittleHeadline(): String = "There is not enough from this week to say much yet."

    fun stretchLine(stretchMin: Double, longestMin: Double?): String {
        val base = "You stayed in your work apps for about ${minutes(stretchMin)} at a stretch"
        return if (longestMin != null) "$base (longest ${whole(longestMin)})." else "$base."
    }

    fun staysLine(staysPerHour: Double): String = "Something pulled you away ${times(staysPerHour)}."
    fun returnLine(returnMin: Double): String = "It took you about ${minutes(returnMin)} to get back."
    fun quietLine(quietShare: Double): String = "Your phone was quiet for ${percent(quietShare)} of your work time."

    /** Gentle mode's headline, and the Mirror's return line. */
    fun gentleReturnLine(returnMin: Double): String = "After a stay, it took you about ${minutes(returnMin)} to get back."

    fun stonesLine(total: Int, stones: Int, selfStarted: Int): String = when {
        stones + selfStarted == 0 -> "I could not tell which of your ${count(total, "stay", "stays")} began with a ping."
        selfStarted > stones -> "Most of your stays began with no ping."
        else -> "$stones of your $total stays began with a ping."
    }

    fun clearHourLine(startHour: Int, endHour: Int, stretchMin: Double): String =
        "Between ${clock(startHour)} and ${clock(endHour)} your stretches average ${minutes(stretchMin)}."

    fun nothingToFix(): String = "Nothing to fix this week."
    fun notEnoughForSuggestion(): String = "I need a few more days before I can say anything about what to try."

    // ---------- patterns (P1 to P5, cross-day, clustering) ----------
    fun patternLine(p: Pattern): String {
        val body = when (p.kind) {
            PatternKind.RHYTHM -> rhythm(p)
            PatternKind.TREND -> trend(p)
            PatternKind.SHIFT -> shift(p)
            PatternKind.SHAPE -> shape(p)
            PatternKind.BREAK_POINT -> breakPoint(p)
            PatternKind.CROSS_DAY -> crossDay(p)
        }
        return withEvidence(body, p.evidenceWindows, p.evidenceDays)
    }

    private fun rhythm(p: Pattern): String {
        val (part, week) = (p.args["cell"] ?: "").split("-").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
        val when_ = when (part) {
            "MORNING" -> "morning"; "AFTERNOON" -> "afternoon"; "EVENING" -> "evening"; "NIGHT" -> "late-night"; else -> "work"
        }
        val days = if (week == "WEEKEND") " on weekends" else if (week == "WEEKDAY") " on weekdays" else ""
        val more = if (p.args["direction"] == "CLEAR") "fewer" else "more"
        return "Your $when_ windows$days tend to have $more pulls away than your other windows"
    }

    private fun partNoun(part: String?): String = when (part) {
        "stretch" -> "stretches"; "stays" -> "pulls away"; "return" -> "return time"; "quiet" -> "quiet time"; else -> "pattern"
    }

    private fun trend(p: Pattern): String {
        val better = p.args["direction"] == "BETTER"
        val text = when (p.args["part"]) {
            "stretch" -> if (better) "Your stretches have been getting longer" else "Your stretches have been getting shorter"
            "stays" -> if (better) "Fewer things have been pulling you away" else "More things have been pulling you away"
            "return" -> if (better) "Your return time has been getting shorter" else "Your return time has been getting longer"
            "quiet" -> if (better) "Your phone has been quieter during work time" else "Your phone has been less quiet during work time"
            else -> "This pattern has been changing"
        }
        return "$text over the last four weeks"
    }

    private fun shift(p: Pattern): String {
        val up = p.args["direction"] == "UP"
        val week = (p.args["weekIndex"]?.toIntOrNull() ?: 0) + 1
        val text = when (p.args["part"]) {
            "stretch" -> if (up) "your stretches have been longer than before" else "your stretches have been shorter than before"
            "stays" -> if (up) "things have pulled you away more often than before" else "things have pulled you away less often than before"
            "return" -> if (up) "your return time has been longer than before" else "your return time has been shorter than before"
            "quiet" -> if (up) "your phone has been quieter than before" else "your phone has been less quiet than before"
            else -> "your ${partNoun(p.args["part"])} changed"
        }
        return "Since week $week, $text"
    }

    private fun shape(p: Pattern): String {
        if (p.key.startsWith("cluster:")) {
            val size = p.args["size"]?.toIntOrNull() ?: 0
            val group = if (p.args["name"].equals("Pinged", ignoreCase = true)) "began with pings" else "began with you reaching for the phone"
            return "${count(size, "window", "windows")} in a similar time slot $group"
        }
        val held = p.args["held"]?.toIntOrNull() ?: 0
        val pinged = p.args["pinged"]?.toIntOrNull() ?: 0
        val reached = p.args["reached"]?.toIntOrNull() ?: 0
        val total = held + pinged + reached
        val after = p.args["reachedStartsAfter"]?.takeIf { it != "null" }
        val tail = if (after != null) "; the reaching mostly began after $after" else ""
        return "Of your $total windows, $held had no pull away, $pinged were pulled by pings and $reached by you reaching for the phone$tail"
    }

    private fun breakPoint(p: Pattern): String = breakPointText(p.args)

    private fun breakPointText(args: Map<String, String>): String {
        val start = args["bandStartMin"]?.toIntOrNull() ?: 0
        val share = args["bandShare"]?.toDoubleOrNull() ?: 0.0
        val cause = when {
            (args["cause"] ?: "").startsWith("PING:") -> ", usually just after a ping"
            args["cause"] == "SELF" -> ", usually when you reach for the phone yourself"
            else -> ""
        }
        return "${percent(share)} of your stretches end between $start and ${start + 5} minutes$cause"
    }

    private fun crossDay(p: Pattern): String {
        val nights = p.args["lateNights"]?.toIntOrNull() ?: 0
        val shorter = p.args["shorterByMin"]?.toDoubleOrNull() ?: 0.0
        val after = p.args["lateAfter"] ?: "late"
        return "On the ${count(nights, "night", "nights")} the screen stayed on past $after, the next day's first stretch was about ${minutes(shorter)} shorter"
    }

    // ---------- suggestions (S1 to S9) and observations (S10 to S12) ----------
    /** One plain line, built from the facts the rule put in `args`. */
    fun suggestionLine(c: Candidate): String {
        val a = c.args["count"]?.toIntOrNull() ?: 0
        val b = c.args["of"]?.toIntOrNull() ?: 0
        return when (c.kind) {
            SuggestionKind.S1 -> "Between ${clock(c.args["startHour"]?.toIntOrNull() ?: 0)} and ${clock(c.args["endHour"]?.toIntOrNull() ?: 0)} " +
                "your stretches are longer and you are pulled away less than in the rest of your day."
            SuggestionKind.S2 -> "One app is behind $a of your last $b stays."
            SuggestionKind.S3 -> "$a of your last $b stays began with no ping."
            SuggestionKind.S4 -> "One app is behind $a of your last $b ping-driven stays."
            SuggestionKind.S5 -> "$a of your last $b returns took more than one and a half times your usual time to get back."
            SuggestionKind.S6 -> "$a of your last $b windows had a pull away in the first ${c.args["minutes"] ?: "5"} minutes."
            SuggestionKind.S7 -> "One app outside your work set took about ${c.args["pct"] ?: "0"}% of your window time."
            SuggestionKind.S8 -> crossDayNote(c)
            SuggestionKind.S9 -> breakPointText(c.args) + "."
            SuggestionKind.S10, SuggestionKind.S11, SuggestionKind.S12 -> observationLine(c)
        }
    }

    private fun crossDayNote(c: Candidate): String {
        val nights = c.args["lateNights"]?.toIntOrNull() ?: 0
        val after = c.args["lateAfter"] ?: "late"
        return "On $nights of the last 14 nights the screen stayed on past $after, and the next day's first stretch was shorter."
    }

    /** The single action under a suggestion. Empty when there is none. */
    fun actionLabel(action: ActionType): String = when (action) {
        ActionType.OPEN_NOTIFICATION_SETTINGS -> "Review its notifications"
        ActionType.MOVE_ICON -> "Move its icon to a second screen"
        ActionType.PHONE_FACE_DOWN -> "Put the phone face down while you work"
        ActionType.FIRST_THING_HARDEST -> "Put the hardest thing first"
        ActionType.LEAVE_PAGE_OPEN -> "Leave the page you were on open"
        ActionType.PHONE_IN_OTHER_ROOM -> "Keep the phone in another room while you start"
        ActionType.ADD_TO_WORK_SET -> "Add it to your work set"
        ActionType.SCREEN_OFF_BY_0030 -> "Turn the screen off by 00:30"
        ActionType.PLAN_SHORT_PUT_DOWN -> "Plan a short put-down at that mark"
        ActionType.NONE -> ""
    }

    /** S10 to S12: exempt observations, no action. */
    fun observationLine(c: Candidate): String = when (c.kind) {
        SuggestionKind.S10 -> "You looked at other apps ${c.args["glances"] ?: "0"} times this week for under 30 seconds each, and ${c.args["stays"] ?: "0"} of those looks became stays."
        SuggestionKind.S11 -> lapseLine(c.args["days"]?.toIntOrNull() ?: 0)
        SuggestionKind.S12 -> winLine(c)
        else -> ""
    }

    private fun winLine(c: Candidate): String {
        val values = (c.args["values"] ?: "").split(",").mapNotNull { it.toDoubleOrNull() }
        val change = if (values.size >= 4) {
            val last = (values[2] + values[3]) / 2.0
            val prior = (values[0] + values[1]) / 2.0
            if (prior != 0.0) kotlin.math.abs((last - prior) / prior) else null
        } else null
        val p = change?.let { percent(it) }
        val body = when (c.args["part"]) {
            "stretch" -> if (p != null) "Your stretches got $p longer over two weeks" else "Your stretches got longer over two weeks"
            "stays" -> if (p != null) "Pulls away dropped by $p over two weeks" else "Pulls away dropped over two weeks"
            "return" -> if (p != null) "Your return time got $p shorter over two weeks" else "Your return time got shorter over two weeks"
            "quiet" -> if (p != null) "Your phone was $p quieter during work time over two weeks" else "Your phone was quieter during work time over two weeks"
            else -> "One of your parts improved over two weeks"
        }
        val w = c.args["evidenceWindows"]?.toIntOrNull()
        val d = c.args["evidenceDays"]?.toIntOrNull()
        return if (w != null && d != null) withEvidence(body, w, d) else "$body."
    }

    fun lapseLine(days: Int): String = "You were away ${count(days, "day", "days")}. Your starting normal is still here."

    // ---------- verdicts (F12) ----------
    private fun metricName(t: TargetMetric): String = when (t) {
        TargetMetric.STAYS_PER_HOUR_FROM_PKG -> "The number of stays per hour from that app"
        TargetMetric.SELF_STARTED_PER_HOUR -> "The number of self-started stays per hour"
        TargetMetric.PINGS_FROM_PKG_IN_WINDOWS -> "The number of stays per hour that began with a ping from that app"
        TargetMetric.MEDIAN_RETURN -> "Your usual time to get back (in minutes)"
        TargetMetric.FLINCH_RATE -> "The share of windows with a pull away in the first minutes"
        TargetMetric.WORKSET_COVERAGE -> "The share of window time in apps outside your work set"
        TargetMetric.STRETCH_IN_SLOT -> "Your usual stretch length (in minutes)"
        TargetMetric.NEXT_DAY_FIRST_STRETCH -> "Your first stretch the next day (in minutes)"
        TargetMetric.PUTDOWN_SHARE -> "The share of stretches that ended in a put-down"
    }

    private fun metricValue(t: TargetMetric, v: Double): String = when (t) {
        TargetMetric.FLINCH_RATE, TargetMetric.WORKSET_COVERAGE, TargetMetric.PUTDOWN_SHARE -> percent(v)
        else -> decimal(v)
    }

    /** Null for PENDING: nothing to say until the two weeks are over. Every line says what it cannot say. */
    fun verdictLine(verdict: Verdict, target: TargetMetric, before: Double?, after: Double?, approxMix: Boolean): String? {
        val honest = "This is correlation, not cause; it could also have been a lighter fortnight."
        val approx = if (approxMix) " Weekday and weekend windows were not balanced, so this comparison is approximate." else ""
        return when (verdict) {
            Verdict.PENDING -> null
            Verdict.MOVED -> if (before != null && after != null)
                "${metricName(target)} went from ${metricValue(target, before)} to ${metricValue(target, after)} in the two weeks after you tried it. $honest$approx" else null
            Verdict.NO_CHANGE -> if (before != null && after != null)
                "${metricName(target)} was ${metricValue(target, before)} before and ${metricValue(target, after)} after the two weeks you tried it, which is not a clear change. This is correlation, not cause.$approx" else null
            Verdict.TOO_LITTLE -> "I did not see enough finished windows in those two weeks to say whether anything changed."
            Verdict.UNCLEAR -> "Those two weeks included an unusual week or a change to your apps, so I cannot say whether anything changed."
        }
    }

    // ---------- the teacher line (F14) ----------
    fun teacherLine(opensThisWeek: Int, opensPrevWeek: Int?, prevWeekNumber: Int?): String {
        val now = if (opensThisWeek == 1) "once" else "$opensThisWeek times"
        return if (opensPrevWeek != null && prevWeekNumber != null && opensThisWeek < opensPrevWeek) {
            val then = if (opensPrevWeek == 1) "once" else "$opensPrevWeek times"
            "You opened me $then in week $prevWeekNumber and $now this week."
        } else "You opened me $now this week."
    }

    // ---------- the Lake (F7) ----------
    fun lakePhrase(state: LakeState, gentle: Boolean): String = when (state) {
        LakeState.STILL -> if (gentle) "Calm." else "Still water."
        LakeState.RIPPLED -> if (gentle) "Some ripples." else "A few ripples."
        LakeState.CHOPPY -> if (gentle) "Some waves." else "Choppy water."
        LakeState.LEARNING -> "Learning your normal."
        LakeState.NO_DATA -> "Nothing to show yet."
    }

    // ---------- Today (F8) ----------
    fun todayLine(finishedWindows: Int, stretchMin: Double?): String = when {
        finishedWindows == 0 -> "No finished window yet today."
        stretchMin == null -> "Today so far: ${count(finishedWindows, "finished window", "finished windows")}."
        else -> "Today so far: ${count(finishedWindows, "finished window", "finished windows")}. You held ${whole(stretchMin)}-minute stretches."
    }

    fun shapeWord(shape: Shape?): String? = when (shape) {
        Shape.HELD -> "Held"; Shape.PINGED -> "Pinged"; Shape.REACHED -> "Reached"; null -> null
    }

    // ---------- honest data states (F17) ----------
    fun dataLines(flags: List<DataFlag>, notSeenDays: Int, pausedDays: Int, lastPingDaysAgo: Int?): List<String> {
        val fixed = DataStates.sentences(flags, notSeenDays, pausedDays, lastPingDaysAgo)
        return if (DataFlag.INTERNAL_PARTIAL in flags) fixed + "I could not read part of this week." else fixed
    }

    // ---------- What I see (F10) ----------
    fun whatISeeLines(listenerCoverage7d: Double?, lastRunMinutesAgo: Int?, runs7d: Int): List<String> = listOf(
        "I can see which app is in front and when (package names and times). I never read what is on your screen.",
        "I note when an app sends a notification: its name, time and category. Never the words.",
        "Everything is stored on this phone and nowhere else.",
        if (listenerCoverage7d != null) "Heard pings for ${percent(listenerCoverage7d)} of the last 7 days." else "I have not heard any pings yet.",
        if (lastRunMinutesAgo != null) "Last background run ${minutes(lastRunMinutesAgo.toDouble())} ago; ${count(runs7d, "run", "runs")} in the last 7 days."
        else "I have not run in the background yet.",
    )

    // ---------- week labels and the Saying shelf ----------
    /** "5–11 Oct", or "28 Sep–4 Oct" when the week crosses a month. */
    fun weekLabel(weekStart: WeekStart, zone: ZoneId = ZoneId.of("UTC")): String {
        val first = LocalDate.ofEpochDay(weekStart.studyDay.epochDay)
        val last = first.plusDays(6)
        fun month(d: LocalDate) = d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return if (first.month == last.month) "${first.dayOfMonth}–${last.dayOfMonth} ${month(last)}"
        else "${first.dayOfMonth} ${month(first)}–${last.dayOfMonth} ${month(last)}"
    }

    /** Computed from the tier letter, never stored. No tier D saying can be labelled "his own writing". */
    fun tierLabel(tier: Char): String = when (tier) {
        'A' -> "his own writing or letter"
        'B' -> "recorded lecture"
        'C' -> "reported by others"
        else -> "type not resolved in the Outcome Map"
    }
}
