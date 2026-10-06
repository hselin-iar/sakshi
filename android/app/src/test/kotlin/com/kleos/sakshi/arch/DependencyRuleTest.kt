package com.kleos.sakshi.arch

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyRuleTest {
    private val engineDir = File("src/main/kotlin/com/kleos/sakshi/engine")
    private val message = "engine must not import android/host/data and must not read time or randomness directly"

    private val forbiddenImport =
        Regex("""^import\s+(android\.|androidx\.|com\.kleos\.sakshi\.(host|data)\.|io\.flutter)""")
    private val forbiddenCalls =
        listOf("System.currentTimeMillis", "Instant.now", "LocalDateTime.now", "Random()", "Math.random")

    private fun engineFiles(): List<File> =
        engineDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun engineDirectoryIsFound() {
        assertTrue("engine dir not found at ${engineDir.absolutePath}", engineDir.isDirectory)
    }

    @Test
    fun engineImportsNoPlatformOrHostOrData() {
        val offenders = engineFiles().flatMap { f ->
            f.readLines().filter { forbiddenImport.containsMatchIn(it) }.map { "${f.name}: $it" }
        }
        assertTrue("$message\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    @Test
    fun engineReadsNoClockOrRandomnessDirectly() {
        val offenders = engineFiles()
            .filter { it.invariantSeparatorsPath.endsWith("demo/EventSynthesizer.kt").not() }
            .flatMap { f ->
                f.readLines().filter { line -> forbiddenCalls.any { line.contains(it) } }.map { "${f.name}: $it" }
            }
        assertTrue("$message\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }
}
