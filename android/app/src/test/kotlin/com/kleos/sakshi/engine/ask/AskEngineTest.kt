package com.kleos.sakshi.engine.ask

import com.kleos.sakshi.engine.ask.OfflineAnswerer.Intent
import com.kleos.sakshi.engine.demo.DemoReplay
import com.kleos.sakshi.engine.demo.DemoRig
import com.kleos.sakshi.engine.demo.PERSONAS
import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.testkit.FakeSayingShelf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.TimeZone

class AskEngineTest {
    private val saved = TimeZone.getDefault()
    @Before fun zone() = TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    @After fun restore() = TimeZone.setDefault(saved)

    /** The real shelf the app ships, so these tests see the real quotes and their tags. */
    private val shelf: List<Saying> = Json.parseToJsonElement(File("src/main/assets/sakshi/sayings.json").readText()).jsonArray.map { e ->
        val o = e.jsonObject
        fun f(n: String) = o.getValue(n).jsonPrimitive.content
        Saying(f("id"), f("q"), f("text"), f("source"), f("tier").single(), f("usedFor"))
    }

    private fun context(persona: String, day: Int): Pair<DemoRig, AskContext> {
        val rig = DemoRig(PERSONAS.getValue(persona))
        (rig.ports.shelf as FakeSayingShelf).setSayings(shelf)
        DemoReplay.run(rig.engine, rig::dayStart, day)
        return rig to rig.engine.askContext(rig.endOf(day))
    }

    private val questions = listOf(
        "How was my week?", "hello", "thanks", "Give me a Vivekananda quote", "what is this app", "What should I do to improve?",
        "Am I steadier?", "how long are my stretches", "why do I keep getting pulled away", "how quickly do I come back",
        "how quiet was my phone", "do notifications pull me away", "what about today", "tell me about the lake", "do you see any patterns",
        "did the thing I tried work", "how often do I open Sakshi", "was there a lapse", "asdf qwerty", "I am so distracted and lazy, am I failing?",
    )

    @Test fun `the facts carry the app's own numbers and no app names`() {
        val (rig, c) = context("aarav", 59)
        val facts = AskFacts.render(c)
        assertTrue(facts.contains("STEADINESS: "))
        assertTrue(facts.contains("PATTERNS"))
        assertTrue(facts.contains("TODAY: "))
        // the persona's packages are demo.chat, demo.notes ...; none may appear, and neither may the label "Chat" or "Mail"
        rig.history.launcherApps.forEach { assertFalse("leaked ${it.pkg.value}", facts.contains(it.pkg.value)) }
        assertFalse(facts.contains("demo."))
    }

    @Test fun `gentle mode gives the model fewer facts, on purpose`() {
        val (_, c) = context("meera", 59)
        val facts = AskFacts.render(c)
        assertTrue(facts.contains("MODE: gentle"))
        assertFalse(facts.contains("STEADINESS:"))
        assertFalse(facts.contains("PATTERNS"))
    }

    @Test fun `a first look is described as provisional`() {
        val (_, c) = context("aarav", 3)
        assertTrue(AskFacts.render(c).contains("provisional"))
    }

    @Test fun `every offline answer, for every persona and stage, keeps the copy rules and names no app`() {
        for ((persona, day) in listOf("aarav" to 59, "aarav" to 31, "aarav" to 3, "meera" to 59, "rohan" to 59)) {
            val (rig, c) = context(persona, day)
            for (q in questions) {
                val a = OfflineAnswerer.answer(q, c, rig.ports.random)
                assertTrue("$persona/$day '$q' is blank", a.text.isNotBlank())
                assertTrue("$persona/$day '$q' -> ${a.text} :: ${AskGuard.violations(a.text)}", AskGuard.isClean(a.text))
                assertFalse("'$q' leaked a package: ${a.text}", a.text.contains("demo."))
                a.quote?.let { assertTrue("quote not on the shelf", it in shelf) }
            }
        }
    }

    @Test fun `questions are routed to the subject they ask about`() {
        val table = mapOf(
            "How was my week?" to Intent.WEEK, "hello" to Intent.GREETING, "thanks a lot" to Intent.THANKS, "give me a Vivekananda quote" to Intent.QUOTE,
            "what is this app" to Intent.ABOUT, "what should I do" to Intent.SUGGEST, "am I steadier now" to Intent.STEADINESS,
            "how long are my stretches" to Intent.STRETCH, "why do I get pulled away" to Intent.PULLS, "how fast do I come back" to Intent.RETURN,
            "was my phone quiet" to Intent.QUIET, "do pings pull me" to Intent.PINGS, "what about today" to Intent.TODAY, "show me the lake" to Intent.LAKE,
            "any patterns" to Intent.PATTERNS, "did it work" to Intent.RESULT, "how often do I open the app" to Intent.TEACHER,
        )
        table.forEach { (q, intent) -> assertEquals(q, intent, OfflineAnswerer.intentOf(q)) }
        assertNull(OfflineAnswerer.intentOf("asdf qwerty"))
    }

    @Test fun `asking for a quote gives a real saying from the shelf with its source`() {
        val (rig, c) = context("aarav", 59)
        val a = OfflineAnswerer.answer("Give me a Vivekananda quote", c, rig.ports.random)
        assertNotNull(a.quote); assertTrue(a.quote!!.text.isNotBlank()); assertTrue(a.quote!!.source.isNotBlank())
    }

    @Test fun `a question about advice gets a watching answer, never an order`() {
        val (rig, c) = context("aarav", 59)
        val text = OfflineAnswerer.answer("what should I do?", c, rig.ports.random).text
        assertTrue(text.contains("I do not tell people what to do"))
        assertFalse(Regex("you (should|must|need to)", RegexOption.IGNORE_CASE).containsMatchIn(text))
    }

    @Test fun `the same question gets the same answer`() {
        val (rig, c) = context("aarav", 59)
        assertEquals(OfflineAnswerer.answer("how was my week", c, rig.ports.random), OfflineAnswerer.answer("how was my week", c, rig.ports.random))
    }

    @Test fun `the guard catches thinking out loud and internal labels, and scrubs bracketed ones`() {
        assertFalse(AskGuard.isClean("Okay, the user wants the week."))
        assertFalse(AskGuard.isClean("Let me look at this. You held 14-minute stretches."))
        assertFalse(AskGuard.isClean("<think>hmm</think> You held 14-minute stretches."))
        assertFalse(AskGuard.isClean("Your STEADINESS is 138."))
        assertTrue(AskGuard.isClean("Your Steadiness is 138, which I call Steadier."))
        assertEquals("26 began with a ping, based on 60 windows.", AskGuard.scrub("26 began with a ping (PINGS AND REACHING), based on 60 windows."))
        assertEquals("A short (based on 4 windows) note.", AskGuard.scrub("A short (based on 4 windows) note."))   // ordinary brackets stay
    }

    @Test fun `the guard catches the banned words and exclamation marks`() {
        assertFalse(AskGuard.isClean("You should rest!"))
        assertFalse(AskGuard.isClean("You were so distracted"))
        assertFalse(AskGuard.isClean("A failure of focus, a wasted hour"))
        assertFalse(AskGuard.isClean("Keep your streak going"))
        assertFalse(AskGuard.isClean("   "))
        assertTrue(AskGuard.isClean("You held 14-minute stretches (based on 28 windows over 28 days)."))
    }
}
