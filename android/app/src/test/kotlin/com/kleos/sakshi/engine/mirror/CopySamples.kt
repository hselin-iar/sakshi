package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.metrics.Word
import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekStart

/** Every sentence SentenceBuilder can produce, over a generated matrix of arguments. Shared by the copy-rule tests. */
object CopySamples {
    private val numbers = listOf(0.0, 0.4, 0.5, 1.0, 1.4, 1.5, 2.0, 4.6, 12.0, 17.0, 99.5, 240.0)
    private val counts = listOf(0, 1, 2, 3, 9, 17, 100)

    private fun pattern(kind: PatternKind, key: String, args: Map<String, String>, w: Int, d: Int) =
        Pattern(kind, key, 1.0, w, d, EpochMs(0), EpochMs(0), args)

    fun patterns(): List<Pattern> {
        val out = mutableListOf<Pattern>()
        for (w in listOf(4, 9, 40)) for (d in listOf(3, 5, 14)) {
            for (cell in listOf("MORNING-WEEKDAY", "AFTERNOON-WEEKEND", "EVENING-WEEKDAY", "NIGHT-WEEKEND", "NIGHT-WEEKDAY"))
                for (dir in listOf("CHOPPY", "CLEAR"))
                    out += pattern(PatternKind.RHYTHM, "cell:$cell", mapOf("cell" to cell, "cellRate" to "6.0", "overallRate" to "3.0", "direction" to dir), w, d)
            for (part in listOf("stretch", "stays", "return", "quiet")) {
                for (dir in listOf("BETTER", "WORSE"))
                    out += pattern(PatternKind.TREND, "trend:$part", mapOf("part" to part, "values" to "7.0,6.0,5.0,4.0", "direction" to dir, "run3" to "true"), w, d)
                for (dir in listOf("UP", "DOWN"))
                    out += pattern(PatternKind.SHIFT, "shift:$part", mapOf("part" to part, "weekIndex" to "3", "direction" to dir), w, d)
            }
            for (after in listOf("22:30", "null"))
                out += pattern(PatternKind.SHAPE, "mix", mapOf("held" to "12", "pinged" to "9", "reached" to "9", "reachedStartsAfter" to after), w, d)
            for (name in listOf("Pinged", "Reached"))
                out += pattern(PatternKind.SHAPE, "cluster:EVENING-WEEKEND", mapOf("cell" to "EVENING-WEEKEND", "name" to name, "size" to "7"), w, d)
            for (cause in listOf("PING:com.example.app", "SELF", "MIXED"))
                out += pattern(PatternKind.BREAK_POINT, "break", mapOf("bandStartMin" to "20", "bandShare" to "0.55", "cause" to cause), w, d)
            out += pattern(PatternKind.CROSS_DAY, "crossday", mapOf("lateNights" to "5", "shorterByMin" to "6.0", "lateAfter" to "01:30"), w, d)
        }
        return out
    }

    fun candidates(): List<Candidate> {
        val out = mutableListOf<Candidate>()
        for (a in counts) for (b in counts) {
            val args = mapOf("count" to "$a", "of" to "$b", "minutes" to "5", "pct" to "$a", "startHour" to "9", "endHour" to "11", "stretchMin" to "22.0",
                "lateNights" to "$a", "lateAfter" to "01:30", "bandStartMin" to "20", "bandShare" to "0.5", "cause" to "SELF",
                "glances" to "$a", "stays" to "$b", "days" to "$a", "part" to "return", "values" to "7.0,6.0,5.0,4.0",
                "evidenceWindows" to "14", "evidenceDays" to "7")
            SuggestionKind.entries.forEach { k -> out += Candidate(k, null, 0.5, null, args, ActionType.NONE) }
            listOf("stretch", "stays", "return", "quiet").forEach { part -> out += Candidate(SuggestionKind.S12, null, 0.3, null, args + ("part" to part), ActionType.NONE) }
        }
        return out
    }

    fun all(): List<String> {
        val s = SentenceBuilder
        val out = mutableListOf<String>()
        for (c in numbers) for (r in numbers) Word.entries.forEach { out += s.headline(c, r, it) }
        for (c in numbers) for (p in numbers) out += s.provisionalHeadline(c, p)
        for (c in numbers) { out += s.stretchLine(c, null); out += s.stretchLine(c, c + 10); out += s.staysLine(c); out += s.returnLine(c); out += s.gentleReturnLine(c); out += s.todayLine(2, c) }
        for (q in listOf(0.0, 0.12, 0.5, 1.0)) out += s.quietLine(q)
        for (t in counts) for (st in counts) for (se in counts) out += s.stonesLine(t, st, se)
        for (h in 0..23) out += s.clearHourLine(h, h + 2, 22.0)
        out += listOf(s.learningHeadline(), s.tooLittleHeadline(), s.nothingToFix(), s.notEnoughForSuggestion(), s.todayLine(0, null), s.todayLine(1, null))
        for (v in 0..8) for (n in listOf(8, 14)) out += s.learningLine(v, n)
        out += patterns().map { s.patternLine(it) }
        for (c in candidates()) { out += s.suggestionLine(c) }
        ActionType.entries.forEach { out += s.actionLabel(it) }
        for (v in Verdict.entries) for (t in TargetMetric.entries) for (approx in listOf(false, true)) {
            s.verdictLine(v, t, 4.1, 3.0, approx)?.let { out += it }
            s.verdictLine(v, t, 0.5, 0.25, approx)?.let { out += it }
        }
        for (d in 0..30) out += s.lapseLine(d)
        for (a in counts) for (b in listOf<Int?>(null, 0, 3, 9)) { out += s.teacherLine(a, b, 2) }
        for (state in LakeState.entries) for (g in listOf(false, true)) out += s.lakePhrase(state, g)
        for (shape in Shape.entries) out += s.shapeWord(shape)!!
        for (d in listOf(0, 1, 2, 7)) out += s.dataLines(DataFlag.entries, d, d, if (d == 0) null else d)
        for (cov in listOf<Double?>(null, 0.0, 0.61, 1.0)) for (m in listOf<Int?>(null, 0, 1, 38)) out += s.whatISeeLines(cov, m, 61)
        for (w in 20000L..20020L step 7) out += s.weekLabel(WeekStart(StudyDay(w)))
        for (tier in "ABCD") out += s.tierLabel(tier)
        return out.filter { it.isNotEmpty() }
    }
}
