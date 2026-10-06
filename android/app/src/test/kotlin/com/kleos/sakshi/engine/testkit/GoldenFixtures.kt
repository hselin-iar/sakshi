package com.kleos.sakshi.engine.testkit

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

/**
 * Loads golden fixtures from src/test/resources/{fixtures,golden}/*.json and
 * asserts actual output against them. No fixture-specific logic lives here —
 * callers pass their own serializer for whatever shape a given step's golden
 * examples need.
 */
object GoldenFixtures {
    private val json = Json { ignoreUnknownKeys = false }

    fun <T> load(resourcePath: String, serializer: KSerializer<T>): T {
        val text = readResource(resourcePath)
        return json.decodeFromString(serializer, text)
    }

    fun <T> assertMatches(expected: T, actual: T, fixtureName: String) {
        if (expected != actual) {
            throw AssertionError(
                "golden fixture mismatch: $fixtureName\n  expected: $expected\n  actual:   $actual"
            )
        }
    }

    private fun readResource(path: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
            "golden fixture not found on classpath: $path"
        }
        return stream.readBytes().toString(StandardCharsets.UTF_8)
    }
}
