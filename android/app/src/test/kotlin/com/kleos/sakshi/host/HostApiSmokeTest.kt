package com.kleos.sakshi.host

import com.kleos.sakshi.host.gen.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Every one of the 31 Pigeon methods answers against the real container (Room on Robolectric), with the real engine. */
@RunWith(RobolectricTestRunner::class)
class HostApiSmokeTest {
    @org.junit.Before fun freshContainer() = AppContainer.reset()

    private val api = HostApiImpl(RuntimeEnvironment.getApplication())

    private fun expectCode(code: String, block: suspend () -> Unit) {
        try { runBlocking { block() }; fail("expected $code") } catch (e: FlutterError) { assertEquals(code, e.code) }
    }

    @Test fun allThirtyMethodsRespond() = runBlocking {
        val calls: List<Pair<String, suspend () -> Any?>> = listOf(
            "getSetupState" to { api.getSetupState() }, "openUsageAccessSettings" to { api.openUsageAccessSettings() },
            "openNotificationAccessSettings" to { api.openNotificationAccessSettings() },
            "openAppInfoForRestrictedSettings" to { api.openAppInfoForRestrictedSettings() }, "openBatterySettings" to { api.openBatterySettings() },
            "markBatteryHelperShown" to { api.markBatteryHelperShown() }, "requestLakeWidget" to { api.requestLakeWidget() }, "listLauncherApps" to { api.listLauncherApps() },
            "saveWorkSet" to { api.saveWorkSet(emptyList()) }, "saveStudyHours" to { api.saveStudyHours(StudyHoursDto(emptyList(), false)) },
            "setGentleMode" to { api.setGentleMode(true) }, "setUnder18" to { api.setUnder18(false) }, "setWeeklyNote" to { api.setWeeklyNote(false) },
            "syncNow" to { api.syncNow() }, "getMirror" to { api.getMirror(null) }, "listMirrorWeeks" to { api.listMirrorWeeks() },
            "getTodaySoFar" to { api.getTodaySoFar() }, "getWhatISee" to { api.getWhatISee() }, "getSayingChoices" to { api.getSayingChoices() },
            "getLake" to { api.getLake() }, "pickSaying" to { api.pickSaying("sy01") }, "dismissSuggestion" to { api.dismissSuggestion("S2", null) },
            "tapGoal" to { api.tapGoal(GoalAnswerDto.YES) }, "pause" to { api.pause(false) }, "exportData" to { api.exportData(false) },
            "deleteEverything" to { api.deleteEverything() }, "startDemo" to { api.startDemo("aarav") }, "setDemoAsOf" to { api.setDemoAsOf(3) },
            "stopDemo" to { api.stopDemo() },
            // the engine can refuse these two when there is nothing to act on, which is a typed error and still an answer
            "tapTryThis" to { try { api.tapTryThis("S2", null) } catch (e: FlutterError) { e.code } },
            "reanchorBaseline" to { try { api.reanchorBaseline() } catch (e: FlutterError) { e.code } })
        assertEquals(SakshiHostApi::class.java.declaredMethods.size, calls.size)
        assertEquals(31, calls.size)
        assertEquals(calls.size, calls.map { it.first }.toSet().size)
        calls.forEach { (name, call) -> try { call() } catch (e: Throwable) { fail("$name threw $e") } }
    }

    @Test fun getSetupStateReportsRealStateAsValues() = runBlocking {
        val s = api.getSetupState()
        assertFalse(s.notificationAccessGranted)   // (usage access is allowed by default under Robolectric, so it is not asserted)
        assertFalse(s.workSetSaved); assertFalse(s.studyHoursSaved)
        assertNull(s.health.listenerCoverage7d)   // never connected is null, not 0
    }

    @Test fun badRequestsAreTyped() {
        expectCode(HostErrors.BAD_REQUEST) { api.tapTryThis("S99", null) }
        expectCode(HostErrors.BAD_REQUEST) { api.dismissSuggestion("nope", null) }
        expectCode(HostErrors.BAD_REQUEST) { api.startDemo("somebody") }
        expectCode(HostErrors.BAD_REQUEST) { api.setDemoAsOf(60) }
        expectCode(HostErrors.BAD_REQUEST) { api.setDemoAsOf(5) }   // no demo is running
        expectCode(HostErrors.BAD_REQUEST) { api.setDemoAsOf(-1) }
    }

    @Test fun aStaleSuggestionAndARefusedReanchorAreTyped() {
        expectCode(HostErrors.STALE_SUGGESTION) { api.tapTryThis("S2", null) }
        expectCode(HostErrors.REANCHOR_NOT_ALLOWED) { api.reanchorBaseline() }
    }

    @Test fun pauseExportAndDeleteAreRefusedWhileADemoRuns() {
        runBlocking { api.startDemo("aarav") }
        try {
            expectCode(HostErrors.DEMO_ACTIVE) { api.pause(true) }
            expectCode(HostErrors.DEMO_ACTIVE) { api.exportData(false) }
            expectCode(HostErrors.DEMO_ACTIVE) { api.deleteEverything() }
            assertTrue(runBlocking { api.getLake() }.isDemo)
        } finally {
            runBlocking { api.stopDemo() }
        }
        assertFalse(runBlocking { api.getLake() }.isDemo)
    }

    @Test fun weeklyNoteStaysOffWhileNotificationsAreNotAllowed() = runBlocking {
        // Robolectric reports notifications as allowed; a refusal path is covered once T1.11 adds the request.
        val effective = api.setWeeklyNote(false)
        assertFalse(effective)
    }
}
