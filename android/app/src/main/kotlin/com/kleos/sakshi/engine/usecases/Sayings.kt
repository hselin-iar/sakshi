package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.model.SayingPick
import com.kleos.sakshi.engine.ports.Ports

/** The tags the current week matches (F13); they come from the Mirror's own facts, so this file holds no attention logic. */
object SayingTags {
    const val STONE_AND_WAVE = "Stone and wave"
    const val RETURN = "Return"
    const val QUIET_RHYTHM = "Quiet; rhythm"
    const val GLANCE_REASSURANCE = "Glance reassurance"
    const val TEACHER_LEAVES = "The Teacher Leaves"
    const val WINS = "Wins"
    const val AFTER_LAPSE = "After a lapse"
}

object ChooseSayings {
    private const val WEEK_MS = 7L * 86_400_000L
    private const val EXCLUDE_RECENT = 9
    private const val OFFER = 3
    private const val FIRST_OFFER_WEEK = 3

    /**
     * Offered when none has been picked and the user is in week 3 or later, or when 3 or more weeks have passed since the last pick.
     * Returns exactly three or nothing. Ties are broken by Randomness forked on the week number, so a given week always offers the same three.
     */
    fun run(ports: Ports, tags: Set<String>, weekNumber: Int, asOf: EpochMs): List<Saying> {
        val picks = ports.state.sayingPicks().sortedBy { it.pickedAt.value }
        val last = picks.lastOrNull()
        val offered = if (last == null) weekNumber >= FIRST_OFFER_WEEK
        else asOf.value - last.pickedAt.value >= com.kleos.sakshi.engine.tuning.Tuning.SAYING_OFFER_EVERY_WEEKS * WEEK_MS
        if (!offered) return emptyList()

        val shelf = ports.shelf.all()
        val recent = picks.takeLast(EXCLUDE_RECENT).map { it.sayingId }.toMutableList()
        // the exclusion window shrinks until at least three sayings remain
        while (shelf.count { it.id !in recent } < OFFER && recent.isNotEmpty()) recent.removeAt(0)
        val pool = shelf.filter { it.id !in recent }
        if (pool.size < OFFER) return emptyList()

        val rng = ports.random.fork(weekNumber.toLong())
        val keyed = pool.map { s -> Triple(s, tags.count { tag -> s.usedFor.contains(tag, ignoreCase = true) }, rng.nextDouble()) }
        return keyed.sortedWith(compareByDescending<Triple<Saying, Int, Double>> { it.second }.thenBy { it.third }).take(OFFER).map { it.first }
    }
}

object PickSaying {
    /** One tap, optional, never a command. An id that is not on the shelf is ignored. */
    fun run(ports: Ports, id: String, asOf: EpochMs) {
        if (ports.shelf.all().none { it.id == id }) return
        val next = (ports.state.sayingPicks().maxOfOrNull { it.id } ?: 0L) + 1
        ports.state.savePick(SayingPick(next, id, asOf))
    }
}
