package com.kleos.sakshi.engine.demo

import com.kleos.sakshi.engine.SakshiEngine
import com.kleos.sakshi.engine.model.EpochMs

/**
 * Lives through a synthetic history the way a phone would have: one run on each Monday morning, the Mirror read on each of those
 * mornings except the last one, then a final run at the moment the slider points to. Reading the Mirror is what offers a suggestion,
 * shows a verdict and lets a footprint experiment start, so a demo that only ran once at the end would never show any of that.
 * The Mirror of the latest completed week is left unread: it is the one the person is about to open, and reading it here would use up
 * the verdict it is meant to show. Nothing here reads a clock; every instant comes from `dayStartMs`.
 */
object DemoReplay {
    private const val MONDAY_MORNING_MS = 5L * 60 * 60 * 1000      // 09:00 local, five hours after a study day starts at 04:00
    private const val ONE_MINUTE_MS = 60_000L

    /** Study day `i` begins at `dayStartMs(i)`; day 0 is a Monday. `upToDay` is the day the slider is on. */
    fun run(engine: SakshiEngine, dayStartMs: (Int) -> Long, upToDay: Int) {
        val asOf = EpochMs(dayStartMs(upToDay + 1) - ONE_MINUTE_MS)
        val mornings = (7..upToDay step 7).map { EpochMs(dayStartMs(it) + MONDAY_MORNING_MS) }.filter { it.value <= asOf.value }
        mornings.forEachIndexed { i, at ->
            engine.processNewEvents(at)
            if (i < mornings.lastIndex) engine.mirror(null, at)
        }
        engine.processNewEvents(asOf)
    }
}
