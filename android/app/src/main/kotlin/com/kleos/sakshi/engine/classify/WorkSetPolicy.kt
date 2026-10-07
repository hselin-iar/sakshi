package com.kleos.sakshi.engine.classify

import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.model.WorkSetEntry
import com.kleos.sakshi.engine.tuning.Tuning

data class WorkSetValidation(val ok: Boolean, val userMessage: String?)

/**
 * F2: the cap is enforced here, not only in the UI. IN_SET and DEPENDS both count toward WORKSET_CAP; NONE does not.
 * The message is a fixed set; the UI shows a counter but never owns the rule.
 */
object WorkSetPolicy {
    fun chosen(entries: List<WorkSetEntry>): List<WorkSetEntry> = entries.filter { it.userClass != UserClass.NONE }

    fun validate(entries: List<WorkSetEntry>): WorkSetValidation =
        if (chosen(entries).distinctBy { it.pkg }.size > Tuning.WORKSET_CAP) WorkSetValidation(false, "Pick up to ${Tuning.WORKSET_CAP}. Fewer is better.")
        else WorkSetValidation(true, null)
}
