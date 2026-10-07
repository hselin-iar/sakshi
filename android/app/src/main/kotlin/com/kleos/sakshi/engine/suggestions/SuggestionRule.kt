package com.kleos.sakshi.engine.suggestions

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.LapseInfo
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.patterns.ClearHourResult
import java.time.ZoneId

/**
 * Not in Entities.kt despite DOC 3's module list naming it there loosely --
 * same reasoning as PatternContext (T2.11): kept next to the interface it
 * serves, in the suggestions package.
 */
data class SuggestionContext(
    val windows: List<WindowWithDetail>,
    val week: WeekSummary?,
    val priorWeeks: List<WeekSummary>, // chronological, most recent last, not including `week`
    val days: List<DaySummary>,
    val baseline: Baseline?,
    val patterns: List<Pattern>,
    val clearHour: ClearHourResult?,
    val lapse: LapseInfo?,
    val isFirstMirrorAfterLapse: Boolean,
    val currentWeekNumber: Int,
    val notificationAccessGranted: Boolean,
    val listenerCoverageFraction14d: Double,
    val suggestionStates: List<SuggestionState>,
    val experiments: List<Experiment>,
    val asOf: EpochMs,
    val zone: ZoneId,
)

interface SuggestionRule {
    val kind: SuggestionKind
    fun evaluate(ctx: SuggestionContext): Candidate?
}
