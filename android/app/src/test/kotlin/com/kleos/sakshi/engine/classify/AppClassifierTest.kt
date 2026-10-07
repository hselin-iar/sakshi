package com.kleos.sakshi.engine.classify

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.testkit.FakeAppCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * classify() per DOC 3 (Foreground Intervals and App Classification):
 * 1. user class IN_SET/DEPENDS if the user set it
 * 2. NEUTRAL if bundled-neutral or the default dialer/launcher (AppCatalog)
 * 3. else OFF_SET
 * Own package is always NEUTRAL (DOC 4 T2.3), checked ahead of the above.
 * Four classes only.
 */
class AppClassifierTest {
    private val ownPkg = Pkg("com.kleos.sakshi")

    @Test
    fun `own package is always NEUTRAL even if the user classified it otherwise`() {
        val catalog = FakeAppCatalog(own = ownPkg)
        val classifier = AppClassifier(mapOf(ownPkg to UserClass.IN_SET), catalog)

        assertEquals(AppClass.NEUTRAL, classifier.classify(ownPkg))
    }

    @Test
    fun `own package is NEUTRAL even when it is also on the bundled neutral list`() {
        val catalog = FakeAppCatalog(own = ownPkg).apply { setNeutral(ownPkg, true) }
        val classifier = AppClassifier(emptyMap(), catalog)

        assertEquals(AppClass.NEUTRAL, classifier.classify(ownPkg))
    }

    @Test
    fun `user class IN_SET wins over the bundled neutral list`() {
        val pkg = Pkg("com.example.study")
        val catalog = FakeAppCatalog(own = ownPkg).apply { setNeutral(pkg, true) }
        val classifier = AppClassifier(mapOf(pkg to UserClass.IN_SET), catalog)

        assertEquals(AppClass.IN_SET, classifier.classify(pkg))
    }

    @Test
    fun `user class DEPENDS is honoured`() {
        val pkg = Pkg("com.example.notes")
        val catalog = FakeAppCatalog(own = ownPkg)
        val classifier = AppClassifier(mapOf(pkg to UserClass.DEPENDS), catalog)

        assertEquals(AppClass.DEPENDS, classifier.classify(pkg))
    }

    @Test
    fun `bundled neutral list applies when the user has not classified the app`() {
        val pkg = Pkg("com.android.dialer")
        val catalog = FakeAppCatalog(own = ownPkg).apply { setNeutral(pkg, true) }
        val classifier = AppClassifier(emptyMap(), catalog)

        assertEquals(AppClass.NEUTRAL, classifier.classify(pkg))
    }

    @Test
    fun `an explicit NONE user class falls through to the neutral list or off-set`() {
        val pkg = Pkg("com.example.random")
        val catalog = FakeAppCatalog(own = ownPkg)
        val classifier = AppClassifier(mapOf(pkg to UserClass.NONE), catalog)

        assertEquals(AppClass.OFF_SET, classifier.classify(pkg))
    }

    @Test
    fun `anything unclassified and not neutral is off-set`() {
        val pkg = Pkg("com.example.game")
        val catalog = FakeAppCatalog(own = ownPkg)
        val classifier = AppClassifier(emptyMap(), catalog)

        assertEquals(AppClass.OFF_SET, classifier.classify(pkg))
    }
}
