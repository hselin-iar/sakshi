package com.kleos.sakshi.host

import com.kleos.sakshi.host.gen.AskTurnDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.net.InetAddress
import java.net.ServerSocket

/** A local stand-in for the NVIDIA endpoint, so what is sent and what is done with the reply can be checked exactly. */
@RunWith(RobolectricTestRunner::class)
class AskSakshiTest {
    private var server: ServerSocket? = null
    private var received: String? = null
    private var auth: String? = null
    private var reply: () -> Pair<Int, String> = { 200 to completion("""{"answer":"x","quote_id":null}""") }
    private var delayMs = 0L

    private fun completion(content: String) = """{"choices":[{"message":{"role":"assistant","content":${Json.encodeToString(kotlinx.serialization.serializer<String>(), content)}}}]}"""

    @Before fun start() {
        AppContainer.reset()
        val socket = ServerSocket(0, 5, InetAddress.getByName("127.0.0.1")).also { server = it }
        Thread {
            while (!socket.isClosed) {
                val client = try { socket.accept() } catch (_: Exception) { break }
                Thread {
                    client.use { c ->
                        val input = c.getInputStream()
                        fun line(): String { val sb = StringBuilder(); while (true) { val b = input.read(); if (b < 0 || b == '\n'.code) break; if (b != '\r'.code) sb.append(b.toChar()) }; return sb.toString() }
                        line()                                                  // request line
                        var length = 0
                        while (true) {
                            val h = line(); if (h.isEmpty()) break
                            if (h.startsWith("content-length:", ignoreCase = true)) length = h.substringAfter(':').trim().toInt()
                            if (h.startsWith("authorization:", ignoreCase = true)) auth = h.substringAfter(':').trim()
                        }
                        val body = ByteArray(length).also { var read = 0; while (read < length) { val n = input.read(it, read, length - read); if (n < 0) break; read += n } }
                        received = body.toString(Charsets.UTF_8)
                        if (delayMs > 0) Thread.sleep(delayMs)
                        val (code, text) = reply()
                        val bytes = text.toByteArray(Charsets.UTF_8)
                        c.getOutputStream().apply {
                            write("HTTP/1.1 $code X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(bytes); flush()
                        }
                    }
                }.start()
            }
        }.start()
    }
    @After fun stop() { server?.close(); runBlocking { runCatching { api().stopDemo() } } }

    private fun url() = "http://127.0.0.1:${server!!.localPort}/v1"
    private fun api(key: String = "nvapi-test", timeout: Int = 5_000, baseUrl: String = url()) =
        HostApiImpl(RuntimeEnvironment.getApplication(), null, NimClient(key, "test/model", baseUrl, timeout, ideasModel = "test/ideas-model"))
    private fun demo(a: HostApiImpl) = runBlocking { a.startDemo("aarav"); a.setDemoAsOf(59) }

    @Test fun theModelsAnswerIsShownWithAQuotePrintedFromTheShelfNotFromTheModel() = runBlocking {
        val a = api(); demo(a)
        reply = { 200 to completion("""{"answer":"Your stretches are about 14 minutes (based on 28 windows over 28 days), and you came back in around 4 minutes.","quote_id":"sy01"}""") }
        val r = a.askSakshi("How was my week?", emptyList())
        assertEquals("LLM", r.source); assertTrue(r.isDemo)
        assertTrue(r.text.startsWith("Your stretches are about 14 minutes"))
        assertEquals("I am watching my mind act", r.quote)             // sy01, verbatim from sayings.json, not whatever the model might say
        assertTrue(r.quoteSource!!.contains("Complete Works"))
    }

    @Test fun whatIsSentIsTheNumbersAndTheAppsOwnSentencesNeverAppNames() = runBlocking {
        val a = api(); demo(a)
        a.askSakshi("Why do I get pulled away?", listOf(AskTurnDto("user", "hello"), AskTurnDto("sakshi", "Namaste.")))
        val body = Json.parseToJsonElement(received!!).jsonObject
        assertEquals("Bearer nvapi-test", auth)
        assertEquals("test/model", body["model"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject["role"]!!.jsonPrimitive.content to it.jsonObject["content"]!!.jsonPrimitive.content }
        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it.first })
        assertEquals("Why do I get pulled away?", messages.last().second)
        val system = messages.first().second
        assertTrue(system.contains("STEADINESS: ")); assertTrue(system.contains("QUOTES")); assertTrue(system.contains("you should"))   // the rule is stated
        // the demo's packages are demo.chat, demo.notes ...: not one may leave the phone, nor the labels the catalog shows for them
        assertFalse("package name sent", received!!.contains("demo."))
        for (label in listOf("Chat", "Mail", "Social", "Notes", "Reader", "Practice")) assertFalse("label $label sent", Regex("\\b$label\\b").containsMatchIn(system.substringBefore("QUOTES")))
    }

