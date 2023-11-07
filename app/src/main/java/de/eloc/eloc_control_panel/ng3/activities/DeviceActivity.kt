package de.eloc.eloc_control_panel.ng3.activities

import android.os.Build
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.ng3.DeviceDriver
import de.eloc.eloc_control_panel.ng3.data.ConnectionStatus
import de.eloc.eloc_control_panel.ng3.data.LabelColor
import de.eloc.eloc_control_panel.ng3.data.LocationHelper
import java.text.NumberFormat
import java.util.Locale

// todo: show free space text under storage gauge
// todo: show % of free storage in gauge after firmware update
// todo: show battery % after firmware update
// todo: STATUS section will have data loaded after firmware update.
// todo: DETECTOR SETTINGS section -> RecordWhenDetected, Model, Communication -> after AI has been implemented
// todo: mic gain is bitshift
// todo: hrs per file is seconds per file
// todo: recording since boot is totalRecordingTime
// todo: detecting button to be activate when detection is implemented.
// todo: add refresh menu item for old API levels
// todo: is LabelColor enum used?
class DeviceActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_RANGER_NAME = "ranger_name"
        private const val SECOND_MILLIS = 1000L
    }

    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = ""
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var deviceName = ""
    private var rangerName = ""
    private var refreshing = false
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        LocationHelper.stopUpdates()
    }

    override fun onStop() {
        super.onStop()
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
                    updateGpsViews()
                    connect()
                    setListeners()
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
        binding.elocAppBar.setOnBackButtonClickedListener { goBack() }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!refreshing) {
                refreshing = true
                connectToDevice()
            }
        }
        binding.startDetectingButton.setOnClickListener { DeviceDriver.disconnect() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.contentScrollView.setOnScrollChangeListener(scrollChangeListener)
        }
    }

    private fun connectToDevice() {
        DeviceDriver.disconnect()
        DeviceDriver.connect(deviceAddress, ::onConnectionChanged)
        binding.swipeRefreshLayout.isRefreshing = true
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
        LocationHelper.startUpdates {
            locationAccuracy = it.accuracy.toDouble()
            val code = OpenLocationCode(it.latitude, it.longitude)
            locationCode = code.code
            updateGpsViews()
        }
    }

    private fun updateGpsViews() {
        binding.gpsGauge.updateValue(locationAccuracy)
        binding.gpsStatus.text = formatNumber(locationAccuracy, "m", 0)
    }

    private fun setLabelColor(label: TextView, color: LabelColor, foreground: Boolean) {
        if (foreground) {
            label.setTextColor(color.color)
        } else {
            label.setBackgroundColor(color.color)
        }
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
        binding.elocAppBar.title = "ChangeMe"
        binding.elocAppBar.userName = "Jane Doe"
        updateBatteryLevel(24.0)
        updateStorageStatus(78.0)
        updateGpsAccuracy(7.0)
        binding.detectingSinceTextView.text = "1D 10H 22M"
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
        val colorRes = if (recordWhenDetected) {
            binding.recordWhenDetectedTextView.setText(R.string.on)
            R.color.status_field_green
        } else {
            binding.recordWhenDetectedTextView.setText(R.string.off)
            R.color.status_field_red
        }
        val color = ContextCompat.getColor(this, colorRes)
        binding.recordWhenDetectedTextView.setTextColor(color)
        binding.modelTextView.text = "Trumpet_V12"
        binding.communicationTextView.text = "LoRa"
        binding.sampleRateTextView.text = "1600"
        binding.gainTextView.text = "HIGH"
        binding.hoursPerFileTextView.text = "1"
        binding.sessionIdTextView.text = "ELOC234214324"
    }

    private fun updateBatteryLevel(level: Double) {
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
    }

    private fun updateStorageStatus(free: Double) {
        binding.storageGauge.updateValue(free)
        binding.storageStatus.text = getString(R.string.gauge_free_space, free.toInt())
    }

    private fun updateGpsAccuracy(meters: Double) {
        binding.gpsGauge.updateValue(meters)
        val res =
            if (meters >= 100) R.string.gauge_gps_long_distance else R.string.gauge_gps_distance
        binding.gpsStatus.text = getString(res, meters.toInt())
    }
}