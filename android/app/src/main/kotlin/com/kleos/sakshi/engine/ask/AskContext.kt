package com.kleos.sakshi.engine.ask

import com.kleos.sakshi.engine.model.LakeView
import com.kleos.sakshi.engine.model.MirrorView
import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.model.TodayView
import java.time.ZoneId

/** Everything "Ask Sakshi" may talk about: the views the app already shows, the shelf of sayings, and the zone for clock times. Nothing else. */
data class AskContext(
    val mirror: MirrorView,
    val today: TodayView,
    val lake: LakeView,
    val sayings: List<Saying>,
    val zone: ZoneId,
)

/** A reply, before the host adds its source. `quote` is always a saying from the shelf, never text from anywhere else. */
data class AskAnswer(val text: String, val quote: Saying?)
