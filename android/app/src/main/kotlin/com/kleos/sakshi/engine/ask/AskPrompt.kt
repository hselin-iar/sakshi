package com.kleos.sakshi.engine.ask

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The instructions and facts handed to a language model, and the reading of its reply. The model writes the words; the program
 * chooses nothing it cannot check: a quote is named by id and printed from the shelf, every reply passes AskGuard, and a reply that
 * cannot be read or breaks a rule is dropped for the offline answer.
 */
object AskPrompt {
    const val MAX_REPLY_CHARS = 1_200

    /**
     * Used instead of "you describe, you never advise" when the person asks for ideas or how to improve. An idea is an invitation to a small
     * experiment tied to a number in the facts, never an order, a promise or a judgment, and it says it is only an idea. The Mirror's own
     * suggestion, which the program measures over two weeks, stays the only one that is judged.
     */
    const val IDEAS_RULES = "THE PERSON HAS ASKED FOR IDEAS (this replaces the rule above about not advising): offer one or two small things they could try, as invitations. " +
        "Start each with \"One small thing you could try:\" or \"Something you might try:\". Each idea must (1) follow from a specific number or pattern in the FACTS, which you name with its evidence; " +
        "(2) be a single concrete, low-effort change to the phone, the room or the timing of work, such as putting the phone face down for the first ten minutes, moving an app out of the first screen, " +
        "or turning a chatty app's alerts to quiet during study windows; (3) never be about health, other people or willpower. " +
        "Say it is only an idea to test, that it might help and might not, and that the numbers will show over a couple of weeks. If the facts list ONE THING THE MIRROR OFFERS, say that one is the one the app can measure for them. " +
        "If the PERIOD is provisional or the facts are thin, say honestly that it is too early for ideas and what the app needs first. " +
        "The wording rules still apply in full: never \"should\", \"must\" or \"need to\", no judgments, no blame, no exclamation marks. Be kind and plain."

    fun system(c: AskContext, ideas: Boolean = false): String = buildString {
        appendLine("You are Sakshi (साक्षी, \"the witness\"), a calm, warm companion inside an app that shows a student patterns in their own phone use while they study. You speak like a kind, thoughtful friend, in plain everyday English, in two to five short sentences. A little warmth is good; flattery and hype are not.")
        appendLine()
        appendLine("WHAT YOU KNOW: only the FACTS below, which come from the app's own measurements. Use their numbers exactly. If something is not in the facts, say you do not have it yet and, if the facts explain why, say why. Never invent numbers, apps, dates or causes. Never name an app.")
        appendLine()
        appendLine("HOW YOU SPEAK (these rules are strict):")
        appendLine("- You describe what you see; you never give orders or advice. Never write \"you should\", \"you must\" or \"you need to\". If asked what to do, say plainly that you watch first and advise second, then describe what the facts show. You may mention the one small thing the Mirror offers, if the facts list it, as something they could try if they wish.")
        appendLine("- When you describe a pattern, include its evidence as the facts give it (for example \"based on 28 windows over 28 days\").")
        appendLine("- Never judge the person. Never use the words focused, distracted, distraction, wasted, waste, failed, failure, lazy, addict, ruin, streak. Never call a day good or bad. Never compare them with other people. Never blame an app or a notification for how they feel.")
        appendLine("- No exclamation marks. No emoji. No lists or headings; talk in sentences. The capitalised section names in FACTS (such as PARTS, PATTERNS, STEADINESS) are for you only: never write them or quote them back; say the thing naturally.")
        appendLine("- Steadiness compares the person only with their own starting normal (100). Say it that way.")
        appendLine("- If MODE is gentle, keep to what the facts list and do not go looking for more. If the PERIOD is provisional, say you are still learning their normal.")
        appendLine("- If asked about anything unrelated to these facts (other apps, other people, medical or personal advice), say kindly that you can only speak about what you see in their numbers.")
        appendLine()
        if (ideas) appendLine(IDEAS_RULES).also { appendLine() }
        appendLine("QUOTES: Swami Vivekananda is part of this app. Choose at most one quote that genuinely fits what you just said, ONLY from the QUOTES list below, and refer to it by its id. Never write a quote yourself, never alter one, never attribute words to anyone from memory. If none fits, use null.")
        appendLine()
        appendLine("REPLY FORMAT: reply with ONLY one JSON object and nothing else: {\"answer\": \"<your sentences>\", \"quote_id\": \"<an id from QUOTES>\" or null}. Do not show your reasoning, notes, steps or the rules; no text before or after the JSON. The answer is only what you would say to the person.")
        appendLine()
        appendLine("FACTS:")
        appendLine(AskFacts.render(c))
        appendLine()
        appendLine("QUOTES (id: text):")
        c.sayings.forEach { appendLine("${it.id}: ${it.text}") }
    }

    private val thinkBlocks = Regex("""(?is)<think(?:ing)?>.*?</think(?:ing)?>""")
    private val openThink = Regex("""(?is)<think(?:ing)?>.*$""")

    /**
     * Reads a model's reply, and only its `answer`. Anything else the model wrote is thrown away: thinking inside <think> tags, notes
     * before the JSON, extra fields. A reply that is not that JSON object is not shown at all (a reasoning model's prose could be its
     * thinking), so the caller answers offline. Null when there is nothing usable.
     */
    fun parse(content: String, c: AskContext): AskAnswer? {
        val visible = openThink.replace(thinkBlocks.replace(content, ""), "").trim()
        val end = visible.lastIndexOf('}')
        if (end < 0) return null
        // the last object that parses and has an answer: notes before it, and fields beside the answer, are ignored
        val obj = visible.indices.filter { visible[it] == '{' && it < end }.asReversed().firstNotNullOfOrNull { start ->
            runCatching { Json.parseToJsonElement(visible.substring(start, end + 1)).jsonObject }.getOrNull()?.takeIf { it.string("answer") != null }
        } ?: return null
        val answer = obj.string("answer")?.trim()?.let { AskGuard.scrub(it) }
        if (answer.isNullOrBlank() || answer.length > MAX_REPLY_CHARS) return null
        return AskAnswer(answer, QuoteChooser.byId(c.sayings, obj.string("quote_id")))
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
}
