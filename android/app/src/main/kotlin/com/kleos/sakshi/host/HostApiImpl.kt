package com.kleos.sakshi.host

import android.content.Context
import android.util.Log
import com.kleos.sakshi.engine.model.EpochMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kleos.sakshi.host.gen.*

/** T1.2: every method returns a canned, valid DTO or is a no-op. T1.9 wires the engine and mappers. */
class HostApiImpl(context: Context) : SakshiHostApi {
    private val appContext = context.applicationContext
    private val permissions = PermissionGateway(appContext)

    // setup
    override suspend fun getSetupState(): SetupStateDto {
        permissions.requestRebindIfNeeded()
        val state = SetupStateDto(
            usageAccessGranted = permissions.usageAccessGranted(),
            notificationAccessGranted = permissions.notificationListenerEnabled(),
            restrictedSettingsSuspected = permissions.restrictedSettingsSuspected(),
            workSetSaved = false, studyHoursSaved = false, batteryHelperShown = false, weeklyNoteEnabled = false,
            gentleMode = false, isDemo = false,
            health = CollectionHealthDto(workerRuns7d = 0, paused = false))
        Log.i("SakshiSetup", "usage=${state.usageAccessGranted} notif=${state.notificationAccessGranted} restricted=${state.restrictedSettingsSuspected}")
        return state
    }
    override suspend fun openUsageAccessSettings() = permissions.openUsageAccessSettings()
    override suspend fun openNotificationAccessSettings() = permissions.openNotificationAccessSettings()
    override suspend fun openAppInfoForRestrictedSettings() = permissions.openAppInfo()
    override suspend fun openBatterySettings() {}
    override suspend fun markBatteryHelperShown() {}
    override suspend fun listLauncherApps(): List<AppDto> = emptyList()
    override suspend fun saveWorkSet(entries: List<WorkSetEntryDto>): SaveResultDto = SaveResultDto(ok = true, savedCount = 0)
    override suspend fun saveStudyHours(hours: StudyHoursDto) {}
    override suspend fun setGentleMode(on: Boolean) {}
    override suspend fun setUnder18(on: Boolean) {}
    override suspend fun setWeeklyNote(enabled: Boolean): Boolean = false

    // read
    override suspend fun syncNow(): SyncStatusDto = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        when (val report = AppContainer.from(appContext).ingest(EpochMs(now))) {
            is IngestReport.Ran -> SyncStatusDto(state = SyncStateDto.OK, lastSyncEpochMs = now)
            IngestReport.Paused -> SyncStatusDto(state = SyncStateDto.PAUSED)
            IngestReport.NoPermission -> SyncStatusDto(state = SyncStateDto.NO_PERMISSION)
            // The reason is a short code in ingest_state; this fixed sentence is all the user sees.
            is IngestReport.Failed -> SyncStatusDto(state = SyncStateDto.OK, message = "I could not finish reading just now.")
        }
    }
    override suspend fun getMirror(weekStartEpochMs: Long?): MirrorDto = MirrorDto(
        isDemo = false, provisional = true, gentle = false, weekStartEpochMs = weekStartEpochMs ?: 0L, weekLabel = "",
        dataState = DataStateDto.LEARNING_BASELINE, dataFlags = emptyList(), dataLines = emptyList(), headline = "",
        patterns = emptyList(), nothingToFix = false, goalTap = GoalTapDto(offered = false), reanchorOffered = false)
    override suspend fun listMirrorWeeks(): List<WeekRefDto> = emptyList()
    override suspend fun getTodaySoFar(): TodayDto = TodayDto(
        isDemo = false, windows = emptyList(), line = "", dataFlags = emptyList(), dataLines = emptyList())
    override suspend fun getWhatISee(): WhatISeeDto = WhatISeeDto(
        isDemo = false, usageAccessGranted = false, notificationAccessGranted = false, rawEventCount = 0, notifEventCount = 0,
        derivedDays = 0, workerRuns7d = 0, paused = false, oddEventPairs = 0, lines = emptyList())
    override suspend fun getSayingChoices(): List<SayingDto> = emptyList()
    override suspend fun getLake(): LakeDto = LakeDto(state = LakeStateDto.NO_DATA, phrase = "", isDemo = false)

    // write
    override suspend fun pickSaying(sayingId: String) {}
    override suspend fun tapTryThis(kindId: String, subjectKey: String?) {}
    override suspend fun dismissSuggestion(kindId: String, subjectKey: String?) {}
    override suspend fun tapGoal(answer: GoalAnswerDto) {}
    override suspend fun reanchorBaseline() {}
    override suspend fun pause(on: Boolean) {}
    override suspend fun exportData(includeRaw: Boolean): ExportDto = ExportDto(fileName = "", byteSize = 0)
    override suspend fun deleteEverything() {}

    // demo
    override suspend fun startDemo(personaId: String) {}
    override suspend fun setDemoAsOf(dayIndex: Long) {}
    override suspend fun stopDemo() {}
}
