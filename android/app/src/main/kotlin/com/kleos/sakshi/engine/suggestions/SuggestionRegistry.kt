package com.kleos.sakshi.engine.suggestions

import com.kleos.sakshi.engine.suggestions.rules.S01ClearHour
import com.kleos.sakshi.engine.suggestions.rules.S02RepeatLeak
import com.kleos.sakshi.engine.suggestions.rules.S03SelfStarted
import com.kleos.sakshi.engine.suggestions.rules.S04PingDriven
import com.kleos.sakshi.engine.suggestions.rules.S05SlowReturn
import com.kleos.sakshi.engine.suggestions.rules.S06EarlyFlinch
import com.kleos.sakshi.engine.suggestions.rules.S07WorkSetLeak
import com.kleos.sakshi.engine.suggestions.rules.S08LateNight
import com.kleos.sakshi.engine.suggestions.rules.S09NaturalRhythm
import com.kleos.sakshi.engine.suggestions.rules.S10GlanceReassurance
import com.kleos.sakshi.engine.suggestions.rules.S11AfterLapse
import com.kleos.sakshi.engine.suggestions.rules.S12Win

/** The twelve rules. Adding a thirteenth = one new file + one line here. */
object SuggestionRegistry {
    val all: List<SuggestionRule> = listOf(
        S01ClearHour, S02RepeatLeak, S03SelfStarted, S04PingDriven, S05SlowReturn, S06EarlyFlinch,
        S07WorkSetLeak, S08LateNight, S09NaturalRhythm, S10GlanceReassurance, S11AfterLapse, S12Win,
    )
}
