package com.kleos.sakshi.engine.classify

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F4's Depends rule. Input intervals must already be chronological and
 * non-overlapping (the Reconstruction contract); baseClasses are each
 * interval's AppClassifier.classify(pkg) result, same size and order.
 */
object DependsResolver {
    fun resolve(intervals: List<ForegroundInterval>, baseClasses: List<AppClass>): List<AppClass> {
        val joinWindowMs = Tuning.DEPENDS_JOIN_SEC * 1_000L
        var lastInSetEnd: Long? = null

        return intervals.indices.map { i ->
            val interval = intervals[i]
            when (baseClasses[i]) {
                AppClass.IN_SET -> {
                    lastInSetEnd = interval.end.value
                    AppClass.IN_SET
                }
                AppClass.NEUTRAL -> {
                    // transparent: lastInSetEnd passes through unchanged
                    AppClass.NEUTRAL
                }
                AppClass.DEPENDS -> {
                    val chained = lastInSetEnd?.let { interval.start.value - it <= joinWindowMs } ?: false
                    if (chained) {
                        lastInSetEnd = interval.end.value
                        AppClass.IN_SET
                    } else {
                        lastInSetEnd = null
                        AppClass.OFF_SET
                    }
                }
                AppClass.OFF_SET -> {
                    lastInSetEnd = null
                    AppClass.OFF_SET
                }
            }
        }
    }
}
