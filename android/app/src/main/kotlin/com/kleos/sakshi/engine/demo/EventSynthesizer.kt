package com.kleos.sakshi.engine.demo

import com.kleos.sakshi.engine.model.AppInfo
import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ListenerSession
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.RemovalKind
import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.UserClass
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

data class SyntheticHistory(
    val events: List<RawEvent>, val notifs: List<NotifEvent>, val sessions: List<ListenerSession>,
    val studyBlocks: List<StudyBlock>, val apps: List<AppMeta>,
    /** Every package the history mentions, as the phone's launcher would list it, so the engine sees them as installed. */
    val launcherApps: List<AppInfo>, val firstReadDay: Int, val days: Int,
)

/**
 * The only engine file that builds timestamps from scratch, and it takes them from the caller's day starts, never from a clock.
 * Deterministic: the same persona and seed give the same history. It is written so that the REAL engine, run over the result, lands on
 * each week's targets; when a number is off, this file is calibrated, never the engine.
 *
 * Shape of a day: one window (the persona's evening block). The window holds n stays (n = round(P x hours x the day's spread)), each
 * lasting about R minutes (median forced to R), and n + 1 stretches between them. The stretches are cut so their length-weighted median
 * is C: enough of them are exactly C long to cover half the stretch time and the rest share what is left. Quiet is made of screen-off
 * spans shorter than three minutes inside the stretches (a longer one would be a put-down and cut the stretch). A stone is a ping from
 * the stay's own app 5-25 seconds before it. Opens of this app are placed outside the window.
 * DOC 3 wording: "number of stays = round(P x window-hours) (deterministic, no random counts)"; the day-to-day spread keeps the weekly
 * mean and gives the shape labels (Held, Pinged, Reached) something to separate.
 */
object EventSynthesizer {
    /**
     * From this day the leak app's pings fall, so the footprint experiment starts by itself on the Monday of week 8 and is judged two weeks
     * later, in time for the verdict to be on the Mirror the Week 8 preset opens. (DOC 3 says "after day 20"; that would put the verdict a
     * week before the preset.)
     */
    const val FOOTPRINT_FALL_DAY = 41
    private const val MIN = 60_000L
    private const val SEC = 1_000L
    private const val LEAK_SHARE_BEFORE = 0.85
    private const val LEAK_SHARE_AFTER = 0.25
    private const val MIN_PIECE_MIN = 0.6           // an in-set piece must last long enough to count as coming back (30 s) with room to spare
    private const val PUT_DOWN_SPAN_MIN = 3.2      // a screen-off span over PUT_DOWN_MIN but under RETURN_SCREEN_OFF_MIN: it ends a stretch without ending a return
    private const val MIN_PUT_DOWN_STRETCH = PUT_DOWN_SPAN_MIN + 1.5
    private const val MAX_SPAN_MIN = 2.9            // under PUT_DOWN_MIN so a quiet span never cuts a stretch
    private val SPREAD_PLAIN = listOf(0.75, 1.25, 1.0, 1.0, 0.75, 1.25, 1.0)
    private val SPREAD_SUNDAY_CHOPPY = listOf(0.75, 1.25, 1.0, 0.75, 1.0, 0.75)      // Monday to Saturday; Sunday is 1.5
    private val OPEN_DAYS = listOf(0, 3, 5, 1, 4, 6, 2)
    private val SPILL_NIGHTS = setOf(1, 3, 6, 9, 12)

