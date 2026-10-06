package com.kleos.sakshi.arch

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SayingShelfIntegrityTest {
    // Gradle runs unit tests with the module (android/app) as the working directory.
    private val shelf = File("src/main/assets/sakshi/sayings.json")
    private val qindex = File("../../tools/qindex.json")

    private fun sayings(): JsonArray = Json.parseToJsonElement(shelf.readText()).jsonArray
    private fun field(o: JsonObject, name: String) = o.getValue(name).jsonPrimitive.content

    private fun normalise(s: String): String =
        s.replace('‘', '\'').replace('’', '\'').replace('“', '"').replace('”', '"')
            .replace(Regex("\\s+"), " ").trim().trimEnd('.')

    @Test
    fun shelfHasExactly27Entries() = assertEquals(27, sayings().size)

    @Test
    fun idsAreUnique() {
        val ids = sayings().map { field(it.jsonObject, "id") }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun questionIdsAndTiersAreWellFormed() {
        sayings().forEach {
            val o = it.jsonObject
            assertTrue("bad q ${field(o, "q")}", Regex("Q\\d{3}").matches(field(o, "q")))
            assertTrue("bad tier ${field(o, "tier")}", field(o, "tier") in listOf("A", "B", "C", "D"))
        }
    }

    @Test
    fun noQ077OrQ190() {
        val qs = sayings().map { field(it.jsonObject, "q") }
        assertTrue("Q077 is a chain in disguise", "Q077" !in qs)
        assertTrue("Q190 is a caution", "Q190" !in qs)
    }

    @Test
    fun everyTextIsASubstringOfItsOutcomeMapQuote() {
        val index = Json.parseToJsonElement(qindex.readText()).jsonObject
        sayings().forEach {
            val o = it.jsonObject
            val key = field(o, "q").removePrefix("Q")
            val entry = index[key]
            assertTrue("no qindex entry for ${field(o, "q")}", entry != null)
            val quote = normalise(field(entry!!.jsonObject, "quote"))
            val text = normalise(field(o, "text"))
            assertTrue("${field(o, "id")}: text not found in ${field(o, "q")}", quote.contains(text))
        }
    }
}
