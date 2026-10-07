package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.EpochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The persona numbers must survive the real storage layer: window ids, joins and every table, not only the in-memory fakes. */
@RunWith(RobolectricTestRunner::class)
class DemoThroughRoomTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private lateinit var container: AppContainer

    /** Room refuses the main thread; this runs the block on another and hands back what it returned. */
    private fun <T> off(block: () -> T): T {
        var result: Result<T>? = null
        Thread { result = runCatching(block) }.also { it.start(); it.join() }
        return result!!.getOrThrow()
    }

    @Before fun fresh() { AppContainer.reset(); container = AppContainer.from(app) }

    @Test fun aaravAtWeek8ReadsThroughRoomLikeTheFakes() {
        off { container.demoController.start("aarav") }
        val c = container.demoController
        off { c.setAsOf(59) }
        val asOf = container.clock.now()
        val m = off { container.engine.mirror(null, asOf) }
        assertFalse(m.provisional); assertTrue(m.isDemo)
        assertNotNull(m.steadiness); assertTrue("steadiness ${m.steadiness}", m.steadiness!!.value in 130..142)
        assertEquals("Steadier", m.steadiness!!.word)
        assertNotNull(m.verdict); assertEquals("MOVED", m.verdict!!.verdict)
        assertTrue(m.patterns.isNotEmpty())
        val today = off { container.engine.today(asOf) }
        assertTrue(today.windows.isNotEmpty() && today.windows.all { it.stays >= 0 })
        assertTrue(off { container.engine.lake(asOf) }.phrase.isNotBlank())
        off { c.stop() }
    }

    @Test fun day1IsAProvisionalFirstLook() {
        off { container.demoController.start("aarav") }
        val m = off { container.engine.mirror(null, container.clock.now()) }
        assertTrue(m.provisional); assertNull(m.steadiness)
        off { container.demoController.stop() }
    }

    @Test fun movingBackwardsForgetsWhatHappenedInTheFuture() {
        off { container.demoController.start("aarav") }
        off { container.demoController.setAsOf(59) }
        assertTrue(off { container.state.experiments() }.isNotEmpty())
        off { container.demoController.setAsOf(20) }
        assertTrue(off { container.state.experiments() }.isEmpty())
        assertTrue(off { container.engine.mirror(null, container.clock.now()) }.steadiness != null)
        off { container.demoController.stop() }
    }
}
