package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.prettifyTime
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import de.eloc.eloc_control_panel.databinding.ActivitySurveyBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import org.json.JSONObject

/**
 * Live readout for the LoRa coverage survey, and the "measure here" button.
 *
 * Deliberately its own screen rather than a card on the status page: during a survey the ranger is
 * holding the phone and looking at exactly one thing, and the reading has to be legible at arm's
 * length in daylight. It also keeps the survey's own polling off the status page, which already
 * refreshes plenty on its own.
 *
 * The measure button is the counterpart to GPIO0 on the device. They are deliberately different:
 * the physical button forces an extra *uplink* (free — it just drops another point on the map, and
 * it is the button anyone can press by accident), while this one forces an uplink that also asks
 * for a *downlink*, which is what actually returns a margin. Downlinks are the scarce resource —
 * TTN's fair-use policy allows ten a day — so spending one is a deliberate act, behind the app.
 */
class SurveyActivity : ThemableActivity() {

    private lateinit var binding: ActivitySurveyBinding

    private val handler = Handler(Looper.getMainLooper())
    private var polling = false
    private var awaitingMeasurement = false

    // The device answers getLinkCheck as soon as the check is *scheduled*, not when it completes:
    // one uplink plus both RX windows blocks for 6-7 s, and holding the Bluetooth task that long
    // would starve SPP and drop the connection. So the result arrives through a later poll, and
    // the button stays busy until it does.
    private val pollIntervalMs = 3000L
    private val measurementTimeoutMs = 20000L

    private val levelLabels by lazy {
        listOf(
            getString(R.string.survey_level_0),
            getString(R.string.survey_level_1),
            getString(R.string.survey_level_2),
            getString(R.string.survey_level_3),
            getString(R.string.survey_level_4),
            getString(R.string.survey_level_5),
        )
    }

    private val poll = object : Runnable {
        override fun run() {
            requestStatus()
            if (polling) {
                handler.postDelayed(this, pollIntervalMs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.measureButton.setOnClickListener { measureHere() }
        binding.startStopButton.setOnClickListener { toggleSurvey() }

        showReading(level = -1, marginDb = -1, gwCnt = 0, sf = 0)
    }

    override fun onResume() {
        super.onResume()
        polling = true
        handler.post(poll)
    }

    override fun onPause() {
        super.onPause()
        polling = false
        handler.removeCallbacks(poll)
    }

    private fun runCommand(command: Command) = DeviceDriver.processCommandQueue(command)

    private fun requestStatus() {
        if (DeviceDriver.firmwareTransferActive) {
            return
        }
        runCommand(Command.from("getSurveyStatus") { response ->
            runOnUiThread { applyStatus(response) }
        })
    }

    private fun measureHere() {
        awaitingMeasurement = true
        updateMeasureButton()
        handler.postDelayed({
            if (awaitingMeasurement) {
                awaitingMeasurement = false
                updateMeasureButton()
            }
        }, measurementTimeoutMs)

        runCommand(Command.from("getLinkCheck") { response ->
            runOnUiThread {
                if (!DeviceDriver.commandSucceeded(response)) {
                    // Almost always "survey mode is not running" — the firmware refuses rather
                    // than silently spending a downlink outside a session.
                    awaitingMeasurement = false
                    updateMeasureButton()
                    binding.measureHintTextView.text = getString(R.string.survey_measure_refused)
                }
            }
        })
    }

    private fun toggleSurvey() {
        val turningOn = !DeviceDriver.survey.enabled
        val mode = if (turningOn) "on" else "off"
        runCommand(Command.from("setSurveyMode#mode=$mode") { response ->
            runOnUiThread {
                if (DeviceDriver.commandSucceeded(response)) {
                    DeviceDriver.survey.enabled = turningOn
                }
                requestStatus()
            }
        })
    }

    private fun applyStatus(response: String) {
        val payload = try {
            JSONObject(response).optJSONObject("payload")
        } catch (_: Exception) {
            null
        } ?: return

        val active = payload.optBoolean("active", false)
        DeviceDriver.survey.enabled = active

        val marginDb = payload.optInt("marginDb", -1)
        val level = if (marginDb < 0) -1 else payload.optInt("level", 0)
        val gwCnt = payload.optInt("gwCnt", 0)
        val sf = payload.optInt("sf", 0)

        // A fresh margin means the link check we asked for has come back.
        if (awaitingMeasurement && marginDb >= 0) {
            awaitingMeasurement = false
            binding.measureHintTextView.text = getString(R.string.survey_measure_hint)
        }

        showReading(level, marginDb, gwCnt, sf)
        updateMeasureButton()

        val elapsed = payload.optInt("elapsedS", 0)
        binding.stateItem.valueText = if (active) {
            getString(R.string.survey_state_running, prettifyTime(elapsed))
        } else {
            getString(R.string.survey_state_stopped)
        }
        binding.samplesItem.valueText = payload.optInt("samples", 0).toString()
        binding.budgetItem.valueText = getString(
            R.string.survey_budget_value,
            payload.optInt("uplinksToday", 0),
            payload.optInt("maxUplinksPerDay", 0),
            payload.optInt("downlinksToday", 0),
            payload.optInt("maxDownlinksPerDay", 0),
        )
        binding.spacingItem.valueText = getString(
            R.string.survey_spacing_value,
            payload.optInt("minDistanceM", 0),
            prettifyTime(payload.optInt("minIntervalS", 0)),
        )
        val file = payload.optString("file", "")
        binding.fileItem.valueText = file.substringAfterLast('/').ifEmpty {
            getString(R.string.survey_no_session)
        }

        binding.startStopButton.text = getString(
            if (active) R.string.survey_stop else R.string.survey_start
        )
    }

    private fun showReading(level: Int, marginDb: Int, gwCnt: Int, sf: Int) {
        if (level < 0) {
            // No link check has come back yet this session. Showing a stale or invented level here
            // would be worse than showing nothing — the whole point of the reading is that it is
            // current at the spot the ranger is standing.
            binding.levelTextView.text = getString(R.string.survey_no_reading_symbol)
            binding.levelLabelTextView.text = getString(R.string.survey_no_reading)
            binding.readingDetailTextView.text = if (sf > 0) {
                getString(R.string.survey_reading_sf_only, sf)
            } else {
                ""
            }
            return
        }

        binding.levelTextView.text = level.toString()
        binding.levelLabelTextView.text = levelLabels.getOrElse(level) { "" }
        binding.readingDetailTextView.text = getString(
            R.string.survey_reading_detail, marginDb, gwCnt, sf
        )
    }

    private fun updateMeasureButton() {
        binding.measureButton.isEnabled = DeviceDriver.survey.enabled && !awaitingMeasurement
        binding.measureButton.text = getString(
            if (awaitingMeasurement) R.string.survey_measuring else R.string.survey_measure_here
        )
    }
}
