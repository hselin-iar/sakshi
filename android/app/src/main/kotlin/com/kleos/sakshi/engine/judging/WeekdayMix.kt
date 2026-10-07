package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WindowWithDetail
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * F12: a before period that is mostly weekdays cannot be compared with an after period that is mostly weekend, because weekends
 * differ on their own. The before value is therefore re-weighted to the after period's mix: the metric per day-class in the
 * before period, combined with the after period's class weights (window-hours). Without it a change in mix reads as improvement.
 */
object WeekdayMix {
    enum class DayClass { WEEKDAY, WEEKEND }

    data class Result(val beforeValue: Double?, val afterValue: Double?, val approxMix: Boolean)

    fun classOf(day: StudyDay): DayClass =
        when (LocalDate.ofEpochDay(day.epochDay).dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> DayClass.WEEKEND
            else -> DayClass.WEEKDAY
        }

    /**
     * [measure] turns a list of windows into the target's value (or null). The after value is simply pooled. If a day-class is
     * present on only one side, the before value falls back to pooled and `approxMix` is set [DOC 3 assumption].
     */
    fun reweight(before: List<WindowWithDetail>, after: List<WindowWithDetail>, measure: (List<WindowWithDetail>) -> Double?): Result {
        val afterValue = measure(after)
        val beforeByClass = before.groupBy { classOf(it.window.day) }
        val afterByClass = after.groupBy { classOf(it.window.day) }

        if (beforeByClass.keys != afterByClass.keys) return Result(measure(before), afterValue, approxMix = true)

        var weighted = 0.0
        var weightSum = 0.0
        for ((cls, afterWindows) in afterByClass) {
            val value = measure(beforeByClass.getValue(cls)) ?: return Result(null, afterValue, approxMix = false)
            val weight = afterWindows.sumOf { (it.window.end.value - it.window.start.value) / 3_600_000.0 }
            weighted += weight * value
            weightSum += weight
        }
        val beforeValue = if (weightSum > 0.0) weighted / weightSum else null
        return Result(beforeValue, afterValue, approxMix = false)
    }
}
