package com.kleos.sakshi.host

import com.kleos.sakshi.host.gen.FlutterError

/**
 * The error codes of LC-4. Each carries a plain user message from this fixed set and a developer detail
 * (an exception class name at most; never a package name).
 */
object HostErrors {
    const val NO_PERMISSION = "NO_PERMISSION"
    const val DEMO_ACTIVE = "DEMO_ACTIVE"
    const val BAD_REQUEST = "BAD_REQUEST"
    const val STALE_SUGGESTION = "STALE_SUGGESTION"
    const val REANCHOR_NOT_ALLOWED = "REANCHOR_NOT_ALLOWED"
    const val EXPORT_FAILED = "EXPORT_FAILED"
    const val INTERNAL = "INTERNAL"

    private val userMessages = mapOf(
        NO_PERMISSION to "I need access first. You can give it in Settings.",
        DEMO_ACTIVE to "This is demo data. Stop the demo first.",
        BAD_REQUEST to "I could not do that.",
        STALE_SUGGESTION to "That suggestion is no longer current.",
        REANCHOR_NOT_ALLOWED to "Your starting normal can be reset once, from week 4.",
        EXPORT_FAILED to "I could not write the file.",
        INTERNAL to "Something went wrong on my side.")

    fun error(code: String, devDetail: String? = null) = FlutterError(code, userMessages.getValue(code), devDetail)
    fun codes(): Set<String> = userMessages.keys
}
