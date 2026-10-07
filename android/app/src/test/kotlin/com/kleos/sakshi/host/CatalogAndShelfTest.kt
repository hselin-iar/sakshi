package com.kleos.sakshi.host

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.telecom.TelecomManager
import com.kleos.sakshi.engine.model.Pkg
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CatalogAndShelfTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val spm get() = shadowOf(context.packageManager)
    private val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    private fun install(pkg: String, label: String, category: Int = ApplicationInfo.CATEGORY_UNDEFINED, launcher: Boolean = true): ApplicationInfo {
        val app = ApplicationInfo().apply { packageName = pkg; name = pkg; nonLocalizedLabel = label; this.category = category }
        spm.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = app })
        if (launcher) spm.addResolveInfoForIntent(launcherIntent, ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = pkg; name = "$pkg.Main"; applicationInfo = app }
        })
        return app
    }

    private lateinit var catalog: AppCatalogImpl
    @Before fun setUp() { catalog = AppCatalogImpl(context, NeutralPackages(setOf("com.android.calculator2"))) }

    @Test fun launcherAppsComeBackWithLabelsAndCategoriesSortedByName() {
        install("com.zeta", "Zeta Notes", ApplicationInfo.CATEGORY_PRODUCTIVITY)
        install("com.alpha", "alpha Chat", ApplicationInfo.CATEGORY_SOCIAL)
        install("com.plain", "Plain")
        val apps = catalog.launcherApps()
        assertEquals(listOf("alpha Chat", "Plain", "Zeta Notes"), apps.map { it.label })
        assertEquals(listOf(ApplicationInfo.CATEGORY_SOCIAL, null, ApplicationInfo.CATEGORY_PRODUCTIVITY), apps.map { it.category })
    }

    @Test fun sakshiItselfIsNotOfferedAsAWorkSetChoice() {
        install(context.packageName, "Sakshi")
        assertFalse(catalog.launcherApps().any { it.pkg.value == context.packageName })
    }

    @Test fun aPackageWithTwoLauncherActivitiesIsListedOnce() {
        val app = install("com.twin", "Twin")
        spm.addResolveInfoForIntent(launcherIntent, ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = "com.twin"; name = "com.twin.Other"; applicationInfo = app }
        })
        assertEquals(1, catalog.launcherApps().count { it.pkg.value == "com.twin" })
    }

    @Test fun anAppSeenInUsageEventsButNotInstalledOrVisibleFallsBackToItsPackageName() {
        install("com.known", "Known App", launcher = false)
        assertEquals("Known App", catalog.labelOf("com.known"))
        assertEquals("com.ghost.app", catalog.labelOf("com.ghost.app"))
        assertNull(catalog.category(Pkg("com.ghost.app")))
        assertFalse(catalog.launcherApps().any { it.pkg.value == "com.ghost.app" })
    }

    @Test fun neutralComesFromTheBundledListTheDefaultDialerAndTheDefaultLauncher() {
        assertTrue(catalog.isNeutral(Pkg("com.android.calculator2")))
        assertFalse(catalog.isNeutral(Pkg("com.some.game")))

        shadowOf(context.getSystemService(TelecomManager::class.java)).setDefaultDialer("com.oem.dialer")
        assertTrue(catalog.isNeutral(Pkg("com.oem.dialer")))

        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcherApp = ApplicationInfo().apply { packageName = "com.oem.home"; name = "x" }
        spm.addResolveInfoForIntent(home, ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "com.oem.home"; name = "Home"; applicationInfo = launcherApp } })
        assertTrue(catalog.isNeutral(Pkg("com.oem.home")))
    }

    @Test fun ownPackageIsSakshi() = assertEquals(context.packageName, catalog.ownPackage().value)

    @Test fun theBundledNeutralListLoadsAndHoldsNothingButPackageNames() {
        val neutral = NeutralPackages.parse(context.assets.open("sakshi/neutral_packages.json").bufferedReader().use { it.readText() })
        assertTrue(neutral.size > 30)
        assertTrue(neutral.contains("com.android.systemui"))
        assertTrue(neutral.contains("com.google.android.calculator"))
        assertFalse(neutral.contains("com.google.android.youtube"))
    }

    @Test fun aMissingOrBrokenNeutralFileGivesAnEmptyListNotACrash() {
        assertEquals(0, NeutralPackages.parse(null).size)
        assertEquals(0, NeutralPackages.parse("{not json").size)
    }

    // ---- Saying shelf ----
    @Test fun theBundledShelfHasTwentySevenSayings() {
        val shelf = SayingShelfImpl.create(context)
        assertEquals(27, shelf.all().size)
        val first = shelf.all().first()
        assertEquals("sy01", first.id); assertEquals('C', first.tier); assertEquals("Q086", first.q)
    }

    @Test fun theShelfIsParsedOnceAndCached() {
        var loads = 0
        val shelf = SayingShelfImpl { loads++; """[{"id":"a","q":"Q001","text":"t","source":"s","tier":"A","usedFor":"u"}]""" }
        shelf.all(); shelf.all()
        assertEquals(1, loads)
        assertEquals(1, shelf.all().size)
    }

    @Test fun aMissingOrCorruptShelfIsEmptyAndNeverThrows() {
        assertEquals(emptyList<Any>(), SayingShelfImpl { null }.all())
        assertEquals(emptyList<Any>(), SayingShelfImpl { "[{\"id\":" }.all())
        assertEquals(emptyList<Any>(), SayingShelfImpl { """[{"id":"a"}]""" }.all())   // a missing field voids the file, not half of it
    }
}
