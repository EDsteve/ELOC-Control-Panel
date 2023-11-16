package de.eloc.eloc_control_panel.ng3.activities

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng3.DeviceDriver
import de.eloc.eloc_control_panel.ng3.data.ConnectionStatus
import de.eloc.eloc_control_panel.ng3.data.DeviceState
import de.eloc.eloc_control_panel.ng3.data.GainType
import de.eloc.eloc_control_panel.ng3.data.LocationHelper
import de.eloc.eloc_control_panel.ng3.interfaces.BluetoothDeviceListener
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

// todo: show free space text under storage gauge
// todo: show % of free storage in gauge after firmware update
// todo: show battery % after firmware update
// todo: STATUS section will have data loaded after firmware update.
// todo: DETECTOR SETTINGS section -> RecordWhenDetected, Model, Communication -> after AI has been implemented
// todo: detecting button to be activated when detection is implemented.
// todo: add refresh menu item for old API levels
// todo: is LabelColor enum used?

class DeviceActivity : AppCompatActivity() {
    companion object {
        private const val ONE_MINUTE_SECONDS = 60.0
        private const val ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60
        private const val ONE_DAY_SECONDS = ONE_HOUR_SECONDS * 24

        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_RANGER_NAME = "ranger_name"

        private const val PATH_SEPARATOR = ":"
        private const val KEY_CMD = "cmd"
        private const val KEY_MICROPHONE_GAIN = "MicBitShift"
        private const val KEY_MICROPHONE_TYPE = "MicType"
        private const val KEY_MICROPHONE_SAMPLE_RATE = "MicSampleRate"
        private const val KEY_SECONDS_PER_FILE = "secondsPerFile"
        private const val KEY_MICROPHONE = "mic"
        private const val KEY_CONFIG = "config"
        private const val KEY_SESSION = "session"
        private const val KEY_DEVICE = "device"
        private const val KEY_LOCATION_ACCURACY = "locationAccuracy"
        private const val KEY_RECORDING_TIME = "recordingTime[h]"
        private const val KEY_TOTAL_RECORDING_TIME = "totalRecordingTime[h]"
        private const val KEY_BLUETOOTH_ENABLED_DURING_REC = "bluetoothEnableDuringRecord"
        private const val KEY_LOCATION = "location"
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_PAYLOAD = "payload"
        private const val KEY_FIRMWARE = "firmware"
        private const val CMD_GET_CONFIG = "getConfig"
        private const val CMD_GET_STATUS = "getStatus"
    }

