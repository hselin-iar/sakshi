package com.kleos.sakshi.engine.demo

import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StudyBlock

// [CONTRACT GAP: DOC 3 (LC-6) names PersonaSpec, WeekTarget and Quirk and says they live in Entities.kt, which Track 2 may not edit
//  beyond additive needs. They are kept here, next to their only users, with the field lists exactly as DOC 3 writes them.]

/** What a persona's week should look like once the real engine has run over it. All numbers are targets for the synthesizer, never engine input. */
data class WeekTarget(
    val stretchMin: Double, val staysPerHour: Double, val returnMin: Double, val quietShare: Double, val stoneShare: Double, val opens: Int,
)

/** Data, not code: the synthesizer reads these flags and the engine never does. */
enum class Quirk {
    /** One extra heavy self-started late window is NOT built; the shape labels come from the day-to-day spread instead (see EventSynthesizer). */
    REACHED_AFTER_2230,
    /** On 5 of every 14 nights the screen runs past 01:30 and the next day's first stretch is 6 minutes shorter. */
    LATE_NIGHT_SPILLOVER,
    /** One app is behind most of the pings that start a stay. */
    PING_APP_LEAK,
    /** From day [EventSynthesizer.FOOTPRINT_FALL_DAY] that app's pings fall, so the footprint experiment resolves MOVED. */
    FOOTPRINT_FALL,
    /** The listener is off for about two and three-quarter days from the start of week 5. */
    LISTENER_GAP,
    /** Sunday evenings are choppy: more stays than the other days. */
    SUNDAY_EVENING_CHOPPY,
    /** Many sub-30-second looks at another app: glances, not stays. */
    PANIC_GLANCES,
    /** A stay is two apps in a row, not one. */
    TAB_HOPPER,
}

data class PersonaSpec(
    val id: String, val name: String, val gentleDefault: Boolean,
    val studyBlocks: List<StudyBlock>, val workSetPkgs: List<Pkg>, val dependsPkgs: List<Pkg>,
    val leakPkg: Pkg, val otherPingPkgs: List<Pkg>, val weeks: List<WeekTarget>, val quirks: Set<Quirk>,
    val days: Int = 60, val firstReadDay: Int = 3,
) {
    init { require(weeks.size == 8) { "a persona has eight weekly targets" } }
}

/** Week 1-2 hold the starting normal; weeks 3-8 walk to the week-4 target, then on to the week-8 target. */
private fun walk(normal: WeekTarget, week4: WeekTarget, week8: WeekTarget): List<WeekTarget> {
    fun mix(a: WeekTarget, b: WeekTarget, t: Double) = WeekTarget(
        a.stretchMin + (b.stretchMin - a.stretchMin) * t, a.staysPerHour + (b.staysPerHour - a.staysPerHour) * t,
        a.returnMin + (b.returnMin - a.returnMin) * t, a.quietShare + (b.quietShare - a.quietShare) * t,
        a.stoneShare + (b.stoneShare - a.stoneShare) * t, Math.round(a.opens + (b.opens - a.opens) * t).toInt(),
    )
    return listOf(normal, normal, mix(normal, week4, 0.5), week4, mix(week4, week8, 0.25), mix(week4, week8, 0.5), mix(week4, week8, 0.75), week8)
}

private val eveningBlock = listOf(StudyBlock(19 * 60, 21 * 60))      // 19:00 to 21:00 every day

val PERSONAS: Map<String, PersonaSpec> = listOf(
    // DOC 1 §1.4.3: Steadiness 100, 116 and 136 at weeks 1-2, 4 and 8.
    PersonaSpec(
        id = "aarav", name = "Aarav", gentleDefault = false, studyBlocks = eveningBlock,
        workSetPkgs = listOf(Pkg("demo.notes"), Pkg("demo.reader"), Pkg("demo.practice")), dependsPkgs = emptyList(),
        leakPkg = Pkg("demo.chat"), otherPingPkgs = listOf(Pkg("demo.mail"), Pkg("demo.social")),
        weeks = walk(
            WeekTarget(10.0, 4.0, 6.0, 0.36, 0.75, 5), WeekTarget(12.0, 3.5, 5.0, 0.38, 0.75, 3), WeekTarget(14.0, 3.0, 4.0, 0.41, 0.50, 2),
        ),
        quirks = setOf(Quirk.REACHED_AFTER_2230, Quirk.LATE_NIGHT_SPILLOVER, Quirk.PING_APP_LEAK, Quirk.FOOTPRINT_FALL, Quirk.SUNDAY_EVENING_CHOPPY),
    ),
    // Gentle by default: shorter stretches, more quiet, few stays, many quick looks. Shows gentle mode and the return line.
    PersonaSpec(
        id = "meera", name = "Meera", gentleDefault = true, studyBlocks = listOf(StudyBlock(18 * 60, 20 * 60)),
        workSetPkgs = listOf(Pkg("demo.notes"), Pkg("demo.reader")), dependsPkgs = emptyList(),
        leakPkg = Pkg("demo.chat"), otherPingPkgs = listOf(Pkg("demo.social")),
        weeks = walk(
            WeekTarget(8.0, 2.5, 4.0, 0.50, 0.40, 4), WeekTarget(9.0, 2.2, 3.8, 0.51, 0.35, 3), WeekTarget(10.0, 2.0, 3.5, 0.52, 0.30, 2),
        ),
        quirks = setOf(Quirk.PANIC_GLANCES, Quirk.PING_APP_LEAK),
    ),
    // Depends-heavy tab-hopper; a two-day listener outage shows the honest partial-ping sentence.
    PersonaSpec(
        id = "rohan", name = "Rohan", gentleDefault = false, studyBlocks = listOf(StudyBlock(20 * 60, 22 * 60)),
        workSetPkgs = listOf(Pkg("demo.notes"), Pkg("demo.practice")), dependsPkgs = listOf(Pkg("demo.video"), Pkg("demo.browser")),
        leakPkg = Pkg("demo.social"), otherPingPkgs = listOf(Pkg("demo.chat"), Pkg("demo.mail")),
        weeks = walk(
            WeekTarget(7.0, 4.0, 5.0, 0.25, 0.60, 6), WeekTarget(8.0, 3.6, 4.6, 0.27, 0.55, 5), WeekTarget(9.0, 3.2, 4.2, 0.30, 0.50, 4),
        ),
        quirks = setOf(Quirk.TAB_HOPPER, Quirk.LISTENER_GAP, Quirk.PING_APP_LEAK),
    ),
).associateBy { it.id }
