package de.eloc.eloc_control_panel.activities.themable

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.databinding.ActivityFirmwareUpdateBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.FirmwareImage
import de.eloc.eloc_control_panel.driver.FirmwareUpdater
import de.eloc.eloc_control_panel.services.FirmwareUpdateService
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/**
 * MVP firmware-update screen (Phase 2 of the firmware-update plan): pick a
 * local .bin (SAF), show its embedded version + SHA-256, run the preflight-
 * gated transfer via [FirmwareUpdater], then confirm the new version after
 * the device's flash-and-restart cycle. Only reachable when the connected
 * firmware advertises fwUpdateProto in getStatus.
 */
class FirmwareUpdateActivity : ThemableActivity() {

    private lateinit var binding: ActivityFirmwareUpdateBinding
    private val listenerId = "firmwareUpdateActivity"
    private val executor = Executors.newSingleThreadExecutor()

    private var stagedFile: File? = null
    private var imageInfo: FirmwareImage.Info? = null
    private var imageVariant = ""

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                loadImageFile(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirmwareUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.chooseFileButton.setOnClickListener {
            filePicker.launch(arrayOf("application/octet-stream", "application/*"))
        }
        binding.startButton.setOnClickListener { confirmAndStart() }
        binding.abortButton.setOnClickListener { FirmwareUpdater.abort() }
        binding.stopRecordingButton.setOnClickListener { stopRecording() }

        binding.installedVersionValue.text = DeviceDriver.general.version
        binding.variantValue.text = DeviceDriver.general.buildVariant

        FirmwareUpdater.addStateListener(listenerId, ::onUpdaterState)
        FirmwareUpdater.addProgressListener(listenerId, ::onUpdaterProgress)
        // Reflect an update that is already running (e.g. re-entered via the notification)
        onUpdaterState(FirmwareUpdater.state, FirmwareUpdater.lastMessage)
        updateFileViews()
    }

    override fun onDestroy() {
        FirmwareUpdater.removeStateListener(listenerId)
        FirmwareUpdater.removeProgressListener(listenerId)
        executor.shutdown()
        super.onDestroy()
    }

    // Don't auto-close on disconnect like other device screens: disconnects are
    // an expected part of the update cycle (device restarts to flash).

    private fun loadImageFile(uri: Uri) {
        binding.fileStatusText.text = getString(R.string.firmware_reading_file)
        executor.execute {
            var info: FirmwareImage.Info? = null
            var staged: File? = null
            try {
                val target = File(cacheDir, "fwupdate.bin")
                contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                info = FirmwareImage.inspect(target)
                staged = target
            } catch (_: Exception) {
            }

            // Release-file naming convention carries the build variant
            // (ELOC-<version>-<variant>.bin) since the image itself has no
            // variant marker.
            val fileName = displayName(uri).lowercase(Locale.US)
            imageVariant = when {
                fileName.contains("no-ai") -> "no-ai"
                fileName.contains("-ei") || fileName.contains("_ei") -> "ei"
                else -> ""
            }

            runOnUiThread {
                if (info == null) {
                    stagedFile = null
                    imageInfo = null
                    showModalAlert(
                        getString(R.string.error),
                        getString(R.string.firmware_invalid_image)
                    )
                } else {
                    stagedFile = staged
                    imageInfo = info
                }
                updateFileViews()
            }
        }
    }

