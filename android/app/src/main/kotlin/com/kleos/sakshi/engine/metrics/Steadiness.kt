package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.tuning.Tuning
import kotlin.math.pow
import kotlin.math.round

enum class Word { WAVERING, STEADY, STEADIER }

data class Steadiness(val value: Int, val word: Word)

/**
 * F5: s = clamp(C/C0)^0.35 * clamp(P0/P)^0.30 * clamp(R0/R)^0.20 * clamp(Q/Q0)^0.15,
 * each ratio clamped to [0.5, 1.5]; value = round(100*s). Null if any of the
 * four parts is missing — never a fabricated number.
 */
fun steadiness(parts: Parts, baseline: Baseline): Steadiness? {
    val c = parts.stretchMin ?: return null
    val p = parts.staysPerHour ?: return null
    val r = parts.returnMin ?: return null
    val q = parts.quietShare ?: return null

    val cRatio = clampRatio(c / baseline.c0)
    val pRatio = clampRatio(baseline.p0 / p)
    val rRatio = clampRatio(baseline.r0 / r)
    val qRatio = clampRatio(q / baseline.q0)

    val s = cRatio.pow(Tuning.W_STRETCH) * pRatio.pow(Tuning.W_STAYS) *
        rRatio.pow(Tuning.W_RETURN) * qRatio.pow(Tuning.W_QUIET)
    val value = round(100 * s).toInt()

    val word = when {
        value < Tuning.WORD_WAVERING_BELOW -> Word.WAVERING
        value > Tuning.WORD_STEADIER_ABOVE -> Word.STEADIER
        else -> Word.STEADY
    }
    return Steadiness(value, word)
}

private fun clampRatio(x: Double): Double = x.coerceIn(Tuning.RATIO_CLAMP_LO, Tuning.RATIO_CLAMP_HI)
