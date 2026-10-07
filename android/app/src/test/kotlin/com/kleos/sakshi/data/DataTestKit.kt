package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.*
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** In-memory Room under Robolectric: the real SQLite schema and queries, no phone. */
@RunWith(RobolectricTestRunner::class)
abstract class DbTest {
    protected lateinit var db: SakshiDatabase

    @Before fun openDb() { db = SakshiDatabase.inMemory(RuntimeEnvironment.getApplication()) }
    @After fun closeDb() { db.close() }
}

fun t(ms: Long) = EpochMs(ms)
const val MIN = 60_000L
const val DAY = 24 * 60 * MIN

fun raw(ts: Long, type: RawType = RawType.ACTIVITY_RESUMED, pkg: String? = "a") = RawEvent(t(ts), type, pkg?.let(::Pkg))
fun notif(ts: Long, pkg: String = "a") = NotifEvent(t(ts), Pkg(pkg), "msg", NotifKind.POSTED, null, false)

fun window(id: Long, day: Long = 100, start: Long = 1_000, end: Long = 2_000) =
    Window(id, StudyDay(day), t(start), t(end), WindowSource.BOTH, partial = false, finalised = true, shape = Shape.HELD)

fun stretch(id: Long, windowId: Long) =
    Stretch(id, windowId, t(1_000), t(1_500), 5.0, 4.0, 1.0, EndedBy.STAY)

fun stay(id: Long, windowId: Long) =
    Stay(id, windowId, t(1_500), t(1_600), Pkg("b"), Pkg("b"), Origin.STONE, Pkg("b"), true, 2.5, 3)

fun daySummary(day: Long) = DaySummary(
    StudyDay(day), true, 60.0, 5.0, 50.0, 0.9, 12, 3.5, false, 1.5, t(9_000), 14.0, 20)

fun derivation(day: Long, vararg windowIds: Long) = DayDerivation(
    windows = windowIds.map {
        WindowWithDetail(window(it, day = day, start = it * 1_000, end = it * 1_000 + 500), listOf(stretch(it * 10, it)), listOf(stay(it * 10 + 1, it)), 1.25, 7)
    },
    summary = daySummary(day))

/** Production reads and writes Room off the main thread; tests that use the real container do the same. */
fun off(block: () -> Unit) {
    var error: Throwable? = null
    Thread { try { block() } catch (t: Throwable) { error = t } }.also { it.start(); it.join() }
    error?.let { throw it }
}
