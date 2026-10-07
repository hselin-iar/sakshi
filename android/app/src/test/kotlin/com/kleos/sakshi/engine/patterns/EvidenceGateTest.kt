package com.kleos.sakshi.engine.patterns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceGateTest {
    @Test
    fun `passes at the documented thresholds`() {
        assertTrue(EvidenceGate.passes(windows = 4, days = 3, ratio = 1.5))
        assertTrue(EvidenceGate.passes(windows = 4, days = 3, ratio = 0.67))
        assertFalse(EvidenceGate.passes(windows = 3, days = 3, ratio = 2.0))
        assertFalse(EvidenceGate.passes(windows = 4, days = 2, ratio = 2.0))
        assertFalse(EvidenceGate.passes(windows = 4, days = 3, ratio = 1.0))
    }
}