    /** `dayStartMs(i)` is the instant study day i begins (04:00 local). Day 0 must be a Monday so persona weeks are calendar weeks. */
    fun synthesize(spec: PersonaSpec, seed: Long, dayStartMs: (Int) -> Long, ownPackage: Pkg): SyntheticHistory {
        val events = ArrayList<RawEvent>(); val notifs = ArrayList<NotifEvent>()
        val block = spec.studyBlocks.first()
        val winOffset = offset(block.startMinute); val winLen = (offset(block.endMinute, after = winOffset) - winOffset)
        val lengthMin = winLen / MIN.toDouble()
        val hours = lengthMin / 60.0
        val leakFall = Quirk.FOOTPRINT_FALL in spec.quirks
        var staysCarry = 0.0
        val spreadByDay = spreads(spec, seed)

        for (d in 0 until spec.days) {
            val rng = Random(seed * 1_000_003L + d)
            val target = spec.weeks[min(d / 7, 7)]
            val start = dayStartMs(d) + winOffset
            val spread = spreadByDay[d]

            // The engine smooths stays per hour as (stays + 1) / (hours + 1) over a week of seven windows; scaling by (H + 1) / H puts the
            // measured value on the target. A carry across days keeps the weekly total exact while each day stays a whole number.
            val smoothing = (hours * 7 + 1.0) / (hours * 7)
            val wanted = target.staysPerHour * hours * spread * smoothing + staysCarry
            val nStays = max(2, wanted.roundToInt())
            staysCarry = wanted - nStays
            val spilledLastNight = Quirk.LATE_NIGHT_SPILLOVER in spec.quirks && (d - 1) >= 0 && ((d - 1) % 14) in SPILL_NIGHTS
            val stoneShare = (target.stoneShare + 0.5 * (spread - 1.0)).coerceIn(0.0, 1.0)
            val leakShare = if (leakFall && d >= FOOTPRINT_FALL_DAY) LEAK_SHARE_AFTER else LEAK_SHARE_BEFORE
            val listenerDown = Quirk.LISTENER_GAP in spec.quirks && d in 28..30

            // ---- the stays: durations with the median forced to R, then which are stones and which app ----
            val raw = List(nStays) { rng.nextDouble(-1.0, 1.0) }.sorted()
            val centred = raw.map { it - median(raw) }
            val durations = centred.map { max(0.8, target.returnMin * (1.0 + 0.15 * it)) }.shuffled(rng).toMutableList()
            val stayMin = durations.sum()
            val room = lengthMin - stayMin
            if (room < (nStays + 1) * 1.6) durations.indices.forEach { durations[it] = durations[it] * ((lengthMin - (nStays + 1) * 1.6) / stayMin).coerceIn(0.3, 1.0) }
            val staySum = durations.sum()
            val stretchTotal = lengthMin - staySum

            // ---- the stretches: h of them exactly C long, the rest share what remains. When the stays leave stretches longer than C,
            //      extra breaks are made from put-downs (a screen-off span of a little over three minutes) ----
            val c = target.stretchMin
            val m0 = nStays + 1
            var count = if (stretchTotal / m0 > c) max(m0, (stretchTotal / c).toInt() + 1) else m0
            var lengths: MutableList<Double>
            var putDowns: Set<Int>
            while (true) {
                lengths = stretchLengths(stretchTotal, count, c).toMutableList()
                if (spilledLastNight && lengths[0] > 3.0) { val cut = min(6.0, lengths[0] - 2.0); lengths[0] -= cut; lengths[lengths.lastIndex] += cut }
                lengths.shuffle(rng)
                if (spilledLastNight) { val i = lengths.indices.minByOrNull { lengths[it] }!!; val first = lengths[0]; lengths[0] = lengths[i]; lengths[i] = first }
                val extra = count - m0
                val candidates = (0 until count - 1).filter { lengths[it] >= MIN_PUT_DOWN_STRETCH }.shuffled(rng)
                if (candidates.size >= extra || count == m0) { putDowns = candidates.take(extra).toSet(); break }
                count--
            }
            val stretches = lengths
            val m = count
            val actualStays = m - 1 - putDowns.size
            while (durations.size < actualStays) durations += target.returnMin
            val putDownMin = putDowns.size * PUT_DOWN_SPAN_MIN
            // busy days (more stays, shorter stretches) hold less quiet and calm days more, so the week's share still comes out at the target
            val quietTotal = target.quietShare * lengthMin * (2.0 - spread).coerceIn(0.4, 1.6)
            val quietFraction = ((quietTotal - putDownMin) / (stretchTotal - putDownMin)).coerceIn(0.0, 0.85)

            val stoneCount = (actualStays * stoneShare).roundToInt()
            val isStone = List(actualStays) { it < stoneCount }.shuffled(rng)

            // ---- lay the day out ----
            var t = start.toDouble()
            var workIdx = rng.nextInt(spec.workSetPkgs.size)
            fun work(): Pkg = spec.workSetPkgs[workIdx % spec.workSetPkgs.size].also { workIdx++ }
            fun at(ms: Double) = EpochMs(ms.roundToLong())
            var current: Pkg? = null
            fun resume(pkg: Pkg, ms: Double) { events += RawEvent(at(ms), RawType.ACTIVITY_RESUMED, pkg); current = pkg }
            fun pause(ms: Double) { current?.let { events += RawEvent(at(ms), RawType.ACTIVITY_PAUSED, it) }; current = null }

            val bigStretches = ArrayList<Pair<Double, Double>>()   // stretches long enough to hold a stray ping, for the pings that start no stay
            var stayNo = 0
            for (i in 0 until m) {
                val s = stretches[i]
                val endsInPutDown = i in putDowns
                layStretch(s, quietFraction, t, spec, rng, ::work, ::resume, ::pause, events, endsInPutDown)
                if (s >= 4.0) bigStretches += t to s
                t += s * MIN
                if (endsInPutDown) {
                    events += RawEvent(at(t), RawType.SCREEN_INTERACTIVE, null)
                } else if (i < m - 1) {
                    val dur = durations[stayNo]
                    val stone = isStone[stayNo]
                    stayNo++
                    val leakPick = rng.nextDouble() < leakShare
                    val pingPkg = if (leakPick || spec.otherPingPkgs.isEmpty()) spec.leakPkg else spec.otherPingPkgs[rng.nextInt(spec.otherPingPkgs.size)]
                    val ownPick = if (stone) pingPkg else pickSelfStarted(spec, rng)
                    pause(t)
                    if (Quirk.TAB_HOPPER in spec.quirks && dur > 2.0) {
                        val second = spec.otherPingPkgs.filter { it != ownPick }.ifEmpty { listOf(spec.leakPkg) }.let { it[rng.nextInt(it.size)] }
                        resume(ownPick, t); resume(second, t + dur * 0.55 * MIN)
                    } else resume(ownPick, t)
                    if (stone && !listenerDown) {
                        notifs += NotifEvent(at(t - (5 + rng.nextInt(21)) * SEC), pingPkg, "msg", NotifKind.POSTED, null, false)
                        if (rng.nextDouble() < 0.5) notifs += NotifEvent(at(t + SEC), pingPkg, "msg", NotifKind.REMOVED, RemovalKind.CLICK, false)
                    }
                    t += dur * MIN
                    pause(t)
                }
            }
            pause(t)

            // ---- pings that start no stay: from the leak app, in the middle of a long stretch, never near a stay ----
            if (!listenerDown) {
                val noise = max(1, (stoneCount * 0.8).roundToInt())
                repeat(noise) {
                    if (bigStretches.isEmpty()) return@repeat
                    val (from, len) = bigStretches[rng.nextInt(bigStretches.size)]
                    val pkg = if (rng.nextDouble() < leakShare) spec.leakPkg else spec.otherPingPkgs.firstOrNull() ?: spec.leakPkg
                    notifs += NotifEvent(at(from + len * MIN * (0.4 + 0.2 * rng.nextDouble())), pkg, "msg", NotifKind.POSTED, null, false)
                }
            }

            // ---- opens of this app: outside the window, a minute and a half each ----
            val perWeek = target.opens
            val slot = d % 7
            if (OPEN_DAYS.indexOf(slot) in 0 until perWeek) {
                val ts = dayStartMs(d) + (8 * 60 + 30) * MIN
                events += RawEvent(EpochMs(ts), RawType.ACTIVITY_RESUMED, ownPackage)
                events += RawEvent(EpochMs(ts + 90 * SEC), RawType.ACTIVITY_PAUSED, ownPackage)
            }

            // ---- a late night: the screen stays on past 01:30 and goes off at 01:40, in an app that is not in the set ----
            if (Quirk.LATE_NIGHT_SPILLOVER in spec.quirks && (d % 14) in SPILL_NIGHTS) {
                val video = spec.otherPingPkgs.firstOrNull() ?: spec.leakPkg
                events += RawEvent(EpochMs(start + winLen + 20 * MIN), RawType.ACTIVITY_RESUMED, video)
                events += RawEvent(EpochMs(dayStartMs(d) + 21 * 60 * MIN + 40 * MIN), RawType.SCREEN_NON_INTERACTIVE, null)
            }
        }

        val sessions = if (Quirk.LISTENER_GAP in spec.quirks)
            // off from the start of day 28 until 21:00 on day 30: about two and three-quarter days, so that week's coverage is under 70 percent
            listOf(ListenerSession(EpochMs(dayStartMs(0)), EpochMs(dayStartMs(28))), ListenerSession(EpochMs(dayStartMs(30) + 17 * 60 * MIN), null))
        else listOf(ListenerSession(EpochMs(dayStartMs(0)), null))

        val addedAt = EpochMs(dayStartMs(0))
        val apps = spec.workSetPkgs.map { AppMeta(it, label(it), null, UserClass.IN_SET, addedAt) } +
            spec.dependsPkgs.map { AppMeta(it, label(it), null, UserClass.DEPENDS, addedAt) }
        val everyPkg = (spec.workSetPkgs + spec.dependsPkgs + spec.leakPkg + spec.otherPingPkgs).distinct()
        val launcher = everyPkg.map { AppInfo(it, label(it), null) }
        return SyntheticHistory(events.sortedBy { it.ts.value }, notifs.sortedBy { it.ts.value }, sessions, spec.studyBlocks, apps, launcher, spec.firstReadDay, spec.days)
    }

