package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.ports.SayingShelf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Parses assets/sakshi/sayings.json once. A missing or corrupt file gives an empty shelf and the Mirror hides the saying. */
class SayingShelfImpl(private val load: () -> String?) : SayingShelf {
    private val sayings: List<Saying> by lazy { parse(load()) }

    override fun all(): List<Saying> = sayings

    private fun parse(json: String?): List<Saying> = try {
        json?.let {
            Json.parseToJsonElement(it).jsonArray.map { e ->
                val o = e.jsonObject
                fun field(name: String) = o.getValue(name).jsonPrimitive.content
                Saying(field("id"), field("q"), field("text"), field("source"), field("tier").single(), field("usedFor"))
            }
        } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        fun create(context: Context) = SayingShelfImpl {
            try {
                context.assets.open("sakshi/sayings.json").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