    @Test fun aReplyThatBreaksTheCopyRulesIsNeverShown() = runBlocking {
        val a = api(); demo(a)
        reply = { 200 to completion("""{"answer":"You should stop being so distracted!","quote_id":null}""") }
        val r = a.askSakshi("How was my week?", emptyList())
        assertEquals("OFFLINE", r.source)
        assertFalse(r.text.contains("distracted")); assertFalse(r.text.contains("!"))
        assertNotNull(r.quote)
    }

    @Test fun aServerErrorFallsBackToTheOfflineAnswerWithNoErrorShown() = runBlocking {
        val a = api(); demo(a)
        reply = { 500 to "boom" }
        val r = a.askSakshi("How was my week?", emptyList())
        assertEquals("OFFLINE", r.source); assertTrue(r.text.isNotBlank()); assertNotNull(r.quote)
    }

    @Test fun aSlowServerFallsBackAfterTheTimeout() = runBlocking {
        val a = api(timeout = 300); demo(a)
        delayMs = 1_500
        val started = System.currentTimeMillis()
        val r = a.askSakshi("How was my week?", emptyList())
        assertEquals("OFFLINE", r.source)
        assertTrue("took ${System.currentTimeMillis() - started} ms", System.currentTimeMillis() - started < 1_400)
    }

    @Test fun noNetworkFallsBack() = runBlocking {
        val a = api(baseUrl = "http://127.0.0.1:1/v1"); demo(a)        // nothing listens on port 1
        assertEquals("OFFLINE", a.askSakshi("hello", emptyList()).source)
    }

    @Test fun withoutAKeyNothingIsSentAndTheOfflineEngineAnswers() = runBlocking {
        val a = api(key = ""); demo(a)
        val r = a.askSakshi("How long are my stretches?", emptyList())
        assertEquals("OFFLINE", r.source); assertNull(received)
        assertTrue(r.text.contains("14 minutes"))
    }

    @Test fun aPlainProseReplyIsUsedAndAnUnknownQuoteIdStillGetsARealQuote() = runBlocking {
        val a = api(); demo(a)
        reply = { 200 to completion("Your week looked steady, based on 28 windows over 28 days.") }
        val prose = a.askSakshi("How was my week?", emptyList())
        assertEquals("OFFLINE", prose.source)                       // not the JSON asked for: never shown, it could be the model thinking aloud
        assertNotNull(prose.quote)
        reply = { 200 to completion("""{"answer":"All of that is in your numbers.","quote_id":"sy999"}""") }
        val unknown = a.askSakshi("anything", emptyList())
        assertEquals("LLM", unknown.source)
        assertNotNull(unknown.quote)
    }

    @Test fun aBlankQuestionIsABadRequestAndLongInputsAreCut() {
        val a = api(); demo(a)
        try { runBlocking { a.askSakshi("   ", emptyList()) }; fail() } catch (e: com.kleos.sakshi.host.gen.FlutterError) { assertEquals(HostErrors.BAD_REQUEST, e.code) }
        runBlocking { a.askSakshi("x".repeat(5_000), List(20) { AskTurnDto("user", "y".repeat(2_000)) }) }
        val body = Json.parseToJsonElement(received!!).jsonObject["messages"]!!.jsonArray
        assertEquals(1 + 6 + 1, body.size)                                                         // system, the last 6 turns, the question
        assertTrue(body.last().jsonObject["content"]!!.jsonPrimitive.content.length <= 300)
    }

    @Test fun inRealModeTheAnswerIsNotMarkedDemo() = runBlocking {
        val a = api()
        assertFalse(a.askSakshi("hello", emptyList()).isDemo)
    }

