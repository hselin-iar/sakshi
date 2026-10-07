package com.kleos.sakshi.host

import android.content.Context
import android.util.Log
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.host.gen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Pigeon host. Thin on purpose: check the gate, call the engine or an adapter, map the result.
 * Nothing here decides anything about attention; that belongs in the engine.
 * Every call runs on a background dispatcher, and an unexpected failure becomes a typed INTERNAL error.
 */
class HostApiImpl(context: Context, private val notificationRequester: PostNotificationsRequester? = null) : SakshiHostApi {
    private val appContext = context.applicationContext
    private val container get() = AppContainer.from(appContext)
    private val engine get() = container.engine
    private val permissions = PermissionGateway(appContext)

    /** The container's clock: the phone's during real use, the demo's own while a demo runs (the demo path reads no other time). */
    private fun now() = container.clock.now()

    /** Runs `block` off the main thread; FlutterErrors pass through, anything else becomes INTERNAL (class name only as detail). */
    private suspend fun <T> call(block: suspend () -> T): T = withContext(Dispatchers.IO) {
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

    // ---- setup ----
    override suspend fun getSetupState(): SetupStateDto = call {
        permissions.requestRebindIfNeeded()
        val week = now()
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
    override suspend fun openBatterySettings() = call { BatterySetup(appContext).open(); Unit }
    override suspend fun requestLakeWidget(): Boolean = call {
        val manager = android.appwidget.AppWidgetManager.getInstance(appContext)
        // Not every launcher can pin; the screen then says how to add it by hand.
        manager.isRequestPinAppWidgetSupported &&
            manager.requestPinAppWidget(android.content.ComponentName(appContext, LakeWidget::class.java), null, null)
    }
    override suspend fun markBatteryHelperShown() = call { engine.markBatteryHelperShown() }
    override suspend fun listLauncherApps(): List<AppDto> = call {
        // "Suggested" here is what is already chosen, so changing the work set starts from the current one rather than from nothing.
        // Smarter pre-ticking for a first choice is the engine's (F2) and is not in the façade yet.
        val saved = container.state.apps().associate { it.pkg.value to it.userClass }
        container.catalog.launcherApps().map {
            AppDto(
                pkg = it.pkg.value, label = it.label,
                suggestedInSet = saved[it.pkg.value] == com.kleos.sakshi.engine.model.UserClass.IN_SET,
                suggestedDepends = saved[it.pkg.value] == com.kleos.sakshi.engine.model.UserClass.DEPENDS)
        }
    }
    override suspend fun saveWorkSet(entries: List<WorkSetEntryDto>): SaveResultDto = call { engine.saveWorkSet(entries.map { it.toModel() }).toDto() }
    override suspend fun saveStudyHours(hours: StudyHoursDto) = call { engine.saveStudyHours(hours.toModel()) }
    override suspend fun setGentleMode(on: Boolean) = call { engine.setGentle(on); LakeWidget.refresh(appContext) }
    override suspend fun setUnder18(on: Boolean) = call { engine.setUnder18(on); LakeWidget.refresh(appContext) }
    override suspend fun setWeeklyNote(enabled: Boolean): Boolean = call {
        val allowed = permissions.canPostNotifications()
        // Asking needs the Activity. With none (cold start, worker) the request cannot be made, so it is a bad request.
        val granted = if (enabled && !allowed) {
            (notificationRequester ?: throw HostErrors.error(HostErrors.BAD_REQUEST, "no activity")).request()
        } else null
        val effective = WeeklyNoteNotifier.effective(enabled, allowed, granted)
        engine.setWeeklyNote(effective)          // a refusal leaves the setting off
        if (!effective) container.noteNotifier.cancel()
        effective
    }

    // ---- read ----
    override suspend fun syncNow(): SyncStatusDto = call {
        val at = now()
        val report = container.ingest(at)
        LakeWidget.refresh(appContext)   // app open: redraw from the stored row even when nothing new arrived
        when (report) {
            is IngestReport.Ran -> SyncStatusDto(state = SyncStateDto.OK, lastSyncEpochMs = at.value)
            IngestReport.Paused -> SyncStatusDto(state = SyncStateDto.PAUSED)
            IngestReport.DemoActive -> SyncStatusDto(state = SyncStateDto.OK, lastSyncEpochMs = at.value)   // nothing is read from the phone during a demo
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
    override suspend fun getWhatISee(): WhatISeeDto = call { engine.whatISee(now(), container.hostFacts()).toDto() }
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
        val result = try {
            container.exporter.export(includeRaw, now()).also { ExportSharer(appContext).share(it.file) }
        } catch (e: Exception) {
            throw HostErrors.error(HostErrors.EXPORT_FAILED, e::class.simpleName)
        }
        ExportDto(fileName = result.fileName, byteSize = result.byteSize)
    }
    override suspend fun deleteEverything() = call { refuseDuringDemo(); container.deleteEverything() }

    // ---- demo ----
    override suspend fun startDemo(personaId: String) = call {
        if (personaId !in DemoController.PERSONAS) throw HostErrors.error(HostErrors.BAD_REQUEST, "persona")
        container.demoController.start(personaId)
    }
    override suspend fun setDemoAsOf(dayIndex: Long) = call {
        if (dayIndex !in 0..59) throw HostErrors.error(HostErrors.BAD_REQUEST, "dayIndex")
        if (!container.demoActive) throw HostErrors.error(HostErrors.BAD_REQUEST, "no demo")
        container.demoController.setAsOf(dayIndex.toInt())
    }
    override suspend fun stopDemo() = call { container.demoController.stop() }

}
