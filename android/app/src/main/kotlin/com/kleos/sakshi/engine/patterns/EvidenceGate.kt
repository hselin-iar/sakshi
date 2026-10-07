package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.tuning.Tuning

object EvidenceGate {
    fun passes(windows: Int, days: Int, ratio: Double): Boolean =
        windows >= Tuning.EVIDENCE_MIN_WINDOWS &&
            days >= Tuning.EVIDENCE_MIN_DAYS &&
            (ratio >= Tuning.EVIDENCE_RATIO_HI || ratio <= Tuning.EVIDENCE_RATIO_LO)
}
