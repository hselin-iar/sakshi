package com.kleos.sakshi.engine.metrics

/**
 * F5's four scored parts, plus the two shown-not-scored extras that have a
 * concrete DOC 3 formula (longestStretchMin, inSetShare). The rest of F5's
 * "extras" (switches per hour, ramp-up, flinch, endings, work-set coverage)
 * are NOT here: DOC 3 names them but gives no formula or golden example for
 * any of them, so adding fields for them now would be guessing. Deferred
 * until a step actually specifies them.
 *
 * Parts are nullable, never zero — a missing part means "too little data",
 * not "zero attention".
 */
data class Parts(
    val stretchMin: Double?,
    val longestStretchMin: Double?,
    val staysPerHour: Double?,
    val returnMin: Double?,
    val quietShare: Double?,
    val inSetShare: Double?,
)
