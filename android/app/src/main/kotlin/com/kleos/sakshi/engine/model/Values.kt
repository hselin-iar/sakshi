package com.kleos.sakshi.engine.model

import com.kleos.sakshi.engine.tuning.Tuning
import java.time.Instant
import java.time.ZoneId

@JvmInline value class Pkg(val value: String)                 // Android package name, never empty
@JvmInline value class EpochMs(val value: Long)               // UTC milliseconds
@JvmInline value class Minutes(val value: Double)
@JvmInline value class Ratio(val value: Double)               // always clamped when used in Steadiness

@JvmInline value class StudyDay(val epochDay: Long) {          // the local calendar date whose 04:00 starts it
    companion object {
        // local time minus STUDY_DAY_START_HOUR, then the local date (DOC 3, Event Vocabulary and Tuning)
        fun of(ts: EpochMs, zone: ZoneId): StudyDay {
            val localDate = Instant.ofEpochMilli(ts.value)
                .atZone(zone)
                .minusHours(Tuning.STUDY_DAY_START_HOUR.toLong())
                .toLocalDate()
            return StudyDay(localDate.toEpochDay())
        }
    }
}

@JvmInline value class WeekStart(val studyDay: StudyDay)      // Monday study day
