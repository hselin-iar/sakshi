package com.kleos.sakshi.host

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Package names of apps that are neither study nor distraction: dialer, camera, calculator, clock, maps, Settings, launchers. */
class NeutralPackages(private val names: Set<String>) {
    fun contains(pkg: String): Boolean = pkg in names
    val size: Int get() = names.size

    companion object {
        /** A missing or unreadable file gives an empty list, never a crash: the default dialer and launcher lookups still work. */
        fun parse(json: String?): NeutralPackages = NeutralPackages(
            try {
                json?.let { Json.parseToJsonElement(it).jsonArray.map { e -> e.jsonPrimitive.content }.toSet() } ?: emptySet()
            } catch (_: Exception) {
                emptySet()
            })
    }
}
