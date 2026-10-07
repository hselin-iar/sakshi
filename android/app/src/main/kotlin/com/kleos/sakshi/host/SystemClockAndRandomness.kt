package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.ports.Clock
import com.kleos.sakshi.engine.ports.Randomness
import java.util.Random

/** The phone's real clock. The engine never reads time itself; it asks this port. */
class SystemClock : Clock {
    override fun now() = EpochMs(System.currentTimeMillis())
}

/** A seeded source for the engine's randomness port; `fork` gives an independent, repeatable stream. */
class SeededRandomness(seed: Long = System.nanoTime()) : Randomness {
    private val random = Random(seed)
    override fun nextDouble() = random.nextDouble()
    override fun nextInt(bound: Int) = random.nextInt(bound)
    override fun fork(seed: Long): Randomness = SeededRandomness(seed)
}
