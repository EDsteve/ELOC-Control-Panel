package de.eloc.eloc_control_panel.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing of `getMicTest` replies. The frames are built the way the firmware prints them
 * (`bt_sendResponse` in BluetoothServer.cpp), and the payloads follow README-MicTest-Plan.md.
 */
class MicTestTest {

    private fun frame(payload: String, ecode: Int = 0, error: String = "") =
        """{"id" : 7, "ecode" : $ecode, "error" : "$error", "cmd" : "getMicTest", "payload" : $payload}"""

    // Broken mic #6 from the bench session: verdict from the PC replay, numbers shaped like the
    // first bench result.
    private val brokenResult = """
        {"state":"done","test":2,"time":"Tue, Nov 17 2026 17:59:10",
         "verdict":{"result":"broken","finding":"buried",
           "reason":"beeps buried in noise: the best beep is only 15 dB above the background (limit 30) - the microphone is broken - found in both runs",
           "bestRise[dB]":14.7,"background[dBFS]":-77.2,"runs":["buried","buried"]},
         "fs[Hz]":16000,"fsMeasured[Hz]":16024,"duration[ms]":7936,"i2sWasRunning":false,"micVolume2_pwr":-3,
         "level":{"noise[dBFS]":-60.1,"peak[dBFS]":-40.2,"dc[dBFS]":-113.4,"clipped":0,"stuckRun":3},
         "spectrum":{"edges[Hz]":[31,63,125,250,500,1000,2000,4000,8000],
           "bands[dBFS]":[-82.7,-90.4,-95.4,-96.4,-97.3,-96.6,-92.6,-94],"slope[dB/oct]":-0.2},
         "crackle":{"impulses":4,"crest[dB]":19.7,"kurtosis":3.1,"dropouts":0,"analysed[ms]":3116},
         "tones":[
           {"f[Hz]":2700,"on[dBFS]":-62.5,"off[dBFS]":-77.2,"rise[dB]":14.7,"riseMin[dB]":13.9,"peak[Hz]":2703,"onTotal[dBFS]":-58.1},
           {"f[Hz]":4000,"on[dBFS]":-66.0,"off[dBFS]":-75.0,"rise[dB]":9.0,"riseMin[dB]":8.1,"peak[Hz]":4000,"onTotal[dBFS]":-60.0},
           {"f[Hz]":5400,"valid":false}
         ],
         "wav":"/sdcard/mictest/ELOC_00247_20261117_175910_run2.wav"}
    """.trimIndent()

    @Test
    fun doneResultIsParsed() {
        val status = MicTest.parseStatus(frame(brokenResult))
        assertTrue(status is MicTest.Status.Done)
        val result = (status as MicTest.Status.Done).result
        assertEquals(MicTest.Verdict.Broken, result.verdict)
        assertEquals("buried", result.finding)
        assertTrue(result.reason.startsWith("beeps buried in noise"))
        assertEquals(14.7, result.bestRiseDb!!, 0.001)
        assertEquals(-77.2, result.backgroundDbfs!!, 0.001)
        assertEquals(listOf("buried", "buried"), result.runs)
        assertEquals(16000, result.sampleRateHz)
        assertEquals(16024, result.measuredSampleRateHz)
        assertEquals(4, result.crackleImpulses)
        assertEquals(3, result.tones.size)
        assertEquals(2700, result.tones[0].frequencyHz)
        assertEquals(14.7, result.tones[0].riseDb, 0.001)
        assertTrue(result.tones[0].valid)
        assertFalse(result.tones[2].valid)
        assertTrue(result.tones[2].riseDb.isNaN())
        assertEquals("/sdcard/mictest/ELOC_00247_20261117_175910_run2.wav", result.wavPath)
        assertEquals("", result.warning)
        assertEquals("2|Tue, Nov 17 2026 17:59:10", result.identity)
    }

    @Test
    fun everyVerdictCodeIsKnown() {
        val codes = mapOf(
            "ok" to MicTest.Verdict.Ok,
            "broken" to MicTest.Verdict.Broken,
            "checkPort" to MicTest.Verdict.CheckPort,
            "suspect" to MicTest.Verdict.Suspect,
            "repeat" to MicTest.Verdict.Repeat,
            "none" to MicTest.Verdict.None,
        )
        for ((code, verdict) in codes) {
            assertEquals(code, verdict, MicTest.Verdict.parse(code))
        }
        assertEquals(MicTest.Verdict.Unknown, MicTest.Verdict.parse("somethingNew"))
        assertEquals(MicTest.Verdict.Unknown, MicTest.Verdict.parse(""))
    }