    private var hasSDCardError = false
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = "UNKNOWN"
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var deviceName = ""
    private var rangerName = ""
    private var refreshing = false
    private var recordingSeconds = 0.0
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }

    private val bluetoothDeviceListener = object : BluetoothDeviceListener() {
        override fun onRead(data: ByteArray) {
            try {
                val json = String(data)
                val root = JSONObject(json)
                when (root.getString(KEY_CMD)) {
                    CMD_GET_CONFIG -> {
                        setMicrophoneType(root)
                        setMicrophoneGain(root)
                        setMicrophoneSampleRate(root)
                        setHoursPerFile(root)
                        setLastGPSAccuracy(root)
                        setLastGPSLocation(root)
                        setBluetoothDuringRecording(root)
                    }

                    CMD_GET_STATUS -> {
                        setSessionID(root)
                        setRecordingTime(root)
                        setRecSinceBoot(root)
                        setFirmwareVersion(root)
                    }

                    else -> {}
                }
            } catch (_: Exception) {
            }
        }
    }

    private var deviceState = DeviceState.Ready
        set(value) {
            field = value
            when (field) {
                DeviceState.Recording -> {
                    binding.recordingItem.itemValue = getString(R.string.on).uppercase()
                    binding.startRecordingButton.text = getString(R.string.stop_recording)
                    setLabelColor(
                        binding.startRecordingButton,
                        null,
                        R.color.recording_on_button,
                    )
                }

                DeviceState.Ready -> {
                    binding.recordingItem.itemValue = getString(R.string.off).uppercase()
                    binding.startRecordingButton.text = getString(R.string.start_recording)
                    setLabelColor(
                        binding.startRecordingButton,
                        null,
                        R.color.recording_off_button
                    )
                }

                DeviceState.Stopping -> {
                    binding.recordingItem.itemValue = getString(R.string.stopping).uppercase()
                    binding.startRecordingButton.text = getString(R.string.stopping)
                    setLabelColor(
                        binding.startRecordingButton,
                        null,
                        R.color.recording_on_button
                    )
                    recordingSeconds = 0.0
                }

                DeviceState.Detecting -> {
                    setLabelColor(
                        binding.startRecordingButton,
                        R.color.disabled_button_text,
                        R.color.disabled_button
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setLaunchers()
        initialize()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        testConnect()
    }

    override fun onPause() {
        super.onPause()
        LocationHelper.stopUpdates()
        locationAccuracy = 100.0
        locationCode = "UNKNOWN"
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.disconnect()
    }

    private fun initialize() {
        onConnectionChanged(ConnectionStatus.Inactive)
        val extras = intent.extras
        deviceAddress = extras?.getString(EXTRA_DEVICE_ADDRESS, "")?.trim() ?: ""
        if (deviceAddress.isEmpty()) {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.device_address_required),
                ::goBack
            )
        } else {
            deviceName = extras?.getString(EXTRA_DEVICE_NAME, "")?.trim() ?: ""
            if (deviceName.isEmpty()) {
                showModalAlert(
                    getString(R.string.required),
                    getString(R.string.device_name_required),
                    ::goBack
                )
            } else {
                rangerName = extras?.getString(EXTRA_RANGER_NAME, "")?.trim() ?: ""
                if (rangerName.isEmpty()) {
                    showModalAlert(
                        getString(R.string.required),
                        getString(R.string.ranger_name_required),
                        ::goBack
                    )
                } else {
                    binding.elocAppBar.title = deviceName
                    binding.elocAppBar.userName = rangerName
                    binding.appVersionItem.itemValue = App.versionName
                    updateGpsViews()
                    testConnect() // connect()
                    setListeners()
                }
            }
        }
    }

    private fun setLaunchers() {
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val extras = intent.extras
                    if (extras != null) {
                        val command = extras.getString(DeviceSettingsActivity.COMMAND, "")
                        if (command.isNotEmpty()) {
                            DeviceDriver.write(command)
                            showSnack(
                                binding.coordinator,
                                getString(R.string.command_sent_successfully)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun connect() {
        binding.elocAppBar.visibility = View.GONE
        binding.startDetectingButton.visibility = View.GONE
        binding.startRecordingButton.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
        binding.loadingLinearLayout.visibility = View.VISIBLE
        binding.connectionProgressIndicator.visibility = View.VISIBLE
        binding.connectionStatusTextView.setText(R.string.connecting)
        connectToDevice()
    }

    private fun setListeners() {
        binding.startRecordingButton.setOnClickListener { recordButtonClicked() }
        binding.elocAppBar.setOnBackButtonClickedListener { goBack() }
        binding.elocAppBar.setOnSettingsButtonClickedListener { openSettings() }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!refreshing) {
                refreshing = true
                connectToDevice()
            }
        }
        binding.startDetectingButton.setOnClickListener {
            // todo: tests
            deviceState = DeviceState.Recording
            testGet()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.contentScrollView.setOnScrollChangeListener(scrollChangeListener)
        }
    }

    private fun openSettings() {
        if (deviceState == DeviceState.Ready) {
            val intent = Intent(this, DeviceSettingsActivity::class.java)
            // todo: device name might be changed in DeviceSettingsActivity
            intent.putExtra(EXTRA_DEVICE_NAME, deviceName)
            settingsLauncher.launch(intent)
        } else {
            showModalAlert(
                getString(R.string.not_available),
                getString(R.string.device_settings_not_available)
            )
        }
    }

    private fun connectToDevice() {
        /*DeviceDriver.connect(deviceAddress, ::onConnectionChanged, bluetoothDeviceListener)
        binding.swipeRefreshLayout.isRefreshing = true*/
    }

    private fun onConnectionChanged(status: ConnectionStatus) {
        runOnUiThread {
            refreshing = false
            binding.swipeRefreshLayout.isRefreshing = false
            when (status) {
                ConnectionStatus.Active -> {
                    binding.elocAppBar.visibility = View.VISIBLE
                    binding.elocAppBar.showSettingsButton = true
                    binding.startDetectingButton.visibility = View.VISIBLE
                    binding.startRecordingButton.visibility = View.VISIBLE
                    binding.contentScrollView.visibility = View.VISIBLE
                    binding.loadingLinearLayout.visibility = View.GONE
                    binding.swipeRefreshLayout.isEnabled = true
                    getDeviceInfo()
                }

                ConnectionStatus.Inactive -> {
                    binding.elocAppBar.visibility = View.VISIBLE
                    binding.elocAppBar.showSettingsButton = false
                    binding.startDetectingButton.visibility = View.GONE
                    binding.startRecordingButton.visibility = View.GONE
                    binding.contentScrollView.visibility = View.GONE
                    binding.loadingLinearLayout.visibility = View.VISIBLE
                    binding.connectionProgressIndicator.visibility = View.GONE
                    binding.connectionStatusTextView.setText(R.string.disconnected_swipe_to_reconnect)
                    binding.swipeRefreshLayout.isEnabled = true
                }

                ConnectionStatus.Pending -> {
                    binding.elocAppBar.visibility = View.GONE
                    binding.startDetectingButton.visibility = View.GONE
                    binding.startRecordingButton.visibility = View.GONE
                    binding.contentScrollView.visibility = View.GONE
                    binding.loadingLinearLayout.visibility = View.VISIBLE
                    binding.connectionProgressIndicator.visibility = View.VISIBLE
                    binding.connectionStatusTextView.setText(R.string.connecting)
                    binding.swipeRefreshLayout.isEnabled = false
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val listener = object : LocationListener {
            @Deprecated("Deprecated in Java")
            @Suppress("deprecated")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onLocationChanged(location: Location) {
                locationAccuracy = location.accuracy.toDouble()
                val code = OpenLocationCode(location.latitude, location.longitude)
                locationCode = code.code
                updateGpsViews()
            }
        }

        LocationHelper.startUpdates(listener)
    }

    private fun updateGpsViews() {
        binding.gpsGauge.updateValue(locationAccuracy)
        binding.gpsStatus.text = formatNumber(locationAccuracy, "m", 0)
    }

    private fun setMicrophoneSampleRate(jsonObject: JSONObject) {
        // Sample rate
        val sampleRatePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_SAMPLE_RATE"
        val sampleRate = getJSONNumberAttribute(sampleRatePath, jsonObject)
        val prettyRate = formatNumber(sampleRate / 1000, "kHz", maxFractionDigits = 0)
        binding.sampleRateItem.itemValue = prettyRate
    }

    private fun setHoursPerFile(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_SECONDS_PER_FILE"
        val secondsPerFile = getJSONNumberAttribute(path, jsonObject)
        val hoursPerFile = secondsPerFile / ONE_HOUR_SECONDS
        binding.hoursPerFileItem.itemValue = formatNumber(hoursPerFile, "h")
    }

    private fun setLastGPSAccuracy(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_LOCATION_ACCURACY"
        val lastLocationAccuracy = getJSONNumberAttribute(path, jsonObject)
        binding.lastGpsAccuracyItem.itemValue = formatNumber(lastLocationAccuracy, "m")
    }

    private fun setLastGPSLocation(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_LOCATION"
        val location = getJSONStringAttribute(path, jsonObject)
        binding.lastGpsLocationItem.itemValue = location
    }

    private fun setBluetoothDuringRecording(jsonObject: JSONObject) {
        val path =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BLUETOOTH_ENABLED_DURING_REC"
        val enabled = getJSONBooleanAttribute(path, jsonObject)
        val enabledLabel = getString(if (enabled) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.itemValue = enabledLabel
    }

    private fun setSessionID(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_IDENTIFIER"
        val sessionId = getJSONStringAttribute(path, jsonObject)
        binding.sessionIdItem.itemValue = sessionId
    }

    private fun setRecordingTime(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_TIME"
        val hours = getJSONNumberAttribute(path, jsonObject)
        recordingSeconds = hours * ONE_HOUR_SECONDS
    }

    private fun setRecSinceBoot(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_TOTAL_RECORDING_TIME"
        val hours = getJSONNumberAttribute(path, jsonObject)
        binding.recSinceBootItem.itemValue = formatHours(hours)
    }

    private fun setFirmwareVersion(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FIRMWARE"
        val version = getJSONStringAttribute(path, jsonObject)
        binding.firmwareVersionItem.itemValue = version
    }

    private fun setMicrophoneType(jsonObject: JSONObject) {
        val typePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_TYPE"
        val micType = getJSONStringAttribute(typePath, jsonObject)
        binding.microphoneTypeItem.itemValue = micType
    }

    private fun setMicrophoneGain(jsonObject: JSONObject) {
        val gainPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_GAIN"
        val micBitShift = getJSONStringAttribute(gainPath, jsonObject)
        val gainText = when (GainType.fromValue(micBitShift)) {
            GainType.Low -> getString(R.string.low)
            GainType.High -> getString(R.string.high)
        }
        binding.gainItem.itemValue = gainText
    }

    private fun setLabelColor(
        label: TextView,
        @ColorRes foregroundColor: Int? = null,
        @ColorRes backgroundColor: Int? = null
    ) {
        if (foregroundColor != null) {
            val color = ContextCompat.getColor(this, foregroundColor)
            label.setTextColor(color)
        }
        if (backgroundColor != null) {
            val color = ContextCompat.getColor(this, backgroundColor)
            label.setBackgroundColor(color)
        }
    }

    private fun formatHours(hours: Double) = formatTime(hours * ONE_HOUR_SECONDS)

    private fun formatTime(durationSeconds: Double): String {

        val days = (durationSeconds / ONE_DAY_SECONDS).toInt()
        var remaining = durationSeconds - (days * ONE_DAY_SECONDS)
        val prettyDays = if (days > 0) "${days}d" else ""

        val hours = (remaining / ONE_HOUR_SECONDS).toInt()
        remaining -= (ONE_HOUR_SECONDS * hours)
        val prettyHours = if (hours > 0) "${hours}h" else ""

        val minutes = (remaining / ONE_MINUTE_SECONDS).toInt()
        val prettyMinutes = if (minutes > 0) "${minutes}m" else ""

        val result = "$prettyDays $prettyHours $prettyMinutes".trim()
        return result.ifEmpty { getString(R.string.less_than_one_minute) }

    }

    private fun formatNumber(number: Double, units: String, maxFractionDigits: Int = 2): String {
        val format = NumberFormat.getInstance(Locale.ENGLISH)
        format.maximumFractionDigits = maxFractionDigits
        format.minimumFractionDigits = 0
        return format.format(number) + units
    }

    private fun getDeviceInfo() {
// todo
    }

    private fun setUiData() {
        /*
                updateBatteryLevel(24.0)
                updateStorageStatus(78.0)

                binding.detectingSinceItem.itemValue  = "1D 10H 22M"
                val recording = false
                val recordingColor = if (recording) {
                    binding.recordingTextView.setText(R.string.on)
                    ContextCompat.getColor(this, R.color.status_field_green)
                } else {
                    binding.recordingTextView.setText(R.string.off)
                    ContextCompat.getColor(this, R.color.status_field_red)
                }
                binding.recordingTextView.setTextColor(recordingColor)

                val recordWhenDetected = true
                val colorRes =
                    if (recordWhenDetected) R.color.status_field_green else R.color.status_field_red

                val textRes = if (recordWhenDetected) R.string.on else R.string.off
                val color = ContextCompat.getColor(this, colorRes)
                binding.recordWhenDetectedTextView.setTextColor(color)
                binding.modelItem.itemValue = "Trumpet_V12"
                binding.communicationItem.itemValue  = "LoRa"
        */
    }

    private fun updateBatteryLevel(level: Double) {
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
    }

    private fun updateStorageStatus(free: Double) {
        binding.storageGauge.updateValue(free)
        binding.storageStatus.text = getString(R.string.gauge_free_space, free.toInt())
    }

    private fun getJSONBooleanAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Boolean = false,
    ): Boolean {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getBoolean(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONStringAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: String = ""
    ): String {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getString(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONNumberAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Double = 0.0
    ): Double {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getDouble(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONAttributeNode(
        path: String,
        jsonObject: JSONObject
    ): Pair<String, JSONObject?> {
        var result: JSONObject? = null
        var key = ""
        try {
            val parts = path.split(PATH_SEPARATOR)
            val iterator = parts.iterator()
            var node = jsonObject
            while (iterator.hasNext()) {
                val name = iterator.next()
                if (!iterator.hasNext()) {
                    result = node
                    key = name
                } else {
                    node = node.getJSONObject(name)
                }
            }
        } catch (_: Exception) {

        }
        return (key to result)
    }

    private fun showSDCardError() {
        if (hasSDCardError) {
            showModalAlert(
                getString(R.string.sd_card),
                getString(R.string.sd_card_error_message),
            )
        }
    }

    private fun confirmIgnoreAccuracy() {
        showModalOptionAlert(
            getString(R.string.confirm),
            getString(R.string.please_wait_for_gps_accuracy),
            getString(R.string.record_anyway),
            getString(R.string.wait55),
            { startRecording() },
        )
    }

    private fun recordButtonClicked() {
        when (deviceState) {
            DeviceState.Recording -> {
                stopRecording()
            }

            DeviceState.Ready -> {
                if (hasSDCardError) {
                    showSDCardError()
                    return
                }

                if (locationAccuracy <= 8.1) {
                    startRecording()
                } else {
                    confirmIgnoreAccuracy()
                }
            }

            else -> {}
        }
    }

    private fun stopRecording() {
        deviceState = DeviceState.Stopping
        DeviceDriver.stopRecording()
        // TODO: Notes for future: remove line below and wait for update from firmware/device
        deviceState = DeviceState.Ready
    }

    private fun startRecording() {
        binding.startRecordingButton.setText(R.string.please_wait)
        /*
                // Respect the bt recording state setting
                boolean btOnWhenRecording = preferencesHelper.getBluetoothRecordingState();
                send("setGPS^" + locationCode + "#" + locationAccuracy);


                // If bt on ELOC must be off, it means app will lose connection; go back to main screen
                if (!btOnWhenRecording) {
                    String message = getString(R.string.connection_close_message);
                    ActivityHelper.INSTANCE.showAlert(this, message, false, this::onBackPressed);
                }*/
    }

    private fun testGet() {
        DeviceDriver.dummyGetConfig()
        DeviceDriver.dummyGetStatus()
    }

    private fun testConnect() {
        DeviceDriver.connect("deviceAddress", ::onConnectionChanged, bluetoothDeviceListener, true)
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun setDeviceState(state: DeviceState, errorMessage: String = "") {
        deviceState = state
        val err = errorMessage.trim()
        if (err.isNotEmpty()) {
            showModalAlert(getString(R.string.oops), err)
        }
    }
}