    // ---- helpers ----

    /** Minutes after 04:00 for a minute-of-day; a block that crosses midnight ends after the day's own minutes. */
    private fun offset(minuteOfDay: Int, after: Long = 0L): Long {
        val o = (if (minuteOfDay < 240) minuteOfDay + 1440 else minuteOfDay) - 240
        return o * MIN
    }

    /**
     * Each day's multiplier on the week's stay count. A week's multipliers add to seven, so weekly means are exact. The eight days the
     * baseline is frozen from are scaled to average exactly 1.0, so "the starting normal" is the normal the targets describe.
     */
    private fun spreads(spec: PersonaSpec, seed: Long): DoubleArray {
        val out = DoubleArray(spec.days)
        for (week in 0 until (spec.days + 6) / 7) {
            val rng = Random(seed * 7919L + week)
            val plain = if (Quirk.SUNDAY_EVENING_CHOPPY in spec.quirks) SPREAD_SUNDAY_CHOPPY.shuffled(rng) + 1.5 else SPREAD_PLAIN.shuffled(rng)
            for (dow in 0 until 7) if (week * 7 + dow < spec.days) out[week * 7 + dow] = plain[dow]
        }
        val first = spec.firstReadDay
        val baseline = (first until min(first + 8, spec.days)).toList()
        if (baseline.size == 8) {
            val k = 8.0 / baseline.sumOf { out[it] }
            baseline.forEach { out[it] *= k }
            for (week in 0..1) {
                val days = (week * 7 until min(week * 7 + 7, spec.days)).toList()
                val inBaseline = days.filter { it in baseline }; val rest = days.filter { it !in baseline }
                if (rest.isNotEmpty()) { val f = (7.0 - inBaseline.sumOf { out[it] }) / rest.sumOf { out[it] }; rest.forEach { out[it] *= f } }
            }
        }
        return out
    }

