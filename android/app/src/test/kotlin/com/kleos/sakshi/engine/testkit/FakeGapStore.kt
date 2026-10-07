package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.ports.GapStore

class FakeGapStore : GapStore {
    private val gaps = mutableListOf<DataGap>()

    override fun add(gap: DataGap) {
        gaps += gap
    }

    override fun closeOpenPause(at: EpochMs) {
        val openIndex = gaps.indexOfLast { it.kind == GapKind.PAUSED && it.end == null }
        if (openIndex >= 0) {
            gaps[openIndex] = gaps[openIndex].copy(end = at)
        }
    }

    override fun overlapping(from: EpochMs, to: EpochMs): List<DataGap> =
        gaps.filter { gap ->
            gap.start.value < to.value && (gap.end?.value ?: Long.MAX_VALUE) > from.value
        }
}
