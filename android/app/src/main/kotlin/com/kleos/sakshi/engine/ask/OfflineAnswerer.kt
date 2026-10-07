package com.kleos.sakshi.engine.ask

import com.kleos.sakshi.engine.model.MirrorView
import com.kleos.sakshi.engine.ports.Randomness

/**
 * Answers a question from the Mirror, Today and the Lake with no network at all: it picks the one subject the question is about and
 * says it in the app's own sentences (which carry their evidence counts), with a friendly frame and a saying from the shelf. It is
 * what the demo falls back on when the language service cannot be reached, and it keeps every copy rule: observations, never advice.
 */
object OfflineAnswerer {
    enum class Intent { GREETING, THANKS, QUOTE, ABOUT, SUGGEST, STEADINESS, STRETCH, PULLS, RETURN, QUIET, PINGS, TODAY, LAKE, PATTERNS, RESULT, TEACHER, LAPSE, WEEK }

    private val rules: List<Pair<Intent, Regex>> = listOf(
        Intent.QUOTE to Regex("""quote|vivekananda|swami|wisdom|inspir|motivat|saying|teach me"""),
        Intent.THANKS to Regex("""\bthank|\bthanks\b"""),
        Intent.GREETING to Regex("""^\s*(hi|hello|hey|namaste|namaskar|good (morning|afternoon|evening))\b"""),
        Intent.ABOUT to Regex("""what is (this|sakshi)|who are you|what do you do|how does this work|about (you|this|sakshi)|what can you"""),
        Intent.SUGGEST to Regex("""what should|should i|advice|\btips?\b|\bideas?\b|suggest|\btry\b|improve|better|\bfix\b|help me|what can i do"""),
        Intent.RESULT to Regex("""worked|did it (work|help)|result|verdict|experiment|moved"""),
        Intent.LAPSE to Regex("""lapse|missed|been away|a break|gap"""),
        Intent.TEACHER to Regex("""open(ed)? (this|the) app|use (this|the) app|teacher|how often do i (open|use) (sakshi|this)"""),
        Intent.STEADINESS to Regex("""steady|steadier|wavering|score|\bnumber\b|\b1[0-9]{2}\b"""),
        Intent.PATTERNS to Regex("""pattern|trend|habit|usually|always|rhythm|clear hour|best time"""),
        Intent.PINGS to Regex("""\bping|notification|message|buzz|\bstone|\bwave|reach"""),
        Intent.RETURN to Regex("""\breturn|come back|get back|back to"""),
        Intent.QUIET to Regex("""quiet|face down|put (it|the phone) down|screen off"""),
        Intent.PULLS to Regex("""\bpull|\bstay|away|interrupt|switch|how often|distract|phone"""),
        Intent.STRETCH to Regex("""stretch|how long|\bhold|concentrat|\bfocus"""),
        Intent.TODAY to Regex("""today|tonight|this evening|right now|so far"""),
        Intent.LAKE to Regex("""\blake|water|widget"""),
        Intent.WEEK to Regex("""week|how (did|am|was|is)|how's|hows|summary|overview|progress|doing"""),
    )

    fun intentOf(question: String): Intent? {
        val q = question.lowercase()
        return rules.firstOrNull { (_, r) -> r.containsMatchIn(q) }?.first
    }

    fun answer(question: String, c: AskContext, random: Randomness): AskAnswer {
        val intent = intentOf(question)
        val m = c.mirror
        val (text, tags) = when (intent) {
            Intent.GREETING -> "Namaste. I am Sakshi, the witness. I can tell you what I see in your own numbers: your stretches, how often you were pulled away, how quickly you came back, today, and the Lake. What would you like to look at?" to listOf("Name root", "First Look")
            Intent.THANKS -> "You are welcome. I am here whenever you want to look again." to listOf("Watch first")
            Intent.QUOTE -> "Here is a passage to sit with." to emptyList()
            Intent.ABOUT -> about(c) to listOf("Watch first", "Name root")
            Intent.SUGGEST -> suggest(m) to listOf("Watch first", "advise second")
            Intent.STEADINESS -> steadiness(m) to listOf("Steadiness")
            Intent.STRETCH -> part(m.parts?.lines?.stretch, m, "how long you stayed with your work at a stretch") to listOf("Stretch")
            Intent.PULLS -> pulls(m) to listOf("Glance reassurance", "Stone and wave")
            Intent.RETURN -> part(m.parts?.lines?.ret, m, "how quickly you came back after being pulled away") to listOf("Return")
            Intent.QUIET -> part(m.parts?.lines?.quiet, m, "how much of your work time the phone was quiet") to listOf("Quiet", "rhythm")
            Intent.PINGS -> pings(m) to listOf("Stone and wave")
            Intent.TODAY -> today(c) to listOf("First Look")
            Intent.LAKE -> lake(c) to listOf("the Lake")
            Intent.PATTERNS -> patterns(m) to listOf("rhythm", "Stretch")
            Intent.RESULT -> result(m) to listOf("Watch first")
            Intent.TEACHER -> (m.teacher?.line ?: "I only start counting how often you open Sakshi from your second week, so there is nothing to say yet.") to listOf("The Teacher Leaves")
            Intent.LAPSE -> (m.lapseLine ?: "I do not see a break to speak of. The days I can read look continuous.") to listOf("Lapse and return", "After a lapse")
            Intent.WEEK, null -> week(m, c, intent == null) to listOf("Watch first", "Return")
        }
        return AskAnswer(text, QuoteChooser.choose(c.sayings, tags, random.fork(question.hashCode().toLong())))
    }

