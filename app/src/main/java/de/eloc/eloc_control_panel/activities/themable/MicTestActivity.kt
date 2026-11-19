package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.formatNumber
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityMicTestBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.MicTest
import org.json.JSONObject
import kotlin.math.ceil

/**
 * The microphone self-test: the "works best in a quiet place" hint, a start button, progress while
 * the device beeps and listens, and the device's verdict with the numbers behind it.
 *
 * Its own screen rather than a section of the status page, for the same reasons as the survey
 * screen: the test is a short flow the tech watches from start to end, and the status page's own
 * 15 s refresh and pull-to-refresh would only compete with the test's polling.
 *
 * `setMicTest` returns at once and the test runs on the device for about 12 s (about 24 s with a
 * confirming second run), so progress and the result come from polling `getMicTest`. The device
 * keeps the last result until it reboots, so opening this screen again shows it, and a test still
 * running when the tech left (or one started from the command line) is picked up where it is.
 */
class MicTestActivity : ThemableActivity() {

    companion object {
        private const val LISTENER_ID = "micTestActivity"
        private const val POLL_INTERVAL_MS = 1500L

        // The driver gives up on a command after 15 s without calling back, so a poll older than
        // this is treated as lost and the next one may go out.
        private const val POLL_LOST_MS = 18_000L

        // Two runs of ~12-14 s plus saving. Anything far beyond that means the device is not
        // getting anywhere, and the tech should hear about it instead of watching a stuck bar.
        private const val TEST_GIVE_UP_MS = 75_000L
        private const val MAX_UNREADABLE_POLLS = 5

        fun verdictColorRes(verdict: MicTest.Verdict): Int = when (verdict) {
            MicTest.Verdict.Ok -> R.color.ui_green
            MicTest.Verdict.Broken -> R.color.ui_red
            MicTest.Verdict.CheckPort -> R.color.ui_orange
            MicTest.Verdict.Suspect, MicTest.Verdict.Repeat -> R.color.ui_amber
            MicTest.Verdict.None, MicTest.Verdict.Unknown -> R.color.ui_text_secondary
        }

        fun verdictShortLabelRes(verdict: MicTest.Verdict): Int = when (verdict) {
            MicTest.Verdict.Ok -> R.string.mic_short_ok
            MicTest.Verdict.Broken -> R.string.mic_short_broken
            MicTest.Verdict.CheckPort -> R.string.mic_short_check_port
            MicTest.Verdict.Suspect -> R.string.mic_short_suspect
            MicTest.Verdict.Repeat -> R.string.mic_short_repeat
            MicTest.Verdict.None -> R.string.mic_short_none
            MicTest.Verdict.Unknown -> R.string.mic_short_unknown
        }
    }

    private enum class Phase { Ready, Starting, Running, Done }

    private lateinit var binding: ActivityMicTestBinding
    private val handler = Handler(Looper.getMainLooper())

    private var phase = Phase.Ready
    private var paused = true
    private var connectionLost = false
    private var pollSentAt = 0L          // 0 = no poll in flight
    private var testStartedAt = 0L
    private var unreadablePolls = 0
    private var detailsExpanded = false

    // The result on display before a new test starts. A "done" reply carrying the same test is the
    // old result, not the new one.
    private var shownIdentity = ""

