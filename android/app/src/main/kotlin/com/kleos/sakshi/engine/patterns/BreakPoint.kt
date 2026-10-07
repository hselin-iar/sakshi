package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.tuning.Tuning

/** P5: where stretches end, and what usually came just before a stay. */
object BreakPoint : PatternDetector {
    override val kind = PatternKind.BREAK_POINT

    override fun detect(ctx: PatternContext): List<Pattern> {
        val windows = qualifyingWindows(ctx)
        val allStays = windows.flatMap { it.stays }
        if (allStays.size < Tuning.BREAK_MIN_STAYS) return emptyList()

        val endings = windows.flatMap { it.stretches }.filter { it.endedBy == EndedBy.STAY || it.endedBy == EndedBy.PUT_DOWN }
        if (endings.isEmpty()) return emptyList()

        val bands = endings.groupBy { (it.minutes / 5).toInt() * 5 }
        val topBand = bands.entries.maxByOrNull { it.value.size } ?: return emptyList()
        val topBandShare = topBand.value.size.toDouble() / endings.size
        if (topBandShare < 0.50) return emptyList()

        return listOf(
            Pattern(
                kind = PatternKind.BREAK_POINT,
                key = "break",
                strength = topBandShare,
                evidenceWindows = windows.size,
                evidenceDays = windows.map { it.window.day }.distinct().size,
                firstSeen = ctx.asOf,
                lastSeen = ctx.asOf,
                args = mapOf(
                    "bandStartMin" to topBand.key.toString(),
                    "bandShare" to topBandShare.toString(),
                    "cause" to causeOf(allStays),
                ),
            ),
        )
    }

    private fun causeOf(allStays: List<Stay>): String {
        val known = allStays.filter { it.origin == Origin.STONE || it.origin == Origin.SELF_STARTED }
        if (known.isEmpty()) return "MIXED"

        val stoneStays = known.filter { it.origin == Origin.STONE }
        val stoneShare = stoneStays.size.toDouble() / known.size
        if (stoneShare >= 0.50) {
            val topPkgEntry = stoneStays.groupBy { it.stonePkg }.maxByOrNull { it.value.size }
            val topPkgShare = (topPkgEntry?.value?.size ?: 0).toDouble() / stoneStays.size
            if (topPkgShare >= 0.50 && topPkgEntry?.key != null) return "PING:${topPkgEntry.key!!.value}"
        }

        val selfShare = known.count { it.origin == Origin.SELF_STARTED }.toDouble() / known.size
        if (selfShare >= 0.50) return "SELF"

        return "MIXED"
    }
}
