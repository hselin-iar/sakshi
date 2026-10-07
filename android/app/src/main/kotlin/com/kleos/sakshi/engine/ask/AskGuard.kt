package com.kleos.sakshi.engine.ask

/**
 * The copy rules of the whole product (AGENTS.md section 7), applied to anything a language model writes. A reply that breaks one is
 * not shown; the offline answer is shown instead. The same pattern CopyRulesTest holds the app's own sentences to.
 */
object AskGuard {
    private val banned = Regex(
        """\b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\w*|ruin\w*|lost (your )?(focus|concentration)|you (should|must|need to))\b""",
        RegexOption.IGNORE_CASE,
    )

    // The section names of AskFacts: they are for the model only, never for the person.
    private val labels = Regex("""\b(PERIOD|MODE|HEADLINE|STEADINESS|PARTS|PINGS AND REACHING|CLEAR HOUR|PATTERNS|ONE THING THE MIRROR OFFERS|OBSERVATION|NOTHING TO FIX|RESULT OF SOMETHING TRIED|USE OF THIS APP|AFTER A BREAK|RETURN LINE|DATA NOTES|THE LAKE|FACTS|QUOTES)\b""")
    private val labelParenthetical = Regex("""\s*\((?:[A-Z][A-Z' ]{3,})\)""")
    // The openings of thinking out loud, and the tags some models wrap it in.
    private val thinking = Regex("""(?is)^\s*(okay|ok,|hmm|let me|let's|first,? i|we need|the user (is|wants|asks|said)|i need to|i should|i'll (start|need|check)|analysis|reasoning|thinking)\b|<\/?think|\bthe system prompt\b|\bthe facts (say|show|list)\b""")

    /** Takes out a label the model wrote in brackets, such as "(PINGS AND REACHING)". What is left is still checked by [violations]. */
    fun scrub(text: String): String = labelParenthetical.replace(text, "").trim()

    fun violations(text: String): List<String> = buildList {
        banned.findAll(text).forEach { add("word: ${it.value}") }
        labels.find(text)?.let { add("section label: ${it.value}") }
        if (thinking.containsMatchIn(text)) add("thinking out loud")
        if (text.contains('!')) add("exclamation mark")
        if (text.isBlank()) add("empty")
    }

    fun isClean(text: String) = violations(text).isEmpty()
}
