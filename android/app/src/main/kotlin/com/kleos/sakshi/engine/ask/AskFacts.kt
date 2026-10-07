package com.kleos.sakshi.engine.ask

import com.kleos.sakshi.engine.model.TodayWindowView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The facts a language model is given, as plain text built from the views the app already shows. It contains the app's own sentences
 * (with their evidence counts) and numbers; it never contains a package name, an app label, a raw event, a notification or any time
 * but the clock times of finished windows. Gentle mode arrives already applied: a gentle Mirror simply has fewer facts.
 */
object AskFacts {
    fun render(c: AskContext): String = buildString {
        val m = c.mirror
        appendLine("PERIOD: ${m.weekLabel}${if (m.provisional) " (provisional: not enough days yet to compare with the person's own normal)" else ""}")
        appendLine("MODE: ${if (m.gentle) "gentle (only a few things are shown on purpose; do not go looking for more)" else "normal"}")
        if (m.isDemo) appendLine("NOTE: this is made-up demo data for a student called Aarav, Meera or Rohan; it is fine to say so.")
        appendLine("HEADLINE: ${m.headline}")
        m.steadiness?.let {
            appendLine("STEADINESS: ${it.value} (${it.word}). 100 is the person's own starting normal. Under 90 is Wavering, 90 to 110 Steady, over 110 Steadier. It compares the person with themself only.")
        }
        m.parts?.let { p ->
            appendLine("PARTS:")
            listOfNotNull(p.lines.stretch, p.lines.stays, p.lines.ret, p.lines.quiet).forEach { appendLine("- $it") }
            p.inSetShare?.let { appendLine("- ${Math.round(it * 100)}% of work time was in work apps.") }
        }
        m.stones?.let { appendLine("PINGS AND REACHING: ${it.line}") }
        m.clearHour?.let { appendLine("CLEAR HOUR: ${it.line}") }
        if (m.patterns.isNotEmpty()) {
            appendLine("PATTERNS (each with its evidence):")
            m.patterns.forEach { appendLine("- ${it.line}") }
        }
        m.suggestion?.let { appendLine("ONE THING THE MIRROR OFFERS: ${it.line} (action: ${it.actionLabel})") }
        m.observation?.let { appendLine("OBSERVATION: ${it.line}") }
        if (m.nothingToFix) appendLine("NOTHING TO FIX: the Mirror found nothing to suggest this week.")
        m.verdict?.let { appendLine("RESULT OF SOMETHING TRIED: ${it.line}") }
        m.teacher?.let { appendLine("USE OF THIS APP: ${it.line}") }
        m.lapseLine?.let { appendLine("AFTER A BREAK: $it") }
        m.returnLine?.let { appendLine("RETURN LINE: $it") }
        if (m.dataLines.isNotEmpty()) {
            appendLine("DATA NOTES (be honest about these):")
            m.dataLines.forEach { appendLine("- $it") }
        }
        appendLine("TODAY: ${c.today.line}")
        c.today.windows.forEach { appendLine("- ${windowText(it, c.zone)}") }
        if (c.lake.phrase.isNotBlank()) appendLine("THE LAKE (a picture of the last finished window): ${c.lake.phrase}")
    }.trimEnd()

    private fun windowText(w: TodayWindowView, zone: ZoneId): String {
        val f = DateTimeFormatter.ofPattern("HH:mm")
        val from = f.format(Instant.ofEpochMilli(w.start.value).atZone(zone))
        val to = f.format(Instant.ofEpochMilli(w.end.value).atZone(zone))
        val parts = listOfNotNull(
            "${w.stays} pulls away",
            w.stretchMin?.let { "longest stretch ${Math.round(it)} min" },
            w.returnMin?.let { "return ${Math.round(it)} min" },
            w.shape,
        )
        return "Window $from to $to: ${parts.joinToString(", ")}"
    }
}
