package com.kleos.sakshi.engine.lake

import com.kleos.sakshi.engine.mirror.SentenceBuilder
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.engine.model.LakeView
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.patterns.median
import com.kleos.sakshi.engine.patterns.staysPerHourOf
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import com.kleos.sakshi.engine.usecases.BuildWeekSummary
import com.kleos.sakshi.engine.usecases.effectiveGentle
import com.kleos.sakshi.engine.windows.WindowLabeler
import java.time.ZoneId

/**
 * F7: the Lake is the most recently ENDED, finalised window against the user's own median: Held is still water, a window at 1.5x
 * the user's usual pulls per hour is choppy, anything else ripples. Never the live window, never a number. The same builder feeds
 * the in-app Lake and the widget (the stored lake_state row), so they cannot disagree.
 */
object LakeBuilder {
    fun build(ports: Ports, asOf: EpochMs, zone: ZoneId): LakeView {
        val gentle = ports.state.settings().effectiveGentle()
        val days = ports.derived.days(StudyDay(0), StudyDay.of(asOf, zone))
        val ended = BuildWeekSummary.qualifying(ports.derived.windows(EpochMs(0), asOf), days.filter { it.valid }.map { it.day }.toSet())
            .filter { it.window.end.value <= asOf.value }
        val latest = ended.maxByOrNull { it.window.end.value }

        val state = when {
            latest == null -> LakeState.NO_DATA
            ports.state.baseline() == null || ended.size < Tuning.LAKE_MIN_WINDOWS -> LakeState.LEARNING
            else -> {
                val shape = WindowLabeler.labelAll(ended)[ended.indexOf(latest)]
                val usual = median(ended.map { staysPerHourOf(it) })
                when {
                    shape == Shape.HELD -> LakeState.STILL
                    usual > 0.0 && staysPerHourOf(latest) >= Tuning.LAKE_CHOPPY_RATIO * usual -> LakeState.CHOPPY
                    else -> LakeState.RIPPLED
                }
            }
        }
        val asOfLabel = if (state == LakeState.NO_DATA) null else latest?.window?.end
        return LakeView(state, SentenceBuilder.lakePhrase(state, gentle), asOfLabel)
    }
}