    private val poll = object : Runnable {
        override fun run() {
            if (paused || connectionLost || (phase != Phase.Running)) {
                return
            }
            if (SystemClock.elapsedRealtime() - testStartedAt > TEST_GIVE_UP_MS) {
                showMessage(getString(R.string.mic_no_answer))
                return
            }
            requestStatus()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMicTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.startButton.setOnClickListener { confirmAndStart() }
        binding.detailsHeader.setOnClickListener {
            detailsExpanded = !detailsExpanded
            showDetailsExpanded()
        }
        binding.lowRateNoteTextView.visibility =
            if (DeviceDriver.microphone.sampleRate == SampleRate.Rate8k) View.VISIBLE else View.GONE

        DeviceDriver.addConnectionChangedListener(LISTENER_ID) { status ->
            runOnUiThread {
                if (status == ConnectionStatus.Inactive) {
                    connectionLost = true
                    showMessage(getString(R.string.mic_connection_lost))
                    binding.startButton.isEnabled = false
                }
            }
        }
        setPhase(Phase.Ready)
    }

    override fun onResume() {
        super.onResume()
        paused = false
        if (!connectionLost) {
            // Pick up whatever the device has: its last result, or a test still running.
            requestStatus()
            if (phase == Phase.Running) {
                // The time away does not count against the device.
                testStartedAt = SystemClock.elapsedRealtime()
                handler.removeCallbacks(poll)
                handler.postDelayed(poll, POLL_INTERVAL_MS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        paused = true
        handler.removeCallbacks(poll)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        DeviceDriver.removeConnectionChangedListener(LISTENER_ID)
    }

    private fun confirmAndStart() {
        if (DeviceDriver.session.recordingState.isActive) {
            showModalOptionAlert(
                title = getString(R.string.mic_recording_title),
                message = getString(R.string.mic_recording_message),
                positiveButtonLabel = getString(R.string.mic_test_anyway),
                negativeButtonLabel = getString(R.string.cancel),
                positiveCallback = { startTest() },
            )
        } else {
            startTest()
        }
    }

    private fun startTest() {
        if (connectionLost || DeviceDriver.firmwareTransferActive) {
            return
        }
        hideMessage()
        setPhase(Phase.Starting)
        showProgress(getString(R.string.mic_starting), 0)
        val startedFrom = SystemClock.elapsedRealtime()
        DeviceDriver.processCommandQueue(Command.from(MicTest.CMD_START) { response ->
            runOnUiThread { onStartAnswered(response) }
        })
        // The driver drops a command after 15 s without calling back. If the start reply is the
        // one that got lost, a poll still tells whether the test is running.
        handler.postDelayed({
            if ((phase == Phase.Starting) && (testStartedAt < startedFrom)) {
                beginPolling()
            }
        }, POLL_LOST_MS)
    }

    private fun onStartAnswered(response: String) {
        if (phase != Phase.Starting) {
            return
        }
        if (DeviceDriver.commandSucceeded(response)) {
            beginPolling()
            return
        }
        // Refused: another test, a firmware transfer, a survey, an intruder alarm, a setting the
        // firmware rejects - or firmware that does not know the command at all.
        val reason = try {
            JSONObject(response).optString("error", "").trim()
        } catch (_: Exception) {
            ""
        }
        setPhase(if (shownIdentity.isEmpty()) Phase.Ready else Phase.Done)
        showMessage(
            if (reason.isEmpty()) getString(R.string.mic_refused_no_reason)
            else getString(R.string.mic_refused, reason)
        )
    }

    private fun beginPolling() {
        testStartedAt = SystemClock.elapsedRealtime()
        unreadablePolls = 0
        setPhase(Phase.Running)
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, POLL_INTERVAL_MS)
    }

    private fun requestStatus() {
        if (DeviceDriver.firmwareTransferActive) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if ((pollSentAt != 0L) && (now - pollSentAt < POLL_LOST_MS)) {
            // The previous poll has not come back yet; do not pile up the queue behind it.
            return
        }
        if (pollSentAt != 0L) {
            unreadablePolls++
        }
        pollSentAt = now
        DeviceDriver.processCommandQueue(Command.from(MicTest.CMD_STATUS) { response ->
            runOnUiThread {
                pollSentAt = 0L
                applyStatus(MicTest.parseStatus(response))
            }
        })
    }

    private fun applyStatus(status: MicTest.Status) {
        if (connectionLost) {
            return
        }
        // While the start command is out, only "running" means anything: anything else is the
        // reply to a poll sent before Start was tapped (the screen's opening poll).
        if ((phase == Phase.Starting) && (status !is MicTest.Status.Running)) {
            return
        }
        when (status) {
            is MicTest.Status.Running -> {
                unreadablePolls = 0
                if (phase != Phase.Running) {
                    // A test that was already running when the screen opened.
                    hideMessage()
                    beginPolling()
                }
                showRunning(status)
            }

            is MicTest.Status.Done -> {
                unreadablePolls = 0
                if ((phase == Phase.Running) && (status.result.identity == shownIdentity)) {
                    return  // still the old result; the new test has not replaced it yet
                }
                showResult(status.result)
            }

            is MicTest.Status.Failed -> {
                if (phase == Phase.Running) {
                    setPhase(if (shownIdentity.isEmpty()) Phase.Ready else Phase.Done)
                    showMessage(
                        if (status.message.isEmpty()) getString(R.string.mic_failed_no_reason)
                        else getString(R.string.mic_failed, status.message)
                    )
                }
            }

            MicTest.Status.Idle -> {
                // Idle in the middle of a test means the device restarted under it.
                if (phase == Phase.Running) {
                    setPhase(Phase.Ready)
                    showMessage(getString(R.string.mic_no_answer))
                }
            }

            is MicTest.Status.Unreadable -> {
                if (phase == Phase.Running) {
                    unreadablePolls++
                    if (unreadablePolls >= MAX_UNREADABLE_POLLS) {
                        setPhase(if (shownIdentity.isEmpty()) Phase.Ready else Phase.Done)
                        showMessage(getString(R.string.mic_no_answer))
                    }
                }
            }
        }
    }

    private fun showRunning(status: MicTest.Status.Running) {
        val secondsLeft = ceil(status.remainingMs / 1000.0).toInt()
        val text = when {
            (status.state == "analysing") || (status.state == "saving") -> getString(R.string.mic_analysing)
            status.state == "starting" -> getString(R.string.mic_starting)
            status.run >= 2 -> getString(R.string.mic_running_confirm, secondsLeft)
            else -> getString(R.string.mic_running, secondsLeft)
        }
        showProgress(text, status.progressPercent)
    }

    private fun showProgress(text: String, percent: Int) {
        binding.progressPanel.visibility = View.VISIBLE
        binding.progressTextView.text = text
        binding.progressIndicator.isIndeterminate = false
        binding.progressIndicator.setProgressCompat(percent, true)
    }

    private fun showResult(result: MicTest.Result) {
        shownIdentity = result.identity
        hideMessage()
        setPhase(Phase.Done)

        val (titleRes, textRes) = when (result.verdict) {
            MicTest.Verdict.Ok -> R.string.mic_result_ok_title to R.string.mic_result_ok_text
            MicTest.Verdict.Broken -> R.string.mic_result_broken_title to R.string.mic_result_broken_text
            MicTest.Verdict.CheckPort ->
                R.string.mic_result_check_port_title to R.string.mic_result_check_port_text

            MicTest.Verdict.Suspect -> R.string.mic_result_suspect_title to R.string.mic_result_suspect_text
            MicTest.Verdict.Repeat -> R.string.mic_result_repeat_title to R.string.mic_result_repeat_text
            MicTest.Verdict.None -> R.string.mic_result_none_title to R.string.mic_result_none_text
            MicTest.Verdict.Unknown -> R.string.mic_result_unknown_title to R.string.mic_result_unknown_text
        }
        val color = ContextCompat.getColor(this, verdictColorRes(result.verdict))
        binding.resultIcon.setColorFilter(color)
        binding.resultTitleTextView.text = getString(titleRes)
        binding.resultTitleTextView.setTextColor(color)
        binding.resultTextView.text = getString(textRes)
        binding.reasonTextView.visibility = if (result.reason.isEmpty()) View.GONE else View.VISIBLE
        binding.reasonTextView.text = getString(R.string.mic_device_says, result.reason)
        binding.warningTextView.visibility = if (result.warning.isEmpty()) View.GONE else View.VISIBLE
        binding.warningTextView.text = result.warning

        bindDetails(result)

        if (result.verdict != MicTest.Verdict.Unknown) {
            DeviceDriver.deviceAddress?.let { mac ->
                Preferences.saveMicTest(mac, result.verdict.code, result.finding, result.identity)
            }
        }
    }

    private fun bindDetails(result: MicTest.Result) {
        val dash = "—"
        binding.bestRiseItem.valueText =
            result.bestRiseDb?.let { formatNumber(it, " dB", 1) } ?: dash
        binding.backgroundItem.valueText =
            result.backgroundDbfs?.let { formatNumber(it, " dBFS", 1) } ?: dash
        binding.tonesTextView.text = result.tones.joinToString("\n") { tone ->
            if (!tone.valid || tone.riseDb.isNaN()) {
                getString(R.string.mic_tone_invalid, tone.frequencyHz)
            } else {
                getString(R.string.mic_tone_line, tone.frequencyHz, formatNumber(tone.riseDb, " dB", 1))
            }
        }.ifEmpty { dash }
        binding.clicksItem.valueText = result.crackleImpulses.toString()
        binding.sampleRateItem.valueText = if (result.sampleRateHz > 0) {
            getString(R.string.mic_sample_rate_value, result.sampleRateHz, result.measuredSampleRateHz)
        } else {
            dash
        }
        binding.runsItem.valueText = result.runs.joinToString(", ").ifEmpty { dash }
        binding.timeItem.valueText = result.deviceTime.ifEmpty { dash }
        binding.fileItem.valueText = result.wavPath.substringAfterLast('/').ifEmpty { dash }
        showDetailsExpanded()
    }

    private fun showDetailsExpanded() {
        binding.detailsContent.visibility = if (detailsExpanded) View.VISIBLE else View.GONE
        binding.detailsChevron.rotation = if (detailsExpanded) 180f else 0f
    }

    private fun setPhase(newPhase: Phase) {
        phase = newPhase
        val busy = (newPhase == Phase.Starting) || (newPhase == Phase.Running)
        if (busy) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            handler.removeCallbacks(poll)
        }
        binding.progressPanel.visibility = if (busy) View.VISIBLE else View.GONE
        val hasResult = (newPhase == Phase.Done) || (!busy && shownIdentity.isNotEmpty())
        binding.resultPanel.visibility = if (hasResult && !busy) View.VISIBLE else View.GONE
        binding.detailsHeader.visibility = binding.resultPanel.visibility
        binding.detailsContent.visibility =
            if ((binding.resultPanel.visibility == View.VISIBLE) && detailsExpanded) View.VISIBLE else View.GONE
        binding.startButton.isEnabled = !busy && !connectionLost
        binding.startButton.text = getString(
            if (shownIdentity.isEmpty()) R.string.mic_start else R.string.mic_test_again
        )
    }

    private fun showMessage(text: String) {
        if (phase == Phase.Running || phase == Phase.Starting) {
            setPhase(if (shownIdentity.isEmpty()) Phase.Ready else Phase.Done)
        }
        binding.messageTextView.text = text
        binding.messageTextView.visibility = View.VISIBLE
    }

    private fun hideMessage() {
        binding.messageTextView.visibility = View.GONE
    }
}