    // ---- the answers ----

    private fun week(m: MirrorView, c: AskContext, unknown: Boolean): String {
        val lead = if (m.provisional) "I am still getting to know your normal, so this is a first look at ${m.weekLabel.lowercase()}." else "Here is how ${m.weekLabel} looked."
        val body = listOfNotNull(m.headline.takeIf { it.isNotBlank() }, m.stones?.line, m.returnLine.takeIf { m.gentle }).joinToString(" ")
        val note = m.dataLines.firstOrNull()?.let { " One honest note: $it" }.orEmpty()
        val ask = if (unknown) " I could not tell what you were asking, so I gave you the whole picture. You can also ask about your stretches, pulls, returns, quiet, pings, today or the Lake." else ""
        return "$lead $body$note$ask".trim()
    }

    private fun part(line: String?, m: MirrorView, what: String): String =
        if (line != null) "Here is $what. $line"
        else if (m.gentle) "In gentle mode I keep to a few things on purpose: ${m.returnLine ?: m.headline}"
        else "I do not have $what yet. ${m.dataLines.firstOrNull() ?: "I need a few more days of reading."}"

    private fun pulls(m: MirrorView): String {
        val p = m.parts ?: return part(null, m, "how often something pulled you away")
        val staysLine = p.lines.stays ?: return part(null, m, "how often something pulled you away")
        val glance = if (p.glances > 0) " There were also ${p.glances} quick looks, which I do not count as being pulled away." else ""
        return "$staysLine$glance"
    }

    private fun pings(m: MirrorView): String =
        m.stones?.let { s ->
            val ripple = s.noRippleRate?.let { " Of the pings that arrived while you worked, ${Math.round(it * 100)}% led to no stay at all." }.orEmpty()
            "${s.line}$ripple"
        } ?: "I do not have enough finished windows to say what started your stays yet."

    private fun steadiness(m: MirrorView): String {
        val s = m.steadiness
            ?: return if (m.gentle) "Gentle mode keeps the Steadiness number out of sight on purpose. What I can say: ${m.returnLine ?: m.headline}"
            else "There is no Steadiness number yet. ${m.dataLines.firstOrNull() ?: "I am still learning your starting normal."}"
        return "Your Steadiness is ${s.value}, which I call ${s.word}. It only compares you with your own starting normal, which is 100; nobody else is in this number. ${m.headline}"
    }

    private fun patterns(m: MirrorView): String = when {
        m.gentle -> "In gentle mode I leave patterns out on purpose. ${m.returnLine ?: m.headline}"
        m.patterns.isEmpty() -> "I have not seen a pattern with enough evidence yet. I would rather say nothing than guess." + (m.clearHour?.let { " One thing I can say: ${it.line}" }.orEmpty())
        else -> "Here is what I can say, with the evidence for each: " + (listOfNotNull(m.clearHour?.line) + m.patterns.map { it.line }).joinToString(" ")
    }

    private fun suggest(m: MirrorView): String = when {
        m.gentle -> "I do not give suggestions in gentle mode. I can tell you what I see: ${m.returnLine ?: m.headline}"
        m.suggestion != null -> "I do not tell people what to do; I watch first. Here is what I see: ${m.suggestion.line} If you ever want to try something, the Mirror offers one small thing: ${m.suggestion.actionLabel}."
        m.nothingToFix -> "There is nothing to fix this week. That is a real answer, and I mean it."
        else -> "I do not tell people what to do; I watch first. I do not have enough days yet to offer even one small thing to try. ${m.dataLines.firstOrNull().orEmpty()}".trim()
    }

    private fun result(m: MirrorView): String =
        m.verdict?.line?.let { "Here is how something you tried turned out. $it" } ?: "Nothing you tried has finished its two weeks yet, so there is no result to read."

    private fun today(c: AskContext): String {
        val windows = c.today.windows
        val detail = if (windows.isEmpty()) "" else " " + windows.joinToString(" ") { w ->
            val parts = listOfNotNull("${w.stays} pulls away", w.stretchMin?.let { "longest stretch ${Math.round(it)} minutes" }, w.returnMin?.let { "came back in ${Math.round(it)} minutes" })
            "One window had ${parts.joinToString(", ")}."
        }
        return c.today.line + detail
    }

    private fun lake(c: AskContext): String = when {
        c.lake.phrase.isBlank() -> "The Lake has nothing to show yet."
        else -> "The Lake says: ${c.lake.phrase} It is only a picture of your last finished window against your own usual, with no numbers on purpose."
    }

    private fun about(c: AskContext): String =
        "I am Sakshi, the witness. I read which app is in front and when, and the time a notification arrives, never what is inside. I turn that into what I see about your stretches, how often you were pulled away and how quickly you came back, always against your own normal." +
            if (c.mirror.isDemo) " What you are looking at now is made-up demo data." else ""
}
