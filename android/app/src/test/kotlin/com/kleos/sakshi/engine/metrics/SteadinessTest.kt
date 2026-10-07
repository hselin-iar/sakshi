package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** G-F1..G-F4 and G-F7 are the spec (DOC 3, F5. Steadiness and Its Four Parts). */
class SteadinessTest {

    private fun baseline(c0: Double, p0: Double, r0: Double, q0: Double) = Baseline(
        id = 0L, frozenAt = EpochMs(0), c0 = c0, p0 = p0, r0 = r0, q0 = q0, daysUsed = 8, isActive = true,
    )

    private fun parts(c: Double, p: Double, r: Double, q: Double) =
        Parts(stretchMin = c, longestStretchMin = null, staysPerHour = p, returnMin = r, quietShare = q, inSetShare = null)

    @Test
    fun `G-F1 baseline (10, 4-0, 6, 0-36) week (12, 3-5, 5, 0-38) is 116 Steadier`() {
        // Hand calculation (DOC4's "your job alongside"):
        //   ratios: C/C0=12/10=1.20, P0/P=4.0/3.5=1.1429, R0/R=6/5=1.20, Q/Q0=0.38/0.36=1.0556
        //   s = 1.20^0.35 * 1.1429^0.30 * 1.20^0.20 * 1.0556^0.15
        //     = exp(0.35*ln1.20 + 0.30*ln1.1429 + 0.20*ln1.20 + 0.15*ln1.0556)
        //     = exp(0.063812 + 0.040059 + 0.036464 + 0.008111) = exp(0.148445) = 1.1601
        //   value = round(100 * 1.1601) = 116 -> > 110 -> Steadier
        val result = steadiness(parts(12.0, 3.5, 5.0, 0.38), baseline(10.0, 4.0, 6.0, 0.36))!!

        assertEquals(116, result.value)
        assertEquals(Word.STEADIER, result.word)
    }

    @Test
    fun `G-F2 week (8, 5-5, 9, 0-30) is 75 Wavering`() {
        // Hand calculation:
        //   ratios: C/C0=8/10=0.80, P0/P=4.0/5.5=0.7273, R0/R=6/9=0.6667, Q/Q0=0.30/0.36=0.8333
        //   s = 0.80^0.35 * 0.7273^0.30 * 0.6667^0.20 * 0.8333^0.15
        //     = exp(0.35*ln0.80 + 0.30*ln0.7273 + 0.20*ln0.6667 + 0.15*ln0.8333)
        //     = exp(-0.078099 - 0.095550 - 0.081100 - 0.027345) = exp(-0.282094) = 0.7542
        //   value = round(100 * 0.7542) = 75 -> < 90 -> Wavering
        val result = steadiness(parts(8.0, 5.5, 9.0, 0.30), baseline(10.0, 4.0, 6.0, 0.36))!!

        assertEquals(75, result.value)
        assertEquals(Word.WAVERING, result.word)
    }

    @Test
    fun `G-F3 a stretch ratio of 3-0 is clamped the same as 1-5`() {
        // Isolate the stretch (C) term: p, r, q all equal their baseline (ratio 1).
        val base = baseline(10.0, 4.0, 6.0, 0.36)
        val atCap = steadiness(parts(15.0, 4.0, 6.0, 0.36), base)!! // ratio exactly 1.5
        val overCap = steadiness(parts(30.0, 4.0, 6.0, 0.36), base)!! // ratio 3.0, must clamp to 1.5

        assertEquals(atCap.value, overCap.value)
    }

    @Test
    fun `G-F3 a stretch ratio of 0-2 is clamped the same as 0-5`() {
        val base = baseline(10.0, 4.0, 6.0, 0.36)
        val atFloor = steadiness(parts(5.0, 4.0, 6.0, 0.36), base)!! // ratio exactly 0.5
        val underFloor = steadiness(parts(2.0, 4.0, 6.0, 0.36), base)!! // ratio 0.2, must clamp to 0.5

        assertEquals(atFloor.value, underFloor.value)
    }

    @Test
    fun `G-F4 boundaries - 90 and 110 are Steady, 111 is Steadier, 89 is Wavering`() {
        // All four ratios set equal to x (base = 100 for every part), so s = x^1.0 = x exactly.
        fun atRatio(x: Double) = steadiness(parts(100 * x, 100 / x, 100 / x, 100 * x), baseline(100.0, 100.0, 100.0, 100.0))!!

        assertEquals(90, atRatio(0.90).value)
        assertEquals(Word.STEADY, atRatio(0.90).word)
        assertEquals(110, atRatio(1.10).value)
        assertEquals(Word.STEADY, atRatio(1.10).word)
        assertEquals(111, atRatio(1.11).value)
        assertEquals(Word.STEADIER, atRatio(1.11).word)
        assertEquals(89, atRatio(0.89).value)
        assertEquals(Word.WAVERING, atRatio(0.89).word)
    }

    @Test
    fun `G-F7 steadiness is deterministic`() {
        val p = parts(12.0, 3.5, 5.0, 0.38)
        val b = baseline(10.0, 4.0, 6.0, 0.36)

        assertEquals(steadiness(p, b), steadiness(p, b))
    }

    @Test
    fun `missing any single part yields null, never a fabricated number`() {
        val b = baseline(10.0, 4.0, 6.0, 0.36)
        assertEquals(null, steadiness(parts(12.0, 3.5, 5.0, 0.38).copy(stretchMin = null), b))
        assertEquals(null, steadiness(parts(12.0, 3.5, 5.0, 0.38).copy(staysPerHour = null), b))
        assertEquals(null, steadiness(parts(12.0, 3.5, 5.0, 0.38).copy(returnMin = null), b))
        assertEquals(null, steadiness(parts(12.0, 3.5, 5.0, 0.38).copy(quietShare = null), b))
    }

    @Test
    fun `value is exactly 100 when parts equal the baseline`() {
        val b = baseline(10.0, 4.0, 6.0, 0.36)
        val result = steadiness(parts(10.0, 4.0, 6.0, 0.36), b)!!
        assertEquals(100, result.value)
    }

    // Property: improving any single part (others fixed) never lowers the value. Seeds 1..10.
    @Test
    fun `improving any single part never lowers the value`() {
        val b = baseline(10.0, 4.0, 6.0, 0.36)
        for (seed in 1L..10L) {
            val random = Random(seed)
            val before = parts(10.0, 4.0, 6.0, 0.36)
            val beforeValue = steadiness(before, b)!!.value

            val after = when (random.nextInt(4)) {
                0 -> before.copy(stretchMin = before.stretchMin!! * 1.1) // higher C is better
                1 -> before.copy(staysPerHour = before.staysPerHour!! * 0.9) // lower P is better
                2 -> before.copy(returnMin = before.returnMin!! * 0.9) // lower R is better
                else -> before.copy(quietShare = before.quietShare!! * 1.1) // higher Q is better
            }
            val afterValue = steadiness(after, b)!!.value

            assertTrue("seed=$seed", afterValue >= beforeValue)
        }
    }
}
