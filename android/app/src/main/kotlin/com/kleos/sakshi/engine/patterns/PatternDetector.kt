package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.Randomness
import java.time.ZoneId

/**
 * DOC 3 names PatternContext alongside Pattern/PatternKind in Entities.kt,
 * but it needs Randomness (a port type); putting it there would make
 * engine/model depend on engine/ports, inverting the real dependency
 * (ports already depends on model). It lives here instead, next to the
 * interface that consumes it.
 *
 * zone is not in DOC 3's PatternContext description, but every detector
 * needs local hour/weekday math (P1's day-parts, CrossDay's local
 * midnight) and there is still no zone source anywhere in Settings/Ports
 * (same gap flagged in T2.8) -- carrying it on the context avoids
 * repeating that gap by reaching for ZoneId.systemDefault() again.
 */
data class PatternContext(
    val windows: List<WindowWithDetail>,
    val weeks: List<WeekSummary>,
    val days: List<DaySummary>,
    val baseline: Baseline?,
    val asOf: EpochMs,
    val random: Randomness,
    val zone: ZoneId,
)

interface PatternDetector {
    val kind: PatternKind
    fun detect(ctx: PatternContext): List<Pattern>
}

/** DOC 3: "Windows used by every detector: non-partial, finalised, in valid days." */
fun qualifyingWindows(ctx: PatternContext): List<WindowWithDetail> {
    val validDays = ctx.days.filter { it.valid }.map { it.day }.toSet()
    return ctx.windows.filter { !it.window.partial && it.window.finalised && it.window.day in validDays }
}

/** DOC 3: "Weeks used: non-unusual." (P1/Rhythm is explicitly exempt from this -- it uses windows, not weeks.) */
fun nonUnusualWeeks(ctx: PatternContext): List<WeekSummary> = ctx.weeks.filter { !it.unusual }
