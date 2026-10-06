package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Zones {
    val KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
    val UTC: ZoneId = ZoneId.of("UTC")
}

/** The date DOC 3's golden examples (G-I1..G-I5) are written against. */
val GOLDEN_DATE: LocalDate = LocalDate.of(2026, 10, 6)

fun epochMs(date: LocalDate, time: LocalTime, zone: ZoneId = Zones.KOLKATA): EpochMs =
    EpochMs(LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli())

/** Accepts "HH:MM:SS" or "HH:MM:SS.fff". */
fun epochMsAt(time: String, date: LocalDate = GOLDEN_DATE, zone: ZoneId = Zones.KOLKATA): EpochMs =
    epochMs(date, parseTimeOfDay(time), zone)

private fun parseTimeOfDay(time: String): LocalTime {
    val parts = time.split(":")
    require(parts.size == 3) { "expected HH:MM:SS[.fff], got $time" }
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    val secParts = parts[2].split(".")
    val second = secParts[0].toInt()
    val nanos = if (secParts.size > 1) {
        secParts[1].padEnd(9, '0').substring(0, 9).toLong()
    } else 0L
    return LocalTime.of(hour, minute, second, nanos.toInt())
}
