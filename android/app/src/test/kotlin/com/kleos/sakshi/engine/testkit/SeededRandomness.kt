package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.ports.Randomness
import kotlin.random.Random

/**
 * Test double for the Randomness port. This file lives under src/test/, not
 * under engine/, so DependencyRuleTest's ban on calling Random() directly
 * inside the engine does not apply to it — the engine itself must still
 * only ever reach randomness through the port.
 */
class SeededRandomness(private val seed: Long) : Randomness {
    private val delegate = Random(seed)

    override fun nextDouble(): Double = delegate.nextDouble()

    override fun nextInt(bound: Int): Int = delegate.nextInt(bound)

    override fun fork(seed: Long): Randomness = SeededRandomness(seed)
}
