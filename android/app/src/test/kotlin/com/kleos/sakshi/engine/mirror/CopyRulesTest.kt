package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.PatternKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LC-8: the forbidden-word list and the "!" ban, over every template and a generated matrix of arguments. */
class CopyRulesTest {
    companion object {
        val FORBIDDEN = Regex(
            """\b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\w*|ruin\w*|lost (your )?(focus|concentration)|you (should|must|need to))\b""",
            RegexOption.IGNORE_CASE,
        )
    }

    private val all = CopySamples.all()

    @Test fun `the matrix really is big enough to mean something`() {
        assertTrue("only ${all.size} sentences", all.size > 2_000)
        assertTrue(all.distinct().size > 300)
    }

    @Test fun `no sentence matches the forbidden-word pattern`() {
        val bad = all.distinct().filter { FORBIDDEN.containsMatchIn(it) }
        assertEquals("forbidden wording: $bad", emptyList<String>(), bad)
    }

    @Test fun `no sentence contains an exclamation mark`() {
        assertEquals(emptyList<String>(), all.distinct().filter { it.contains('!') })
    }

    @Test fun `no sentence names an app or a package`() {
        val bad = all.distinct().filter { Regex("""\b(com|org|net)\.[a-z]+""").containsMatchIn(it) }
        assertEquals(emptyList<String>(), bad)
    }

    @Test fun `every pattern line ends with its evidence count`() {
        val ends = Regex("""\(based on \d+ windows? over \d+ days?\)\.$""")
        val lines = CopySamples.patterns().map { SentenceBuilder.patternLine(it) }
        assertTrue(lines.isNotEmpty())
        lines.forEach { assertTrue("no evidence count: $it", ends.containsMatchIn(it)) }
        PatternKind.entries.forEach { kind -> assertTrue("no sample for $kind", CopySamples.patterns().any { it.kind == kind }) }
    }

    @Test fun `steadiness words appear only through the headline relation, exactly as specified`() {
        val h = com.kleos.sakshi.engine.metrics.Word.entries.map { SentenceBuilder.headline(18.0, 4.0, it) }
        assertEquals(
            listOf(
                "You held 18-minute stretches and 4-minute returns; steadier than your starting normal.",
                "You held 18-minute stretches and 4-minute returns; close to your starting normal.",
                "You held 18-minute stretches and 4-minute returns; less steady than your starting normal.",
            ).sorted(),
            h.sorted(),
        )
    }

    @Test fun `the document's own example wordings come out exactly`() {
        val s = SentenceBuilder
        assertEquals("You were away 4 days. Your starting normal is still here.", s.lapseLine(4))
        assertEquals("Your phone was quiet for 36% of your work time.", s.quietLine(0.36))
        assertEquals("It took you about 4 minutes to get back.", s.returnLine(4.0))
        assertEquals("You opened me 3 times in week 2 and 2 times this week.", s.teacherLine(2, 3, 2))
        assertEquals("You opened me 3 times this week.", s.teacherLine(3, 3, 2))
        assertEquals("Still water.", s.lakePhrase(com.kleos.sakshi.engine.model.LakeState.STILL, false))
        assertEquals("Calm.", s.lakePhrase(com.kleos.sakshi.engine.model.LakeState.STILL, true))
        assertEquals("Ping awareness is off, so I can't tell a ping from a reach.", s.dataLines(listOf(com.kleos.sakshi.engine.model.DataFlag.PING_OFF), 0, 0, null).single())
    }

    @Test fun `no sentence says null or NaN or prints a dangling number`() {
        val bad = all.distinct().filter { it.contains("null") || it.contains("NaN") || it.contains("Infinity") }
        assertEquals(emptyList<String>(), bad)
    }

    @Test fun `tier D can never be labelled his own writing`() {
        assertFalse(SentenceBuilder.tierLabel('D').contains("own writing"))
        assertEquals("his own writing or letter", SentenceBuilder.tierLabel('A'))
    }

    @Test fun `print every distinct template for the human read-through`() {
        // One representative per template shape: digits collapsed, so the list is short enough to read aloud.
        val shapes = linkedMapOf<String, String>()
        all.forEach { shapes.putIfAbsent(it.replace(Regex("\\d+(\\.\\d+)?"), "#"), it) }
        val out = File("build/template_review.txt")
        out.parentFile.mkdirs()
        out.writeText(shapes.values.sorted().joinToString("\n") + "\n")
        assertTrue(shapes.size > 100)
    }
}
