package com.kleos.sakshi.host

import android.content.Intent
import android.provider.Settings
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BatterySetupTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Test fun opensAndroidsOwnBatteryOptimizationListAndNothingElse() {
        assertEquals(BatterySetup.Opened.BATTERY_OPTIMIZATION_LIST, BatterySetup(app).open())
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, started.action)
        assertNull(shadowOf(app).nextStartedActivity)       // exactly one page, one tap
    }

    @Test fun neverUsesTheSystemExemptionRequestOrHoldsItsPermission() {
        BatterySetup(app).open()
        assertNotEquals("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", shadowOf(app).nextStartedActivity?.action)
        val pm = app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        assertFalse(pm.requestedPermissions.orEmpty().contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"))
        // and no source file asks for it: the only mention allowed is none at all
        val sources = File("src/main/kotlin/com/kleos/sakshi/host").walkTopDown().filter { it.extension == "kt" }
        sources.forEach { assertFalse("${it.name} references the exemption request", it.readText().contains("ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")) }
    }

    @Test fun fallsBackWhenAPhoneLacksThePage() {
        shadowOf(app).checkActivities(true)       // an intent with no handler now throws ActivityNotFoundException
        val pm = shadowOf(app.packageManager)
        val saver = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        pm.addResolveInfoForIntent(saver, android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply { packageName = "com.android.settings"; name = "Saver" }
        })
        assertEquals(BatterySetup.Opened.BATTERY_SAVER, BatterySetup(app).open())
        assertEquals(Settings.ACTION_BATTERY_SAVER_SETTINGS, shadowOf(app).nextStartedActivity.action)
    }

    @Test fun reportsNoneWhenNothingCanOpen() {
        shadowOf(app).checkActivities(true)
        assertEquals(BatterySetup.Opened.NONE, BatterySetup(app).open())
    }

    // ---- the OEM table ----
    @Test fun manufacturersMapToTheirOem() {
        assertEquals(BatteryTips.Oem.VIVO, BatteryTips.oemFor("vivo")); assertEquals(BatteryTips.Oem.VIVO, BatteryTips.oemFor("iQOO"))
        assertEquals(BatteryTips.Oem.NOTHING, BatteryTips.oemFor("Nothing"))
        assertEquals(BatteryTips.Oem.XIAOMI, BatteryTips.oemFor("Xiaomi")); assertEquals(BatteryTips.Oem.XIAOMI, BatteryTips.oemFor("POCO"))
        assertEquals(BatteryTips.Oem.OPPO, BatteryTips.oemFor("realme")); assertEquals(BatteryTips.Oem.OPPO, BatteryTips.oemFor("OnePlus"))
        assertEquals(BatteryTips.Oem.SAMSUNG, BatteryTips.oemFor(" samsung "))
        assertEquals(BatteryTips.Oem.OTHER, BatteryTips.oemFor("Google")); assertEquals(BatteryTips.Oem.OTHER, BatteryTips.oemFor(null))
        assertNull(BatteryTips.tipsFor("Google"))
    }

    @Test fun everyTableHasAtMostFiveShortPlainSteps() {
        BatteryTips.all.forEach { t ->
            assertTrue("${t.oem}: 1 to 5 steps", t.steps.size in 1..5)
            t.steps.forEach { s -> assertFalse("$s contains !", s.contains("!")); assertTrue(s.endsWith(".")) }
        }
        assertEquals(BatteryTips.Oem.entries.filter { it != BatteryTips.Oem.OTHER }.toSet(), BatteryTips.all.map { it.oem }.toSet())
    }

    @Test fun noPathIsClaimedAsVerifiedUntilSomeoneRanItOnAPhone() {
        assertTrue(BatteryTips.all.none { it.status == BatteryTips.Status.VERIFIED_ON_DEVICE })
    }

    @Test fun theResearchFileAndTheCodeTableSayTheSame() {
        val doc = File("../../docs/research/oem_battery.md").readText()
        BatteryTips.all.forEach { t ->
            assertTrue("${t.oem} heading with status", doc.contains("${t.systemName} — ${t.status.name}"))
            t.steps.forEachIndexed { i, step -> assertTrue("${t.oem} step ${i + 1} missing from oem_battery.md", doc.contains("${i + 1}. $step")) }
        }
    }
}