    private fun pickSelfStarted(spec: PersonaSpec, rng: Random): Pkg {
        val pool = listOf(spec.leakPkg) + spec.otherPingPkgs
        return pool[rng.nextInt(pool.size)]
    }

    private fun median(sorted: List<Double>): Double =
        if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0

    /** m lengths adding to `total`; the length-weighted median of them is `c` whenever that is reachable. */
    internal fun stretchLengths(total: Double, m: Int, c: Double): List<Double> {
        if (total / m >= c) return List(m) { c }.toMutableList().also { it[0] += total - m * c }
        val h = min(m, ceil(total / 2.0 / c).toInt().coerceAtLeast(1))
        val rest = if (h < m) (total - h * c) / (m - h) else 0.0
        return List(h) { c } + List(m - h) { max(rest, 1.5) }
    }

    /** A stretch of `s` minutes starting at `from`: in-set pieces with short screen-off spans between them, glances and depends hops on top. */
    private fun layStretch(
        s: Double, quietFraction: Double, from: Double, spec: PersonaSpec, rng: Random,
        work: () -> Pkg, resume: (Pkg, Double) -> Unit, pause: (Double) -> Unit, events: MutableList<RawEvent>, endsInPutDown: Boolean,
    ) {
        val body = if (endsInPutDown) s - PUT_DOWN_SPAN_MIN else s
        var q = body * quietFraction
        var k = ceil(q / MAX_SPAN_MIN).toInt()
        while (k > 0 && (body - q) / (k + 1) < MIN_PIECE_MIN) { k--; q = min(q, k * MAX_SPAN_MIN) }
        if (k == 0) q = 0.0
        val piece = (body - q) / (k + 1)
        val span = if (k > 0) q / k else 0.0
        var t = from
        for (i in 0..k) {
            val app = work()
            resume(app, t)
            if (Quirk.PANIC_GLANCES in spec.quirks && piece >= 2.5) {
                var g = 1.0
                while (g + 0.5 < piece && g < 7.0) {
                    val gt = t + g * MIN
                    val away = if (spec.otherPingPkgs.isNotEmpty()) spec.otherPingPkgs[0] else spec.leakPkg
                    events += RawEvent(EpochMs(gt.roundToLong()), RawType.ACTIVITY_RESUMED, away)
                    events += RawEvent(EpochMs((gt + 12 * SEC).roundToLong()), RawType.ACTIVITY_RESUMED, app)
                    g += 1.0
                }
            }
            if (spec.dependsPkgs.isNotEmpty() && piece >= 3.0) {
                val hop = spec.dependsPkgs[rng.nextInt(spec.dependsPkgs.size)]
                val ht = t + piece * 0.5 * MIN
                events += RawEvent(EpochMs(ht.roundToLong()), RawType.ACTIVITY_RESUMED, hop)
                events += RawEvent(EpochMs((ht + 70 * SEC).roundToLong()), RawType.ACTIVITY_RESUMED, app)
            }
            t += piece * MIN
            if (i == k && endsInPutDown) {
                pause(t)
                events += RawEvent(EpochMs(t.roundToLong()), RawType.SCREEN_NON_INTERACTIVE, null)
            }
            if (i < k) {
                pause(t)
                events += RawEvent(EpochMs(t.roundToLong()), RawType.SCREEN_NON_INTERACTIVE, null)
                t += span * MIN
                events += RawEvent(EpochMs(t.roundToLong()), RawType.SCREEN_INTERACTIVE, null)
            }
        }
    }

    private fun label(pkg: Pkg) = pkg.value.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}
