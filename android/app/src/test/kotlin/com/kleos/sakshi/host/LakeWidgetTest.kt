package com.kleos.sakshi.host

import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.TextView
import com.kleos.sakshi.R
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.LakeRow
import com.kleos.sakshi.engine.model.LakeState
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LakeWidgetTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @org.junit.Before fun freshContainer() = AppContainer.reset()

    private val kolkata = ZoneId.of("Asia/Kolkata")
    /** Production reads Room off the main thread (the worker, the widget's own thread); the test does the same. */
    private fun off(block: () -> Unit) { var error: Throwable? = null; Thread { try { block() } catch (t: Throwable) { error = t } }.also { it.start(); it.join() }; error?.let { throw it } }

    private fun row(state: LakeState, phrase: String, asOf: Long? = 1_759_000_000_000) = LakeRow(state, phrase, asOf?.let(::EpochMs))

    @Test fun eachStatePicksItsDrawableAndLearningAndNoDataUseTheStillOne() {
        assertEquals(R.drawable.lake_still, LakeWidget.faceFor(row(LakeState.STILL, "Still water.")).drawableRes)
        assertEquals(R.drawable.lake_rippled, LakeWidget.faceFor(row(LakeState.RIPPLED, "A few ripples.")).drawableRes)
        assertEquals(R.drawable.lake_choppy, LakeWidget.faceFor(row(LakeState.CHOPPY, "Choppy water.")).drawableRes)
        assertEquals(R.drawable.lake_still, LakeWidget.faceFor(row(LakeState.LEARNING, "Learning your normal.")).drawableRes)
        assertEquals(R.drawable.lake_still, LakeWidget.faceFor(row(LakeState.NO_DATA, "Nothing to show yet.", asOf = null)).drawableRes)
    }

    @Test fun thePhraseIsShownExactlyAsStoredAndNothingIsComputed() {
        assertEquals("Learning your normal.", LakeWidget.faceFor(row(LakeState.LEARNING, "Learning your normal.")).phrase)
        assertEquals("Still water. (demo)", LakeWidget.faceFor(row(LakeState.STILL, "Still water. (demo)")).phrase)
    }

    @Test fun aMissingRowDrawsNoDataWithNoAsOf() {
        val face = LakeWidget.faceFor(null)
        assertEquals(R.drawable.lake_still, face.drawableRes)
        assertEquals("", face.phrase)
        assertNull(face.asOfText)
    }

    @Test fun asOfIsTheStoredTimeInThePhonesOwnZoneInLowercase() {
        // 2025-09-27 19:46:40 UTC = 01:16 on 28 Sep in Kolkata
        val face = LakeWidget.faceFor(row(LakeState.STILL, "p", asOf = 1_759_002_400_000), kolkata, Locale.US)
        assertEquals("as of 1:16 am", face.asOfText)
        assertEquals("as of 9:42 pm", LakeWidget.faceFor(row(LakeState.STILL, "p", asOf = java.time.ZonedDateTime.of(2026, 10, 7, 21, 42, 0, 0, kolkata).toInstant().toEpochMilli()), kolkata, Locale.US).asOfText)
    }

    @Test fun theInflatedWidgetShowsPhraseAndAsOfAndHidesAsOfWhenThereIsNone() {
        // Applied to a plain parent, so the provider's own onUpdate thread cannot redraw underneath the assertions.
        val parent = android.widget.FrameLayout(context)
        val shown = LakeWidget.views(context, LakeWidget.faceFor(row(LakeState.CHOPPY, "Choppy water."), kolkata, Locale.US)).apply(context, parent)
        assertEquals("Choppy water.", shown.findViewById<TextView>(R.id.lake_phrase).text.toString())
        assertEquals(View.VISIBLE, shown.findViewById<View>(R.id.lake_as_of).visibility)
        assertTrue(shown.findViewById<TextView>(R.id.lake_as_of).text.startsWith("as of "))
        assertEquals("Choppy water.", shown.findViewById<View>(R.id.lake_image).contentDescription)

        val bare = LakeWidget.views(context, LakeWidget.faceFor(row(LakeState.NO_DATA, "Nothing to show yet.", asOf = null))).apply(context, parent)
        assertEquals(View.GONE, bare.findViewById<View>(R.id.lake_as_of).visibility)
    }

    @Test fun refreshRedrawsFromTheStoredLakeStateRow() {
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager).createWidget(LakeWidget::class.java, R.layout.lake_widget)
        Thread.sleep(500)   // createWidget triggers the provider's own onUpdate on a background thread; let it finish first
        val state = AppContainer.from(context).state

        off { state.saveLake(row(LakeState.RIPPLED, "A few ripples.")); LakeWidget.refresh(context) }
        assertEquals("A few ripples.", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.lake_phrase).text.toString())

        off { state.saveLake(row(LakeState.CHOPPY, "Choppy water.")); LakeWidget.refresh(context) }   // follows the row, as the debug action does
        assertEquals("Choppy water.", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.lake_phrase).text.toString())
    }

    @Test fun refreshNeverThrowsEvenWhenTheStoreIsBroken() {
        off { AppContainer.from(context).database.close(); LakeWidget.refresh(context) }   // must return normally
    }

    @Test fun tappingOpensTheMirrorThroughAnExtraTheActivityTurnsIntoARoute() {
        val views = LakeWidget.views(context, LakeWidget.faceFor(null))
        assertNotNull(views)
        assertEquals("route", LakeWidget.EXTRA_ROUTE)
        assertEquals("mirror", LakeWidget.ROUTE_MIRROR)
    }
}
