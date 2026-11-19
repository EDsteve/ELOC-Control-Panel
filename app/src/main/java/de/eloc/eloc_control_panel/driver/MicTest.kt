package de.eloc.eloc_control_panel.driver

import org.json.JSONObject

/**
 * Microphone self-test (`setMicTest` / `getMicTest`, firmware V1.83 and newer).
 *
 * The device plays buzzer tones and measures what its own microphone hears. The buzzer sits on the
 * same board as the mic, so the test needs nothing from the technician except a reasonably quiet
 * place. It takes about 12 s, or about 24 s when a second run has to confirm a finding.
 *
 * The verdict is the firmware's, not the app's: the thresholds come from bench and forest data and
 * live in the firmware's `MicAnalyzer.hpp` (`judge()`). The app only shows the result. Field meanings
 * are in the firmware's `README-MicTest-Plan.md` and on the firmware wiki (App-Interface page).
 */
object MicTest {

    // Sent without arguments: the firmware defaults (three tones, two repetitions, a confirming
    // second run) are the ones the verdict thresholds were measured with.
    const val CMD_START = "setMicTest"
    const val CMD_STATUS = "getMicTest"

    /**
     * The first firmware with a verdict in the result. V1.81 and V1.82 had the test but only raw
     * numbers, and never left a development branch.
     */
    const val MIN_FIRMWARE_VERSION = "ELOC-P_V1.83"

    /**
     * False only when [version] is definitely older than [MIN_FIRMWARE_VERSION]. An unreadable or
     * different-line version gets the benefit of the doubt: the device then answers the command
     * itself, and its error message is shown.
     */
    fun isSupported(version: String): Boolean {
        if (version.isBlank()) {
            return true
        }
        val result = FirmwareVersion.compare(version, MIN_FIRMWARE_VERSION) ?: return true
        return result >= 0
    }

    enum class Verdict(val code: String) {
        Ok("ok"),
        Broken("broken"),
        CheckPort("checkPort"),
        Suspect("suspect"),
        Repeat("repeat"),
        None("none"),
        Unknown("");

        companion object {
            fun parse(code: String): Verdict =
                entries.firstOrNull { it.code.isNotEmpty() && (it.code == code.trim()) } ?: Unknown
        }
    }

    data class Tone(
        val frequencyHz: Int,
        val valid: Boolean,
        val riseDb: Double,
        val offDbfs: Double,
    )

    data class Result(
        val testId: Int,
        val deviceTime: String,
        val verdict: Verdict,
        val finding: String,
        val reason: String,
        val bestRiseDb: Double?,
        val backgroundDbfs: Double?,
        val runs: List<String>,
        val sampleRateHz: Int,
        val measuredSampleRateHz: Int,
        val crackleImpulses: Int,
        val tones: List<Tone>,
        val wavPath: String,
        val warning: String,
    ) {
        /** Identifies one test on one device boot; the device counts tests from 1 after a reboot. */
        val identity get() = "$testId|$deviceTime"
    }

    sealed class Status {
        /** No test since the device booted. */
        data object Idle : Status()

        data class Running(
            val state: String,
            val run: Int,
            val progressPercent: Int,
            val remainingMs: Int,
        ) : Status()

        data class Done(val result: Result) : Status()

        data class Failed(val message: String) : Status()

        /** The reply could not be read at all (timeout, garbage, firmware without the command). */
        data class Unreadable(val message: String) : Status()
    }

    /** Reads a `getMicTest` reply (the whole EOT-JSON frame, `payload` inside). */
    fun parseStatus(response: String): Status {
        val root = try {
            JSONObject(response)
        } catch (_: Exception) {
            return Status.Unreadable("")
        }
        if (root.optInt("ecode", -1) != 0) {
            return Status.Unreadable(root.optString("error", "").trim())
        }
        val payload = root.optJSONObject("payload") ?: return Status.Unreadable("")
        return when (val state = payload.optString("state", "")) {
            "idle" -> Status.Idle
            "starting", "capturing", "analysing", "saving" -> Status.Running(
                state = state,
                run = payload.optInt("run", 1),
                progressPercent = payload.optInt("progress[%]", 0).coerceIn(0, 100),
                remainingMs = payload.optInt("remaining[ms]", 0).coerceAtLeast(0),
            )

            "done" -> Status.Done(parseResult(payload))
            "error" -> Status.Failed(payload.optString("error", "").trim())
            else -> Status.Unreadable("")
        }
    }

    private fun parseResult(payload: JSONObject): Result {
        val verdict = payload.optJSONObject("verdict")
        val runs = mutableListOf<String>()
        verdict?.optJSONArray("runs")?.let { array ->
            for (i in 0 until array.length()) {
                runs.add(array.optString(i, ""))
            }
        }
        val tones = mutableListOf<Tone>()
        payload.optJSONArray("tones")?.let { array ->
            for (i in 0 until array.length()) {
                val tone = array.optJSONObject(i) ?: continue
                tones.add(
                    Tone(
                        frequencyHz = tone.optInt("f[Hz]", 0),
                        valid = tone.optBoolean("valid", true),
                        riseDb = tone.optDouble("rise[dB]", Double.NaN),
                        offDbfs = tone.optDouble("off[dBFS]", Double.NaN),
                    )
                )
            }
        }
        return Result(
            testId = payload.optInt("test", 0),
            deviceTime = payload.optString("time", ""),
            verdict = Verdict.parse(verdict?.optString("result", "") ?: ""),
            finding = verdict?.optString("finding", "") ?: "",
            reason = verdict?.optString("reason", "")?.trim() ?: "",
            bestRiseDb = verdict?.optNumberOrNull("bestRise[dB]"),
            backgroundDbfs = verdict?.optNumberOrNull("background[dBFS]"),
            runs = runs,
            sampleRateHz = payload.optInt("fs[Hz]", 0),
            measuredSampleRateHz = payload.optInt("fsMeasured[Hz]", 0),
            crackleImpulses = payload.optJSONObject("crackle")?.optInt("impulses", 0) ?: 0,
            tones = tones,
            wavPath = payload.optString("wav", ""),
            warning = payload.optString("warning", "").trim(),
        )
    }

    private fun JSONObject.optNumberOrNull(key: String): Double? {
        if (!has(key)) {
            return null
        }
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }
}
