package com.kleos.sakshi.engine.model

@JvmInline value class Pkg(val value: String)                 // Android package name, never empty
@JvmInline value class EpochMs(val value: Long)               // UTC milliseconds
@JvmInline value class Minutes(val value: Double)
@JvmInline value class Ratio(val value: Double)               // always clamped when used in Steadiness
@JvmInline value class StudyDay(val epochDay: Long)           // the local calendar date whose 04:00 starts it
@JvmInline value class WeekStart(val studyDay: StudyDay)      // Monday study day
