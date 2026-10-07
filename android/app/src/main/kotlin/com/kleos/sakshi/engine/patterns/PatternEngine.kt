package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pattern

// DOC 3/DOC4 name "14 days" as the retirement threshold, but Tuning.kt has no
// constant for it (checked: neither RAW_RETENTION_DAYS nor JUDGE_DAYS is this).
// Flagged the same way as T2.4's learn-study-hours gap -- this really belongs
// in Tuning, pending a Contract Change, not hardcoded as a private constant
// here. Kept rather than skipped because retirement is this step's own job,
// unlike T2.4's optional learn-study-hours feature.
private const val RETIRE_AFTER_MS = 14L * 24 * 60 * 60 * 1_000

/**
 * Runs every registered detector and upserts by (kind, key): a freshly
 * redetected pattern keeps its previous firstSeen (so a returning pattern's
 * age survives); a pattern not redetected is kept as-is (so firstSeen
 * survives it coming back) rather than deleted -- isRetired() is how a
 * caller decides whether to still show it.
 */
object PatternEngine {
    /** All seven detectors (P1-P5, CrossDay, Clustering), in the order DOC 3 lists them. */
    val allDetectors: List<PatternDetector> = listOf(Rhythm, Trend, Shift, WindowShape, BreakPoint, CrossDay, Clustering)

    fun run(ctx: PatternContext, detectors: List<PatternDetector> = allDetectors, existing: List<Pattern>): List<Pattern> {
        val existingByKey = existing.associateBy { it.kind to it.key }
        val detected = detectors.flatMap { it.detect(ctx) }
        val detectedByKey = detected.associateBy { it.kind to it.key }

        val refreshed = detected.map { fresh ->
            val previous = existingByKey[fresh.kind to fresh.key]
            if (previous != null) fresh.copy(firstSeen = previous.firstSeen) else fresh
        }
        val notRedetected = existing.filterNot { (it.kind to it.key) in detectedByKey }

        return refreshed + notRedetected
    }

    fun isRetired(pattern: Pattern, asOf: EpochMs): Boolean =
        asOf.value - pattern.lastSeen.value > RETIRE_AFTER_MS
}
