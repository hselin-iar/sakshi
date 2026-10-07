package com.kleos.sakshi.engine.windows

import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.patterns.WindowShape
import com.kleos.sakshi.engine.patterns.longestStretchOf
import com.kleos.sakshi.engine.patterns.median
import com.kleos.sakshi.engine.patterns.staysPerHourOf

/**
 * P4's per-window label, for setting each Window's own shape field. Returns
 * a list parallel to the input (same size and order) rather than a map
 * keyed by the window itself -- WindowWithDetail is a data class, so two
 * structurally-identical windows would collapse under a Map key (the exact
 * bug fixed in Rhythm.kt during T2.10).
 */
object WindowLabeler {
    fun labelAll(windows: List<WindowWithDetail>): List<Shape?> {
        if (windows.isEmpty()) return emptyList()

        val userMedianStaysPerHour = median(windows.map { staysPerHourOf(it) })
        val userMedianStretchMin = median(windows.map { longestStretchOf(it) })

        return windows.map { wd ->
            WindowShape.label(
                staysPerHour = staysPerHourOf(wd),
                stretchMin = longestStretchOf(wd),
                originsOfStays = wd.stays.map { it.origin },
                userMedianStaysPerHour = userMedianStaysPerHour,
                userMedianStretchMin = userMedianStretchMin,
            )
        }
    }
}
