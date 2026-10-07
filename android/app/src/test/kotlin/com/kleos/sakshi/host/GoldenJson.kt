package com.kleos.sakshi.host

import java.io.File
import java.lang.reflect.Modifier

/**
 * Renders a Pigeon DTO as JSON: fields in declaration order, enums as lowerCamel (the Dart enum spelling), nulls explicit.
 * These files are the shared fixtures: Track 4's Flutter tests decode the same JSON.
 */
object GoldenJson {
    private val dir = File("src/test/resources/golden")

    fun render(value: Any?, indent: Int = 0): String = when (value) {
        null -> "null"
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Boolean, is Long, is Int -> value.toString()
        is Double -> value.toString()
        is Enum<*> -> "\"" + lowerCamel(value.name) + "\""
        is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(",\n", "[\n", "\n" + pad(indent) + "]") { pad(indent + 1) + render(it, indent + 1) }
        else -> {
            val fields = value.javaClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            fields.joinToString(",\n", "{\n", "\n" + pad(indent) + "}") { f ->
                f.isAccessible = true
                pad(indent + 1) + "\"" + f.name + "\": " + render(f.get(value), indent + 1)
            }
        }
    }

    private fun pad(n: Int) = "  ".repeat(n)
    private fun lowerCamel(s: String) = s.lowercase().split('_').mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar { it.uppercase() } }.joinToString("")

    /** Same check for text that is already JSON (the export file). */
    fun checkText(name: String, actual: String): String? {
        val file = dir.resolve("$name.json")
        if (System.getenv("UPDATE_GOLDEN") == "1") { dir.mkdirs(); file.writeText(actual); return null }
        if (!file.isFile) return "missing golden ${file.path}"
        return if (file.readText() == actual) null else "golden ${file.path} differs from the exported file"
    }

    /** Compares to the checked-in file; with UPDATE_GOLDEN=1 it rewrites the file instead. */
    fun check(name: String, dto: Any): String? {
        val file = dir.resolve("$name.json")
        val actual = render(dto) + "\n"
        if (System.getenv("UPDATE_GOLDEN") == "1") { dir.mkdirs(); file.writeText(actual); return null }
        if (!file.isFile) return "missing golden ${file.path}"
        return if (file.readText() == actual) null else "golden ${file.path} differs from the mapped DTO"
    }
}
