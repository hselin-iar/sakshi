package com.kleos.sakshi.engine.ask

import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.ports.Randomness

/** Picks a saying from the shelf, preferring ones whose "used for" matches. Quotes only ever come from the shelf, which holds real passages with their sources. */
object QuoteChooser {
    fun choose(shelf: List<Saying>, tags: List<String>, random: Randomness): Saying? {
        if (shelf.isEmpty()) return null
        val matching = shelf.filter { s -> tags.any { t -> s.usedFor.contains(t, ignoreCase = true) } }
        val pool = matching.ifEmpty { shelf }
        return pool[random.nextInt(pool.size)]
    }

    fun byId(shelf: List<Saying>, id: String?): Saying? = id?.let { wanted -> shelf.firstOrNull { it.id == wanted } }
}