    /**
     * Real file name of a SAF document. [Uri.getLastPathSegment] is not
     * reliable for this — e.g. the Downloads provider returns an opaque
     * "msf:<id>" — which would silently disable the variant guard.
     */
    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index) ?: ""
                    }
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: ""
    }

    private fun updateFileViews() {
        val info = imageInfo
        if (info == null) {
            binding.fileDetailsLayout.visibility = View.GONE
            binding.fileStatusText.text = getString(R.string.firmware_no_file_selected)
            binding.startButton.isEnabled = false
            return
        }
        binding.fileDetailsLayout.visibility = View.VISIBLE
        binding.fileStatusText.text = ""
        binding.fileVersionValue.text = info.version
        binding.fileProjectValue.text = info.projectName
        binding.fileSizeValue.text = formatSize(info.size)
        binding.fileShaValue.text = info.sha256
        binding.fileVariantValue.text =
            imageVariant.ifEmpty { getString(R.string.firmware_variant_unknown) }
        binding.startButton.isEnabled = !FirmwareUpdater.isBusy
    }

    private fun confirmAndStart() {
        val info = imageInfo ?: return
        val deviceVariant = DeviceDriver.general.buildVariant

        // Variant guard: refuse a definite mismatch (an "ei" build on a "no-ai"
        // device or vice versa); warn when the filename didn't reveal a variant.
        if (imageVariant.isNotEmpty() && deviceVariant.isNotEmpty() && imageVariant != deviceVariant) {
            showModalAlert(
                getString(R.string.firmware_variant_mismatch_title),
                getString(R.string.firmware_variant_mismatch, imageVariant, deviceVariant)
            )
            return
        }

        val warning = if (imageVariant.isEmpty()) {
            getString(R.string.firmware_variant_unknown_warning) + "\n\n"
        } else {
            ""
        }
        showModalOptionAlert(
            getString(R.string.firmware_update),
            warning + getString(
                R.string.firmware_confirm_update,
                DeviceDriver.general.version,
                info.version
            ),
            getString(R.string.firmware_update_now),
            positiveCallback = { startUpdate() }
        )
    }

    private fun startUpdate() {
        val file = stagedFile ?: return
        val info = imageInfo ?: return
        // Preflight the app can see immediately: recording/AI must be off. The
        // firmware enforces this (and battery/SD checks) again in Begin.
        val recordingOff =
            DeviceDriver.session.recordingState == RecordState.RecordOffDetectOff
        if (!recordingOff) {
            binding.stopRecordingButton.visibility = View.VISIBLE
            showModalAlert(
                getString(R.string.firmware_update),
                getString(R.string.firmware_stop_recording_first)
            )
            return
        }
        FirmwareUpdater.startUpdate(file, info.sha256, info.version, imageVariant)
        FirmwareUpdateService.start(this)
    }

    private fun stopRecording() {
        binding.stopRecordingButton.isEnabled = false
        DeviceDriver.setRecordState(RecordState.RecordOffDetectOff, null) {
            runOnUiThread {
                binding.stopRecordingButton.isEnabled = true
                binding.stopRecordingButton.visibility = View.GONE
            }
        }
    }

    private fun onUpdaterState(state: FirmwareUpdater.State, message: String) {
        runOnUiThread {
            binding.statusText.text = message
            val busy = FirmwareUpdater.isBusy
            binding.startButton.isEnabled = !busy && (imageInfo != null)
            binding.chooseFileButton.isEnabled = !busy
            binding.abortButton.visibility =
                if (state == FirmwareUpdater.State.Transferring) View.VISIBLE else View.GONE
            binding.progressBar.visibility = if (busy) View.VISIBLE else View.GONE
            binding.progressBar.isIndeterminate = state != FirmwareUpdater.State.Transferring

            val icon = when (state) {
                FirmwareUpdater.State.Success -> "✅ "
                FirmwareUpdater.State.RolledBack, FirmwareUpdater.State.Failed -> "⚠️ "
                else -> ""
            }
            if (icon.isNotEmpty()) {
                binding.statusText.text = icon + message
                binding.installedVersionValue.text = DeviceDriver.general.version
            }
        }
    }

    private fun onUpdaterProgress(sent: Long, total: Long, rate: Double) {
        runOnUiThread {
            if (total > 0) {
                val percent = ((sent * 100) / total).toInt()
                binding.progressBar.isIndeterminate = false
                binding.progressBar.max = 100
                binding.progressBar.progress = percent
                val remaining = if (rate > 1) {
                    val eta = ((total - sent) / rate).toInt()
                    getString(R.string.firmware_progress_eta, eta)
                } else {
                    ""
                }
                binding.statusText.text = getString(
                    R.string.firmware_progress_detail,
                    formatSize(sent),
                    formatSize(total),
                    (rate / 1024).toInt()
                ) + remaining
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        }
    }
}