    @Test
    fun verdictWithoutNumbersLeavesThemEmpty() {
        // An unplugged mic: bestRise is below the firmware's floor, so the two numbers are left out.
        val payload = """
            {"state":"done","test":1,"time":"x",
             "verdict":{"result":"broken","finding":"noSignal","reason":"no signal","runs":["noSignal","noSignal"]},
             "fs[Hz]":16000,"fsMeasured[Hz]":16000,"tones":[]}
        """.trimIndent()
        val result = (MicTest.parseStatus(frame(payload)) as MicTest.Status.Done).result
        assertNull(result.bestRiseDb)
        assertNull(result.backgroundDbfs)
        assertTrue(result.tones.isEmpty())
        assertEquals(0, result.crackleImpulses)
        assertEquals("", result.wavPath)
    }

    @Test
    fun clockWarningIsKept() {
        val payload = """
            {"state":"done","test":1,"time":"x","verdict":{"result":"repeat","finding":"clock","reason":"r","runs":["clock"]},
             "warning":"measured sample rate is off - I2S clock problem, the other numbers are unreliable"}
        """.trimIndent()
        val result = (MicTest.parseStatus(frame(payload)) as MicTest.Status.Done).result
        assertEquals(MicTest.Verdict.Repeat, result.verdict)
        assertTrue(result.warning.startsWith("measured sample rate is off"))
    }

    @Test
    fun progressIsParsed() {
        val status = MicTest.parseStatus(
            frame("""{"state":"capturing","run":1,"progress[%]":40,"remaining[ms]":7200}""")
        )
        assertEquals(MicTest.Status.Running("capturing", 1, 40, 7200), status)

        val confirm = MicTest.parseStatus(
            frame("""{"state":"capturing","run":2,"progress[%]":55,"remaining[ms]":11000}""")
        ) as MicTest.Status.Running
        assertEquals(2, confirm.run)

        for (state in listOf("starting", "analysing", "saving")) {
            val running = MicTest.parseStatus(frame("""{"state":"$state","run":1,"progress[%]":99,"remaining[ms]":0}"""))
            assertTrue(state, running is MicTest.Status.Running)
        }
    }

    @Test
    fun idleAndErrorStates() {
        assertEquals(MicTest.Status.Idle, MicTest.parseStatus(frame("""{"state":"idle"}""")))
        assertEquals(
            MicTest.Status.Failed("no memory for the result"),
            MicTest.parseStatus(frame("""{"state":"error","error":"no memory for the result"}"""))
        )
        assertEquals(
            MicTest.Status.Failed(""),
            MicTest.parseStatus(frame("""{"state":"error","test":3}"""))
        )
    }

    @Test
    fun rejectedOrBrokenRepliesAreUnreadable() {
        // A rejected command: the firmware sets ecode and error, and the payload is an empty string.
        assertEquals(
            MicTest.Status.Unreadable("command not found"),
            MicTest.parseStatus(frame("\"\"", ecode = 261, error = "command not found"))
        )
        assertEquals(MicTest.Status.Unreadable(""), MicTest.parseStatus("not json"))
        assertEquals(MicTest.Status.Unreadable(""), MicTest.parseStatus(frame("""{"state":"weird"}""")))
        assertEquals(MicTest.Status.Unreadable(""), MicTest.parseStatus("""{"ecode":0}"""))
    }

    @Test
    fun firmwareGate() {
        assertFalse(MicTest.isSupported("ELOC-P_V1.80"))
        assertFalse(MicTest.isSupported("ELOC-P_V1.82"))
        assertTrue(MicTest.isSupported("ELOC-P_V1.83"))
        assertTrue(MicTest.isSupported("ELOC-P_V1.87"))
        assertTrue(MicTest.isSupported("ELOC-P_V2.00"))
        // Unknown: the device answers the command itself.
        assertTrue(MicTest.isSupported(""))
        assertTrue(MicTest.isSupported("ELOC_V3.10"))
        assertTrue(MicTest.isSupported("garbage"))
    }
}