    @Test fun askingForIdeasSwitchesTheModelToInvitationsAndOtherQuestionsDoNot() = runBlocking {
        val a = api(); demo(a)
        a.askSakshi("Give me an idea to try", emptyList())
        val ideasSystem = Json.parseToJsonElement(received!!).jsonObject["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonPrimitive.content
        assertTrue(ideasSystem.contains("THE PERSON HAS ASKED FOR IDEAS")); assertTrue(ideasSystem.contains("One small thing you could try"))
        assertTrue(ideasSystem.contains("never \"should\", \"must\" or \"need to\""))
        assertEquals("test/ideas-model", Json.parseToJsonElement(received!!).jsonObject["model"]!!.jsonPrimitive.content)

        a.askSakshi("How was my week?", emptyList())
        val plainSystem = Json.parseToJsonElement(received!!).jsonObject["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonPrimitive.content
        assertFalse(plainSystem.contains("THE PERSON HAS ASKED FOR IDEAS"))
        assertEquals("test/model", Json.parseToJsonElement(received!!).jsonObject["model"]!!.jsonPrimitive.content)
    }

    @Test fun anIdeaWrittenAsAnInvitationIsShownButAnOrderIsNot() = runBlocking {
        val a = api(); demo(a)
        reply = { 200 to completion("""{"answer":"Something you might try: your pulls away sit at about 3 an hour (based on 56 windows over 56 days), so turning the chat app's alerts to quiet during study windows is an idea to test. It might help and might not; the numbers will show over a couple of weeks.","quote_id":"sy02"}""") }
        val ok = a.askSakshi("How can I improve?", emptyList())
        assertEquals("LLM", ok.source); assertTrue(ok.text.startsWith("Something you might try"))
        reply = { 200 to completion("""{"answer":"You should turn off your alerts and you must stop checking your phone.","quote_id":null}""") }
        val order = a.askSakshi("How can I improve?", emptyList())
        assertEquals("OFFLINE", order.source)
        assertFalse(order.text.contains("must stop"))
    }

    @Test fun theModelsThinkingNeverReachesThePerson() = runBlocking {
        val a = api(); demo(a)
        val good = """{"answer":"Your stretches are about 14 minutes (based on 56 windows over 56 days).","quote_id":"sy01"}"""
        val cases = listOf(
            "<think>The user asks about the week. I need to check STEADINESS 138 and the PATTERNS.</think>$good",
            "<thinking>hmm {not json} let me see</thinking>\n$good",
            "<think>unfinished thought with no end tag $good",                                    // an unclosed tag hides everything after it
            "Okay, the user wants the week. I should use the facts. {\"plan\": 1}\n$good",     // notes before the JSON are dropped
            """{"reasoning":"I need to be careful","answer":"Your stretches are about 14 minutes (based on 56 windows over 56 days).","quote_id":"sy01"}""",
        )
        for ((i, c) in cases.withIndex()) {
            reply = { 200 to completion(c) }
            val r = a.askSakshi("How was my week?", emptyList())
            if (i == 2) { assertEquals("unclosed think tag must not be shown", "OFFLINE", r.source); continue }
            assertEquals("case $i", "LLM", r.source)
            assertEquals("case $i", "Your stretches are about 14 minutes (based on 56 windows over 56 days).", r.text)
            assertFalse(r.text.contains("think", ignoreCase = true)); assertFalse(r.text.contains("need to"))
        }
    }

    @Test fun anAnswerThatThinksAloudOrNamesAnInternalLabelIsNotShown() = runBlocking {
        val a = api(); demo(a)
        for (leak in listOf(
            "Okay, so the user is asking about their week and I should look at the facts.",
            "Let me think about this. Your stretches are 14 minutes.",
            "According to STEADINESS: 138 you are doing well.",
            "The facts say your stretches are 14 minutes.",
        )) {
            reply = { 200 to completion("""{"answer":"$leak","quote_id":null}""") }
            assertEquals(leak, "OFFLINE", a.askSakshi("How was my week?", emptyList()).source)
        }
        // a label in brackets is scrubbed, and the rest is kept
        reply = { 200 to completion("""{"answer":"Of your 45 stays, 26 began with a ping (PINGS AND REACHING), based on 60 windows over 60 days.","quote_id":null}""") }
        val scrubbed = a.askSakshi("Do pings pull me?", emptyList())
        assertEquals("LLM", scrubbed.source)
        assertEquals("Of your 45 stays, 26 began with a ping, based on 60 windows over 60 days.", scrubbed.text)
    }
}
