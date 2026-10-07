package com.kleos.sakshi.host

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A plain HTTPS call to an OpenAI-compatible chat endpoint (NVIDIA NIM at integrate.api.nvidia.com). No library: HttpURLConnection and
 * the JSON the project already uses. It only carries text the engine built (numbers and the app's own sentences, never app names) plus
 * what the person typed. Any failure throws; the caller answers offline instead, so a bad network never shows an error.
 */
class NimClient(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String,
    private val timeoutMs: Int = 30_000,
    private val ideasModel: String = model,
) {
    val configured: Boolean get() = apiKey.isNotBlank() && model.isNotBlank() && (baseUrl.startsWith("https://") || baseUrl.startsWith("http://127.0.0.1"))

    class Turn(val role: String, val text: String)

    /** `ideas` uses the reasoning model with a bigger budget: it thinks before it writes, and the thinking counts against the limit. */
    fun chat(system: String, history: List<Turn>, user: String, ideas: Boolean = false): String {
        val body = buildJsonObject {
            put("model", if (ideas) ideasModel else model)
            put("temperature", 0.5)
            put("max_tokens", if (ideas) 1_800 else 900)
            put("stream", false)
            put("messages", buildJsonArray {
                add(message("system", system))
                history.forEach { add(message(if (it.role == "user") "user" else "assistant", it.text)) }
                add(message("user", user))
            })
        }.toString()

        val connection = (URL(baseUrl.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            val text = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val content = Json.parseToJsonElement(text).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            return content?.takeIf { it.isNotBlank() } ?: throw java.io.IOException("empty reply")
        } finally {
            connection.disconnect()
        }
    }

    private fun message(role: String, content: String): JsonObject = buildJsonObject { put("role", role); put("content", content) }

    companion object {
        /** The values compiled in from android/nim.properties (git-ignored). Blank when the file is absent: the app then answers offline. */
        fun fromBuildConfig() = NimClient(
            com.kleos.sakshi.BuildConfig.NIM_API_KEY, com.kleos.sakshi.BuildConfig.NIM_MODEL, com.kleos.sakshi.BuildConfig.NIM_BASE_URL,
            ideasModel = com.kleos.sakshi.BuildConfig.NIM_IDEAS_MODEL,
        )
    }
}
