package com.kleos.sakshi.host

import android.content.Context
import android.util.Log
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.HostFacts
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.host.gen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Pigeon host. Thin on purpose: check the gate, call the engine or an adapter, map the result.
 * Nothing here decides anything about attention; that belongs in the engine.
 * Every call runs on a background dispatcher, and an unexpected failure becomes a typed INTERNAL error.
 */
class HostApiImpl(context: Context) : SakshiHostApi {
    private val appContext = context.applicationContext
    private val container get() = AppContainer.from(appContext)
    private val engine get() = container.engine
    private val permissions = PermissionGateway(appContext)

    private fun now() = EpochMs(System.currentTimeMillis())

    /** Runs `block` off the main thread; FlutterErrors pass through, anything else becomes INTERNAL (class name only as detail). */
    private suspend fun <T> call(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: FlutterError) {
            throw e
        } catch (e: Exception) {
            Log.w("SakshiHost", "failed: ${e::class.simpleName}")
            throw HostErrors.error(HostErrors.INTERNAL, e::class.simpleName)
        }
    }

    private fun refuseDuringDemo() {
        if (container.demoActive) throw HostErrors.error(HostErrors.DEMO_ACTIVE)
    }

    private fun hostFacts() = HostFacts(
        usageAccessGranted = permissions.usageAccessGranted(), notificationAccessGranted = permissions.notificationListenerEnabled(),
        canPostNotifications = permissions.canPostNotifications(), lastError = container.state.ingest().lastError)

    // ---- setup ----
    override suspend fun getSetupState(): SetupStateDto = call {
        permissions.requestRebindIfNeeded()
        val week = EpochMs(System.currentTimeMillis())
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        // Null, not zero, when the listener has never been connected: "never heard" is not "heard nothing".
        val heard = container.notifs.sessions(EpochMs(0), EpochMs(week.value + 1)).isNotEmpty()
        val state = setupStateDto(
            usageAccessGranted = permissions.usageAccessGranted(),
            notificationAccessGranted = permissions.notificationListenerEnabled(),
            restrictedSettingsSuspected = permissions.restrictedSettingsSuspected(),
            settings = container.state.settings(),
            workSetSaved = container.state.apps().any { it.userClass != com.kleos.sakshi.engine.model.UserClass.NONE },
            ingest = container.state.ingest(),
            listenerCoverage7d = if (heard) container.notifs.coverageFraction(EpochMs(week.value - sevenDays), week) else null,
            isDemo = container.demoActive)
        Log.i("SakshiSetup", "usage=${state.usageAccessGranted} notif=${state.notificationAccessGranted} restricted=${state.restrictedSettingsSuspected}")
        state
    }
    override suspend fun openUsageAccessSettings() = call { permissions.openUsageAccessSettings() }
    override suspend fun openNotificationAccessSettings() = call { permissions.openNotificationAccessSettings() }
    override suspend fun openAppInfoForRestrictedSettings() = call { permissions.openAppInfo() }
    override suspend fun openBatterySettings() = call { /* T1.12 opens Android's own battery page */ }
    override suspend fun markBatteryHelperShown() = call { engine.markBatteryHelperShown() }
    override suspend fun listLauncherApps(): List<AppDto> = call {
        // Pre-ticking is the engine's (F2 suggestPreticks); it is not part of the façade yet, so nothing is suggested.
        container.catalog.launcherApps().map { AppDto(pkg = it.pkg.value, label = it.label, suggestedInSet = false, suggestedDepends = false) }
    }
    override suspend fun saveWorkSet(entries: List<WorkSetEntryDto>): SaveResultDto = call { engine.saveWorkSet(entries.map { it.toModel() }).toDto() }
    override suspend fun saveStudyHours(hours: StudyHoursDto) = call { engine.saveStudyHours(hours.toModel()) }
    override suspend fun setGentleMode(on: Boolean) = call { engine.setGentle(on) }
    override suspend fun setUnder18(on: Boolean) = call { engine.setUnder18(on) }
    override suspend fun setWeeklyNote(enabled: Boolean): Boolean = call {
        // Asking for POST_NOTIFICATIONS needs the Activity; T1.11 adds that. Until then "on" holds only if it is already allowed.
        val effective = enabled && permissions.canPostNotifications()
        engine.setWeeklyNote(effective)
        effective
    }

    // ---- read ----
    override suspend fun syncNow(): SyncStatusDto = call {
        val at = now()
        when (container.ingest(at)) {
            is IngestReport.Ran -> SyncStatusDto(state = SyncStateDto.OK, lastSyncEpochMs = at.value)
            IngestReport.Paused -> SyncStatusDto(state = SyncStateDto.PAUSED)
            IngestReport.NoPermission -> SyncStatusDto(state = SyncStateDto.NO_PERMISSION)
            // The reason is a short code in ingest_state; this fixed sentence is all the user sees.
            is IngestReport.Failed -> SyncStatusDto(state = SyncStateDto.OK, message = "I could not finish reading just now.")
        }
    }
    override suspend fun getMirror(weekStartEpochMs: Long?): MirrorDto = call {
        engine.mirror(weekStartEpochMs?.let { weekStartOf(it) }, now()).toDto()
    }
    override suspend fun listMirrorWeeks(): List<WeekRefDto> = call { engine.listMirrorWeeks(now()).map { it.toDto() } }
    override suspend fun getTodaySoFar(): TodayDto = call { engine.today(now()).toDto() }
    override suspend fun getWhatISee(): WhatISeeDto = call { engine.whatISee(now(), hostFacts()).toDto() }
    override suspend fun getSayingChoices(): List<SayingDto> = call { engine.chooseSayings(now()).map { it.toDto() } }
    override suspend fun getLake(): LakeDto = call { engine.lake(now()).toDto(isDemo = container.demoActive) }

    // ---- write ----
    override suspend fun pickSaying(sayingId: String) = call { engine.pickSaying(sayingId, now()) }
    override suspend fun tapTryThis(kindId: String, subjectKey: String?) = call {
        val kind = suggestionKindOf(kindId) ?: throw HostErrors.error(HostErrors.BAD_REQUEST, "kind")
        if (!engine.tapTryThis(kind, subjectKey?.let(::Pkg), now()).ok) throw HostErrors.error(HostErrors.STALE_SUGGESTION)
    }
    override suspend fun dismissSuggestion(kindId: String, subjectKey: String?) = call {
        val kind = suggestionKindOf(kindId) ?: throw HostErrors.error(HostErrors.BAD_REQUEST, "kind")
        engine.dismissSuggestion(kind, subjectKey?.let(::Pkg), now())
    }
    override suspend fun tapGoal(answer: GoalAnswerDto) = call { engine.tapGoal(answer.toModel(), now()) }
    override suspend fun reanchorBaseline() = call {
        if (!engine.reanchor(now()).ok) throw HostErrors.error(HostErrors.REANCHOR_NOT_ALLOWED)
    }
    override suspend fun pause(on: Boolean) = call { refuseDuringDemo(); engine.pause(on, now()) }
    override suspend fun exportData(includeRaw: Boolean): ExportDto = call {
        refuseDuringDemo()
        engine.export(includeRaw, now())
        // T1.13: Exporter writes the file, then the share sheet opens. Until then there is no file to describe.
        ExportDto(fileName = "", byteSize = 0)
    }
    override suspend fun deleteEverything() = call { refuseDuringDemo(); engine.deleteEverything() }

    // ---- demo (DemoController arrives in T1.13; requests are validated here, nothing starts yet) ----
    override suspend fun startDemo(personaId: String) = call {
        if (personaId !in DEMO_PERSONAS) throw HostErrors.error(HostErrors.BAD_REQUEST, "persona")
    }
    override suspend fun setDemoAsOf(dayIndex: Long) = call {
        if (dayIndex !in 0..59) throw HostErrors.error(HostErrors.BAD_REQUEST, "dayIndex")
    }
    override suspend fun stopDemo() = call { container.demoActive = false }

    private companion object {
        val DEMO_PERSONAS = setOf("aarav", "meera", "rohan")
    }
}
