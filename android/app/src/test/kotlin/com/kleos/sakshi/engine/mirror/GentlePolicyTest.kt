package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.ClearHourView
import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.model.DataState
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GoalTapView
import com.kleos.sakshi.engine.model.MirrorView
import com.kleos.sakshi.engine.model.ObservationView
import com.kleos.sakshi.engine.model.PartLines
import com.kleos.sakshi.engine.model.PartsView
import com.kleos.sakshi.engine.model.PatternLine
import com.kleos.sakshi.engine.model.SayingView
import com.kleos.sakshi.engine.model.SteadinessView
import com.kleos.sakshi.engine.model.StonesView
import com.kleos.sakshi.engine.model.StudyBlockView
import com.kleos.sakshi.engine.model.SuggestionView
import com.kleos.sakshi.engine.model.TeacherView
import com.kleos.sakshi.engine.model.VerdictView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F16 Gentle Mode: for a full Mirror, apply() leaves only what the policy keeps. */
class GentlePolicyTest {
    private val steadinessWords = Regex("""\b(Wavering|Steady|Steadier|Steadiness)\b""")

    private fun fullView(returnLine: String? = "After a stay, it took you about 4 minutes to get back.") = MirrorView(
        isDemo = false, provisional = false, gentle = false, weekStart = EpochMs(1_000), weekLabel = "5–11 Oct",
        dataState = DataState.OK, dataFlags = listOf(DataFlag.PARTIAL_PING), dataLines = listOf("Ping awareness is partial; I have not heard a ping in the days I could check. Stays I could not check are left out of the ping count."),
        headline = "You held 18-minute stretches and 4-minute returns; steadier than your starting normal.",
        parts = PartsView(18.0, 34.0, 0.74, 0.15, 2.4, 6, 4.0, PartLines("stretch line", "Something pulled you away about 2 times an hour.", "It took you about 4 minutes to get back.", "quiet line"), listOf("extra")),
        steadiness = SteadinessView(116, "Steadier"),
        stones = StonesView(17, 9, 6, 2, "Messaging", 0.4, "9 of your 17 stays began with a ping."),
        clearHour = ClearHourView(9, 11, 22.0, "Between 09:00 and 11:00 your stretches average 22 minutes."),
        patterns = listOf(PatternLine("RHYTHM", "a pattern (based on 9 windows over 4 days).", 9, 4)),
        suggestion = SuggestionView("S2", "com.x", "One app is behind 11 of your last 17 stays.", "Move its icon to a second screen", "MOVE_ICON", false),
        observation = ObservationView("S12", "Your return time improved by 20%."), nothingToFix = true,
        verdict = VerdictView("MOVED", "A verdict line.", 4.1, 3.0, false), goalTap = GoalTapView(true, "PARTLY"),
        teacher = TeacherView(2, 3, 11.5, "You opened me 3 times in week 2 and 2 times this week."), lapseLine = "You were away 4 days. Your starting normal is still here.",
        saying = SayingView("sy01", "I am watching my mind act", "src", "reported by others", 86), returnLine = returnLine,
        reanchorOffered = true, suggestedStudyBlock = StudyBlockView(1320, 90),
    )

    @Test fun `gentle removes Steadiness patterns suggestions observations verdicts and the rest of the removal list`() {
        val g = GentlePolicy.apply(fullView())
        assertTrue(g.gentle)
        assertNull(g.steadiness); assertTrue(g.patterns.isEmpty()); assertNull(g.suggestion); assertNull(g.observation)
        assertFalse(g.nothingToFix); assertFalse(g.goalTap.offered); assertNull(g.goalTap.answer); assertNull(g.teacher); assertNull(g.clearHour)
        assertNull(g.verdict); assertNull(g.stones); assertFalse(g.reanchorOffered); assertNull(g.suggestedStudyBlock)
    }

    @Test fun `gentle keeps the return line the stay count the Saying the lapse line and the honest data states`() {
        val v = fullView(); val g = GentlePolicy.apply(v)
        assertEquals(v.returnLine, g.headline)
        assertEquals(v.parts!!.lines.stays, g.parts!!.lines.stays); assertEquals(2.4, g.parts!!.staysPerHour!!, 0.0)
        assertEquals(v.parts!!.lines.ret, g.parts!!.lines.ret); assertEquals(4.0, g.parts!!.returnMin!!, 0.0)
        assertNull(g.parts!!.lines.stretch); assertNull(g.parts!!.lines.quiet); assertNull(g.parts!!.stretchMin); assertNull(g.parts!!.quietShare)
        assertEquals(v.saying, g.saying); assertEquals(v.lapseLine, g.lapseLine)
        assertEquals(v.dataFlags, g.dataFlags); assertEquals(v.dataLines, g.dataLines); assertEquals(v.dataState, g.dataState)
        assertEquals(v.weekLabel, g.weekLabel); assertEquals(v.weekStart, g.weekStart)
    }

    @Test fun `no string anywhere in a gentle view holds a Steadiness word`() {
        fun strings(v: MirrorView): List<String> = listOfNotNull(
            v.headline, v.weekLabel, v.returnLine, v.lapseLine, v.parts?.lines?.stays, v.parts?.lines?.ret, v.parts?.lines?.stretch,
            v.parts?.lines?.quiet, v.steadiness?.word, v.saying?.text, v.saying?.source,
        ) + v.dataLines + v.patterns.map { it.line } + listOfNotNull(v.suggestion?.line, v.observation?.line, v.verdict?.line, v.teacher?.line)
        val g = GentlePolicy.apply(fullView())
        strings(g).forEach { assertFalse("Steadiness word in: $it", steadinessWords.containsMatchIn(it)) }
        // and the full view really did contain them, so the check can fail
        assertTrue(strings(fullView()).any { steadinessWords.containsMatchIn(it) })
    }

    @Test fun `apply is idempotent`() {
        val once = GentlePolicy.apply(fullView())
        assertEquals(once, GentlePolicy.apply(once))
        val noReturn = GentlePolicy.apply(fullView(returnLine = null))
        assertEquals(noReturn, GentlePolicy.apply(noReturn))
    }

    @Test fun `with no return line the headline is the learning sentence`() {
        assertEquals("I am still learning your starting normal.", GentlePolicy.apply(fullView(returnLine = null)).headline)
    }

    @Test fun `a failure falls back to the most restrictive view never the full one`() {
        // a parts object whose lines throw when copied cannot be built, so exercise the fallback path directly
        val fallback = GentlePolicy.javaClass.getDeclaredMethod("mostRestrictive", MirrorView::class.java).apply { isAccessible = true }
            .invoke(GentlePolicy, fullView()) as MirrorView
        assertNotNull(fallback.headline); assertTrue(fallback.gentle)
        assertNull(fallback.parts); assertNull(fallback.steadiness); assertNull(fallback.saying); assertNull(fallback.lapseLine)
        assertTrue(fallback.patterns.isEmpty()); assertNull(fallback.suggestion); assertFalse(fallback.reanchorOffered)
    }
}
