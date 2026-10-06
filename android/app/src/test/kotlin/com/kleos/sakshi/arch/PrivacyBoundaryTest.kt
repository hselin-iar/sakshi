package com.kleos.sakshi.arch

import com.kleos.sakshi.engine.model.NotifEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyBoundaryTest {
    private val allowedStringFields = setOf("pkg", "category")
    private val forbiddenNames = listOf("title", "text", "extras", "icon", "intent", "channel")

    private val fields = NotifEvent::class.java.declaredFields.filter { !it.isSynthetic }

    @Test
    fun notifEventHasNoStringFieldOtherThanPkgAndCategory() {
        // Pkg is a value class: its backing field is a String, so `pkg` shows up here and is allowed by name.
        val offenders = fields.filter { it.type == String::class.java && it.name !in allowedStringFields }.map { it.name }
        assertTrue("NotifEvent must not carry text-like fields: $offenders", offenders.isEmpty())
    }

    @Test
    fun notifEventHasNoFieldNamedLikeNotificationContent() {
        val offenders = fields.map { it.name }.filter { n -> forbiddenNames.any { n.contains(it, ignoreCase = true) } }
        assertTrue("NotifEvent must not carry content fields: $offenders", offenders.isEmpty())
    }

    @Test
    fun notifEventFieldSetIsExactlyTheLockedOne() {
        assertEquals(
            setOf("ts", "pkg", "category", "kind", "removal", "ongoing"),
            fields.map { it.name }.toSet())
    }
}
