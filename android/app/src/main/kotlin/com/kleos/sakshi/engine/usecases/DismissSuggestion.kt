package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F11: dismissing silences that kind and subject for SUGGEST_DISMISS_WEEKS and nothing else. S7's "never ask again about this
 * app" is dismissed forever for that subject.
 */
object DismissSuggestion {
    private const val WEEK_MS = 7L * 86_400_000L

    fun run(ports: Ports, kind: SuggestionKind, subject: Pkg?, asOf: EpochMs) {
        val until = if (kind == SuggestionKind.S7 && subject != null) EpochMs(Long.MAX_VALUE)
        else EpochMs(asOf.value + Tuning.SUGGEST_DISMISS_WEEKS * WEEK_MS)
        val existing = ports.state.suggestionStates().firstOrNull { it.kind == kind && it.subject == subject }
        ports.state.saveSuggestionState(
            (existing ?: SuggestionState(kind, subject, null, null, null, null, null, "dismissed")).copy(dismissedUntil = until),
        )
    }
}
