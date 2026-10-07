package com.kleos.sakshi.engine.demo

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.windows.WindowLabeler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/** The real engine over each persona's synthetic history. When a number is off, the synthesizer is calibrated, never the engine. */
class PersonaReplayTest {
    private val saved = TimeZone.getDefault()
    @Before fun zone() = TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    @After fun restore() = TimeZone.setDefault(saved)

    private fun aarav(day: Int) = DemoRig(PERSONAS.getValue("aarav")).also { DemoReplay.run(it.engine, it::dayStart, day) }

    @Test fun `the same persona and seed give an identical history`() {
        val a = EventSynthesizer.synthesize(PERSONAS.getValue("aarav"), 7L, { DemoRig(PERSONAS.getValue("aarav")).dayStart(it) }, com.kleos.sakshi.engine.model.Pkg("com.kleos.sakshi"))
        val b = EventSynthesizer.synthesize(PERSONAS.getValue("aarav"), 7L, { DemoRig(PERSONAS.getValue("aarav")).dayStart(it) }, com.kleos.sakshi.engine.model.Pkg("com.kleos.sakshi"))
        assertEquals(a, b)
        val other = EventSynthesizer.synthesize(PERSONAS.getValue("aarav"), 8L, { DemoRig(PERSONAS.getValue("aarav")).dayStart(it) }, com.kleos.sakshi.engine.model.Pkg("com.kleos.sakshi"))
        assertTrue(a.events != other.events)
    }

    @Test fun `Day 1 is a provisional First look with no steadiness`() {
        val rig = aarav(3)
        val m = rig.engine.mirror(null, rig.endOf(3))
        assertTrue(m.provisional); assertNull(m.steadiness); assertNull(m.suggestion)
        assertTrue(m.isDemo)
    }

    @Test fun `Week 4 is Steadier near 116 and every part is within 15 percent of its target`() {
        val rig = aarav(31)
        val m = rig.engine.mirror(null, rig.endOf(31))
        println("REPLAY week4 headline=${m.headline} steadiness=${m.steadiness} parts=${m.parts}")
        assertFalse(m.provisional)
        assertEquals("Steadier", m.steadiness!!.word)
        assertTrue("steadiness ${m.steadiness!!.value}", m.steadiness!!.value in 110..122)
        val t = rig.spec.weeks[3]
        assertWithin(m.parts!!.stretchMin!!, t.stretchMin); assertWithin(m.parts!!.returnMin!!, t.returnMin)
        assertWithin(m.parts!!.staysPerHour!!, t.staysPerHour); assertWithin(m.parts!!.quietShare!!, t.quietShare)
    }

    @Test fun `Week 8 is Steadier near 136 and every part is within 15 percent of its target`() {
        val rig = aarav(59)
        val m = rig.engine.mirror(null, rig.endOf(59))
        println("REPLAY week8 headline=${m.headline} steadiness=${m.steadiness} parts=${m.parts}")
        assertEquals("Steadier", m.steadiness!!.word)
        assertTrue("steadiness ${m.steadiness!!.value}", m.steadiness!!.value in 130..142)
        val t = rig.spec.weeks[7]
        assertWithin(m.parts!!.stretchMin!!, t.stretchMin); assertWithin(m.parts!!.returnMin!!, t.returnMin)
        assertWithin(m.parts!!.staysPerHour!!, t.staysPerHour); assertWithin(m.parts!!.quietShare!!, t.quietShare)
    }

    @Test fun `opens of this app fall 5 then 3 then 2 across weeks 1, 4 and 8`() {
        val rig = aarav(59)
        val opens = rig.ports.derived.weeks().sortedBy { it.weekStart.studyDay.epochDay }.map { it.appOpens }
        assertEquals(5, opens[0]); assertEquals(3, opens[3]); assertEquals(2, opens[7])
    }

    @Test fun `a footprint experiment resolves MOVED and the verdict is shown once`() {
        val rig = aarav(59)
        val moved = rig.ports.state.experiments().filter { it.verdict == Verdict.MOVED }
        println("REPLAY experiments=${rig.ports.state.experiments()}")
        assertTrue("experiments: ${rig.ports.state.experiments()}", moved.isNotEmpty())
        val m = rig.engine.mirror(null, rig.endOf(59))
        println("REPLAY week8 verdict=${m.verdict}")
        assertNotNull(m.verdict); assertEquals("MOVED", m.verdict!!.verdict)
        assertNull(rig.engine.mirror(null, rig.endOf(59)).verdict)       // said once
    }

    @Test fun `patterns are present at week 8`() {
        val rig = aarav(59)
        val m = rig.engine.mirror(null, rig.endOf(59))
        println("REPLAY patterns=${m.patterns.map { it.kindId + ": " + it.line }} stones=${m.stones} clearHour=${m.clearHour} suggestion=${m.suggestion?.line}")
        assertTrue(m.patterns.isNotEmpty())
    }

    @Test fun `some window is labelled Reached or Pinged`() {
        val rig = aarav(59)
        val windows = rig.ports.derived.windows(EpochMs(0), rig.endOf(59)).filter { !it.window.partial }
        val shapes = WindowLabeler.labelAll(windows)
        assertTrue(shapes.any { it == Shape.REACHED || it == Shape.PINGED })
    }

    @Test fun `events after the slider never matter`() {
        val full = aarav(31)
        val cut = DemoRig(PERSONAS.getValue("aarav"))
        val keep = cut.history.events.filter { it.ts.value <= cut.endOf(31).value }
        val trimmed = DemoRig(PERSONAS.getValue("aarav"))
        trimmed.ports.events.purgeBefore(EpochMs(Long.MAX_VALUE))       // empty the store, then load only what had happened by the slider
        trimmed.ports.events.append(keep)
        DemoReplay.run(trimmed.engine, trimmed::dayStart, 31)
        assertEquals(full.engine.mirror(null, full.endOf(31)), trimmed.engine.mirror(null, trimmed.endOf(31)))
    }

    @Test fun `Meera is gentle by default and shows only the gentle parts`() {
        val rig = DemoRig(PERSONAS.getValue("meera")).also { DemoReplay.run(it.engine, it::dayStart, 59) }
        val m = rig.engine.mirror(null, rig.endOf(59))
        assertTrue(m.gentle); assertNull(m.steadiness); assertTrue(m.patterns.isEmpty()); assertNull(m.suggestion)
        assertNotNull(m.returnLine)
    }

    @Test fun `Rohan's listener outage shows an honest data line`() {
        val rig = DemoRig(PERSONAS.getValue("rohan")).also { DemoReplay.run(it.engine, it::dayStart, 59) }
        val weeks = (4..5).map { w -> rig.engine.mirror(com.kleos.sakshi.engine.model.WeekStart(com.kleos.sakshi.engine.model.StudyDay(rig.firstMonday.toEpochDay() + 7L * w)), rig.endOf(59)) }
        println("REPLAY rohan dataLines=${weeks.map { it.dataLines }} flags=${weeks.map { it.dataFlags }}")
        assertTrue(weeks.any { it.dataLines.isNotEmpty() })
    }

    private fun assertWithin(actual: Double, target: Double) =
        assertTrue("$actual is not within 15% of $target", actual in target * 0.85..target * 1.15)
}
