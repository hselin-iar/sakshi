package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.classify.WorkSetPolicy
import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.SaveResult
import com.kleos.sakshi.engine.model.Settings
import com.kleos.sakshi.engine.model.StudyHours
import com.kleos.sakshi.engine.model.WorkSetEntry
import com.kleos.sakshi.engine.ports.Ports

/** F16: an explicit gentle choice wins; until one is made the age default decides. */
fun Settings.effectiveGentle(): Boolean = if (gentleExplicit) gentleMode else ageUnder18

/** One-line settings writes. None of them touches derived data except where a recompute is the caller's job (work set, study hours). */
object SettingsUseCases {
    private fun update(ports: Ports, change: (Settings) -> Settings) = ports.state.saveSettings(change(ports.state.settings()))

    /** An explicit choice, so it overrides the age default from then on. */
    fun setGentle(ports: Ports, on: Boolean) = update(ports) { it.copy(gentleMode = on, gentleExplicit = true) }

    /** The age flag is one boolean used for the gentle default and nothing else. */
    fun setUnder18(ports: Ports, on: Boolean) = update(ports) { it.copy(ageUnder18 = on) }

    fun setWeeklyNote(ports: Ports, on: Boolean) = update(ports) { it.copy(weeklyNoteEnabled = on) }
    fun markBatteryHelperShown(ports: Ports) = update(ports) { it.copy(batteryHelperShown = true) }
    fun saveStudyHours(ports: Ports, h: StudyHours) = update(ports) { it.copy(studyBlocks = h.blocks, learnStudyHours = h.learnForMe) }

    /** First contact with the phone: the baseline counts from here (D2), and createdAt stops being the epoch. */
    fun ensureInitialised(ports: Ports, now: EpochMs) {
        val s = ports.state.settings()
        if (s.firstReadAt != null && s.createdAt.value != 0L) return
        val first = s.firstReadAt ?: ports.state.ingest().firstReadAt ?: now
        ports.state.saveSettings(s.copy(firstReadAt = first, createdAt = if (s.createdAt.value == 0L) now else s.createdAt))
    }

    /**
     * F2: replaces every AppMeta row with a class. An app that is no longer installed is dropped and counted in the message;
     * over the cap nothing is saved. Re-deriving the days under the new set is the caller's step.
     */
    fun saveWorkSet(ports: Ports, entries: List<WorkSetEntry>, now: EpochMs): SaveResult {
        val validation = WorkSetPolicy.validate(entries)
        if (!validation.ok) return SaveResult(ok = false, savedCount = 0, userMessage = validation.userMessage)

        val installed = ports.catalog.launcherApps().associateBy { it.pkg }
        val previous = ports.state.apps().associateBy { it.pkg }
        val chosen = WorkSetPolicy.chosen(entries).distinctBy { it.pkg }
        val kept = chosen.filter { installed.isEmpty() || it.pkg in installed }
        val dropped = chosen.size - kept.size

        ports.state.replaceApps(kept.map { e ->
            AppMeta(
                pkg = e.pkg, label = installed[e.pkg]?.label ?: e.pkg.value, systemCategory = ports.catalog.category(e.pkg),
                userClass = e.userClass, addedAt = previous[e.pkg]?.addedAt ?: now,
            )
        })
        val note = if (dropped > 0) "${if (dropped == 1) "1 app was" else "$dropped apps were"} no longer installed and left out." else null
        return SaveResult(ok = true, savedCount = kept.size, userMessage = note)
    }
}
