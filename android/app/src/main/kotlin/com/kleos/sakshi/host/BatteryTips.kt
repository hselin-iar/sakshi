package com.kleos.sakshi.host

/**
 * The OEM battery paths, kept in one place so docs/research/oem_battery.md and this table cannot drift (a test compares them).
 * This wording is NOT sent over the bridge: Track 3 copies the verified text into ui_strings.dart (T3.5, chrome text, LC-8).
 */
object BatteryTips {
    enum class Oem { VIVO, NOTHING, XIAOMI, OPPO, SAMSUNG, OTHER }

    /** How far a path can be trusted. Nothing here is VERIFIED_ON_DEVICE until someone follows it on the phone. */
    enum class Status { VERIFIED_ON_DEVICE, PUBLIC_SOURCE, UNVERIFIED }

    data class Tips(val oem: Oem, val systemName: String, val status: Status, val steps: List<String>)

    val all: List<Tips> = listOf(
        Tips(Oem.VIVO, "Vivo and iQOO (Funtouch OS / OriginOS)", Status.PUBLIC_SOURCE, listOf(
            "Press and hold the Sakshi icon, then tap App info.",
            "Tap Battery, then choose Unrestricted (on some versions: Battery optimization, then Not optimized).",
            "Open Settings, then Battery, then Background power consumption management, and allow Sakshi.",
            "Open Settings, then More settings, then Applications, then Autostart, and turn Sakshi on.")),
        Tips(Oem.NOTHING, "Nothing OS", Status.UNVERIFIED, listOf(
            "Open Settings, then Apps, then Sakshi.",
            "Tap App battery usage (or Battery) and choose Unrestricted.")),
        Tips(Oem.XIAOMI, "Xiaomi, Redmi and POCO (HyperOS / MIUI)", Status.PUBLIC_SOURCE, listOf(
            "Open Settings, then Apps, then Sakshi, then App permissions, and turn on Autostart.",
            "Open Security, then Battery, then App battery saver, find Sakshi and choose No restrictions.")),
        Tips(Oem.OPPO, "Oppo, Realme and OnePlus (ColorOS)", Status.UNVERIFIED, listOf(
            "Open Settings, then Apps, then App management, then Sakshi.",
            "Turn on Allow auto-launch (older versions: Allow auto start-up).",
            "Tap Battery usage and choose Allow background activity.")),
        Tips(Oem.SAMSUNG, "Samsung (One UI)", Status.PUBLIC_SOURCE, listOf(
            "Open Settings, then Apps, then Sakshi, then Battery, and choose Unrestricted.",
            "Open Settings, then Battery, then Background usage limits, and turn off Put unused apps to sleep, or remove Sakshi from the sleeping apps list.")))

    /** Which table applies to a phone, from Build.MANUFACTURER. Anything else gets Android's own page and no OEM steps. */
    fun oemFor(manufacturer: String?): Oem = when (manufacturer?.trim()?.lowercase()) {
        "vivo", "iqoo" -> Oem.VIVO
        "nothing" -> Oem.NOTHING
        "xiaomi", "redmi", "poco" -> Oem.XIAOMI
        "oppo", "realme", "oneplus" -> Oem.OPPO
        "samsung" -> Oem.SAMSUNG
        else -> Oem.OTHER
    }

    fun tipsFor(manufacturer: String?): Tips? = oemFor(manufacturer).let { oem -> all.firstOrNull { it.oem == oem } }
}
