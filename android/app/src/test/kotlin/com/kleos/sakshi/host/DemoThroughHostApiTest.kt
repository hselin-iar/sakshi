package com.kleos.sakshi.host

import com.kleos.sakshi.host.gen.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** What the Flutter screens actually call: the Pigeon host over the real container, engine and Room, with the demo's history. */
@RunWith(RobolectricTestRunner::class)
class DemoThroughHostApiTest {
    private val api by lazy { HostApiImpl(RuntimeEnvironment.getApplication()) }
    @Before fun fresh() = AppContainer.reset()
    @After fun stop() { runBlocking { runCatching { api.stopDemo() } } }

    private fun week8() = runBlocking { api.startDemo("aarav"); api.setDemoAsOf(59) }

    @Test fun everyScreenHasRealDataAtWeek8() = runBlocking {
        week8()
        val setup = api.getSetupState()
        assertTrue(setup.isDemo); assertTrue(setup.workSetSaved)

        val m = api.getMirror(null)
        assertTrue(m.isDemo); assertFalse(m.provisional)
        assertTrue(m.headline.isNotBlank())
        assertTrue(m.steadiness!!.value in 130..142); assertEquals("Steadier", m.steadiness!!.word)
        assertNotNull(m.parts!!.lines.stretch); assertNotNull(m.parts!!.lines.ret)
        assertNotNull(m.stones); assertTrue(m.stones!!.totalStays > 0)
        assertTrue(m.patterns.isNotEmpty())
        assertEquals("MOVED", m.verdict!!.verdict)
        assertNotNull(m.teacher)

        assertTrue(api.listMirrorWeeks().size >= 6)
        assertTrue(api.getTodaySoFar().windows.isNotEmpty())
        assertTrue(api.getLake().phrase.isNotBlank())
        val see = api.getWhatISee()
        assertTrue(see.isDemo); assertTrue(see.rawEventCount > 1000); assertTrue(see.notifEventCount > 100); assertTrue(see.derivedDays > 40)
        assertTrue(see.lines.isNotEmpty())
    }

    @Test fun theMirrorOfEarlierWeeksAndTheDayOneFirstLookWork() = runBlocking {
        week8()
        val weeks = api.listMirrorWeeks()
        val week4 = api.getMirror(weeks.sortedBy { it.weekStartEpochMs }[3].weekStartEpochMs)
        assertNotNull(week4.steadiness)
        api.setDemoAsOf(3)
        val firstLook = api.getMirror(null)
        assertTrue(firstLook.provisional); assertNull(firstLook.steadiness)
        assertTrue(firstLook.headline.isNotBlank())
    }

    @Test fun tapsChangeStateAndAreRememberedAcrossReads() = runBlocking {
        week8()
        api.tapGoal(GoalAnswerDto.PARTLY)
        assertEquals(GoalAnswerDto.PARTLY, api.getMirror(null).goalTap.answer)
        val choices = api.getSayingChoices()
        if (choices.isNotEmpty()) {
            api.pickSaying(choices.first().id)
            assertEquals(choices.first().id, api.getMirror(null).saying?.id)
        }
    }

    @Test fun demoRefusesPauseExportAndDelete() {
        week8()
        for (call in listOf<suspend () -> Unit>({ api.pause(true) }, { api.exportData(false) }, { api.deleteEverything() })) {
            try { runBlocking { call() }; fail("expected DEMO_ACTIVE") } catch (e: FlutterError) { assertEquals(HostErrors.DEMO_ACTIVE, e.code) }
        }
    }

    @Test fun meeraIsGentleAndRohanIsNot() = runBlocking {
        api.startDemo("meera"); api.setDemoAsOf(59)
        val meera = api.getMirror(null)
        assertTrue(meera.gentle); assertNull(meera.steadiness); assertNotNull(meera.returnLine)
        api.startDemo("rohan"); api.setDemoAsOf(59)
        val rohan = api.getMirror(null)
        assertFalse(rohan.gentle); assertNotNull(rohan.steadiness)
    }
}
