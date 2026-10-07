package com.kleos.sakshi.engine.patterns

fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
}

/** Median absolute deviation from the median. */
fun mad(values: List<Double>): Double {
    val m = median(values)
    return median(values.map { kotlin.math.abs(it - m) })
}
